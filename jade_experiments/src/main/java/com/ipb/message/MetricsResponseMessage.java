package com.ipb.message;

import java.io.Serializable;

import com.ipb.agents.Metrics;

public class MetricsResponseMessage implements Serializable {
  private String name;
  private String skill;
  private String type;
  private Metrics metrics;

  public MetricsResponseMessage(String name, String skill, String type, Metrics metrics) {
    this.name = name;
    this.skill = skill;
    this.type = type;
    this.metrics = metrics;
  }

  public Metrics getMetrics() {
    return metrics;
  }

  public String getType() {
    return type;
  }

  public String getSkill() {
    return skill;
  }

  public String getName() {
    return name;
  }
}
