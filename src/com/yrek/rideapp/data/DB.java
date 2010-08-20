package com.yrek.rideapp.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.yrek.rideapp.storage.Storage;

@Singleton
public class DB {
    private static final Logger LOG = Logger.getLogger(DB.class.getName());

    @Inject private Storage storage;

    private static final String TRACKS = "/trk/";
    private static final String COURSES = "/crs/";
    private static final String RIVALS = "/rvl";
    private static final String HOME = "/h";

    private static final SimpleDateFormat timestampFormat;
    static {
        timestampFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        timestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public int getMaxTrackPoints(String userId) {
        return 1000;
    }

    public int getMaxCoursePoints(String userId) {
        return 10;
    }

    public int getMaxTracks(String userId) {
        return 5;
    }

    public int getMaxCourses(String userId) {
        return 5;
    }

    public int getMaxRivals(String userId) {
        return 5;
    }

    public int getMaxNameLength(String userId) {
        return 50;
    }

    private String addFile(String userId, byte[] data, String type, int maxFiles) {
        String[] files = storage.listFiles(userId + type);
        if (files.length >= maxFiles)
            for (int i = 0; i < files.length - maxFiles + 1; i++)
                storage.deleteFile(userId + type + files[i]);
        String id = timestampFormat.format(new Date());
        storage.writeFile(userId + type + id, data);
        return id;
    }

    private String[] listFiles(String userId, String type) {
        return storage.listFiles(userId + type);
    }

    private byte[] getFile(String userId, String file, String type) {
        return storage.readFile(userId + type + file);
    }

    private void deleteFile(String userId, String file, String type) {
        storage.deleteFile(userId + type + file);
    }

    public String addTrack(String userId, byte[] data) {
        return addFile(userId, data, TRACKS, getMaxTracks(userId));
    }

    public String[] listTracks(String userId) {
        return listFiles(userId, TRACKS);
    }

    public byte[] getTrack(String userId, String track) {
        return getFile(userId, track, TRACKS);
    }

    public void deleteTrack(String userId, String track) {
        deleteFile(userId, track, TRACKS);
    }

    public String addCourse(String userId, byte[] data) {
        return addFile(userId, data, COURSES, getMaxCourses(userId));
    }

    public String[] listCourses(String userId) {
        return listFiles(userId, COURSES);
    }

    public byte[] getCourse(String userId, String course) {
        return getFile(userId, course, COURSES);
    }

    public void deleteCourse(String userId, String course) {
        deleteFile(userId, course, COURSES);
    }

    public String[] getRivals(String userId) {
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        byte[] rvl = storage.readFile(userId + RIVALS);
        if (rvl == null)
            return new String[0];
        for (byte b : rvl)
            if (b == ',') {
                if (sb.length() > 0)
                    list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append((char) b);
            }
        if (sb.length() > 0)
            list.add(sb.toString());
        return list.toArray(new String[list.size()]);
    }

    public void addRival(String userId, String rival) throws IOException {
        String[] rivals = getRivals(userId);
        for (String r : rivals)
            if (rival.equals(r))
                return;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = Math.max(0, rivals.length + 1 - getMaxRivals(userId)); i < rivals.length; i++) {
            bytes.write(rivals[i].getBytes());
            bytes.write(',');
        }
        bytes.write(rival.getBytes());
        storage.writeFile(userId + RIVALS, bytes.toByteArray());
    }

    public void removeRival(String userId, String rival) throws IOException {
        String[] rivals = getRivals(userId);
        int index = -1;
        for (int i = 0; i < rivals.length; i++)
            if (rival.equals(rivals[i])) {
                index = i;
                break;
            }
        if (index < 0)
            return;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean comma = false;
        for (int i = 0; i < rivals.length; i++)
            if (i != index) {
                if (comma)
                    bytes.write(',');
                comma = true;
                bytes.write(rivals[i].getBytes());
            }
        storage.writeFile(userId + RIVALS, bytes.toByteArray());
    }

    public byte[] getHome(String userId) {
        return storage.readFile(userId + HOME);
    }

    public void setHome(String userId, byte[] home) {
        storage.writeFile(userId + HOME, home);
    }
}
