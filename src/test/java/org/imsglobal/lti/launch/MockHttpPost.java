package org.imsglobal.lti.launch;

import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.util.HttpMethod;
import org.apache.http.client.methods.HttpPost;

/**
 * @author Paul Gray
 */
public class MockHttpPost extends BaseMockHttpServletRequest {

    private HttpPost post;

    public MockHttpPost(HttpPost req) throws Exception {
        super(req);
        this.post = req;
    }

    @Override
    public HttpMethod method() {
        return Method.POST;
    }

}
