package org.imsglobal.lti;

import com.mastfrog.acteur.HttpEvent;
import static com.mastfrog.url.Protocols.HTTPS;
import com.mastfrog.url.URL;
import java.io.IOException;
import java.net.URISyntaxException;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.SimpleOAuthValidator;
import net.oauth.signature.OAuthSignatureMethod;
import org.imsglobal.lti.launch.LtiError;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.MockHttpGet;
import org.junit.Assert;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OAuthSignatureMethod.class, SimpleOAuthValidator.class, BasicLTIUtil.class, HttpEvent.class})
public class BasicLTIUtilTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetRealPath() {
        String fixed = BasicLTIUtil.getRealPath("http://localhost/path/blah/", "https://right.com");
        assertEquals("https://right.com/path/blah/", fixed);
        fixed = BasicLTIUtil.getRealPath("https://localhost/path/blah/", "https://right.com");
        assertEquals("https://right.com/path/blah/", fixed);
        fixed = BasicLTIUtil.getRealPath("https://localhost/path/blah/", "http://right.com");
        assertEquals("http://right.com/path/blah/", fixed);

        // Test folks sending in URL with extra stuff...
        fixed = BasicLTIUtil.getRealPath("https://localhost/path/blah/", "https://right.com/path/blah");
        assertEquals("https://right.com/path/blah/", fixed);
    }
    
    @Test
    public void testValidateMessageFailsWhenNoConsumerKey() throws IOException, Exception{
        
        String url = "https://example.com/lti-launch";
        HttpEvent requestMock = new MockHttpGet(url);
        
        OAuthMessage messageMock = Mockito.mock(OAuthMessage.class);
        
//        PowerMockito.when(getMessage(requestMock, url)).thenReturn(messageMock);

        Mockito.when(messageMock.getConsumerKey()).thenThrow(new IOException("io exception"));

        LtiVerificationResult result = BasicLTIUtil.validateMessage(requestMock, url, "secret");

        Assert.assertEquals(LtiError.BAD_REQUEST, result.getError());
        Assert.assertEquals(Boolean.FALSE, result.getSuccess());
        
    }
    
    
    @Test
    public void testValidateMessageFailWhenUriIsMalformed() throws Exception {
        
        String url = "https://example.com/lti-launch";
        HttpEvent requestMock = new MockHttpGet(url);
        
        PowerMockito.mockStatic(OAuthSignatureMethod.class);
        PowerMockito.when(OAuthSignatureMethod.getBaseString(Matchers.any(OAuthMessage.class))).thenThrow(new URISyntaxException("","",0));

        LtiVerificationResult result = BasicLTIUtil.validateMessage(requestMock, url, "secret");

        Assert.assertEquals(LtiError.BAD_REQUEST, result.getError());
        Assert.assertEquals(Boolean.FALSE, result.getSuccess());
        
    }
    
    @Test
    public void testValidateMessageFailOnIOException() throws Exception {
        
        String url = "https://example.com/lti-launch";
        HttpEvent requestMock = new MockHttpGet(url);
        
        PowerMockito.mockStatic(OAuthSignatureMethod.class);
        PowerMockito.when(OAuthSignatureMethod.getBaseString(Matchers.any(OAuthMessage.class))).thenThrow(new IOException(""));

        LtiVerificationResult result = BasicLTIUtil.validateMessage(requestMock, url, "secret");

        Assert.assertEquals(LtiError.BAD_REQUEST, result.getError());
        Assert.assertEquals(Boolean.FALSE, result.getSuccess());
        
    }

    @Test
    public void testValidateMessageFailOnValidateMessageIOException() throws Exception {

        SimpleOAuthValidator sov = Mockito.mock(SimpleOAuthValidator.class);
        PowerMockito.whenNew(SimpleOAuthValidator.class).withNoArguments().thenReturn(sov);
        Mockito.doThrow(new IOException("failed")).when(sov).validateMessage(Matchers.any(OAuthMessage.class), Matchers.any(OAuthAccessor.class));
        PowerMockito.mockStatic(OAuthSignatureMethod.class);
        PowerMockito.when(OAuthSignatureMethod.getBaseString(Matchers.any(OAuthMessage.class))).thenReturn("");

        String uri = "https://example.com/lti-launch";
        MockHttpGet get = new MockHttpGet(uri);
        LtiVerificationResult result = BasicLTIUtil.validateMessage(get, uri, "secret");

        Assert.assertEquals(LtiError.BAD_REQUEST, result.getError());
        Assert.assertEquals(Boolean.FALSE, result.getSuccess());
        Assert.assertEquals(null, result.getLtiLaunchResult());
    }

    @Test
    public void testValidateMessageFailOnValidateMessageOAuthException() throws Exception {

        SimpleOAuthValidator sov = Mockito.mock(SimpleOAuthValidator.class);
        PowerMockito.whenNew(SimpleOAuthValidator.class).withNoArguments().thenReturn(sov);
        Mockito.doThrow(new OAuthException("failed")).when(sov).validateMessage(Matchers.any(OAuthMessage.class), Matchers.any(OAuthAccessor.class));
        PowerMockito.mockStatic(OAuthSignatureMethod.class);
        PowerMockito.when(OAuthSignatureMethod.getBaseString(Matchers.any(OAuthMessage.class))).thenReturn("");

        String uri = "https://example.com/lti-launch";
        MockHttpGet get = new MockHttpGet(uri);
        LtiVerificationResult result = BasicLTIUtil.validateMessage(get, uri, "secret");

        Assert.assertEquals(LtiError.BAD_REQUEST, result.getError());
        Assert.assertEquals(Boolean.FALSE, result.getSuccess());
        Assert.assertEquals(null, result.getLtiLaunchResult());
    }

    @Test
    public void testValidateMessageFailOnValidateMessageURISyntaxException() throws Exception {

        SimpleOAuthValidator sov = Mockito.mock(SimpleOAuthValidator.class);
        PowerMockito.whenNew(SimpleOAuthValidator.class).withNoArguments().thenReturn(sov);
        Mockito.doThrow(new URISyntaxException("failed", "failed")).when(sov).validateMessage(Matchers.any(OAuthMessage.class), Matchers.any(OAuthAccessor.class));
        PowerMockito.mockStatic(OAuthSignatureMethod.class);
        PowerMockito.when(OAuthSignatureMethod.getBaseString(Matchers.any(OAuthMessage.class))).thenReturn("");

        String uri = "https://example.com/lti-launch";
        MockHttpGet get = new MockHttpGet(uri);
        LtiVerificationResult result = BasicLTIUtil.validateMessage(get, uri, "secret");

        Assert.assertEquals(LtiError.BAD_REQUEST, result.getError());
        Assert.assertEquals(Boolean.FALSE, result.getSuccess());
        Assert.assertEquals(null, result.getLtiLaunchResult());
    }

    @Test
    public void testValidateMessagePass() throws Exception {

        SimpleOAuthValidator sov = Mockito.mock(SimpleOAuthValidator.class);
        PowerMockito.whenNew(SimpleOAuthValidator.class).withNoArguments().thenReturn(sov);
        Mockito.doNothing().when(sov).validateMessage(Matchers.any(OAuthMessage.class), Matchers.any(OAuthAccessor.class));
        PowerMockito.mockStatic(OAuthSignatureMethod.class);
        PowerMockito.when(OAuthSignatureMethod.getBaseString(Matchers.any(OAuthMessage.class))).thenReturn("");

        URL url = URL.builder(HTTPS).setHost("example.com").setPath("lti-launch")
                .addQueryPair("user_id", "pgray")
                .addQueryPair("roles", "instructor, teacher,administrator")
                .addQueryPair("lti_version", "lpv1")
                .addQueryPair("lti_message_type", "lti")
                .addQueryPair("resource_link_id", "12345")
                .addQueryPair("context_id", "9876")
                .addQueryPair("launch_presentation_return_url", "http://example.com/return")
                .addQueryPair("tool_consumer_instance_guid", "instance_id")
                .create();

        String uri = url.toString();
        MockHttpGet req = new MockHttpGet(uri);

        assertEquals("pgray", req.urlParameter("user_id"));
        assertEquals("pgray", req.decodedUrlParameter("user_id"));

        System.out.println("URI IS: " + uri);

        LtiVerificationResult result = BasicLTIUtil.validateMessage(req, "https://example.com/lti-launch", "secret1");

        Assert.assertEquals(null, result.getError());
        Assert.assertEquals(Boolean.TRUE, result.getSuccess());
        Assert.assertNotNull(result.getLtiLaunchResult());

        System.out.println("USER " + result.getLtiLaunchResult().getUser());
        
        Assert.assertEquals("pgray", result.getLtiLaunchResult().getUser().getId());
        Assert.assertEquals(3, result.getLtiLaunchResult().getUser().getRoles().size());
        Assert.assertTrue(result.getLtiLaunchResult().getUser().getRoles().contains("instructor"));
        Assert.assertTrue(result.getLtiLaunchResult().getUser().getRoles().contains("teacher"));
        Assert.assertTrue(result.getLtiLaunchResult().getUser().getRoles().contains("administrator"));
        
        Assert.assertEquals("lpv1", result.getLtiLaunchResult().getVersion());
        Assert.assertEquals("lti", result.getLtiLaunchResult().getMessageType());
        Assert.assertEquals("12345", result.getLtiLaunchResult().getResourceLinkId());
        Assert.assertEquals("9876", result.getLtiLaunchResult().getContextId());
        Assert.assertEquals("http://example.com/return", result.getLtiLaunchResult().getLaunchPresentationReturnUrl());
        Assert.assertEquals("instance_id", result.getLtiLaunchResult().getToolConsumerInstanceGuid());
        
    }
    
}
