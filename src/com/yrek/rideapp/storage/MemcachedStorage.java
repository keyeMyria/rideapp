package com.yrek.rideapp.storage;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;

public class MemcachedStorage  extends BaseMemcachedStorage implements Closeable, Storage {
    private final MemcachedClient memcachedClient;

    public MemcachedStorage(Storage storage, String keyPrefix, int expirationTime, List<InetSocketAddress> addresses) throws IOException {
        super(storage, keyPrefix, expirationTime);
        System.setProperty("net.spy.log.LoggerImpl","net.spy.memcached.compat.log.SunLogger");
        memcachedClient = new MemcachedClient(new DefaultConnectionFactory(DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN, DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE, HashAlgorithm.KETAMA_HASH), addresses);
    }

    @Override
    protected MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }

    @Override
    public void close() {
        memcachedClient.shutdown();
    }
}
