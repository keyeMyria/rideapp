package com.yrek.rideapp.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.codehaus.jackson.map.ObjectMapper;

import com.yrek.rideapp.rest.RESTAPI;
import com.yrek.rideapp.storage.DB;

@Singleton
public class UserServlet extends HttpServlet {
    private static final long serialVersionUID = 0L;
    private static final Logger LOG = Logger.getLogger(UserServlet.class.getName());

    // Stupid Guice bugs 455/522
    //
    // @Inject private ObjectMapper objectMapper;
    // @Inject private RESTAPI restAPI;
    // @Inject private DB db;
    public static ObjectMapper objectMapper;
    public static RESTAPI restAPI;
    public static DB db;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userId = request.getPathInfo();
        if (userId != null)
            userId = userId.substring(1);
        if (userId == null)
            userId = request.getParameter("fb_sig_profile_user");
        if (userId == null)
            userId = request.getParameter("user");
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        RESTAPI.Info restapiInfo = restAPI.info(userId);
        if (restapiInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        request.setAttribute("restapiInfo", objectMapper.writeValueAsString(restapiInfo));
        LOG.fine("restapiInfo="+objectMapper.writeValueAsString(restapiInfo));

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (restapiInfo.tracks.length > 0) {
            for (String trackId : restapiInfo.tracks) {
                sb.append("\"").append(trackId).append("\":{\"pts\":");
                for (byte b : db.getTrack(userId, trackId))
                    sb.append((char) b);
                sb.append("},");
            }
            sb.setLength(Math.max(1,sb.length()-1));
        }
        sb.append("}");
        request.setAttribute("tracks", sb.toString());

        request.getRequestDispatcher("/WEB-INF/pages/publicPage.jsp").forward(request, response);
    }
}
