package com.yrek.rideapp.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

@Singleton
public class PingServlet extends HttpServlet {
    private static final long serialVersionUID = 0L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
