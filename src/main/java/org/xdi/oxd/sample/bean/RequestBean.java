package org.xdi.oxd.sample.bean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by jgomer on 2018-01-18.
 */
@Named
@RequestScoped
public class RequestBean {

    @Inject
    private HttpServletRequest req;

    @Inject
    private SessionUtil sessionUtil;

    @Inject
    private OxdService oxdService;

    private Logger logger = LogManager.getLogger(getClass());

    public String getCode(){

        String code=req.getParameter("code");
        sessionUtil.setCode(code);
        return code;
    }

    public String getState(){

        String state=req.getParameter("state");
        sessionUtil.setState(state);
        return state;

    }

}
