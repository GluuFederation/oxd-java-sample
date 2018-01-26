/*
 * oxd-java-sample is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.xdi.oxd.sample.listener;

import org.xdi.oxd.sample.bean.OxdService;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * A Servlet context listener that contains methods meant to be invoked upong application startup and shutdown.
 * @author jgomer
 */
@WebListener
public class ContextListener implements ServletContextListener {

    @Inject
    private OxdService oxdService;

    public void contextDestroyed(ServletContextEvent sce){ }

    public void contextInitialized(ServletContextEvent sce){
        //Execute a "Site registration" or "Setup client" operation with data available
        oxdService.register();
    }

}
