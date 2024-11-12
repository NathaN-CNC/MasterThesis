package com.ipb.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import jade.core.Agent;

public class AgentLogger {
  public static DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'kk:HH:mm:ss.SSS", Locale.ENGLISH)
      .withLocale(Locale.getDefault())
      .withZone(ZoneId.systemDefault());

  private final Agent agent;

  public AgentLogger(Agent agent) {
    this.agent = agent;
  }

  public void debug(String message) {
    log("DEBUG", message);
  }

  public void warn(String message) {
    log("WARN", message);
  }

  public void error(String message) {
    log("ERROR", message);
  }

  public void info(String message) {
    log("INFO", message);
  }

  public void log(String level, String message) {
    String now = formatter.format(Instant.now());
    String name = agent.getLocalName();
    System.out.println("[" + now + "] [" + level + "]: " + name + ": " + message);
  }
}
