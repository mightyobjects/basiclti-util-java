/**
 * Copyright (c) 2013 IMS GLobal Learning Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * Author: Charles Severance <csev@umich.edu>
 */

package org.imsglobal.lti2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import static com.google.common.net.MediaType.HTML_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Response;
import static com.mastfrog.acteur.headers.Headers.CONTENT_TYPE;
import static com.mastfrog.acteur.headers.Headers.X_FORWARDED_PROTO;
import com.mastfrog.acteur.server.PathFactory;
import com.mastfrog.acteur.server.ServerModule;
import com.mastfrog.settings.Settings;
import io.netty.handler.codec.http.HttpHeaderNames;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_IMPLEMENTED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

import org.imsglobal.lti.BasicLTIConstants;
import org.imsglobal.lti.BasicLTIUtil;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.json.IMSJSONRequest;
import org.imsglobal.lti2.objects.consumer.ServiceOffered;
import org.imsglobal.lti2.objects.consumer.StandardServices;
import org.imsglobal.lti2.objects.consumer.ToolConsumer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Notes:
 *
 * This is a sample "Hello World" servlet for LTI2.  It is a simple UI - mostly
 * intended to exercise the APIs and show the way for servlet-based LTI2 code.
 *
 * Here are the web.xml entries:
 * <pre>
 * {@code
 *  <servlet>
 *    <servlet-name>SampleServlet</servlet-name>
 *    <servlet-class>org.imsglobal.lti2.LTI2Servlet</servlet-class>
 *  </servlet>
 *  <servlet-mapping>
 *    <servlet-name>SampleServlet</servlet-name>
 *    <url-pattern>/sample/*</url-pattern>
 *  </servlet-mapping>
 * }
 * </pre>
 *
 *  Then navigate to:
 *  http://localhost/testservlet/sample/register
 *
 *  A PHP endpoint is available at:
 *
 *  https://source.sakaiproject.org/svn/basiclti/trunk/basiclti-docs/resources/docs/sakai-api-test
 *
 *  The tp.php script is the Tool Provider registration endpoint in the PHP code
 *
 */

@SuppressWarnings("deprecation")
public class LTI2Servlet {

	private static final long serialVersionUID = 1L;
	private static Logger M_log = Logger.getLogger(LTI2Servlet.class.getName());

	protected ServiceOffered LTI2ResultItem = null;
	protected ServiceOffered LTI2LtiLinkSettings = null;
	protected ServiceOffered LTI2ToolProxyBindingSettings = null;
	protected ServiceOffered LTI2ToolProxySettings = null;

	private static final String SVC_tc_profile = "tc_profile";
	private static final String SVC_tc_registration = "tc_registration";
	private static final String SVC_Settings = "Settings";
	private static final String SVC_Result = "Result";

	@SuppressWarnings("unused")
	private static final String EMPTY_JSON_OBJECT = "{\n}\n";

    // Normally these would be in a database
	private String key = "42";
    private String secret = "zaphod";

	// Pretending to be a database row :)
	private static Map<String, String> PERSIST = new TreeMap<String, String>();
    private final PathFactory paths;
    private final Settings settings;

    @Inject
    LTI2Servlet(PathFactory paths, Settings settings, String key, String secret) {
        this.key = key;
        this.secret = secret;
        this.paths = paths;
        this.settings = settings;
    }

    private String scheme(HttpEvent evt) {
        String result = evt.header(X_FORWARDED_PROTO.name());
        if (result == null) {
            result = paths.constructURL("/").getProtocol().toString();
        }
        return result;
    }

    protected void onRequest(HttpEvent request, Response response)
            throws IOException	{
		try {
			doRequest(request, response);
		} catch (Exception e) {
                    String ipAddress = request.remoteAddress().toString();
                    String uri = request.request().uri();
			M_log.log(Level.WARNING, "General LTI2 Failure URI="+uri+" IP=" + ipAddress);
			e.printStackTrace();
                    response.status(INTERNAL_SERVER_ERROR);
			doErrorJSON(request, response, null, "General failure", e);
		}
	}

	@SuppressWarnings("unused")
    protected void doRequest(HttpEvent request, Response response)
            throws IOException	{
		System.out.println("getServiceURL="+getServiceURL(request));

            String ipAddress = request.remoteAddress().toString();
		System.out.println("LTI Service request from IP=" + ipAddress);

            String rpi = null; // XXX handle basePath
            String uri = request.request().uri();
		String [] parts = uri.split("/");
		if ( parts.length < 4 ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, null, "Incorrect url format", null);
			return;
		}
		String controller = parts[3];
		if ( "register".equals(controller) ) {
			doRegister(request,response);
			return;
		} else if ( "launch".equals(controller) ) {
			doLaunch(request,response);
			return;
		} else if ( SVC_tc_profile.equals(controller) && parts.length == 5 ) {
			String profile_id = parts[4];
			getToolConsumerProfile(request,response,profile_id);
			return;
		} else if ( SVC_tc_registration.equals(controller) && parts.length == 5 ) {
			String profile_id = parts[4];
			registerToolProviderProfile(request, response, profile_id);
			return;
		} else if ( SVC_Result.equals(controller) && parts.length == 5 ) {
			String sourcedid = parts[4];
			handleResultRequest(request, response, sourcedid);
			return;
		} else if ( SVC_Settings.equals(controller) && parts.length >= 6 ) {
			handleSettingsRequest(request, response, parts);
			return;
		}

		IMSJSONRequest jsonRequest = new IMSJSONRequest(request);
		if ( jsonRequest.valid ) {
			System.out.println(jsonRequest.getPostBody());
		}

            response.status(NOT_IMPLEMENTED);
		M_log.log(Level.WARNING, "Unknown request="+uri);
		doErrorJSON(request, response, null, "Unknown request="+uri, null);
	}

    protected void doRegister(HttpEvent request, Response response)	{
		// Reset our database
		PERSIST.clear();
            String launch_url = request.decodedUrlParameter("launch_url");
            response.add(CONTENT_TYPE, HTML_UTF_8);

		String output = null;
		if ( launch_url != null ) {
			Properties ltiProps = new Properties();

			ltiProps.setProperty(BasicLTIConstants.LTI_VERSION, LTI2Constants.LTI2_VERSION_STRING);
                    ltiProps.setProperty(LTI2Constants.REG_KEY, key);
                    ltiProps.setProperty(LTI2Constants.REG_PASSWORD, secret);
			ltiProps.setProperty(BasicLTIUtil.BASICLTI_SUBMIT, "Press to Launch External Tool");
			ltiProps.setProperty(BasicLTIConstants.LTI_MESSAGE_TYPE, BasicLTIConstants.LTI_MESSAGE_TYPE_TOOLPROXYREGISTRATIONREQUEST);

			String serverUrl = getServiceURL(request);
                    ltiProps.setProperty(LTI2Constants.TC_PROFILE_URL, serverUrl + SVC_tc_profile + "/" + key);
			ltiProps.setProperty(BasicLTIConstants.LAUNCH_PRESENTATION_RETURN_URL, serverUrl + "launch");
			System.out.println("ltiProps="+ltiProps);

			boolean dodebug = true;
			output = BasicLTIUtil.postLaunchHTML(ltiProps, launch_url, dodebug);
		} else {
			output = "<form>Register URL:<br/><input type=\"text\" name=\"launch_url\" size=\"80\"\n" +
				"value=\"http://localhost:8888/sakai-api-test/tp.php\"><input type=\"submit\">\n";
		}

        response.setMessage(output + "\n");
    }

	// We are actually bypassing the activation step.  Usually activation will parse
	// the profile, and install a tool if the admin is happy.  For us we just parse
	// the profile and do a launch.
	@SuppressWarnings("unused")
    protected void doLaunch(HttpEvent request, Response response)	{

		String profile = PERSIST.get("profile");
            response.add(CONTENT_TYPE, HTML_UTF_8);

		String output = null;
		if ( profile == null ) {
			output = "Missing profile";
		} else {
	        JSONObject providerProfile = (JSONObject) JSONValue.parse(profile);

			List<Properties> profileTools = new ArrayList<Properties> ();
	        Properties info = new Properties();
			String retval = LTI2Util.parseToolProfile(profileTools, info, providerProfile);
			String launch = null;
			String parameter = null;
			for ( Properties profileTool : profileTools ) {
				launch = (String) profileTool.get("launch");
				parameter = (String) profileTool.get("parameter");
			}
			JSONObject security_contract = (JSONObject) providerProfile.get(LTI2Constants.SECURITY_CONTRACT);

			String shared_secret = (String) security_contract.get(LTI2Constants.SHARED_SECRET);
			System.out.println("launch="+launch);
			System.out.println("shared_secret="+shared_secret);

			Properties ltiProps = LTI2SampleData.getLaunch();
			ltiProps.setProperty(BasicLTIConstants.LTI_VERSION,BasicLTIConstants.LTI_VERSION_2);

			Properties lti2subst = LTI2SampleData.getSubstitution();
			String settings_url = getServiceURL(request) + SVC_Settings + "/";
			lti2subst.setProperty("LtiLink.custom.url", settings_url + LTI2Util.SCOPE_LtiLink + "/"
					+ ltiProps.getProperty(BasicLTIConstants.RESOURCE_LINK_ID));
			lti2subst.setProperty("ToolProxyBinding.custom.url", settings_url + LTI2Util.SCOPE_ToolProxyBinding + "/"
					+ ltiProps.getProperty(BasicLTIConstants.CONTEXT_ID));
			lti2subst.setProperty("ToolProxy.custom.url", settings_url + LTI2Util.SCOPE_ToolProxy + "/"
                            + key);
			lti2subst.setProperty("Result.url", getServiceURL(request) + SVC_Result + "/"
					+ ltiProps.getProperty(BasicLTIConstants.RESOURCE_LINK_ID));

			// Do the substitutions
			Properties custom = new Properties();
			LTI2Util.mergeLTI2Parameters(custom, parameter);
			LTI2Util.substituteCustom(custom, lti2subst);

			// Place the custom values into the launch
			LTI2Util.addCustomToLaunch(ltiProps, custom);

			ltiProps = BasicLTIUtil.signProperties(ltiProps, launch, "POST",
                            key, shared_secret, null, null, null);

			boolean dodebug = true;
			output = BasicLTIUtil.postLaunchHTML(ltiProps, launch, dodebug);
		}
        response.setMessage(output + "\n");
    }

    protected void getToolConsumerProfile(HttpEvent request,
            Response response, String profile_id)	{
		// Map<String,Object> deploy = ltiService.getDeployForConsumerKeyDao(profile_id);
		Map<String,Object> deploy = null;

		ToolConsumer consumer = buildToolConsumerProfile(request, deploy, profile_id);

		ObjectMapper mapper = new ObjectMapper();
		try {
			// http://stackoverflow.com/questions/6176881/how-do-i-make-jackson-pretty-print-the-json-content-it-generates
			// ***IMPORTANT!!!*** for Jackson 2.x use the line below instead of the one above:
                        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
			// System.out.println(mapper.writeValueAsString(consumer));
			response.add(CONTENT_TYPE, JSON_UTF_8);
                    response.setMessage(writer.writeValueAsString(consumer));
                    // System.out.println(writer.writeValueAsString(consumer));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Normally deploy would have the data about the deployment - for this test
	// it is always null and we allow everything
	protected ToolConsumer buildToolConsumerProfile(HttpEvent request, Map<String, Object> deploy, String profile_id)	{
		// Load the configuration data
		LTI2Config cnf = new org.imsglobal.lti2.LTI2ConfigSample();

		ToolConsumer consumer = new ToolConsumer(profile_id, "LTI-2p0", getServiceURL(request), cnf);

		// Normally we would check permissions before we offer capabilities
		List<String> capabilities = consumer.getCapability_offered();
		LTI2Util.allowEmail(capabilities);
		LTI2Util.allowName(capabilities);
		LTI2Util.allowSettings(capabilities);
		LTI2Util.allowResult(capabilities);

		// Normally we would check permissions before we offer services
		List<ServiceOffered> services = consumer.getService_offered();
		services.add(StandardServices.LTI2Registration(getServiceURL(request) +
			SVC_tc_registration + "/" + profile_id));
		services.add(StandardServices.LTI2ResultItem(getServiceURL(request) +
			SVC_Result + "/{" + BasicLTIConstants.LIS_RESULT_SOURCEDID + "}"));
		services.add(StandardServices.LTI2LtiLinkSettings(getServiceURL(request) +
			SVC_Settings + "/" + LTI2Util.SCOPE_LtiLink + "/{" + BasicLTIConstants.RESOURCE_LINK_ID + "}"));
		services.add(StandardServices.LTI2ToolProxySettings(getServiceURL(request) +
			SVC_Settings + "/" + LTI2Util.SCOPE_ToolProxyBinding + "/{" + BasicLTIConstants.CONTEXT_ID + "}"));
		services.add(StandardServices.LTI2ToolProxySettings(getServiceURL(request) +
			SVC_Settings + "/" + LTI2Util.SCOPE_ToolProxy + "/{" + LTI2Constants.TOOL_PROXY_GUID + "}"));
		return consumer;
	}

	@SuppressWarnings({ "unchecked", "unused", "rawtypes" })
    public void registerToolProviderProfile(HttpEvent request, Response response,
         			String profile_id) throws java.io.IOException
	{
		// Normally we would look up the deployment descriptor
		if (!key.equals(profile_id)) {
                    response.status(NOT_FOUND);
			return;
		}

            String key = this.key;
            String secret = this.secret;

		IMSJSONRequest jsonRequest = new IMSJSONRequest(request);

		if ( ! jsonRequest.valid ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, jsonRequest, "Request is not in a valid format", null);
			return;
		}

		System.out.println(jsonRequest.getPostBody());

		// Lets check the signature
		if ( key == null || secret == null ) {
                    response.status(FORBIDDEN);
			doErrorJSON(request, response, jsonRequest, "Deployment is missing credentials", null);
			return;
		}

		jsonRequest.validateRequest(key, secret, request);
		if ( !jsonRequest.valid ) {
                    response.status(FORBIDDEN);
			doErrorJSON(request, response, jsonRequest, "OAuth signature failure", null);
			return;
		}

		JSONObject providerProfile = (JSONObject) JSONValue.parse(jsonRequest.getPostBody());
		// System.out.println("OBJ:"+providerProfile);
		if ( providerProfile == null  ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, jsonRequest, "JSON parse failed", null);
			return;
		}

		JSONObject default_custom = (JSONObject) providerProfile.get(LTI2Constants.CUSTOM);

		JSONObject security_contract = (JSONObject) providerProfile.get(LTI2Constants.SECURITY_CONTRACT);
		if ( security_contract == null  ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, jsonRequest, "JSON missing security_contract", null);
			return;
		}

		String shared_secret = (String) security_contract.get(LTI2Constants.SHARED_SECRET);
		System.out.println("shared_secret="+shared_secret);
		if ( shared_secret == null  ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, jsonRequest, "JSON missing shared_secret", null);
			return;
		}

		// Make sure that the requested services are a subset of the offered services
		ToolConsumer consumer = buildToolConsumerProfile(request, null, profile_id);

		JSONArray tool_services = (JSONArray) security_contract.get(LTI2Constants.TOOL_SERVICE);
		String retval = LTI2Util.validateServices(consumer, providerProfile);
		if ( retval != null ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, jsonRequest, retval, null);
			return;
		}

		// Parse the tool profile bit and extract the tools with error checking
		retval = LTI2Util.validateCapabilities(consumer, providerProfile);
		if ( retval != null ) {
                    response.status(BAD_REQUEST);
			doErrorJSON(request, response, jsonRequest, retval, null);
			return;
		}

		// Pass the profile to the launch process
		PERSIST.put("profile", providerProfile.toString());

		// Share our happiness with the Tool Provider
		Map jsonResponse = new TreeMap();
		jsonResponse.put(LTI2Constants.CONTEXT,StandardServices.TOOLPROXY_ID_CONTEXT);
		jsonResponse.put(LTI2Constants.TYPE, StandardServices.TOOLPROXY_ID_TYPE);
		jsonResponse.put(LTI2Constants.JSONLD_ID, getServiceURL(request) + SVC_tc_registration + "/" +profile_id);
		jsonResponse.put(LTI2Constants.TOOL_PROXY_GUID, profile_id);
		jsonResponse.put(LTI2Constants.CUSTOM_URL, getServiceURL(request) + SVC_Settings + "/" + LTI2Util.SCOPE_ToolProxy + "/" +profile_id);
            response.add(CONTENT_TYPE.toStringHeader(), StandardServices.TOOLPROXY_ID_FORMAT);
            response.status(CREATED);
		String jsonText = JSONValue.toJSONString(jsonResponse);
        M_log.log(Level.FINE, jsonText);
        response.setMessage(jsonText + "\n");
    }

    public String getServiceURL(HttpEvent request) {

        String scheme = scheme(request);             // http
        String serverName = request.header(HttpHeaderNames.HOST) == null
                ? paths.constructURL("/").getHost().toString() : request.header(HttpHeaderNames.HOST);

        int serverPort = paths.constructURL("/").getPort().intValue();
        String contextPath = settings.getString(ServerModule.SETTINGS_KEY_BASE_PATH) == null
                ? "/" : "/" + settings.getString(ServerModule.SETTINGS_KEY_BASE_PATH);
        String servletPath = request.path().toString();
		String url = scheme+"://"+serverName+":"+serverPort+contextPath+servletPath+"/";
		return url;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    public void handleResultRequest(HttpEvent request, Response response,
         			String sourcedid) throws java.io.IOException
	{
		IMSJSONRequest jsonRequest = null;
		String retval = null;
		if ( "GET".equals(request.getMethod()) ) {
			String grade = PERSIST.get("grade");
			String comment = PERSIST.get("comment");

			Map jsonResponse = new TreeMap();
			Map resultScore = new TreeMap();

			jsonResponse.put(LTI2Constants.CONTEXT,StandardServices.RESULT_CONTEXT);
			jsonResponse.put(LTI2Constants.TYPE, StandardServices.RESULT_TYPE);
			resultScore.put(LTI2Constants.TYPE, LTI2Constants.GRADE_TYPE_DECIMAL);
			jsonResponse.put(LTI2Constants.COMMENT, grade);
			resultScore.put(LTI2Constants.VALUE, comment);
			jsonResponse.put(LTI2Constants.RESULTSCORE,resultScore);
                    response.add(CONTENT_TYPE.toStringHeader(), StandardServices.RESULT_FORMAT);
                    response.status(OK);
			String jsonText = JSONValue.toJSONString(jsonResponse);
                    M_log.log(Level.FINE, jsonText);
                    response.setMessage(jsonText + "\n");
                    return;
		} else if ( "PUT".equals(request.getMethod()) ) {
			retval = "Error parsing input data";
			try {
				jsonRequest = new IMSJSONRequest(request);
				// System.out.println(jsonRequest.getPostBody());
				JSONObject requestData = (JSONObject) JSONValue.parse(jsonRequest.getPostBody());
				String comment = (String) requestData.get(LTI2Constants.COMMENT);
				JSONObject resultScore = (JSONObject) requestData.get(LTI2Constants.RESULTSCORE);
				String sGrade = (String) resultScore.get(LTI2Constants.VALUE);
				Double dGrade = new Double(sGrade);

				PERSIST.put("comment", comment);
				PERSIST.put("grade", dGrade+"");
                            response.status(OK);
				return;
			} catch (Exception e) {
				retval = "Error: "+ e.getMessage();
			}
		} else {
			retval = "Unsupported operation:" + request.getMethod();
		}

            response.status(BAD_REQUEST);
		doErrorJSON(request,response, jsonRequest, (String) retval, null);
	}

	// If this code looks like a hack - it is because the spec is a hack.
	// There are five possible scenarios for GET and two possible scenarios
    // for PUT.  I begged to simplify the business logic but was overrulled.
	// So we write obtuse code.
	@SuppressWarnings("unused")
    public void handleSettingsRequest(HttpEvent request, Response response,
         			String[] parts) throws java.io.IOException
	{

            String URL = getServiceURL(request);
System.out.println("URL="+URL);
		String scope = parts[4];
System.out.println("scope="+scope);

            String acceptHdr = request.header(HttpHeaderNames.ACCEPT);
            String contentHdr = request.header(HttpHeaderNames.CONTENT_TYPE);
		boolean acceptComplex = acceptHdr == null || acceptHdr.indexOf(StandardServices.TOOLSETTINGS_FORMAT) >= 0 ;

System.out.println("accept="+acceptHdr+" ac="+acceptComplex);

		// Check the JSON on PUT and check the oauth_body_hash
		IMSJSONRequest jsonRequest = null;
		JSONObject requestData = null;
            if ("PUT".equals(request.method().toString())) {
			try {
				jsonRequest = new IMSJSONRequest(request);
				requestData = (JSONObject) JSONValue.parse(jsonRequest.getPostBody());
			} catch (Exception e) {
                            response.status(BAD_REQUEST);
				doErrorJSON(request,response, jsonRequest, "Could not parse JSON", e);
				return;
			}
		}

            String consumer_key = key;
		String profile = PERSIST.get("profile");
		JSONObject providerProfile = (JSONObject) JSONValue.parse(profile);
		JSONObject security_contract = (JSONObject) providerProfile.get(LTI2Constants.SECURITY_CONTRACT);
		String oauth_secret = (String) security_contract.get(LTI2Constants.SHARED_SECRET);

		// Validate the incoming message
		LtiVerificationResult result = BasicLTIUtil.validateMessage(request, URL, oauth_secret);
		if (!result.getSuccess()) {
                    response.status(FORBIDDEN);
			doErrorJSON(request,response, jsonRequest, result.getMessage(), null);
			return;
		}

		// The URLs for the various settings resources
		String settingsUrl = getServiceURL(request) + SVC_Settings;
		String proxy_url = settingsUrl + "/" + LTI2Util.SCOPE_ToolProxy + "/" + consumer_key;
		String binding_url = settingsUrl + "/" + LTI2Util.SCOPE_ToolProxyBinding + "/" + "TBD";
		String link_url = settingsUrl + "/" + LTI2Util.SCOPE_LtiLink + "/" + "TBD";

		// Load and parse the old settings...
		JSONObject link_settings = LTI2Util.parseSettings(PERSIST.get(LTI2Util.SCOPE_LtiLink));
		JSONObject binding_settings = LTI2Util.parseSettings(PERSIST.get(LTI2Util.SCOPE_ToolProxyBinding));
		JSONObject proxy_settings = LTI2Util.parseSettings(PERSIST.get(LTI2Util.SCOPE_ToolProxy));

		// For a GET request we depend on LTI2Util to do the GET logic
		if ( "GET".equals(request.getMethod()) ) {
			Object obj = LTI2Util.getSettings(request, scope,
				link_settings, binding_settings, proxy_settings,
				link_url, binding_url, proxy_url);

			if ( obj instanceof String ) {
                            response.status(BAD_REQUEST);
				doErrorJSON(request,response, jsonRequest, (String) obj, null);
				return;
			}

			if ( acceptComplex ) {
                            response.add(CONTENT_TYPE.toStringHeader(), StandardServices.TOOLSETTINGS_FORMAT);
			} else {
                            response.add(CONTENT_TYPE.toStringHeader(), StandardServices.TOOLSETTINGS_SIMPLE_FORMAT);
			}

			JSONObject jsonResponse = (JSONObject) obj;
                    response.status(OK);
                    response.setMessage(jsonResponse.toString() + "\n");
                    System.out.println("jsonResponse=" + jsonResponse);
                    return;
                } else if ("PUT".equals(request.method().toString())) {
			// This is assuming the rule that a PUT of the complex settings
			// format that there is only one entry in the graph and it is
			// the same as our current URL.  We parse without much checking.
			String settings = null;
			try {
				JSONArray graph = (JSONArray) requestData.get(LTI2Constants.GRAPH);
				if ( graph.size() != 1 ) {
                                    response.status(BAD_REQUEST);
					doErrorJSON(request,response, jsonRequest, "Only one graph entry allowed", null);
					return;
				}
				JSONObject firstChild = (JSONObject) graph.get(0);
				JSONObject custom = (JSONObject) firstChild.get(LTI2Constants.CUSTOM);
				settings = custom.toString();
			} catch (Exception e) {
				settings = jsonRequest.getPostBody();
			}
			PERSIST.put(scope,settings);
System.out.println("Stored settings scope="+scope);
System.out.println("settings="+settings);
                    response.status(OK);
		} else {
                    response.status(BAD_REQUEST);
			doErrorJSON(request,response, jsonRequest, "Method not handled="+request.getMethod(), null);
		}
	}

	/* IMS JSON version of Errors */
    public void doErrorJSON(HttpEvent request, Response response,
         			IMSJSONRequest json, String message, Exception e)
		throws java.io.IOException
	{
		if (e != null) {
			M_log.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
        M_log.info(message);
		String output = IMSJSONRequest.doErrorJSON(request, response, json, message, e);
System.out.println(output);
    }

	public void destroy() {
	}

}
