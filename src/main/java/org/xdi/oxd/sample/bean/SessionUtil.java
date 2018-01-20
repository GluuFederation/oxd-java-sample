package org.xdi.oxd.sample.bean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.GetUserInfoResponse;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

/**
 * Created by jgomer on 2018-01-16.
 */
@Named
@SessionScoped
public class SessionUtil implements Serializable {

    @Inject
    private OxdService oxdService;

    @Inject
    private HttpServletRequest req;

    public enum Stage{
        PRE_AUTHZ_URL (1, "Authentication Request preparation"),
        TOKEN_REQUEST (4, "Token request to Authorization Server"),
        TOKEN_RESPONSE (5, "ID Token and Access Token received"),
        USER_INFO (6, "Obtain user claims"),
        LOGOUT_OP (7, "Logout");

        private int step;
        private String summary;

        Stage(int step, String summary){
            this.step=step;
            this.summary=summary;
        }

        public int getStep() {
            return step;
        }

        public String getSummary() {
            return summary;
        }

    }

    private ObjectMapper mapper;
    private Stage stage;
    private boolean loggedIn;
    private Logger logger = LogManager.getLogger(getClass());

    private String authzUrl;
    private String code;
    private String state;
    private String tokensReponseAsJson;
    private String accessToken;
    private String idToken;
    private String idTokenAsJson;
    private String userInfoReponseAsJson;
    private String logoutUrl;

    public String getAuthorizationUrl() throws Exception{
        authzUrl=oxdService.getAuthzUrl();
        return authzUrl;
    }

    public void goAuthenticate() throws IOException {
        redirectExternal(authzUrl);
        stage=Stage.TOKEN_REQUEST;
    }

    public void getTokensResponse() {

        try {
            GetTokensByCodeResponse response = oxdService.getTokens(code, state);
            idTokenAsJson = mapper.writeValueAsString(response.getIdTokenClaims());
            response.setIdTokenClaims(null);
            tokensReponseAsJson=mapper.writeValueAsString(response);

            accessToken = response.getAccessToken();
            idToken = response.getIdToken();
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        stage=Stage.TOKEN_RESPONSE;

    }

    public void getUserInfo(String url) throws IOException{

        try {
            GetUserInfoResponse response = oxdService.getUserInfo(accessToken);
            userInfoReponseAsJson=mapper.writeValueAsString(response.getClaims());
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        redirect(url);
        stage=Stage.USER_INFO;

    }

    public void logout(String url) throws Exception{

        try{
            logoutUrl=oxdService.getLogoutUrl(idToken);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        redirect(url);
        stage=Stage.LOGOUT_OP;

    }

    public void goLogout() throws Exception{
        redirectExternal(logoutUrl);
        stage=Stage.PRE_AUTHZ_URL;
    }

    public Stage getStage() {
        return stage;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokensReponseAsJson() {
        return tokensReponseAsJson;
    }

    public String getIdTokenAsJson() {
        return idTokenAsJson;
    }

    public String getUserInfoReponseAsJson() {
        return userInfoReponseAsJson;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isCurrent(String strStage){
        return stage.toString().equals(strStage);
    }

    public boolean isTokenStep(){
        return stage.equals(Stage.TOKEN_REQUEST) || stage.equals(Stage.TOKEN_RESPONSE);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    private void redirectExternal(String url) throws IOException{

        ExternalContext externalContext=FacesContext.getCurrentInstance().getExternalContext();
        externalContext.redirect(url);
    }

    private void redirect(String url) throws IOException{

        FacesContext facesContext=FacesContext.getCurrentInstance();
        url=facesContext.getApplication().getViewHandler().getRedirectURL(facesContext, url, Collections.emptyMap(), false);
        redirectExternal(url);

    }

    @PostConstruct
    public void init(){

        mapper=new ObjectMapper();
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        mapper.disable(SerializationConfig.Feature.WRITE_NULL_PROPERTIES);

        stage=Stage.PRE_AUTHZ_URL;
    }

}
