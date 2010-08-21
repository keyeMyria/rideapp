package com.yrek.rideapp.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class FileStorage implements Storage {
    private static final Logger LOG = Logger.getLogger(FileStorage.class.getName());

    private final File rootDir;

    public FileStorage(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public String[] listFiles(String dir) {
        File[] files = new File(rootDir, dir).listFiles();
        if (files == null)
            return new String[0];
        String[] list = new String[files.length];
        for (int i = 0; i < files.length; i++)
            list[i] = files[i].getName();
        return list;
    }

    @Override
    public byte[] readFile(String path) {
        File file = new File(rootDir, path);
        if (!file.isFile())
            return null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0)
                bytes.write(buffer, 0, count);
            in.close();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String path) {
        new File(rootDir, path).delete();
    }

    @Override
    public void writeFile(String path, byte[] content) {
        File file = new File(rootDir, path);
        File dir = file.getParentFile();
        if (!dir.isDirectory())
            dir.mkdirs();
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(content);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
