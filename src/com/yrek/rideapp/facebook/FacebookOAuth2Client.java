package com.yrek.rideapp.facebook;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import com.yrek.rideapp.oauth2.AccessToken;
import com.yrek.rideapp.oauth2.OAuth2Client;

@Singleton
public class FacebookOAuth2Client implements OAuth2Client, Closeable {
    private static final Logger LOG = Logger.getLogger(FacebookOAuth2Client.class.getName());

    private static final String AUTHORIZE_URL = "https://graph.facebook.com/oauth/authorize";
    private static final String ACCESS_TOKEN_URL = "https://graph.facebook.com/oauth/access_token";

    private static final String AUTHORIZE_FORWARD = "/WEB-INF/pages/oauth2redirect.jsp";
    private static final String AUTHORIZATION_DENIED_REDIRECT = "http://www.facebook.com/";

    private final String clientID;
    private final String clientSecret;
    private final String appURL;

    private final Client client;
    private final WebResource accessTokenResource;

    public FacebookOAuth2Client(String clientID, String clientSecret, String appURL) {
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.appURL = appURL;

        client = Client.create();
        client.addFilter(new LoggingFilter());
        accessTokenResource = client.resource(ACCESS_TOKEN_URL);
    }

    @Override
    public String getClientID(HttpServletRequest request) {
        return clientID;
    }

    @Override
    public String getClientSecret(HttpServletRequest request) {
        return clientSecret;
    }

    private boolean appAuthorized(HttpServletRequest request) {
        return !"0".equals(request.getParameter("fb_sig_added"));
    }

    @Override
    public String getReturnURI(HttpServletRequest request, String startURI) {
        if (appAuthorized(request))
            return startURI;
        else
            return appURL;
    }

    @Override
    public String getAuthorizeForward(HttpServletRequest request, String redirectURL) throws IOException {
        if (appAuthorized(request))
            return null;
        request.setAttribute("authorizeURL", AUTHORIZE_URL + "?client_id=" + URLEncoder.encode(getClientID(request), "UTF-8") + "&redirect_uri=" + URLEncoder.encode(redirectURL, "UTF-8") + "&display=page");
        return AUTHORIZE_FORWARD;
    }

    @Override
    public String getAuthorizeRedirect(HttpServletRequest request, String redirectURL) throws IOException {
        if (!appAuthorized(request))
            return null;
        return AUTHORIZE_URL + "?client_id=" + URLEncoder.encode(getClientID(request), "UTF-8") + "&redirect_uri=" + URLEncoder.encode(redirectURL, "UTF-8");
    }

    @Override
    public AccessToken getAccessToken(HttpServletRequest request, String redirectURL, String code) throws IOException {
        MultivaluedMapImpl parameters = new MultivaluedMapImpl();
        parameters.add("client_id", clientID);
        parameters.add("client_secret", clientSecret);
        parameters.add("redirect_uri", redirectURL);
        parameters.add("code", code);

        // The draft OAuth2.0 specification says that JSON is returned:
        // 
        // Content-Type: application/json
        //
        // {
        // "access_token":"SlAV32hkKG",
        // "expires_in":3600,
        // "refresh_token":"8xLOxBtZp8"
        // }
        //
        // But what Facebook actually returns is:
        //
        // Content-Type: text/plain; charset=UTF-8
        //
        // access_token=119881418055180|2.EuFaIoVf107KxyBy0DWzbQ__.3600.1277114400-100001237687340|8PNihSAYjxw-1_BpVU1LgVFT8LM.&expires=3656
        //
        String data = accessTokenResource.queryParams(parameters).get(String.class);
        String accessToken = null;
        long expiresAt = 0;
        String refreshToken = null;
        for (String item : data.split("&")) {
            if (item.startsWith("access_token="))
                accessToken = URLDecoder.decode(item.substring(13), "UTF-8");
            else if (item.startsWith("expires="))
                expiresAt = System.currentTimeMillis() + 950L*Integer.parseInt(item.substring(8));
        }

        return new AccessToken(accessToken, expiresAt, refreshToken);
    }

    @Override
    public String getAuthorizationDeniedForward(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getAuthorizationDeniedRedirect(HttpServletRequest request) {
        return AUTHORIZATION_DENIED_REDIRECT;
    }

    @Override
    public void close() {
        client.destroy();
    }
}
