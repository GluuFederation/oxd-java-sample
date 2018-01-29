/*
 * oxd-java-sample is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.xdi.oxd.sample.bean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
 * A managed bean that performs all the interactions with oxd-server (executes the oxd-java API calls).
 * @author jgomer
 */
@Named
@ApplicationScoped
public class OxdService {

    @Inject
    private OxdConfig config;

    //Network clients (don't confuse with openID clients)
    private CommandClient commandClient;
    private ResteasyClient rsClient;

    private Logger logger = LogManager.getLogger(getClass());
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Calls Site Registration (for socket-based) or Setup Client (for https-based oxd connection) operations by supplying
     * current configuration parameters. Actual calls are issued only if all necessary parameters are present.
     * @return True if operation was carried out successfully. False otherwise (failed operation or missing parameters)
     */
    public boolean register() {

        config.resetClient();
        logger.info("Attempting registration with settings: {}", config.toString());
        try {
            boolean nulls = config.getPort()==0 ||
                    Stream.of(config.getOpHost(), config.getHost(), config.getAcrValues(), config.getScopes())
                    .anyMatch(obj -> obj==null || obj.length() == 0);

            if (nulls)
                logger.info("One or more required parameters are missing");
            else {
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
        if (config.getOxdId() == null){
            logger.warn("Registration failed");
            return false;
        }
        else{
            //Make settings persistent
            config.store();

            logger.warn("Registration successful");
            return true;
        }

    }

    /**
     * Calls Site Registration API operation.
     * @throws Exception When the operation failed to succeed
     */
    private void doRegistrationSocket() throws Exception {

        config.setClientName("sampleapp-client-" + System.currentTimeMillis());
        commandClient=new CommandClient(config.getHost(), config.getPort());

        RegisterSiteParams cmdParams = new RegisterSiteParams();
        cmdParams.setOpHost(config.getOpHost());
        cmdParams.setAuthorizationRedirectUri(config.getRedirectUri());
        cmdParams.setPostLogoutRedirectUri(config.getPostLogoutUri());
        cmdParams.setAcrValues(getListValues(config.getAcrValues()));
        cmdParams.setClientName(config.getClientName());

        //For Gluu Server, these scopes should be set to default=true in LDAP (or using oxTrust).
        cmdParams.setScope(getListValues(config.getScopes()));

        cmdParams.setResponseTypes(Collections.singletonList("code"));
        cmdParams.setTrustedClient(true);

        Command command = new Command(CommandType.REGISTER_SITE).setParamsObject(cmdParams);
        RegisterSiteResponse site = commandClient.send(command).dataAsResponse(RegisterSiteResponse.class);
        config.setOxdId(site.getOxdId());

    }

    /**
     * Calls Setup Client API operation.
     * @throws Exception When the operation failed to succeed
     */
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
     * Calls the Get Authorization URL API operation.
     * @return String URL consisting of an authentication request with desired parameters
     * @throws Exception When the operation failed to succeed
     */
    public String getAuthzUrl() throws Exception {

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

    /**
     * Calls the Get Tokens by Code API operation.
     * @param code Parameter code
     * @param state Parameter state
     * @return A {@link GetTokensByCodeResponse org.xdi.oxd.common.response.GetTokensByCodeResponse} object
     * @throws Exception When the operation failed to succeed
     */
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

    /**
     * Calls the Get User Info API operation.
     * @param accessToken Parameter access_token
     * @return A {@link GetUserInfoResponse org.xdi.oxd.common.response.GetUserInfoResponse} object
     * @throws Exception When the operation failed to succeed
     */
    public GetUserInfoResponse getUserInfo(String accessToken) throws Exception {

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

    /**
     * Calls the Get Logout URI API operation.
     * @param idTokenHint Parameter id_token_hint
     * @return A String representing the URL to redirect the user to (in order to log out of the OP)
     * @throws Exception When the operation failed to succeed
     */
    public String getLogoutUrl(String idTokenHint) throws Exception {

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
        return Arrays.asList(commaSeparatedStr.split(",\\s*"));
    }

    private void closeCommandClient(){
        if (commandClient!=null)
            CommandClient.closeQuietly(commandClient);
    }

    private void closeRSClient(){
        if (rsClient!=null)
            rsClient.close();
    }

    /**
     * Issues REST requests to oxd-https-extension
     * @param params An object that represents the payload to use
     * @param path The relative path (with respect to oxd-https-extension root URL) that represents the operation endpoint
     * @param token An access token to perform the request (normally coming out of a call to Get Client Token operation)
     * @param responseClass A Class to which the object being return belongs to
     * @param <T> Type parameter for class
     * @return The response received after being parsed using type T (null if the HTTP response code received was not 200)
     * @throws Exception Anomaly when issuing the request or passing the response
     */
    private <T> T restResponse(IParams params, String path, String token, Class <T> responseClass) throws Exception {

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

    /**
     * Calls the Get Client Token operation
     * @return A String with an access token (usually employed to protect other API calls when using https extension)
     * @throws Exception When the operation failed to succeed
     */
    private String getPAT() throws Exception {

        GetClientTokenParams cmdParams = new GetClientTokenParams();
        cmdParams.setOpHost(config.getOpHost());
        cmdParams.setClientId(config.getClientId());
        cmdParams.setClientSecret(config.getClientSecret());
        cmdParams.setScope(getListValues(config.getScopes()));

        GetClientTokenResponse resp = restResponse(cmdParams, "get-client-token", null, GetClientTokenResponse.class);
        String token=resp.getAccessToken();
        logger.trace("getPAT. token: {}", token);

        return token;

    }

    @PreDestroy
    private void closeClients(){
        closeCommandClient();
        closeRSClient();
    }

}
