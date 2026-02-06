package com.zerofault.monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    //documents the alerts give name
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void alert(String message) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] ALERT: " + message);
    }
    public static void main(String[] args) throws InterruptedException {

        // This object talks to OSHI and reads CPU/RAM data
        MetricsCollector collector = new MetricsCollector();

        //60 readings (60 seconds at 1 sample/sec)
        MoveAvgWindow CPUWindow = new MoveAvgWindow(60);
        MoveAvgWindow RAMWindow = new MoveAvgWindow(60);

        final double CPU_SPIKE_DELTA = 30.0;  // current > avg + 30%
        final int CPU_SPIKE_SECONDS = 3;  // must happen 3 seconds in a row
        final long ALERT_COOLDOWN_MS = 10_000;  // 10 seconds between same alert

        final double RAM_RISE_OVER_WINDOW = 5.0; // +5% RAM over last 60 seconds


        int cpuSpikeCount = 0;
        long lastCpuAlertTime = 0;
        long lastRamAlertTime = 0;
        while (true) {

            //Collect current values from the system, reads the current metrics
            double CurrentCPU = collector.getCpuUsage();      // CPU usage right now
            double CurrentRAM = collector.getMemoryUsage();   // RAM usage right now

            //Add values to history windows
            // This stores the reading and automatically drops old ones
            CPUWindow.add(CurrentCPU);
            RAMWindow.add(CurrentRAM);

            //Compute baseline averages
            //These represent the average behavior over the last minute
            double CPUAvg = CPUWindow.getAvg();
            double RAMAvg = RAMWindow.getAvg();

            //Display current vs historical average
            System.out.printf(
                    "CPU: %.2f%% (avg60s: %.2f%%) | RAM: %.2f%% (avg60s: %.2f%%) | samples: %d/60%n",
                    CurrentCPU, CPUAvg,
                    CurrentRAM, RAMAvg,
                    CPUWindow.size() // how full the window is
            );

            boolean warmedUp = CPUWindow.size() >= 60 && RAMWindow.size() >= 60;

            if (warmedUp) {
                long now = System.currentTimeMillis();

                boolean cpuSpike = CurrentCPU > (CPUAvg + CPU_SPIKE_DELTA);

                if (cpuSpike) {
                    cpuSpikeCount++;
                } else {
                    cpuSpikeCount = 0;
                }

                if (cpuSpikeCount >= CPU_SPIKE_SECONDS) {
                    if (now - lastCpuAlertTime >= ALERT_COOLDOWN_MS) {
                        alert(String.format("CPU spike detected: now=%.2f%% avg60s=%.2f%% (+%.2f%%)",
                                CurrentCPU, CPUAvg, (CurrentCPU - CPUAvg)));
                        lastCpuAlertTime = now;
                    }
                    cpuSpikeCount = CPU_SPIKE_SECONDS;
                }
                // Compare latest RAM to the oldest RAM in the 60-sec window seeking a pattern
                double ramOldest = RAMWindow.oldest();
                double ramLatest = RAMWindow.latest();
                double ramRise = ramLatest - ramOldest;

                if (ramRise >= RAM_RISE_OVER_WINDOW) {
                    if (now - lastRamAlertTime >= ALERT_COOLDOWN_MS) {
                        alert(String.format("Possible memory leak trend: RAM rose %.2f%% over last 60s (old=%.2f%% new=%.2f%% avg=%.2f%%)",
                                ramRise, ramOldest, ramLatest, RAMAvg));
                        lastRamAlertTime = now;
                    }
                }
            }


            // Wait 1 second before the next reading
            Thread.sleep(1000);
        }
    }
}