package com.ipb;

import java.io.IOException;
import java.util.List;

import com.ipb.utils.Constants;
import com.ipb.utils.LoadCsvFromDisk;

public class App {
    public static void main(String[] args) {
        inspectMemoryOfCsvFile(Constants.TRAIN_PATH);
        inspectMemoryOfCsvFile(Constants.TEST_PATH);
    }

    private static void inspectMemoryOfCsvFile(String path) {
        try {
            System.gc();
            List<float[]> data = LoadCsvFromDisk.load(path);
            double memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

            System.out.println("File: " + path);
            System.out.println("Data rows: " + data.size());
            System.out.println("Data columns: " + data.get(0).length);
            System.out.println("Memory usage: " + memory + " MB");
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
