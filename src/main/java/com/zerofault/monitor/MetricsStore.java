package com.zerofault.monitor;

import oshi.software.os.OSProcess;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MetricsStore {

    private final int capacity;
    private final Deque<MetricsRecord> ring = new ArrayDeque<>();
    private final Deque<String> alert = new ArrayDeque<>(); // simple text alerts
    private final int alertCapacity = 50;

    private List<OSProcess> topCpu = List.of();
    private List<OSProcess> topMem = List.of();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MetricsStore(int capacity) {
        this.capacity = capacity;
    }

    public void addSnapshot(MetricsRecord s) {
        lock.writeLock().lock();
        try {
            ring.addLast(s);
            if (ring.size() > capacity) ring.removeFirst();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<MetricsRecord> getLatestN(int n) {
        lock.readLock().lock();
        try {
            int size = ring.size();
            int start = Math.max(0, size - n);
            List<MetricsRecord> out = new ArrayList<>(Math.min(n, size));
            int i = 0;
            for (MetricsRecord s : ring) {
                if (i++ >= start) out.add(s);
            }
            return out;
        } finally {
            lock.readLock().unlock();
        }
    }

    public MetricsRecord getLatestOrNull() {
        lock.readLock().lock();
        try {
            return ring.peekLast();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addAlert(String message) {
        lock.writeLock().lock();
        try {
            alert.addLast(message);
            while (alert.size() > alertCapacity) alert.removeFirst();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getAlerts() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(alert);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTopProcesses(List<OSProcess> topCpu, List<OSProcess> topMem) {
        lock.writeLock().lock();
        try {
            this.topCpu = topCpu == null ? List.of() : List.copyOf(topCpu);
            this.topMem = topMem == null ? List.of() : List.copyOf(topMem);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<OSProcess> getTopCpu() {
        lock.readLock().lock();
        try {
            return topCpu;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<OSProcess> getTopMem() {
        lock.readLock().lock();
        try {
            return topMem;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int capacity() {
        return capacity;
    }
}

