package com.yrek.rideapp.storage;

public interface Storage {
    public String[] listFiles(String dir);
    public byte[] readFile(String path);
    public void deleteFile(String path);
    public void writeFile(String path, byte[] content);
}
