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

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.yrek.rideapp.data.DB;
import com.yrek.rideapp.facebook.User;
import com.yrek.rideapp.facebook.Users;

@Path("/")
public class RESTAPI {
    private static final Logger LOG = Logger.getLogger(RESTAPI.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    {
        objectMapper.getDeserializationConfig().disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    @Inject private DB db;

    public static class Point {
        public double lat;
        public double lon;
    }

    public static class Course {
        public String id;
        public String name;
        public boolean loop;
        public Point[] points;
    }

    public static class Info {
        @XmlElement public int maxTrackPoints;
        @XmlElement public int maxCoursePoints;
        @XmlElement public int maxTracks;
        @XmlElement public int maxCourses;
        @XmlElement public int maxRivals;
        @XmlElement public int maxNameLength;
        @XmlElement public ArrayList<Rival> rivals;
        @XmlElement public String[] tracks;
        @XmlElement public ArrayList<Course> courses;
        @XmlElement public UserInfo userInfo;
    }

    public static class Rival {
        @XmlElement public User user;
        @XmlElement public UserInfo userInfo;
        @XmlElement public String[] tracks;
        @XmlElement public ArrayList<Course> courses;
    }

    public static class UserInfo {
        @XmlElement public User user;
        @XmlElement public Point home;
    }

    @GET @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Info info(@Context HttpServletRequest request) throws IOException {
        Info result = limits(request);
        result.rivals = rivals(request);
        result.tracks = tracks(request);
        result.courses = courses(request);
        result.userInfo = userInfo(request);
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
        result.maxTrackPoints = db.getMaxTrackPoints(user.getId());
        result.maxCoursePoints = db.getMaxCoursePoints(user.getId());
        result.maxTracks = db.getMaxTracks(user.getId());
        result.maxCourses = db.getMaxCourses(user.getId());
        result.maxRivals = db.getMaxRivals(user.getId());
        result.maxNameLength = db.getMaxNameLength(user.getId());
        return result;
    }

    private UserInfo userInfo(User user, boolean create) throws IOException {
        byte[] bytes = db.getUserInfo(user.getId());
        if (bytes != null)
            return objectMapper.readValue(bytes, 0, bytes.length, UserInfo.class);
        if (!create)
            return null;
        UserInfo userInfo = new UserInfo();
        userInfo.user = user;
        db.setUserInfo(user.getId(), objectMapper.writeValueAsBytes(userInfo));
        return userInfo;
    }

    @GET @Path("/userInfo")
    @Produces(MediaType.APPLICATION_JSON)
    public UserInfo userInfo(@Context HttpServletRequest request) throws IOException {
        return userInfo(user(request), true);
    }

    @GET @Path("/home")
    @Produces(MediaType.APPLICATION_JSON)
    public Point home(@Context HttpServletRequest request) throws IOException {
        return userInfo(request).home;
    }

    @PUT @Path("/home")
    @Consumes(MediaType.APPLICATION_JSON)
    public void home(@Context HttpServletRequest request, Point home) throws IOException {
        User user = user(request);
        UserInfo userInfo = new UserInfo();
        userInfo.user = user;
        userInfo.home = home;
        db.setUserInfo(user.getId(), objectMapper.writeValueAsBytes(userInfo));
    }

    @GET @Path("/rivals")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<Rival> rivals(@Context HttpServletRequest request) throws IOException {
        User user = user(request);
        User[] friends = friends(request);
        String[] rivals = db.getRivals(user.getId());
        ArrayList<Rival> result = new ArrayList<Rival>();
        for (String id : rivals)
            for (User friend : friends)
                if (id.equals(friend.getId())) {
                    Rival rival = new Rival();
                    rival.user = friend;
                    rival.userInfo = userInfo(friend, false);
                    rival.tracks = db.listTracks(id);
                    rival.courses = courses(id);
                    result.add(rival);
                    break;
                }
        return result;
    }

    private boolean isFriend(HttpServletRequest request, String id) {
        User[] friends = friends(request);
        for (User friend : friends)
            if (id.equals(friend.getId()))
                return true;
        return false;
    }

    @POST @Path("/rival/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<Rival> addRival(@Context HttpServletRequest request, @PathParam("id") String id) throws IOException {
        if (isFriend(request, id))
            db.addRival(user(request).getId(), id);
        return rivals(request);
    }

    @DELETE @Path("/rival/{id}")
    public void removeRival(@Context HttpServletRequest request, @PathParam("id") String id) throws IOException {
        db.removeRival(user(request).getId(), id);
    }

    @GET @Path("/rival/{id}/track/{trackId}")
    @Produces(MediaType.APPLICATION_JSON)
    public byte[] getRivalTrack(@Context HttpServletRequest request, @PathParam("id") String id, @PathParam("trackId") String trackId) throws IOException {
        if (!isFriend(request, id))
            return null;
        return db.getTrack(id, trackId);
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

    private ArrayList<Course> courses(String userId) throws IOException {
        ArrayList<Course> courses = new ArrayList<Course>();
        for (String id : db.listCourses(userId)) {
            byte[] bytes = db.getCourse(userId, id);
            Course course = objectMapper.readValue(bytes, 0, bytes.length, Course.class);
            course.id = id;
            courses.add(course);
        }
        return courses;
    }

    @GET @Path("/courses")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<Course> courses(@Context HttpServletRequest request) throws IOException {
        return courses(user(request).getId());
    }

    @GET @Path("/course/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public byte[] course(@Context HttpServletRequest request, @PathParam("id") String id) {
        return db.getCourse(user(request).getId(), id);
    }

    private Course addCourse(String userId, Course course) throws IOException {
        int maxNameLength = db.getMaxNameLength(userId);
        if (course.name != null && course.name.length() > maxNameLength)
            course.name = course.name.substring(0, maxNameLength);
        int maxCoursePoints = db.getMaxCoursePoints(userId);
        if (course.points.length > maxCoursePoints) {
            Point[] points = new Point[maxCoursePoints];
            System.arraycopy(course.points, 0, points, 0, maxCoursePoints);
            course.points = points;
        }
        course.id = null;
        course.id = db.addCourse(userId, objectMapper.writeValueAsBytes(course));
        return course;
    }

    @POST @Path("/course")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Course addCourse(@Context HttpServletRequest request, Course course) throws IOException {
        return addCourse(user(request).getId(), course);
    }

    @POST @Path("/course/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Course addCourse(@Context HttpServletRequest request, @PathParam("id") String id, Course course) throws IOException {
        String userId = user(request).getId();
        db.deleteCourse(userId, id);
        return addCourse(userId, course);
    }

    @DELETE @Path("/course/{id}")
    public void deleteCourse(@Context HttpServletRequest request, @PathParam("id") String id) {
        db.deleteCourse(user(request).getId(), id);
    }
}
