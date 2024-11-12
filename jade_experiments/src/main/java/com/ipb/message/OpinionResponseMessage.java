package com.ipb.message;

import java.util.UUID;

import java.io.Serializable;
import java.time.Instant;

public class OpinionResponseMessage implements Serializable {
  private UUID id; // Um identificador único para a mensagem de resposta
  private float[] outputData; // Os dados de saída da análise de opinião
  private Instant startTime;

  public OpinionResponseMessage(UUID id, float[] outputData, Instant startTime) {
    this.id = id; // Inicializa o identificador único
    this.outputData = outputData; // Inicializa os dados de saída
    this.startTime = startTime;
  }

  public UUID getId() { // Método para obter o identificador único da mensagem
    return id; 
  }

  public float[] getOutputData() { // Método para obter os dados de saída
    return outputData;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public float getTotalTime() {
    Instant endTime = Instant.now();
    float totalTime = (float) (endTime.toEpochMilli() - startTime.toEpochMilli()) / 1000;
    return totalTime;
  }
}
