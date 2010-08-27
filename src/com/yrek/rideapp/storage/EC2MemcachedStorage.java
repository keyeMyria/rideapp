package com.yrek.rideapp.storage;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;

public class EC2MemcachedStorage  extends BaseMemcachedStorage implements Closeable, Storage {
    private static final Logger LOG = Logger.getLogger(EC2MemcachedStorage.class.getName());

    private final AmazonEC2 amazonEC2;
    private final String securityGroup;
    private final int memcachedPort;
    private final long pollInterval;

    private final Thread pollThread;

    private MemcachedClient memcachedClient;
    private ArrayList<InetSocketAddress> addresses;

    public EC2MemcachedStorage(Storage storage, String keyPrefix, int expirationTime, AmazonEC2 amazonEC2, String securityGroup, int memcachedPort, int pollIntervalMinutes) throws IOException {
        super(storage, keyPrefix, expirationTime);
        this.amazonEC2 = amazonEC2;
        this.securityGroup = securityGroup;
        this.memcachedPort = memcachedPort;
        this.pollInterval = pollIntervalMinutes*60000L;

        System.setProperty("net.spy.log.LoggerImpl","net.spy.memcached.compat.log.SunLogger");
        addresses = getAddresses();
        memcachedClient = newMemcachedClient();

        if (pollIntervalMinutes <= 0) {
            pollThread = null;
        } else {
            pollThread = new Thread() {
                @Override
                public void run() {
                    while (!isInterrupted()) {
                        MemcachedClient oldClient = null;
                        try {
                            ArrayList<InetSocketAddress> newAddresses = getAddresses();
                            if (!addresses.equals(newAddresses)) {
                                oldClient = memcachedClient;
                                addresses = newAddresses;
                                memcachedClient = newMemcachedClient();
                            }
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE,"",e);
                        }
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException e) {
                        }
                        if (oldClient != null)
                            oldClient.shutdown();
                    }
                    memcachedClient.shutdown();
                }
            };
            pollThread.start();
        }
    }

    private ArrayList<InetSocketAddress> getAddresses() {
        ArrayList<InetSocketAddress> list = new ArrayList<InetSocketAddress>();
        for (Reservation reservation : amazonEC2.describeInstances().getReservations()) {
            if (!reservation.getGroupNames().contains(securityGroup))
                continue;
            for (Instance instance : reservation.getInstances()) {
                String ipAddress = instance.getPrivateIpAddress();
                if (ipAddress != null)
                    list.add(new InetSocketAddress(ipAddress, memcachedPort));
            }
        }
        LOG.fine("addresses="+addresses);
        return list;
    }

    private MemcachedClient newMemcachedClient() throws IOException {
        return new MemcachedClient(new DefaultConnectionFactory(DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN, DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE, HashAlgorithm.KETAMA_HASH), addresses);
    }

    @Override
    protected MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }

    @Override
    public void close() {
        if (pollThread != null)
            try {
                pollThread.interrupt();
                pollThread.join(2000L);
            } catch (InterruptedException e) {
            }
    }
}
