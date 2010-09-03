package com.yrek.rideapp.storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;

public class JCacheStorage implements Storage {
    private static final Logger LOG = Logger.getLogger(JCacheStorage.class.getName());

    private final Storage storage;
    private final Cache cache;

    public JCacheStorage(Storage storage) throws CacheException {
        this.storage = storage;
        this.cache = CacheManager.getInstance().getCacheFactory().createCache(new HashMap());
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

    private String getDir(String path) {
        return path.substring(0, path.lastIndexOf('/') + 1);
    }

    private <T> CacheItem<T> get(String key, Class<T> type) {
        @SuppressWarnings("unchecked")
        CacheItem<T> cached = (CacheItem<T>) cache.get(key);
        return cached;
    }

    @Override
    public String[] listFiles(String dir) {
        CacheItem<String[]> cached = get(dir, String[].class);
        if (cached != null)
            return cached.getItem();
        String[] files = storage.listFiles(dir);
        cache.put(dir, new CacheItem<String[]>(files));
        return files;
    }

    @Override
    public byte[] readFile(String path) {
        CacheItem<byte[]> cached = get(path, byte[].class);
        if (cached != null)
            return cached.getItem();
        byte[] file = storage.readFile(path);
        cache.put(path, new CacheItem<byte[]>(file));
        return file;
    }

    @Override
    public void deleteFile(String path) {
        storage.deleteFile(path);
        cache.put(path, new CacheItem<byte[]>(null));
        cache.remove(getDir(path));
    }

    @Override
    public void writeFile(String path, byte[] content) {
        storage.writeFile(path, content);
        cache.put(path, new CacheItem<byte[]>(content));
        cache.remove(getDir(path));
    }
}
