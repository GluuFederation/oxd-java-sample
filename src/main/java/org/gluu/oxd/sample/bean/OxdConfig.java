/*
 * oxd-java-sample is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxd.sample.bean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * A bean employed to store configuration parameters related to oxd:
 * <ul>
 * <li>oxd-server (or oxd-https-extension) location</li>
 * <li>Settings required to execute oxd-java API operations</li>
 * <li>Output of the latest Site Registration attempt</li>
 * </ul>
 *
 * @author jgomer
 */
@Named
@ApplicationScoped
public class OxdConfig {

    static final String URL_PREFIX = "/oidc";

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String TMP_FILE_NAME = "oxd-java-sample.conf";

    private ObjectMapper mapper;
    private Logger logger = LogManager.getLogger(getClass());

    @Inject
    private ServletContext context;

    //Class fields used to "point" to oxd-server
    private String host;
    private int port;

    //Fields passed to execute API operation
    private String opHost;
    private String redirectUri;
    private String postLogoutUri;
    private String grantTypes;
    private String acrValues;
    private String scopes;

    //Field utilized to store the result of site registration or setup client step
    private String oxdId;
    private String clientId;
    private String clientSecret;
    private String clientName;
    private boolean trustAllClient;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("opHost=").append(opHost).append(", ")
                .append("host=").append(host).append(", ")
                .append("port=").append(port).append(", ");
        return sb.insert(0, "[").append("]").toString();
    }

    /**
     * Nullifies fields related to the openID client created when executing the Site Registration/Setup Client operation
     */
    void resetClient() {
        oxdId = null;
        clientId = null;
        clientSecret = null;
        clientName = null;
    }

    /**
     * Stores key parameters to disk. This allows the app to successfully interact with oxd server avoiding the user to
     * enter the configs manually upon every restart.
     * Files is saved to operating system temporary directory.
     */
    void store() {

        try {
            Map<String, Object> map = (Map<String, Object>) mapper.convertValue(this, new TypeReference<Object>() {
            });
            Collection<String> params = Arrays.asList("opHost", "host", "port", "acrValues", "grantTypes", "scopes");
            map.keySet().retainAll(params);

            Properties props = new Properties();
            params.forEach(prop -> {
                if (map.get(prop) != null)
                    props.setProperty(prop, map.get(prop).toString());
            });

            Path path = Paths.get(TMP_DIR, TMP_FILE_NAME);
            logger.info("Saving oxd settings to {}", path.toString());
            props.store(Files.newOutputStream(path), "oxd-sample-java");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * A method called once upon application start. It tries to read  parameters from disk or from Java system properties.
     * <p>If not found, the default values of a typical oxd installation are assumed.</p>
     * <p>See the accompanying README file for more info about system properties and default values</p>
     */
    @PostConstruct
    private void init() {

        mapper = new ObjectMapper();
        boolean fileParsed = false;
        Path path = Paths.get(TMP_DIR, TMP_FILE_NAME);

        trustAllClient = System.getProperty("trust.all.client") != null ? Boolean.valueOf(System.getProperty("trust.all.client"))  : false;

        if (Files.exists(path) && System.getProperty("oxd.sample.skip-conf-file") == null) {
            Properties props = new Properties();
            try {
                props.load(Files.newInputStream(path));
                BeanUtils.populate(this, props);
                fileParsed = true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (!fileParsed) {
            opHost = System.getProperty("oxd.server.op-host", null);
            host = System.getProperty("oxd.server.host", "localhost");
            String _port = System.getProperty("oxd.server.port");
            try {
                port = Integer.parseInt(_port);
            } catch (Exception e) {
                port = 8443;
                logger.warn("Defaulting oxd port to {}", port);
            }
            //useHttpsExtension=System.getProperty("oxd.server.is-https")!=null;
            acrValues = System.getProperty("oxd.server.acr-values", "auth_ldap_server");
            scopes = System.getProperty("oxd.server.scopes", "openid uma_protection oxd");
            grantTypes = System.getProperty("oxd.server.grant-types", "authorization_code client_credentials");
        }

        String uri = getServerRoot() + context.getContextPath();
        setRedirectUri(uri + URL_PREFIX + "/tokens.xhtml");
        setPostLogoutUri(uri + URL_PREFIX + "/post-logout.xhtml");

    }

    public static String getServerRoot() {

        StringBuilder uri = new StringBuilder();
        uri.append("https://").append(System.getProperty("oxd.sample.host", "localhost"));

        String serverPort = System.getProperty("oxd.sample.port", "8463");

        if (!serverPort.equals("443"))
            uri.append(":").append(serverPort);

        return uri.toString();

    }

    public String getOpHost() {
        return opHost;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getOxdId() {
        return oxdId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientName() {
        return clientName;
    }

    public String getPostLogoutUri() {
        return postLogoutUri;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getGrantTypes() {
        return grantTypes;
    }

    public String getScopes() {
        return scopes;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setPostLogoutUri(String postLogoutUri) {
        this.postLogoutUri = postLogoutUri;
    }

    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public boolean isTrustAllClient() {
        return trustAllClient;
    }

    public void setTrustAllClient(boolean trustAllClient) {
        this.trustAllClient = trustAllClient;
    }
}
