package com.zerofault.monitor;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;
import oshi.hardware.NetworkIF;
import java.util.List;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import java.util.Map;
import java.util.LinkedHashMap;


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

        //collector now has access to disk processes and network interfaces
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

    public Map<String, Double> getDiskUsagePercentPerDrive() {
        FileSystem fs = os.getFileSystem();
        Map<String, Double> result = new LinkedHashMap<>();

        for (OSFileStore store : fs.getFileStores()) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            if (total <= 0) continue; // ignore weird/virtual entries

            double usedPct = (1.0 - (double) usable / total) * 100.0;

            String drive = store.getMount(); // usually "C:\"
            if (drive == null || drive.isBlank()) drive = store.getName();

            result.put(drive, usedPct);
        }
        return result;
    }
}
