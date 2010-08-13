package com.yrek.rideapp.oauth2;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

public interface OAuth2Client {
    public String getClientID(HttpServletRequest request);
    public String getClientSecret(HttpServletRequest request);
    public String getReturnURI(HttpServletRequest request, String startURI) throws IOException;
    public String getAuthorizeForward(HttpServletRequest request, String redirectURL) throws IOException;
    public String getAuthorizeRedirect(HttpServletRequest request, String redirectURL) throws IOException;
    public AccessToken getAccessToken(HttpServletRequest request, String redirectURL, String code) throws IOException;
    public String getAuthorizationDeniedForward(HttpServletRequest request) throws IOException;
    public String getAuthorizationDeniedRedirect(HttpServletRequest request) throws IOException;
}
