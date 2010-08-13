package com.yrek.rideapp.facebook;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import com.yrek.rideapp.oauth2.OAuth2Session;

@Singleton
public class FacebookClient implements Closeable {
    private static final Logger LOG = Logger.getLogger(FacebookClient.class.getName());

    private final OAuth2Session oAuth2Session;
    private final Client client;
    private final WebResource userResource;
    private final WebResource friendsResource;

    @Inject
    public FacebookClient(OAuth2Session oAuth2Session) {
        this.oAuth2Session = oAuth2Session;
        client = Client.create();
        client.addFilter(new LoggingFilter());
        userResource = client.resource("https://graph.facebook.com/me");
        friendsResource = client.resource("https://graph.facebook.com/me/friends");
    }

    public MultivaluedMap<String,String> getParameters(HttpServletRequest request) {
        MultivaluedMap<String,String> parameters = new MultivaluedMapImpl();
        parameters.putSingle("access_token", oAuth2Session.getAccessToken(request).getAccessToken());
        return parameters;
    }

    public User getUser(HttpServletRequest request) throws IOException {
        return userResource.queryParams(getParameters(request)).get(User.class);
    }

    public Users getFriends(HttpServletRequest request) throws IOException {
        return friendsResource.queryParams(getParameters(request)).get(Users.class);
    }

    @Override
    public void close() {
        client.destroy();
    }
}
