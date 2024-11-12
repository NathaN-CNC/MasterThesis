package com.ipb.message;

import java.io.Serializable;

import java.util.Map;

public class PointsResponseMessage implements Serializable {
  private Map<String, Long> pointsPerSkill;
  
  public PointsResponseMessage(Map<String, Long> pointsPerSkill) {
    this.pointsPerSkill = pointsPerSkill;
  }

  public Map<String, Long> getPointsPerSkill() {
    return pointsPerSkill;
  }
}
