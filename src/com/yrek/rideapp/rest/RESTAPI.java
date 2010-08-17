package com.yrek.rideapp.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;

import com.google.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;

import com.yrek.rideapp.data.DB;
import com.yrek.rideapp.facebook.User;
import com.yrek.rideapp.facebook.Users;

@Path("/")
public class RESTAPI {
    private static final Logger LOG = Logger.getLogger(RESTAPI.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject private DB db;

    public static class Point {
        public double lat;
        public double lon;
    }

    public static class Info {
        @XmlElement public int maxPoints;
        @XmlElement public int maxTracks;
        @XmlElement public int maxCourses;
        @XmlElement public int maxRivals;
        @XmlElement public ArrayList<User> rivals;
        @XmlElement public String[] tracks;
        @XmlElement public String[] courses;
        @XmlElement public Point home;
    }

    @GET @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Info info(@Context HttpServletRequest request) throws IOException {
        Info result = limits(request);
        result.rivals = rivals(request);
        result.tracks = tracks(request);
        result.courses = courses(request);
        result.home = home(request);
        return result;
    }

    @GET @Path("/user")
    @Produces(MediaType.APPLICATION_JSON)
    public User user(@Context HttpServletRequest request) {
        return (User) request.getSession().getAttribute("user");
    }

    @GET @Path("/friends")
    @Produces(MediaType.APPLICATION_JSON)
    public User[] friends(@Context HttpServletRequest request) {
        return ((Users) request.getSession().getAttribute("friends")).getData();
    }

    @GET @Path("/limits")
    @Produces(MediaType.APPLICATION_JSON)
    public Info limits(@Context HttpServletRequest request) {
        User user = user(request);
        Info result = new Info();
        result.maxPoints = db.getMaxPoints(user.getId());
        result.maxTracks = db.getMaxTracks(user.getId());
        result.maxCourses = db.getMaxCourses(user.getId());
        result.maxRivals = db.getMaxRivals(user.getId());
        return result;
    }

    @GET @Path("/home")
    @Produces(MediaType.APPLICATION_JSON)
    public Point home(@Context HttpServletRequest request) throws IOException {
        User user = user(request);
        byte[] home = db.getHome(user.getId());
        if (home == null)
            return null;
        return objectMapper.readValue(home, 0, home.length, Point.class);
    }

    @PUT @Path("/home")
    @Consumes(MediaType.APPLICATION_JSON)
    public void home(@Context HttpServletRequest request, Point home) throws IOException {
        db.setHome(user(request).getId(), objectMapper.writeValueAsBytes(home));
    }

    @GET @Path("/rivals")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<User> rivals(@Context HttpServletRequest request) {
        User user = user(request);
        User[] friends = friends(request);
        String[] rivals = db.getRivals(user.getId());
        ArrayList<User> result = new ArrayList<User>();
        for (String id : rivals)
            for (User friend : friends)
                if (id.equals(friend.getId())) {
                    result.add(friend);
                    break;
                }
        return result;
    }

    @POST @Path("/rival/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<User> addRival(@Context HttpServletRequest request, @PathParam("id") String id) throws IOException {
        User user = user(request);
        User[] friends = friends(request);
        for (User friend : friends)
            if (id.equals(friend.getId())) {
                db.addRival(user.getId(), id);
                break;
            }
        return rivals(request);
    }

    @DELETE @Path("/rival/{id}")
    public void removeRival(@Context HttpServletRequest request, @PathParam("id") String id) throws IOException {
        db.removeRival(user(request).getId(), id);
    }

    @GET @Path("/tracks")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] tracks(@Context HttpServletRequest request) {
        return db.listTracks(user(request).getId());
    }

    @GET @Path("/track/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public byte[] track(@Context HttpServletRequest request, @PathParam("id") String id) {
        return db.getTrack(user(request).getId(), id);
    }

    @DELETE @Path("/track/{id}")
    public void deleteTrack(@Context HttpServletRequest request, @PathParam("id") String id) {
        db.deleteTrack(user(request).getId(), id);
    }

    public static class Course {
        public String description;
        public Point[] points;
    }

    @GET @Path("/courses")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] courses(@Context HttpServletRequest request) {
        return db.listCourses(user(request).getId());
    }

    @GET @Path("/course/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public byte[] course(@Context HttpServletRequest request, @PathParam("id") String id) {
        return db.getCourse(user(request).getId(), id);
    }

    @POST @Path("/course")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String[] addCourse(@Context HttpServletRequest request, Course course) throws IOException {
        db.addCourse(user(request).getId(), objectMapper.writeValueAsBytes(course));
        return courses(request);
    }

    @DELETE @Path("/course/{id}")
    public void deleteCourse(@Context HttpServletRequest request, @PathParam("id") String id) {
        db.deleteCourse(user(request).getId(), id);
    }
}
