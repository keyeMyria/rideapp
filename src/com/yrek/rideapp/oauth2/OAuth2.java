package com.yrek.rideapp.oauth2;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

public class OAuth2 {
    private static final Logger LOG = Logger.getLogger(OAuth2.class.getName());
    private static final String ATTRIBUTE_NAME = OAuth2.class.getName();

    @Singleton
    public static class OAuth2Servlet extends HttpServlet {
        private static final long serialVersionUID = 0L;

        @Inject
        private OAuth2Client oAuth2Client;

        @Inject
        private OAuth2Session oAuth2Session;

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            doGet(request, response);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String code = request.getParameter("code");
            String returnURI = oAuth2Session.getReturnURI(request);
            LOG.fine("servletPath="+request.getServletPath()+",contextPath="+request.getContextPath()+",pathInfo="+request.getPathInfo()+",code="+code+",returnURI="+returnURI);

            if (code == null) {
                String authorizationDeniedForward = oAuth2Client.getAuthorizationDeniedForward(request);
                if (authorizationDeniedForward != null)
                    request.getRequestDispatcher(authorizationDeniedForward).forward(request, response);
                else
                    response.sendRedirect(encodeRedirectURL(response, oAuth2Client.getAuthorizationDeniedRedirect(request)));
                return;
            }

            AccessToken accessToken = oAuth2Client.getAccessToken(request, response.encodeURL(request.getRequestURL().toString()), code);
            LOG.fine("accessToken="+accessToken);
            oAuth2Session.saveAccessToken(request, response, accessToken);
            oAuth2Session.clearReturnURI(request, response);
            response.sendRedirect(encodeRedirectURL(response, returnURI != null ? returnURI : request.getContextPath()));
        }
    }

    @Singleton
    public static class OAuth2Filter implements Filter {
        @Inject
        private OAuth2Client oAuth2Client;

        @Inject
        private OAuth2Session oAuth2Session;

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            AccessToken accessToken = oAuth2Session.getAccessToken(request);
            LOG.fine("accessToken="+accessToken+",requestURL="+request.getRequestURL()+",pathInfo="+request.getPathInfo());
            if (accessToken != null && System.currentTimeMillis() < accessToken.getExpiresAt()) {
                filterChain.doFilter(request, response);
            } else if (!"GET".equals(request.getMethod()) && !"POST".equals(request.getMethod())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                oAuth2Session.saveReturnURI(request, response, oAuth2Client.getReturnURI(request, request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "")));
                String redirectURL = getRedirectURL(request, response);
                String authorizeForward = oAuth2Client.getAuthorizeForward(request, redirectURL);
                LOG.fine("returnURI="+oAuth2Session.getReturnURI(request)+",authorizeForward="+authorizeForward);
                if (authorizeForward != null)
                    request.getRequestDispatcher(authorizeForward).forward(request, response);
                else
                    response.sendRedirect(encodeRedirectURL(response, oAuth2Client.getAuthorizeRedirect(request, redirectURL)));
            }
        }

        private String getRedirectURL(HttpServletRequest request, HttpServletResponse response) throws MalformedURLException {
            return new URL(new URL(request.getRequestURL().toString()), response.encodeRedirectURL(request.getContextPath() + "/oauth2")).toString();
        }
    }

    private static String encodeRedirectURL(HttpServletResponse response, String url) {
        // Google AppEngine/Jetty spuriously adds ;jsessionid=sessionid to external URLs
        int colon = url.indexOf(':');
        int slash = url.indexOf('/');
        if (colon > 0 && slash > 0 && colon < slash)
            return url;
        return response.encodeRedirectURL(url);
    }
}
