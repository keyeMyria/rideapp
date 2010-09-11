package com.yrek.rideapp.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.yrek.rideapp.oauth2.OAuth2Session;
import com.yrek.rideapp.facebook.FacebookClient;

@Singleton
public class SetAttributesFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(SetAttributesFilter.class.getName());

    private final FacebookClient facebookClient;
    private final String garminUnlock;
    private final String facebookApplicationId;
    private final String facebookCanvasURL;

    @Inject
    public SetAttributesFilter(FacebookClient facebookClient, String garminUnlock, String facebookApplicationId, String facebookCanvasURL) {
        this.facebookClient = facebookClient;
        this.garminUnlock = garminUnlock;
        this.facebookApplicationId = facebookApplicationId;
        this.facebookCanvasURL = facebookCanvasURL;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest) request).getSession();
        request.setAttribute("sessionId", session.getId());
        if (session.getAttribute("user") == null) {
            session.setAttribute("user", facebookClient.getUser((HttpServletRequest) request));
            session.setAttribute("friends", facebookClient.getFriends((HttpServletRequest) request));
        }
        request.setAttribute("garminUnlock", garminUnlock);
        request.setAttribute("facebookApplicationId", facebookApplicationId);
        request.setAttribute("facebookCanvasURL", facebookCanvasURL);
        filterChain.doFilter(request, response);
    }
}
