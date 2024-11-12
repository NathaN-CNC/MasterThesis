/*
 * A classe OpinionResult é definida no código fornecido como uma classe simples para representar o resultado de uma análise de opinião realizada pelo agente. 
 * Aqui está uma explicação dos campos e métodos desta classe:
 */
package com.ipb.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ipb.utils.Constants;

public class OpinionResult {
  private UUID id;
  private float[] inputData;
  private List<Float> isAttackList = new ArrayList<Float>(); // Uma lista de valores float que representam a opinião do
                                                             // agente sobre se um ataque está ocorrendo.
  private int total = 0; // O total de análises que devem ser realizadas
  private boolean ready = false; // Analise pronta ou n -- ?

  public OpinionResult(UUID id, float[] inputData) {
    this.id = id;
    this.inputData = inputData;
  }

  public void setup(int total) {
    if (!this.ready) {
      this.total = total;
      this.ready = true;
    } else {
      throw new IllegalStateException("OpinionResult is already setup");
    }
  }

  public synchronized void addOpinion(float isAttack) {
    this.isAttackList.add(isAttack);
  }

  public List<Float> getIsAttackList() {
    if (!Constants.FILTER_SURES) {
      return isAttackList;
    }

    List<Float> filteredList = new ArrayList<Float>();
    float threasholdUp = isAttackList.get(0);
    float threasholdDown = 1 - threasholdUp;
    if (threasholdUp < threasholdDown) {
      float temp = threasholdUp;
      threasholdUp = threasholdDown;
      threasholdDown = temp;
    }

    for (float isAttack : isAttackList) {
      if (isAttack <= threasholdDown || threasholdUp <= isAttack) {
        filteredList.add(isAttack);
      }
    }
  
    return filteredList;
  }

  public UUID getId() {
    return id;
  }

  public int getTotal() { // acessar e modificar o total de análises.
    return total;
  }

  public float[] getInputData() {
    return inputData;
  }

  public boolean isReady() { // verificar e modificar o status de pronto.
    return ready;
  }

  public boolean isComplete() { // Um método que verifica se a análise foi concluída com base no número de
                                // análises realizadas (count) e o total de análises esperadas (total).
    return this.isAttackList.size() == total + 1 /* 1 == agent itself */;
  }

  public synchronized float averageIsAttack() { // Um método que calcula a média dos valores float na lista isAttackList.
    if (isAttackList.size() == 0) {
      return 0;
    }
    float sum = 0;
    for (float isAttack : getIsAttackList()) {
      sum += isAttack;
    }
    return sum / isAttackList.size();
  }

  public synchronized float voteIsAttack() {
    int countTrue = 0;
    int countFalse = 0;
    for (float isAttack : getIsAttackList()) {
      if (isAttack >= 0.5) {
        countTrue++;
      } else {
        countFalse++;
      }
    }
    if (countTrue == countFalse) {
      return averageIsAttack();
    } else {
      return countTrue > countFalse ? 1.0f : 0.0f;
    }
  }
}
