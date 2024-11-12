package com.ipb.message;

import java.util.UUID;

import java.io.Serializable;
import java.time.Instant;

public class OpinionRequestMessage implements Serializable {
  private UUID id; // Um identificador único para a mensagem de solicitação
  private float[] inputData; // Os dados de entrada para a análise de opinião
  private Instant startTime;


  public OpinionRequestMessage(UUID id, float[] inputData, Instant startTime) {
    this.id = id; // Inicializa o identificador único
    this.inputData = inputData; // Inicializa os dados de entrada
    this.startTime = startTime;
  }

  public UUID getId() { // Método para obter o identificador único da mensagem
    return id; 
  }

  public float[] getInputData() { // Método para obter os dados de entrada
    return inputData;
  }

  public Instant getStartTime() {
    return startTime;
  }
}
