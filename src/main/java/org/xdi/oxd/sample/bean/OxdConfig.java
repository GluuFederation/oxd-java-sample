package org.xdi.oxd.sample.bean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by jgomer on 2018-01-15.
 */
@Named
@ApplicationScoped
public class OxdConfig {

    private String opHost;
    private String host;
    private int port;
    private boolean useHttpsExtension;

    private String oxdId;
    private String clientId;
    private String clientSecret;
    private String clientName;

    private String redirectUri;
    private String postLogoutUri;
    private String acrValues;
    private String scopes;

    private Logger logger = LogManager.getLogger(getClass());

    public OxdConfig(){

        opHost=System.getProperty("oxd.server.op-host", null);
        host=System.getProperty("oxd.server.host", "localhost");

        String _port=System.getProperty("oxd.server.port");
        try{
            port=Integer.parseInt(_port);
        }
        catch (Exception e){
            port=8098;
            logger.error(e.getMessage(), e);
            logger.warn("Defaulting oxd port to {}", port);
        }

        useHttpsExtension=System.getProperty("oxd.server.isExtension")!=null;
        acrValues=System.getProperty("oxd.server.acr-values", "auth_ldap_server");
        scopes=System.getProperty("oxd.server.scopes", "openid, uma_protection");

    }

    static String getServerRoot(){

        StringBuilder uri=new StringBuilder();
        uri.append("https://").append(System.getProperty("oxd.sample.host","localhost"));

        String serverPort=System.getProperty("oxd.sample.port","8463");

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

    public boolean isUseHttpsExtension() {
        return useHttpsExtension;
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

    public void setUseHttpsExtension(boolean useHttpsExtension) {
        this.useHttpsExtension = useHttpsExtension;
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

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append("opHost=").append(opHost).append(", ")
                .append("host=").append(host).append(", ")
                .append("port=").append(port).append(", ")
                .append("https-extension=").append(useHttpsExtension);
        return sb.insert(0, "[").append("]").toString();
    }

}
