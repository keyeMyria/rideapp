package com.yrek.rideapp.oauth2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface OAuth2Session {
    /**
     * Get OAuth2 access token for the current session.
     * Return null if not authenticated.
     *
     * The access token could be stored in the session or encrypted in a
     * cookie or stored in memcached, depending on the implementation.
     *
     * If retrieval is expensive, the access token could be cached as a
     * request object attribute.
     *
     * Refresh the token if needed.
     *
     * @return OAuth2 access token
     */
    public AccessToken getAccessToken(HttpServletRequest request);

    /**
     * Save the OAuth2 access token in the current session.
     */
    public void saveAccessToken(HttpServletRequest request, HttpServletResponse response, AccessToken accessToken);

    /**
     * Save URI to return to after OAuth2 authorization.
     *
     * Can be stored in the session, in a cookie, or in memcached.
     */
    public void saveReturnURI(HttpServletRequest request, HttpServletResponse response, String returnURI);

    /**
     * Get URI to return to after OAuth2 authorization.
     */
    public String getReturnURI(HttpServletRequest request);

    /**
     * Clear URI to return to after OAuth2 authorization.
     */
    public void clearReturnURI(HttpServletRequest request, HttpServletResponse response);
}
