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

    private static final SimpleDateFormat timestampFormat;
    static {
        timestampFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        timestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public int getMaxPoints(String userId) {
        return 1000;
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

    private void addFile(String userId, byte[] data, String type, int maxFiles) {
        String[] files = storage.listFiles(userId + type);
        if (files.length >= maxFiles)
            for (int i = 0; i < files.length - maxFiles + 1; i++)
                storage.deleteFile(userId + type + files[i]);
        storage.writeFile(userId + type + timestampFormat.format(new Date()), data);
    }

    private String[] listFiles(String userId, String type) {
        return storage.listFiles(userId + type);
    }

    public byte[] getFile(String userId, String file, String type) {
        return storage.readFile(userId + type + file);
    }

    public void addTrack(String userId, byte[] data) {
        addFile(userId, data, "/trk/", getMaxTracks(userId));
    }

    public String[] listTracks(String userId) {
        return listFiles(userId, "/trk/");
    }

    public byte[] getTrack(String userId, String track) {
        return getFile(userId, track, "/trk/");
    }

    public void addCourse(String userId, byte[] data) {
        addFile(userId, data, "/crs/", getMaxCourses(userId));
    }

    public String[] listCourses(String userId) {
        return listFiles(userId, "/crs/");
    }

    public byte[] getCourse(String userId, String track) {
        return getFile(userId, track, "/crs/");
    }

    public String[] getRivals(String userId) {
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (byte b : storage.readFile(userId + "/rvl"))
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
        storage.writeFile(userId + "/rvl", bytes.toByteArray());
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
        storage.writeFile(userId + "/rvl", bytes.toByteArray());
    }
}
