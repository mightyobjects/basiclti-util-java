/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imsglobal.aspect;

import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;

import javax.inject.Inject;
import org.imsglobal.lti.launch.LtiVerificationException;

/**
 *
 * @author pgray
 */
public class LtiLaunchVerifier extends Acteur {

    @Inject
    public LtiLaunchVerifier(LtiKeySecretService keyService, LtiVerifier ltiVerifier, HttpEvent request) throws LtiVerificationException, Exception {
        String oauthSecret = keyService.getSecretForKey(request.decodedUrlParameter("oauth_consumer_key"));
        LtiVerificationResult ltiResult = ltiVerifier.verify(request, oauthSecret);
        //BasicLTIUtil.validateMessage(request, request.getRequestURL().toString(), oauthSecret);
        next(ltiResult);
    }

    public String getErrorMessageForArgumentClass(String argumentClass, String signature){
        return "The LtiLaunchVerifier instance cannot find the " + argumentClass + " argument on method: " + signature + ", are you sure it was declared?";
    }

}
