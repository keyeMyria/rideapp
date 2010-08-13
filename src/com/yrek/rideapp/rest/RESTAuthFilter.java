package com.yrek.rideapp.rest;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.yrek.rideapp.oauth2.OAuth2Session;

@Singleton
public class RESTAuthFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(RESTAuthFilter.class.getName());

    @Inject
    private OAuth2Session oAuth2Session;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (oAuth2Session.getAccessToken((HttpServletRequest) request) != null)
            filterChain.doFilter(request, response);
        else
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
