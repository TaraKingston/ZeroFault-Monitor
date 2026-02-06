package com.zerofault.monitor;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;
import oshi.hardware.NetworkIF;
import java.util.List;

public class MetricsCollector {

    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private long[] prevTicks;
    private final OperatingSystem os;     // stores gives disk + processes
    private final List<NetworkIF> nifs;   // stores list of network adapters

    // prev counters so we can compute KB/s rates
    private long prevRXBytes = -1;
    private long prevTXBytes = -1;
    private long prevNETTimeMs = -1;

    public MetricsCollector() {
        SystemInfo si = new SystemInfo();
        processor = si.getHardware().getProcessor();
        memory = si.getHardware().getMemory();
        prevTicks = processor.getSystemCpuLoadTicks();
        os = si.getOperatingSystem();
        nifs = si.getHardware().getNetworkIFs();
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
