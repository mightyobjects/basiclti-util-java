package org.imsglobal.lti.launch;

import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.util.HttpMethod;
import org.apache.http.client.methods.HttpGet;

/**
 * @author  Paul Gray
 */
public class MockHttpGet extends BaseMockHttpServletRequest {

    private HttpGet get;

    public MockHttpGet(HttpGet req) throws Exception {
        super(req);
        System.out.println("MOCK GET WITH " + req.getURI());
        this.get = req;
    }

    public MockHttpGet(String uri) throws Exception {
        this(new HttpGet(uri));
    }

    @Override
    public HttpMethod method() {
        return Method.GET;
    }

}
