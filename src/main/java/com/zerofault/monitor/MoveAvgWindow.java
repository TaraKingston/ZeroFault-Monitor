package com.zerofault.monitor;

import java.util.ArrayDeque;
import java.util.Deque;
//In this moveAvgWindow class an object is created to store a fixed number of the recent values
//and calc the moving avg
public class MoveAvgWindow {
    private final int windowSize;
    private final Deque<Double> window = new ArrayDeque<>();


    public MoveAvgWindow(int windowSize) {
        if (windowSize <= 0) throw new IllegalArgumentException("Window size must be greater than 0");
        this.windowSize = windowSize;

    }

    private double sum = 0.0;

    //Adds a matric value to the window if the window is full it will remove the oldest value.
    public void add(double value) {
        window.addLast(value); //add value to the end which is the new position
        sum += value; //Increase the running sum

        if (window.size() > windowSize) {
            double removed = window.removeFirst(); // removes the oldest value
            sum -= removed; // improves the sum

        }
    }

    public double getAvg() {
        return window.isEmpty() ? 0.0 : sum / window.size();
    }

    public int size() {
        return window.size();
    }

    // oldest value is now in the window 60 seconds ago
    public double oldest() {
        return window.isEmpty() ? 0.0 : window.peekFirst();
    }

    // latest value is now in the window most recent
    public double latest() {
        return window.isEmpty() ? 0.0 : window.peekLast();
    }

    //expose capacity for checking “warmed up”
    public int capacity() {
        return windowSize;
    }
}
}

