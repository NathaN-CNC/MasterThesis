package com.ipb.agents;

import java.io.Serializable;

public class Metrics implements Serializable {
  private String name;
  private long totalCount = 0;

  private long truePositive = 0;
  private long falsePositive = 0;
  private long trueNegative = 0;
  private long falseNegative = 0;

  private long askOpinionCount = 0;
  private long timeoutCount = 0;
  private Float averageApiRequestTime = null;
  private Float timePerAsk = null;
  private Float timePerVoting = null;

  public Metrics(String name) {
    this.name = name;
  }

  public Metrics reset() {
    return new Metrics(name);
  }

  synchronized public void incrementAskOpinionCount() {
    askOpinionCount++;
  }

  synchronized public void incrementTimeoutCount() {
    timeoutCount++;
  }

  synchronized public void updateApiRequestTime(float apiRequestTime) {
    if (averageApiRequestTime == null) {
      averageApiRequestTime = apiRequestTime;
    } else {
      averageApiRequestTime = (averageApiRequestTime + apiRequestTime) / 2;
    }
  }

  synchronized public void updateTimePerAsk(float opinionTime) {
    if (timePerAsk == null) {
      timePerAsk = opinionTime;
    } else {
      timePerAsk = (timePerAsk + opinionTime) / 2;
    }
  }

  synchronized public void updateTimePerVoting(float opinionTime) {
    if (timePerVoting == null) {
      timePerVoting = opinionTime;
    } else {
      timePerVoting = (timePerVoting + opinionTime) / 2;
    }
  }

  synchronized public void update(float result, float expected) {
    boolean isAttackExpected = expected > 0.5;
    boolean isAttackResult = result > 0.5;

    if (isAttackExpected && isAttackResult) {
      truePositive++;
    } else if (isAttackExpected && !isAttackResult) {
      falseNegative++;
    } else if (!isAttackExpected && isAttackResult) {
      falsePositive++;
    } else {
      trueNegative++;
    }
    totalCount++;
  }

  public String getName() {
    return name;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public long getTruePositive() {
    return truePositive;
  }

  public long getFalsePositive() {
    return falsePositive;
  }

  public long getTrueNegative() {
    return trueNegative;
  }

  public long getFalseNegative() {
    return falseNegative;
  }

  public float getAccuracy() {
    return (float) (truePositive + trueNegative) / totalCount;
  }

  public float getPrecision() {
    return (float) truePositive / (truePositive + falsePositive);
  }

  public float getRecall() {
    return (float) truePositive / (truePositive + falseNegative);
  }

  public float getF1Score() {
    return 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
  }

  public long getPoints() {
    return (truePositive + trueNegative) - (falsePositive + falseNegative);
  }

  public float getTimePerAsk() {
    return timePerAsk == null ? 0 : timePerAsk;
  }

  public float getTimePerVoting() {
    return timePerVoting == null ? 0 : timePerVoting;
  }

  public float getAverageApiRequestTime() {
    return averageApiRequestTime == null ? 0 : averageApiRequestTime;
  }

  public long getTimeoutCount() {
    return timeoutCount;
  }

  public long getAskOpinionCount() {
    return askOpinionCount;
  }
}
