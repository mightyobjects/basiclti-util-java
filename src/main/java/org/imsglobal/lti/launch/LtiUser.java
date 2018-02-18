package org.imsglobal.lti.launch;

import com.mastfrog.acteur.HttpEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 5/28/14.
 */
public class LtiUser {

    private String id;
    private List<String> roles;

    public LtiUser(HttpEvent request) {
        this.id = request.decodedUrlParameter("user_id");
        this.roles = new LinkedList<>();
        if (request.decodedUrlParameter("roles") != null) {
            for (String role : request.decodedUrlParameter("roles").split(",")) {
                this.roles.add(role.trim());
            }
        }
    }

    public LtiUser(Map<String, String> parameters) {
        this.id = parameters.get("user_id");
        this.roles = new LinkedList<>();
        if(parameters.get("roles") != null) {
            for (String role : parameters.get("roles").split(",")) {
                this.roles.add(role.trim());
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
