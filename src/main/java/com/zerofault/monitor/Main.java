package com.zerofault.monitor;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        MetricsCollector collector = new MetricsCollector();

        while (true) {
            System.out.printf("CPU: %.2f%% | RAM: %.2f%%%n",
                    collector.getCpuUsage(),
                    collector.getMemoryUsage());
            Thread.sleep(1000);
        }
    }
}
