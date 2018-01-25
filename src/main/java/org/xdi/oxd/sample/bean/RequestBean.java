package org.xdi.oxd.sample.bean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * A simple class merely used to back one facelet (jsf page) that processes some request parameters.
 * @author jgomer
 */
@Named
@RequestScoped
public class RequestBean {

    @Inject
    private HttpServletRequest req;

    @Inject
    private FlowManager flowManager;

    /**
     * Reads the <code>code</code> query parameter and updates the {@link FlowManager} manager instance with this value.
     * @return Query paramater value
     */
    public String getCode(){

        String code=req.getParameter("code");
        flowManager.setCode(code);
        return code;
    }

    /**
     * Reads the <code>state</code> query parameter and updates the {@link FlowManager} manager instance with this value.
     * @return Query paramater value
     */
    public String getState(){

        String state=req.getParameter("state");
        flowManager.setState(state);
        return state;

    }

}
