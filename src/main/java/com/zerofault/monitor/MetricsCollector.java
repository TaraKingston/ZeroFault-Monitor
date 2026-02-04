package com.zerofault.monitor;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

public class MetricsCollector {

    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private long[] prevTicks;

    public MetricsCollector() {
        SystemInfo si = new SystemInfo();
        processor = si.getHardware().getProcessor();
        memory = si.getHardware().getMemory();
        prevTicks = processor.getSystemCpuLoadTicks();
    }

    public double getCpuUsage() {
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0;
        prevTicks = processor.getSystemCpuLoadTicks();
        return load;
    }

    public double getMemoryUsage() {
        long total = memory.getTotal();
        long available = memory.getAvailable();
        return (1.0 - (double) available / total) * 100.0;
    }
}
