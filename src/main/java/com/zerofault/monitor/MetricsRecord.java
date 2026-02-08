package com.zerofault.monitor;

import java.time.Instant;
// this class takes a snapshot of the system matrics at a certain point in time to that it can be then exported to the
//csv file to be read from there confirmimg fault/ no fault
public record MetricsRecord(
        Instant timestamp,    // When was this snapshot taken
        double CPUNow,        // Current CPU usage in between 0-100%
        double CPUAvg,      // Average CPU over last 60 seconds
        double RAMNow,        // Current RAM usage in between 0-100%
        double RAMAvg,      // Average RAM over last 60 seconds
        double downKBps,      // Download speed in KB per second
        double upKBps         // Upload speed in KB per second
) {

    public MetricsRecord {
        // CPU can exceed 100% on multi-core systems
        if (CPUNow < 0 || CPUNow > 200) {
            throw new IllegalArgumentException("Invalid CPU: " + CPUNow);
        }

        // RAM should be 0-100%
        if (RAMNow < 0 || RAMNow > 100) {
            throw new IllegalArgumentException("Invalid RAM: " + RAMNow);
        }

        // Network speeds can't be negative
        if (downKBps < 0 || upKBps < 0) {
            throw new IllegalArgumentException("Network speeds cannot be negative");
        }
    }
    //records the timestamp/ snapshot to the csv file
    public String toCSV() {
        return String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                timestamp.toString(),
                CPUNow, CPUAvg,
                RAMNow, RAMAvg,
                downKBps, upKBps
        );
    }
}