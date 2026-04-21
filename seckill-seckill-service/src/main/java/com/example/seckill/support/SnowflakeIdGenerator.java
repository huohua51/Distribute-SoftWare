package com.example.seckill.support;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final AtomicLong sequence = new AtomicLong(0L);

    @Value("${app.snowflake.worker-id:-1}")
    private long workerId;

    @Value("${app.snowflake.datacenter-id:1}")
    private long datacenterId;

    private volatile long lastTimestamp = -1L;

    @PostConstruct
    public void init() {
        if (workerId < 0) {
            workerId = buildWorkerIdFromHost();
        }
        if (workerId > MAX_WORKER_ID || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("Snowflake workerId/datacenterId out of range");
        }
    }

    public synchronized long nextId() {
        long currentTimestamp = currentTimestamp();
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards, refusing to generate id");
        }

        if (currentTimestamp == lastTimestamp) {
            long nextSequence = (sequence.incrementAndGet()) & SEQUENCE_MASK;
            if (nextSequence == 0L) {
                currentTimestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence.set(0L);
        }

        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence.get();
    }

    private long waitUntilNextMillis(long timestamp) {
        long current = currentTimestamp();
        while (current <= timestamp) {
            current = currentTimestamp();
        }
        sequence.set(0L);
        return current;
    }

    private long currentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    private long buildWorkerIdFromHost() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            long hash = Math.abs(hostName.hashCode());
            return hash % (MAX_WORKER_ID + 1);
        } catch (UnknownHostException ex) {
            return 1L;
        }
    }
}
