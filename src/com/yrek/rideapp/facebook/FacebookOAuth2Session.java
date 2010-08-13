package com.yrek.rideapp.facebook;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

import com.yrek.rideapp.oauth2.AccessToken;
import com.yrek.rideapp.oauth2.OAuth2Session;

@Singleton
public class FacebookOAuth2Session implements OAuth2Session {
    private static final Logger LOG = Logger.getLogger(FacebookOAuth2Session.class.getName());
    private static final String ACCESS_TOKEN = FacebookOAuth2Session.class.getName() + ".ACCESS_TOKEN";
    private static final String RETURN_URI = FacebookOAuth2Session.class.getName() + ".RETURN_URI";

    @Override
    public AccessToken getAccessToken(HttpServletRequest request) {
        return (AccessToken) request.getSession().getAttribute(ACCESS_TOKEN);
    }

    @Override
    public void saveAccessToken(HttpServletRequest request, HttpServletResponse response, AccessToken accessToken) {
        request.getSession().setAttribute(ACCESS_TOKEN, accessToken);
    }

    @Override
    public void saveReturnURI(HttpServletRequest request, HttpServletResponse response, String returnURI) {
        request.getSession().setAttribute(RETURN_URI, returnURI);
    }

    @Override
    public String getReturnURI(HttpServletRequest request) {
        return (String) request.getSession().getAttribute(RETURN_URI);
    }

    @Override
    public void clearReturnURI(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute(RETURN_URI);
    }
}
