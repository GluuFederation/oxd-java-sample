package org.xdi.oxd.sample.listener;

import org.xdi.oxd.sample.bean.OxdService;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Created by jgomer on 2018-01-15.
 */
@WebListener
public class ContextListener implements ServletContextListener {

    @Inject
    private OxdService oxdService;

    public void contextDestroyed(ServletContextEvent sce){ }

    public void contextInitialized(ServletContextEvent sce){
        oxdService.setAppContextUrl(sce.getServletContext().getContextPath());
        oxdService.register();
    }

}
