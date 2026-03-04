package com.zerofault.monitor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import oshi.software.os.OSProcess;

import java.util.List;

public class MonitorApplication extends Application {

    private MetricsStore store;
    private SamplerService sampler;
    private Thread samplerThread;

    private final XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ramSeries = new XYChart.Series<>();

    private final Label cpuLabel = new Label("CPU: --");
    private final Label ramLabel = new Label("RAM: --");
    private final Label netLabel = new Label("NET: --");

    private final ListView<String> alertsList = new ListView<>();

    private final TableView<OSProcess> cpuTable = new TableView<>();
    private final TableView<OSProcess> memTable = new TableView<>();

    @Override
    public void start(Stage stage) {
        // ---- Create store + start sampler ----
        store = new MetricsStore(600); // store 10 min at 1s, UI chart uses last 60
        MetricsCollector collector = new MetricsCollector();

        MonitorConfig cfg = MonitorConfig.load();
        sampler = new SamplerService(collector, store, cfg);
        samplerThread = new Thread(sampler, "sampler");
        samplerThread.setDaemon(true);
        samplerThread.start();

        // ---- Charts ----
        LineChart<Number, Number> cpuChart = makeChart("CPU %", cpuSeries);
        LineChart<Number, Number> ramChart = makeChart("RAM %", ramSeries);

        VBox charts = new VBox(10, cpuChart, ramChart);
        charts.setPadding(new Insets(10));

        // ---- Alerts ----
        VBox alertsPane = new VBox(8, new Label("Alerts (recent)"), alertsList);
        alertsPane.setPadding(new Insets(10));

        // ---- Process tables ----
        setupProcessTable(cpuTable, "Top CPU Processes");
        setupProcessTable(memTable, "Top Memory Processes");

        VBox tables = new VBox(10, cpuTable, memTable);
        tables.setPadding(new Insets(10));

        SplitPane right = new SplitPane();
        right.setOrientation(javafx.geometry.Orientation.VERTICAL);
        right.getItems().addAll(alertsPane, tables);
        right.setDividerPositions(0.35);

        // ---- Top summary bar ----
        HBox summary = new HBox(20, cpuLabel, ramLabel, netLabel);
        summary.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(summary);
        root.setCenter(new SplitPane(charts, right));

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("System Monitor Dashboard");
        stage.setScene(scene);
        stage.show();

        // ---- UI refresh timer (no OSHI calls here) ----
        Timeline uiTick = new Timeline(new KeyFrame(Duration.millis(500), e -> refreshUI()));
        uiTick.setCycleCount(Timeline.INDEFINITE);
        uiTick.play();
    }

    private void refreshUI() {
        MetricsRecord latest = store.getLatestOrNull();
        if (latest != null) {
            cpuLabel.setText(String.format("CPU: %.2f%% (avg60: %.2f%%)", latest.cpuNow(), latest.cpuAvg60()));
            ramLabel.setText(String.format("RAM: %.2f%% (avg60: %.2f%%)", latest.ramNow(), latest.ramAvg60()));
            netLabel.setText(String.format("NET: ↓ %.2f KB/s ↑ %.2f KB/s", latest.downKBps(), latest.upKBps()));
        }

        // Update charts from last 60 seconds
        List<MetricsRecord> last60 = store.getLatestN(60);
        cpuSeries.getData().clear();
        ramSeries.getData().clear();

        int x = 0;
        for (MetricsRecord s : last60) {
            cpuSeries.getData().add(new XYChart.Data<>(x, s.cpuNow()));
            ramSeries.getData().add(new XYChart.Data<>(x, s.ramNow()));
            x++;
        }

        // Alerts
        alertsList.setItems(FXCollections.observableArrayList(store.getAlerts()));

        // Process tables
        cpuTable.setItems(FXCollections.observableArrayList(store.getTopCpu()));
        memTable.setItems(FXCollections.observableArrayList(store.getTopMem()));
    }

    private LineChart<Number, Number> makeChart(String yLabel, XYChart.Series<Number, Number> series) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Seconds (last 60)");
        xAxis.setAutoRanging(true);

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel(yLabel);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.getData().add(series);
        chart.setMinHeight(300);
        return chart;
    }

    private void setupProcessTable(TableView<OSProcess> table, String title) {
        table.setPlaceholder(new Label("Waiting for data..."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<OSProcess, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<OSProcess, Number> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getProcessID()));

        TableColumn<OSProcess, Number> memCol = new TableColumn<>("RSS (bytes)");
        memCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getResidentSetSize()));

        TableColumn<OSProcess, Number> cpuCol = new TableColumn<>("CPU Load (cumulative)");
        cpuCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getProcessCpuLoadCumulative()));

        table.getColumns().setAll(nameCol, pidCol, cpuCol, memCol);
        table.setPrefHeight(250);
        table.setPrefWidth(500);
        table.setId(title);
    }

    @Override
    public void stop() {
        if (sampler != null) sampler.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

