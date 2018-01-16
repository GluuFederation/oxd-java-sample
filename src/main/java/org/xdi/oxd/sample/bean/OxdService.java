package org.xdi.oxd.sample.bean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.xdi.oxd.client.CommandClient;

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

    private Logger logger = LogManager.getLogger(getClass());

    public OxdService(){ }

    public boolean register(){

        logger.info("Attempting registration with settings: {}", config.toString());

        try {
            logger.debug("ophost {}", config.getOpHost());
        }
        catch (Exception e){
            logger.error("An error occurred: {}", e.getMessage());
        }
        logger.warn("Registration was {} successful", config.getOxdId()==null ? "not" : "");
        return config.getOxdId()!=null;

    }

    private void doRegistration(){

    }

    private void doRegistrationHttps(){

    }

    private void closeCommandClient(){
        if (commandClient!=null)
            CommandClient.closeQuietly(commandClient);
    }

    private void closeRSClient(){
        if (rsClient!=null)
            rsClient.close();
    }

    @PreDestroy
    private void destroy(){
        closeCommandClient();
        closeRSClient();
    }

}
