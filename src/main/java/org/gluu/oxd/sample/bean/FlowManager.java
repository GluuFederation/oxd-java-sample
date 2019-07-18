/*
 * oxd-java-sample is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxd.sample.bean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gluu.oxd.client.GetTokensByCodeResponse2;

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
import java.util.stream.Stream;

/**
 * A class employed to maintain the state of the sample Authentication workflow showcased by this application. It helps
 * modeling the steps as well as do page navigation.
 * @author jgomer
 */
@Named("flow")
@SessionScoped
public class FlowManager implements Serializable {

    @Inject
    private OxdService oxdService;

    @Inject
    private HttpServletRequest req;

    /**
     * An enumeration that represents the possible stages in the sample workflow. See app's page <code>/code_flow.xhtml</code>.
     */
    public enum Stage{
        PRE_AUTHZ_URL (1, "Authentication Request preparation", "pre_authz.xhtml"),
        TOKEN_REQUEST (4, "Token request to Authorization Server", "tokens.xhtml"),
        TOKEN_RESPONSE (5, "ID Token and Access Token received", "tokens.xhtml"),
        USER_INFO (6, "Obtain user claims", "user.xhtml"),
        LOGOUT_OP (7, "Logout", "logout.xhtml");

        private int step;
        private String summary;
        private String url;

        Stage(int step, String summary, String url){
            this.step=step;
            this.summary=summary;
            this.url=url;
        }

        public int getStep() {
            return step;
        }

        public String getSummary() {
            return summary;
        }

        public String getUrl() {
            return url;
        }

    }

    private ObjectMapper mapper;
    private Logger logger = LogManager.getLogger(getClass());
    private Stage stage;

    //These class fields hold data to be displayed in the UI.  Values are grabbed by oxdService being when the workflow is running
    private String authzUrl;
    private String code;
    private String state;
    private String tokensReponseAsJson;
    private String accessToken;
    private String idToken;
    private String idTokenAsJson;
    private String userInfoReponseAsJson;
    private String logoutUrl;

    /**
     * Obtains an authorization URL and updates internal object state
     * @return The String URL that was retrieved
     * @throws Exception If an error was presented when retrieving the information from oxd.
     */
    public String getAuthorizationUrl() throws Exception{
        authzUrl=oxdService.getAuthzUrl();
        return authzUrl;
    }

    /**
     * Starts the second step of the flow ("Client sends the request to the Authorization Server") by redirecting to the
     * URL previously retrieved by {@link #getAuthorizationUrl()}
     * @throws IOException If the redirection could not be carried out
     */
    public void goAuthenticate() throws IOException {
        redirectExternal(authzUrl);
        stage= Stage.TOKEN_REQUEST;
    }

    /**
     * Updates the values of the fields that will be shown in the page of step "ID Token and Access Token received"
     */
    public void retrieveTokens() {

        try {
            GetTokensByCodeResponse2 response = oxdService.getTokens(code, state);
            idTokenAsJson = mapper.writeValueAsString(response.getIdTokenClaims());
            response.setIdTokenClaims(null);
            tokensReponseAsJson=mapper.writeValueAsString(response);

            accessToken = response.getAccessToken();
            idToken = response.getIdToken();
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        stage= Stage.TOKEN_RESPONSE;

    }

    /**
     * Updates the values of the fields that will be shown in the page of step "Obtain user claims" and redirects to such page
     * @throws IOException If there was an error redirecting
     */
    public void retrieveUserInfo() throws IOException{

        try {
            JsonNode response = oxdService.getUserInfo(accessToken);
            userInfoReponseAsJson=mapper.writeValueAsString(response);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        stage= Stage.USER_INFO;
        redirect();

    }

    /**
     * Redirects to the page that shows "Logout" info after obtaining a logout url
     * @throws Exception If there was an error redirecting
     */
    public void logout() throws Exception{

        try{
            logoutUrl=oxdService.getLogoutUrl(idToken);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        stage= Stage.LOGOUT_OP;
        redirect();

    }

    /**
     * Triggers the actual log out step by redirecting to the logout URL
     * @throws Exception If there was an error redirecting
     */
    public void goLogout() throws Exception{
        redirectExternal(logoutUrl);
        resetFields();
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

    /**
     * Determines if the current stage of the flow is contained in the list of stages passed as parameter
     * @param strStage A comma-separated list of stage names
     * @return True if current stage is part of list. False otherwise
     */
    public boolean isCurrent(String strStage){
        return Stream.of(strStage.split(".\\s+")).anyMatch(str -> stage.toString().equals(str));
    }

    private void redirectExternal(String url) throws IOException{

        ExternalContext externalContext=FacesContext.getCurrentInstance().getExternalContext();
        externalContext.redirect(url);
    }

    private void redirect() throws IOException{

        FacesContext facesContext=FacesContext.getCurrentInstance();
        String url= OxdConfig.URL_PREFIX + "/" + stage.getUrl();
        url=facesContext.getApplication().getViewHandler().getRedirectURL(facesContext, url, Collections.emptyMap(), false);
        redirectExternal(url);

    }

    @PostConstruct
    private void init(){

        mapper=new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);

        resetFields();

    }

    private void resetFields(){

        stage= Stage.PRE_AUTHZ_URL;
        authzUrl=null;
        code=null;
        state=null;
        tokensReponseAsJson=null;
        accessToken=null;
        idToken=null;
        idTokenAsJson=null;
        userInfoReponseAsJson=null;
        logoutUrl=null;
    }

}
