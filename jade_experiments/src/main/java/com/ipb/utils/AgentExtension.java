package com.ipb.utils;

import java.io.IOException;
import java.io.Serializable;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AgentExtension {
  public static ApiModel getModel(String skill) {
    return new ApiModel(Constants.SKILL_URLS.get(skill));
  }

  public static long getPeriod(String skill) {
    return Constants.SKILL_PERIODS.get(skill);
  }

  public static boolean registerSkill(Agent agent, String skill) {
    DFAgentDescription desc = new DFAgentDescription();
    ServiceDescription service = new ServiceDescription();
    service.setName(agent.getLocalName());
    service.setType(skill);
    desc.addServices(service);
    try {
      DFService.register(agent, desc);
      return true;
    } catch (FIPAException e) {
      return false;
    }
  }

  public static void deregisterSkill(Agent agent) {
    try {
      DFService.deregister(agent);
    } catch (FIPAException e) {
      e.printStackTrace();
    }
  }

  public static void setTrustyContentObject(ACLMessage message, Serializable contentObject) {
    try {
      message.setContentObject(contentObject);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("unreachable code");
    }
  }
}
