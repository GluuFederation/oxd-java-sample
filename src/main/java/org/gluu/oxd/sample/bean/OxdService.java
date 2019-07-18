/*
 * oxd-java-sample is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxd.sample.bean;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.GetTokensByCodeResponse2;
import org.gluu.oxd.client.OxdClient;
import org.gluu.oxd.common.params.*;
import org.gluu.oxd.common.response.*;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;
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
    private ClientInterface clientInterface;
    private ResteasyClient rsClient;

    private Logger logger = LogManager.getLogger(getClass());

    /**
     * Calls Site Registration operations by supplying
     * current configuration parameters. Actual calls are issued only if all necessary parameters are present.
     * @return True if operation was carried out successfully. False otherwise (failed operation or missing parameters)
     */
    public boolean register() {

        config.resetClient();
        logger.info("Attempting registration with settings: {}", config.toString());

        try {
            boolean nulls = config.getPort()==0 ||
                    Stream.of(config.getOpHost(), config.getHost(), config.getAcrValues(), config.getScopes(), config.getGrantTypes(), config.getRedirectUri(), config.getPostLogoutUri())
                    .anyMatch(obj -> obj==null || obj.length() == 0);

            if (nulls)
                logger.info("One or more required parameters are missing");
            else {
                ClientInterface clientInterface = OxdClient.newTrustAllClient(getTargetHost(config.getHost(), config.getPort()));
                doRegistration(clientInterface);
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
     * Calls Setup Client API operation.
     * @throws Exception When the operation failed to succeed
     */
    private void doRegistration(ClientInterface clientInterface) throws Exception {

        config.setClientName("sampleapp-client-extension-" + System.currentTimeMillis());

        final RegisterSiteParams params = new RegisterSiteParams();
        params.setOpHost(config.getOpHost());
        params.setPostLogoutRedirectUris(Lists.newArrayList(config.getPostLogoutUri().split(" ")));
        params.setRedirectUris(Lists.newArrayList(config.getRedirectUri().split(" ")));
        params.setScope(Lists.newArrayList(config.getScopes().split(" ")));
        params.setTrustedClient(true);
        params.setGrantTypes(Lists.newArrayList(config.getGrantTypes().split(" ")));
        params.setClientName(config.getClientName());
        //params.setResponseTypes(Lists.newArrayList("code"));
        params.setAcrValues(Lists.newArrayList(config.getAcrValues().split(" ")));

        final RegisterSiteResponse resp = clientInterface.registerSite(params);

        config.setOxdId(resp.getOxdId());
        config.setClientId(resp.getClientId());
        config.setClientSecret(resp.getClientSecret());
    }

    /**
     * Calls the Get Authorization URL API operation.
     * @return String URL consisting of an authentication request with desired parameters
     * @throws Exception When the operation failed to succeed
     */
    public String getAuthzUrl() throws Exception {

        ClientInterface clientInterface = OxdClient.newTrustAllClient(getTargetHost(config.getHost(), config.getPort()));

        GetAuthorizationUrlParams cmdParams = new GetAuthorizationUrlParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setAcrValues(Lists.newArrayList(config.getAcrValues().split(" ")));
        cmdParams.setScope(Lists.newArrayList(config.getScopes().split(" ")));

        final GetAuthorizationUrlResponse resp = clientInterface.getAuthorizationUrl(getClientToken(clientInterface), cmdParams);

        return resp.getAuthorizationUrl();

    }

    /**
     * Calls the Get Tokens by Code API operation.
     * @param code Parameter code
     * @param state Parameter state
     * @return A {@link GetTokensByCodeResponse org.gluu.oxd.common.response.GetTokensByCodeResponse2} object
     * @throws Exception When the operation failed to succeed
     */
    public GetTokensByCodeResponse2 getTokens(String code, String state) throws Exception {

        ClientInterface clientInterface = OxdClient.newTrustAllClient(getTargetHost(config.getHost(), config.getPort()));

        GetTokensByCodeParams cmdParams = new GetTokensByCodeParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setCode(code);
        cmdParams.setState(state);

        GetTokensByCodeResponse2 resp = clientInterface.getTokenByCode(getClientToken(clientInterface), cmdParams);
        return resp;
    }

    /**
     * Calls the Get User Info API operation.
     * @param accessToken Parameter access_token
     * @return A {@link JsonNode com.fasterxml.jackson.databind.JsonNode} object
     * @throws Exception When the operation failed to succeed
     */
    public JsonNode getUserInfo(String accessToken) throws Exception {

        ClientInterface clientInterface = OxdClient.newTrustAllClient(getTargetHost(config.getHost(), config.getPort()));

        GetUserInfoParams cmdParams = new GetUserInfoParams();
        cmdParams.setOxdId(config.getOxdId());
        cmdParams.setAccessToken(accessToken);
        JsonNode resp = clientInterface.getUserInfo(getClientToken(clientInterface), cmdParams);

        return resp;

    }

    /**
     * Calls the Get Logout URI API operation.
     * @return A String representing the URL to redirect the user to (in order to log out of the OP)
     * @throws Exception When the operation failed to succeed
     */
    public String getLogoutUrl(String idTokenHint) throws Exception {

        ClientInterface clientInterface = OxdClient.newTrustAllClient(getTargetHost(config.getHost(), config.getPort()));

        final GetLogoutUrlParams params = new GetLogoutUrlParams();
        params.setOxdId(config.getOxdId());
        params.setIdTokenHint("dummy_token");
        params.setPostLogoutRedirectUri(config.getPostLogoutUri());
        params.setState(UUID.randomUUID().toString());
        params.setSessionState(UUID.randomUUID().toString()); // here must be real session instead of dummy UUID

        final GetLogoutUriResponse resp = clientInterface.getLogoutUri(getClientToken(clientInterface), params);

        return resp.getUri();

    }

    private String getClientToken(ClientInterface clientInterface) {
        if (StringUtils.isNotBlank(config.getOxdId())) {
            final GetClientTokenParams params = new GetClientTokenParams();
            params.setOpHost(config.getOpHost());
            params.setScope(Lists.newArrayList(config.getScopes().split(" ")));
            params.setClientId(config.getClientId());
            params.setClientSecret(config.getClientSecret());

            GetClientTokenResponse resp = clientInterface.getClientToken(params);

            return "Bearer " + resp.getAccessToken();
        }
        return null;
    }

    private static String getTargetHost(String host, int port) {
        return "https://" + host + ":" + port;
    }

}
