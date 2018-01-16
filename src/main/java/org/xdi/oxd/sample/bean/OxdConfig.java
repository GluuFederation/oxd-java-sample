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

    private Logger logger = LogManager.getLogger(getClass());

    public OxdConfig(){

        opHost=System.getProperty("oxd.server.op-host");

        String _host=System.getProperty("oxd.server.host");
        host=_host==null ? "localhost" : _host;

        String _port=System.getProperty("oxd.server.port");
        try{
            port=Integer.parseInt(_port);
        }
        catch (Exception e){
            port=8098;
            logger.error("An error occurred: {}", e.getMessage());
            logger.warn("Defaulting oxd port to {}", port);
        }

        useHttpsExtension=System.getProperty("oxd.server.isExtension")!=null;

    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isUseHttpsExtension() {
        return useHttpsExtension;
    }

    public void setUseHttpsExtension(boolean useHttpsExtension) {
        this.useHttpsExtension = useHttpsExtension;
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

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getPostLogoutUri() {
        return postLogoutUri;
    }

    public void setPostLogoutUri(String postLogoutUri) {
        this.postLogoutUri = postLogoutUri;
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
