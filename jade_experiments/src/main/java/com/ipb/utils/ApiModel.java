package com.ipb.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class ApiModel {
    private String baseUrl;

    public ApiModel(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public synchronized float[] predict(float[] inputData) {
        StringBuilder responseBuilder = new StringBuilder();
        float[] result = new float[2];
        HttpURLConnection connection = null;
        try {
            // URL do servidor que receberá a requisição POST
            URL url = new URL(this.baseUrl + "/predict");

            // Abrindo conexão HTTP
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Escrevendo o corpo da requisição
            String requestBodyString = Arrays.toString(inputData);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Lendo a resposta
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    responseBuilder.append(responseLine.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Convertendo a resposta para um float[]
        String response = responseBuilder.toString();
        System.out.println("INFO: API Response: " + response);
        String[] responseArray = response.replace("[", "").replace("]", "").split(",");
        for (int i = 0; i < responseArray.length; i++) {
            result[i] = Float.parseFloat(responseArray[i]);
        }

        return result;
    }
}
