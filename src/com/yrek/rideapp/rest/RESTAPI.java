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

    public static class Info {
        @XmlElement public int maxPoints;
        @XmlElement public int maxTracks;
        @XmlElement public int maxCourses;
        @XmlElement public int maxRivals;
        @XmlElement public ArrayList<User> rivals;
        @XmlElement public String[] tracks;
        @XmlElement public String[] courses;
    }

    @GET @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Info info(@Context HttpServletRequest request) {
        Info result = limits(request);
        result.rivals = rivals(request);
        result.tracks = tracks(request);
        result.courses = courses(request);
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

    @PUT @Path("/rival/{id}")
    public void addRival(@Context HttpServletRequest request, @PathParam("id") String id) throws IOException {
        User user = user(request);
        User[] friends = friends(request);
        for (User friend : friends)
            if (id.equals(friend.getId())) {
                db.addRival(user.getId(), id);
                break;
            }
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

    public static class CoursePoint {
        public double lat;
        public double lon;
    }

    public static class Course {
        public String description;
        public CoursePoint[] points;
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
    public void addCourse(@Context HttpServletRequest request, Course course) throws IOException {
        db.addCourse(user(request).getId(), objectMapper.writeValueAsBytes(course));
    }

    @DELETE @Path("/course/{id}")
    public void deleteCourse(@Context HttpServletRequest request, @PathParam("id") String id) {
        db.deleteCourse(user(request).getId(), id);
    }
}
