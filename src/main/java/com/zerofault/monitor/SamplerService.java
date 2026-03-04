package com.zerofault.monitor;

import oshi.software.os.OSProcess;

import java.time.Instant;
import java.util.List;

public class SamplerService implements Runnable {

    private final MetricsCollector collector;
    private final MetricsStore store;
    private final MonitorConfig cfg;

    private final MoveAvgWindow cpuWindow = new MoveAvgWindow(60);
    private final MoveAvgWindow ramWindow = new MoveAvgWindow(60);

    private int cpuSpikeCount = 0;
    private long lastCpuAlertMs = 0;
    private long lastRamAlertMs = 0;

    private volatile boolean running = true;
    private int tick = 0;

    public SamplerService(MetricsCollector collector, MetricsStore store, MonitorConfig cfg) {
        this.collector = collector;
        this.store = store;
        this.cfg = cfg;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            tick++;

            double cpuNow = collector.getCpuUsage();
            double ramNow = collector.getMemoryUsage();

            cpuWindow.add(cpuNow);
            ramWindow.add(ramNow);

            double cpuAvg = cpuWindow.getAvg();
            double ramAvg = ramWindow.getAvg();

            // Network each second
            double[] net = collector.getNetworkKBps();
            double downKBps = net[0];
            double upKBps = net[1];

            store.addSnapshot(new MetricsRecord(
                    Instant.now(),
                    cpuNow, cpuAvg,
                    ramNow, ramAvg,
                    downKBps, upKBps
            ));

            // Alerts only after window full
            boolean warmedUp = cpuWindow.size() >= 60 && ramWindow.size() >= 60;
            if (warmedUp) {
                long now = System.currentTimeMillis();

                // CPU spike sustained
                if (cpuNow > cpuAvg + cfg.cpuSpikeDelta) cpuSpikeCount++;
                else cpuSpikeCount = 0;

                if (cpuSpikeCount >= cfg.cpuSpikeSeconds && now - lastCpuAlertMs >= cfg.alertCooldownMs) {
                    store.addAlert(String.format(
                            "[%s] CPU spike: now=%.2f avg=%.2f (+%.2f)",
                            Instant.now(), cpuNow, cpuAvg, (cpuNow - cpuAvg)
                    ));
                    lastCpuAlertMs = now;
                }

                // RAM trend across the 60s window
                double rise = ramWindow.latest() - ramWindow.oldest();
                if (rise >= cfg.ramRiseThreshold && now - lastRamAlertMs >= cfg.alertCooldownMs) {
                    store.addAlert(String.format(
                            "[%s] RAM rising: +%.2f%% over 60s (old=%.2f new=%.2f)",
                            Instant.now(), rise, ramWindow.oldest(), ramWindow.latest()
                    ));
                    lastRamAlertMs = now;
                }
            }

            // Processes every 5 seconds
            if (tick % 5 == 0) {
                List<OSProcess> topCpu = collector.getTopCpuProcesses(10);
                List<OSProcess> topMem = collector.getTopMemoryProcesses(10);
                store.setTopProcesses(topCpu, topMem);
            }

            try {
                Thread.sleep(cfg.sampleIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
