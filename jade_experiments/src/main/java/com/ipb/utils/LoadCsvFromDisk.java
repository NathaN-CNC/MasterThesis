package com.ipb.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoadCsvFromDisk {
  public static Collector<float[], ?, ArrayList<float[]>> dataCollector = Collectors.toCollection(ArrayList::new);

  public static Stream<float[]> stream(String filePath) throws IOException {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      System.out.println("File not found: " + filePath);
      return null;
    }

    return Files.lines(path)
        .skip(1) // Skip header
        .map(LoadCsvFromDisk::parseLine);
  }

  public static ArrayList<float[]> load(String filePath) throws IOException {
    return stream(filePath).collect(dataCollector);
  }

  private static float[] parseLine(String line) {
    String[] parts = line.split(",");
    float[] numbers = new float[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i].equals("False")) {
        numbers[i] = 0;
      } else if (parts[i].equals("True")) {
        numbers[i] = 1;
      } else {
        numbers[i] = Float.parseFloat(parts[i]);
      }
    }

    return numbers;
  }
}
