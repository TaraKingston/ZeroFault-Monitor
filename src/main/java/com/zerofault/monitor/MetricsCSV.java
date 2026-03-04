package com.zerofault.monitor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class MetricsCSV {
    private final Path path;
    private BufferedWriter out;
    private int sinceFlush = 0;
    private final int flushEvery;

    public MetricsCSV(String filePath, int flushEvery) {
        this.path = Path.of(filePath);
        this.flushEvery = Math.max(1, flushEvery);
    }

    public void start() throws IOException {
        boolean exists = java.nio.file.Files.exists(path);
        out = new BufferedWriter(new FileWriter(path.toFile(), true));
        if (!exists) {
            out.write("timestamp,cpuNow,cpuAvg60,ramNow,ramAvg60,downKBps,upKBps");
            out.newLine();
            out.flush();
        }
    }

    public synchronized void append(MetricsRecord r) {
        if (out == null || r == null) return;
        try {
            out.write(String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                    r.timestamp().toString(),
                    r.cpuNow(), r.cpuAvg60(),
                    r.ramNow(), r.ramAvg60(),
                    r.downKBps(), r.upKBps()
            ));
            out.newLine();
            sinceFlush++;
            if (sinceFlush >= flushEvery) {
                out.flush();
                sinceFlush = 0;
            }
        } catch (IOException ignored) {}
    }

    public synchronized void stop() {
        try { if (out != null) out.close(); }
        catch (IOException ignored) {}
        out = null;
    }
}
