package com.zerofault.monitor;

import java.io.InputStream;
import java.util.Properties;

public class MonitorConfig {
    public final int sampleIntervalMs;
    public final int storeSize;

    public final double CPUSpikeDelta;
    public final int CPUSpikeSeconds;
    public final double RAMRiseThreshold;
    public final long alertCooldownMs;

    public final int apiPort;

    public final boolean exportCsvEnabled;
    public final String exportCsvPath;
    public final int exportCsvFlushEvery;


    private MonitorConfig(Properties props) {
        this.sampleIntervalMs = getInt(props, "sample.interval.ms", 1000);
        this.storeSize = getInt(props, "store.capacity", 600);

        this.CPUSpikeDelta = getDouble(props, "cpu.spike.delta", 30.0);
        this.CPUSpikeSeconds = getInt(props, "cpu.spike.seconds", 3);
        this.RAMRiseThreshold = getDouble(props, "ram.rise.threshold", 5.0);
        this.alertCooldownMs = getLong(props, "alert.cooldown.ms", 10_000);

        this.apiPort = getInt(props, "api.port", 8080);

        this.exportCsvEnabled = getBoolean(props, "export.csv.enabled", true);
        this.exportCsvPath = props.getProperty("export.csv.path", "metrics.csv");
        this.exportCsvFlushEvery = getInt(props, "export.csv.flushEvery", 5);
    }
        public static MonitorConfig load () {
            Properties props = new Properties();

            // Try to load properties file from classpath
            try (InputStream in = MonitorConfig.class.getClassLoader()
                    .getResourceAsStream("monitor.properties")) {

                if (in != null) {
                    props.load(in);  // File found!
                }
                // If null, file doesn't exist - will use defaults

            } catch (Exception e) {
                System.err.println("Warning: Could not load monitor.properties: " + e.getMessage());
            }

            return new MonitorConfig(props);
        }

        // Helper methods to parse properties with defaults

        private static int getInt (Properties props, String key,int defaultValue){
            try {
                String value = props.getProperty(key, String.valueOf(defaultValue));
                return Integer.parseInt(value.trim());
            } catch (Exception e) {
                return defaultValue;
            }
        }

        private static long getLong (Properties props, String key,long defaultValue){
            try {
                String value = props.getProperty(key, String.valueOf(defaultValue));
                return Long.parseLong(value.trim());
            } catch (Exception e) {
                return defaultValue;
            }
        }

        private static double getDouble (Properties props, String key,double defaultValue){
            try {
                String value = props.getProperty(key, String.valueOf(defaultValue));
                return Double.parseDouble(value.trim());
            } catch (Exception e) {
                return defaultValue;
            }
        }

        private static boolean getBoolean (Properties props, String key,boolean defaultValue){
            String value = props.getProperty(key, String.valueOf(defaultValue));
            return Boolean.parseBoolean(value.trim());
        }
    }
