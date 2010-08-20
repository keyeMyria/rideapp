package com.yrek.rideapp.storage;

import java.io.Serializable;

import net.spy.memcached.MemcachedClient;

public abstract class BaseMemcachedStorage implements Storage {
    private final Storage storage;
    private final String keyPrefix;
    private final int expirationTime;

    protected BaseMemcachedStorage(Storage storage, String keyPrefix, int expirationTime) {
        this.storage = storage;
        this.keyPrefix = keyPrefix;
        this.expirationTime = expirationTime;
    }

    public static class CacheItem<T> implements Serializable {
        private static final long serialVersionUID = 0L;

        private final T item;

        public CacheItem(T item) {
            this.item = item;
        }

        public T getItem() {
            return item;
        }
    }

    protected abstract MemcachedClient getMemcachedClient();

    private String getDir(String path) {
        return path.substring(0, path.lastIndexOf('/') + 1);
    }

    private <T> CacheItem<T> get(String key, Class<T> type) {
        @SuppressWarnings("unchecked")
        CacheItem<T> cached = (CacheItem<T>) getMemcachedClient().get(key);
        return cached;
    }

    @Override
    public String[] listFiles(String dir) {
        String key = keyPrefix + dir;
        CacheItem<String[]> cached = get(key, String[].class);
        if (cached != null)
            return cached.getItem();
        String[] files = storage.listFiles(dir);
        getMemcachedClient().set(key, expirationTime, new CacheItem<String[]>(files));
        return files;
    }

    @Override
    public byte[] readFile(String path) {
        String key = keyPrefix + path;
        CacheItem<byte[]> cached = get(key, byte[].class);
        if (cached != null)
            return cached.getItem();
        byte[] file = storage.readFile(path);
        getMemcachedClient().set(key, expirationTime, new CacheItem<byte[]>(file));
        return file;
    }

    @Override
    public void deleteFile(String path) {
        String key = keyPrefix + path;
        String dirKey = keyPrefix + getDir(path);
        storage.deleteFile(path);
        getMemcachedClient().set(key, expirationTime, new CacheItem<byte[]>(null));
        getMemcachedClient().delete(dirKey);
    }

    @Override
    public void writeFile(String path, byte[] content) {
        String key = keyPrefix + path;
        String dirKey = keyPrefix + getDir(path);
        storage.writeFile(path, content);
        getMemcachedClient().set(key, expirationTime, new CacheItem<byte[]>(content));
        getMemcachedClient().delete(dirKey);
    }
}
