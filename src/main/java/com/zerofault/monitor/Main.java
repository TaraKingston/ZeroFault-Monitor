package com.zerofault.monitor;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // This object talks to OSHI and reads CPU/RAM data
        MetricsCollector collector = new MetricsCollector();

        //60 readings (60 seconds at 1 sample/sec)
        MoveAvgWindow CPUWindow = new MoveAvgWindow(60);
        MoveAvgWindow RAMWindow = new MoveAvgWindow(60);

        while (true) {

            //Collect current values from the system
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

            // Wait 1 second before the next reading
            Thread.sleep(1000);
        }
    }
}