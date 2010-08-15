package com.yrek.rideapp.rest;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;

import com.yrek.rideapp.facebook.User;
import com.yrek.rideapp.facebook.Users;

@Path("/")
public class RESTAPI {
    private static final Logger LOG = Logger.getLogger(RESTAPI.class.getName());

    @GET @Path("/friends")
    @Produces(MediaType.APPLICATION_JSON)
    public User[] friends(@Context HttpServletRequest request) {
        return ((Users) request.getSession().getAttribute("friends")).getData();
    }
}
