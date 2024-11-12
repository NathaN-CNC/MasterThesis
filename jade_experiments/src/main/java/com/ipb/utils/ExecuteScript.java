package com.ipb.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ExecuteScript {
    public static ArrayList<Float> runScript(int value) {
        String prefix = "Result: ";
        ProcessBuilder pb = new ProcessBuilder("python", "script.py", Integer.toString(value));
        pb.directory(new File("E:\\DISK\\_workspace\\x\\jade_experiments\\tmp"));
        ArrayList<Float> result = new ArrayList<Float>();

        try {
            Process process = pb.start();
            InputStream inputStream = process.getInputStream();
            byte[] bytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(bytes)) != -1) {
                String[] lines = new String(bytes, 0, bytesRead).split("\n");
                for (String line : lines) {
                    if (line.startsWith(prefix)) {
                        String[] parts = line.substring(prefix.length()).split(" ");
                        for (String part : parts) {
                            result.add(Float.parseFloat(part));
                        }
                    }
                }
            }
            process.waitFor();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("Erro ao executar o script Python: " + exitCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
