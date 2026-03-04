package com.zerofault.monitor;

public class AgentMain {

    public static void main(String[] args) throws Exception {
        MonitorConfig cfg = MonitorConfig.load();

        MetricsStore store = new MetricsStore(cfg.storeCapacity);
        MetricsCollector collector = new MetricsCollector();

        SamplerService sampler = new SamplerService(collector, store, cfg);
        Thread samplerThread = new Thread(sampler, "sampler");
        samplerThread.setDaemon(true);
        samplerThread.start();

        ApiServer api = new ApiServer(store);
        api.start(cfg.apiPort);

        MetricsCSV exporter;
        Thread exportThread;

        if (cfg.exportCsvEnabled) {
            exporter = new MetricsCSV(cfg.exportCsvPath, cfg.exportCsvFlushEvery);
            exporter.start();

            MetricsCSV finalExporter = exporter;
            exportThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    MetricsRecord r = store.getLatestOrNull();
                    if (r != null) finalExporter.append(r);
                    try { Thread.sleep(cfg.sampleIntervalMs); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }, "exporter");

            exportThread.setDaemon(true);
            exportThread.start();
        } else {
            exportThread = null;
            exporter = null;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                api.stop();
                sampler.stop(); // your sampler should have stop()
                if (exporter != null) exporter.stop();
                if (exportThread != null) exportThread.interrupt();
            } catch (Exception ignored) {}
        }));

        System.out.println("ZeroFault Agent running:");
        System.out.println("  Latest:      http://localhost:" + cfg.apiPort + "/metrics/latest");
        System.out.println("  History:     http://localhost:" + cfg.apiPort + "/metrics/history?seconds=60");
        System.out.println("  Alerts:      http://localhost:" + cfg.apiPort + "/alerts");
        System.out.println("  Prometheus:  http://localhost:" + cfg.apiPort + "/metrics/prometheus");
        System.out.println("CSV Export: " + (cfg.exportCsvEnabled ? cfg.exportCsvPath : "disabled"));
        System.out.println("Press Ctrl+C to stop.");

        // keep alive
        samplerThread.join();
    }
}
