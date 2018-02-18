package org.imsglobal.lti.launch;

import com.mastfrog.giulius.tests.GuiceRunner;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import static org.junit.Assert.*;
import java.net.URI;
import javax.inject.Inject;
import org.apache.http.Header;
import org.junit.runner.RunWith;


/**
 * @author  Paul Gray
 */
@RunWith(GuiceRunner.class)
public class LtiVerifierAndSignerTest {

    @Inject
    LtiVerifier verifier;

    LtiSigner signer = new LtiOauthSigner();

    @Test
    public void verifierShouldVerifyCorrectlySignedLtiLaunches() throws Exception {

        String key = "key";
        String secret = "secret";
        HttpPost ltiLaunch = new HttpPost(new URI("http://example.com/test"));

        signer.sign(ltiLaunch, key, secret);
        MockHttpPost post = new MockHttpPost(ltiLaunch);
        for (Header h : ltiLaunch.getAllHeaders()) {
            assertEquals("Mismatch on " + h.getName(), h.getValue(), post.header(h.getName()));
        }
        LtiVerificationResult result = verifier.verify(new MockHttpPost(ltiLaunch), secret);
        System.out.println("RESULT IS " + result);
        System.out.println("RESULT ERROR IS " + result.getError());
        assertTrue(result.getError() + "", result.getSuccess());
    }

    @Test
    public void verifierShouldRejectIncorrectlySignedLtiLaunches() throws Exception {

        String key = "key";
        String secret = "secret";
        HttpPost ltiLaunch = new HttpPost(new URI("http://example.com/test"));

        signer.sign(ltiLaunch, key, secret);
        LtiVerificationResult result = verifier.verify(new MockHttpPost(ltiLaunch), "wrongSecret");

        assertFalse(result.getSuccess());
    }

    @Test
    public void verifierShouldVerifyCorrectlySignedLtiGetServiceRequests() throws Exception {

        String key = "key";
        String secret = "secret";
        HttpGet ltiServiceGetRequest = new HttpGet(new URI("http://example.com/test"));

        signer.sign(ltiServiceGetRequest, key, secret);
        LtiVerificationResult result = verifier.verify(new MockHttpGet(ltiServiceGetRequest), secret);

        assertTrue(result.getError() + "", result.getSuccess());
    }

    @Test
    public void verifierShouldRejectIncorrectlySignedLtiGetServiceRequests() throws Exception {

        String key = "key";
        String secret = "secret";
        HttpGet ltiServiceGetRequest = new HttpGet(new URI("http://example.com/test"));

        signer.sign(ltiServiceGetRequest, key, secret);
        LtiVerificationResult result = verifier.verify(new MockHttpGet(ltiServiceGetRequest), "anotherWrongSecret");

        assertFalse(result.getSuccess());
    }

}
