package org.xdi.oxd.sample.bean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.ResponseStatus;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.common.response.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by jgomer on 2018-01-15.
 */
@Named
@ApplicationScoped
public class OxdService {

    @Inject
    private OxdConfig config;

    private CommandClient commandClient;
    private ResteasyClient rsClient;

    private String appContextUrl;

    private Logger logger = LogManager.getLogger(getClass());
    private ObjectMapper mapper = new ObjectMapper();

    public OxdService() {
    }

    public boolean register() {

        logger.info("Attempting registration with settings: {}", config.toString());
        try {
            boolean nulls = config.getPort()==0 ||
                    Stream.of(config.getOpHost(), config.getHost(), config.getAcrValues(), config.getScopes())
                    .anyMatch(obj -> obj==null || obj.length() == 0);

            if (nulls)
                logger.info("One or more required parameters are missing");
            else {
                String uri=OxdConfig.getServerRoot() + appContextUrl;
                config.setRedirectUri(uri + "/oidc/tokens.xhtml");
                config.setPostLogoutUri(uri + "/oidc/post-logout.xhtml");

                closeClients();
                if (config.isUseHttpsExtension())
                    doRegistrationHttps();
                else
                    doRegistrationSocket();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.warn("Registration was {} successful", config.getOxdId() == null ? "not" : "");

        return config.getOxdId() != null;

    }

    private void doRegistrationSocket() throws Exception {

        config.setClientName("sampleapp-client-" + System.currentTimeMillis());
        commandClient=new CommandClient(config.getHost(), config.getPort());

        RegisterSiteParams cmdParams = new RegisterSiteParams();
        cmdParams.setOpHost(config.getOpHost());
        cmdParams.setAuthorizationRedirectUri(config.getRedirectUri());
        cmdParams.setPostLogoutRedirectUri(config.getPostLogoutUri());
        cmdParams.setAcrValues(getListValues(config.getAcrValues()));
        cmdParams.setClientName(config.getClientName());

        //These scopes should be set to default=true in LDAP (or using oxTrust). Otherwise the following will have no effect
        cmdParams.setScope(getListValues(config.getScopes()));

        cmdParams.setResponseTypes(Collections.singletonList("code"));
        cmdParams.setTrustedClient(true);

        Command command = new Command(CommandType.REGISTER_SITE).setParamsObject(cmdParams);
        RegisterSiteResponse site = commandClient.send(command).dataAsResponse(RegisterSiteResponse.class);
        config.setOxdId(site.getOxdId());

    }

    private void doRegistrationHttps() throws Exception {

        config.setClientName("sampleapp-client-extension-" + System.currentTimeMillis());
        rsClient = new ResteasyClientBuilder().build();

        SetupClientParams cmdParams = new SetupClientParams();
        cmdParams.setOpHost(config.getOpHost());
        cmdParams.setAuthorizationRedirectUri(config.getRedirectUri());
        cmdParams.setPostLogoutRedirectUri(config.getPostLogoutUri());
        cmdParams.setAcrValues(getListValues(config.getAcrValues()));
        cmdParams.setClientName(config.getClientName());

        //These scopes should be set to default=true in LDAP (or using oxTrust). Otherwise the following will have no effect
        cmdParams.setScope(getListValues(config.getScopes()));

        cmdParams.setResponseTypes(Collections.singletonList("code"));
        cmdParams.setTrustedClient(true);

        SetupClientResponse setup = restResponse(cmdParams, "setup-client", null, SetupClientResponse.class);
        config.setOxdId(setup.getOxdId());
        config.setClientId(setup.getClientId());
        config.setClientSecret(setup.getClientSecret());

    }
    /**
     * Returns a string with an autorization URL to redirect an application (see OpenId connect "code" flow)
     * @return String consisting of an authentication request with desired parameters
     * @throws Exception
     */
    public String getAuthzUrl() throws Exception{

        GetAuthorizationUrlParams cmdParams = new GetAuthorizationUrlParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setAcrValues(getListValues(config.getAcrValues()));

        GetAuthorizationUrlResponse resp;
        if (config.isUseHttpsExtension())
            resp=restResponse(cmdParams, "get-authorization-url", getPAT(), GetAuthorizationUrlResponse.class);
        else {
            Command command = new Command(CommandType.GET_AUTHORIZATION_URL).setParamsObject(cmdParams);
            resp = commandClient.send(command).dataAsResponse(GetAuthorizationUrlResponse.class);
        }
        return resp.getAuthorizationUrl();

    }

    public GetTokensByCodeResponse getTokens(String code, String state) throws Exception {

        GetTokensByCodeParams cmdParams = new GetTokensByCodeParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setCode(code);
        cmdParams.setState(state);

        GetTokensByCodeResponse resp;
        if (config.isUseHttpsExtension())
            resp = restResponse(cmdParams, "get-tokens-by-code", getPAT(), GetTokensByCodeResponse.class);
        else {
            Command command = new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(cmdParams);
            resp = commandClient.send(command).dataAsResponse(GetTokensByCodeResponse.class);
        }

        return resp;

    }

    public GetUserInfoResponse getUserInfo(String accessToken) throws Exception{

        GetUserInfoParams cmdParams = new GetUserInfoParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setAccessToken(accessToken);

        GetUserInfoResponse resp;
        if (config.isUseHttpsExtension())
            resp = restResponse(cmdParams, "get-user-info", getPAT(), GetUserInfoResponse.class);
        else {
            Command command = new Command(CommandType.GET_USER_INFO).setParamsObject(cmdParams);
            resp = commandClient.send(command).dataAsResponse(GetUserInfoResponse.class);
        }
        return resp;

    }

    public String getLogoutUrl(String idTokenHint) throws Exception{

        GetLogoutUrlParams cmdParams = new GetLogoutUrlParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setPostLogoutRedirectUri(config.getPostLogoutUri());
        cmdParams.setIdTokenHint(idTokenHint);

        LogoutResponse resp;
        if (config.isUseHttpsExtension())
            resp = restResponse(cmdParams, "get-logout-uri", getPAT(), LogoutResponse.class);
        else {
            Command command = new Command(CommandType.GET_LOGOUT_URI).setParamsObject(cmdParams);
            resp = commandClient.send(command).dataAsResponse(LogoutResponse.class);
        }
        return resp.getUri();

    }

    private List<String> getListValues(String commaSeparatedStr) {
        return Arrays.asList(commaSeparatedStr.split(",\\s+"));
    }

    private void closeCommandClient(){
        if (commandClient!=null)
            CommandClient.closeQuietly(commandClient);
    }

    private void closeRSClient(){
        if (rsClient!=null)
            rsClient.close();
    }

    private <T> T restResponse(IParams params, String path, String token, Class <T> responseClass) throws Exception{

        String payload = mapper.writeValueAsString(params);
        logger.trace("Sending /{} request to oxd-https-extension with payload \n{}", path, payload);

        String authz = StringUtils.isEmpty(token) ? null : "Bearer " + token;
        ResteasyWebTarget target = rsClient.target(String.format("https://%s:%s/%s", config.getHost(), config.getPort(), path));
        Response response = target.request().header("Authorization", authz).post(Entity.json(payload));

        CommandResponse cmdResponse = response.readEntity(CommandResponse.class);
        logger.trace("Response received was \n{}", cmdResponse==null ? null : cmdResponse.getData().toString());

        if (cmdResponse.getStatus().equals(ResponseStatus.OK))
            return mapper.convertValue(cmdResponse.getData(), responseClass);
        else
            return null;

    }

    private String getPAT() throws Exception {

        GetClientTokenParams cmdParams = new GetClientTokenParams();
        cmdParams.setOpHost(config.getOpHost());
        cmdParams.setClientId(config.getClientId());
        cmdParams.setClientSecret(config.getClientSecret());
        cmdParams.setScope(getListValues(config.getScopes()));

        GetClientTokenResponse resp = restResponse(cmdParams, "get-client-token", null, GetClientTokenResponse.class);
        String token=resp.getAccessToken();
        logger.trace("getPAT. token={}", token);

        return token;

    }

    public void setAppContextUrl(String appContextUrl) {
        this.appContextUrl = appContextUrl;
    }

    @PreDestroy
    private void closeClients(){
        closeCommandClient();
        closeRSClient();
    }

}
