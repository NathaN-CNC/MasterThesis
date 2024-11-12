package com.ipb.agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.ipb.behaviours.MessageListenerBehaviour;
import com.ipb.message.MetricsResponseMessage;
import com.ipb.message.PointsRequestMessage;
import com.ipb.message.PointsResponseMessage;
import com.ipb.message.ReadyMessage;
import com.ipb.utils.AgentExtension;
import com.ipb.utils.AgentLogger;
import com.ipb.utils.Constants;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentMetrics extends Agent {
  private AgentLogger logger = new AgentLogger(this);

  private Map<String, Long> pointsPerSkill = new HashMap<>();

  @Override
  protected void setup() {
    AgentExtension.registerSkill(this, "metrics");
    addBehaviour(createBehaviorMetricsListener());
    addBehaviour(createBehaviourAnalyzeMetrics());
  }

  @Override
  protected void takeDown() {
    AgentExtension.deregisterSkill(this);
  }

  private Behaviour createBehaviorMetricsListener() {
    FloatFormatter floatFormat = new FloatFormatter();

    return new MessageListenerBehaviour(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM)) {
      @Override
      protected void dispatchObjectMessage(ACLMessage message, Serializable contentObject) {
        if (contentObject instanceof MetricsResponseMessage) {
          MetricsResponseMessage metricsResponse = (MetricsResponseMessage) contentObject;
          if (metricsResponse.getMetrics().getTotalCount() == 0) return;

          if (metricsResponse.getType().equals("self-window")) {
            if (!pointsPerSkill.containsKey(metricsResponse.getSkill())) {
              pointsPerSkill.put(metricsResponse.getSkill(), 0L);
            }
            
            pointsPerSkill.put(metricsResponse.getSkill(), metricsResponse.getMetrics().getPoints());
          }

          logger.info("Name=" + metricsResponse.getName()
              + ", Skill=" + metricsResponse.getSkill()
              + ", Type=" + metricsResponse.getType()
              + ", F1=" + floatFormat.format(metricsResponse.getMetrics().getF1Score())
              + ", Accuracy=" + floatFormat.format(metricsResponse.getMetrics().getAccuracy())
              + ", Precision=" + floatFormat.format(metricsResponse.getMetrics().getPrecision())
              + ", Recall=" + floatFormat.format(metricsResponse.getMetrics().getRecall())
              + ", Total=" + metricsResponse.getMetrics().getTotalCount()
              + ", Asks=" + metricsResponse.getMetrics().getAskOpinionCount()
              + ", Timeouts=" + metricsResponse.getMetrics().getTimeoutCount()
              + ", Points=" + metricsResponse.getMetrics().getPoints()
              + ", TP=" + metricsResponse.getMetrics().getTruePositive()
              + ", FP=" + metricsResponse.getMetrics().getFalsePositive()
              + ", TN=" + metricsResponse.getMetrics().getTrueNegative()
              + ", FN=" + metricsResponse.getMetrics().getFalseNegative()
              + ", API time=" + floatFormat.format(metricsResponse.getMetrics().getAverageApiRequestTime())
              + ", Ask time=" + floatFormat.format(metricsResponse.getMetrics().getTimePerAsk())
              + ", Voting time=" + floatFormat.format(metricsResponse.getMetrics().getTimePerVoting()));
        } else if (contentObject instanceof PointsRequestMessage) {
          PointsResponseMessage pointsResponse = new PointsResponseMessage(pointsPerSkill);
          ACLMessage reply = message.createReply();
          AgentExtension.setTrustyContentObject(reply, pointsResponse);
          send(reply);
        } else {
          logger.warn("RECEIVE(UNKNOWN) : " + contentObject);
        }
      }
    };
  }

  private Behaviour createBehaviourAnalyzeMetrics() {
    return new WakerBehaviour(this, 200) {
      @Override
      public void onWake() {
        for (String skill : Constants.SKILLS) {
          DFAgentDescription desc = new DFAgentDescription();
          ServiceDescription service = new ServiceDescription();
          service.setType(skill);
          desc.addServices(service);

          try {
            DFAgentDescription[] result = DFService.search(getAgent(), desc);
            for (DFAgentDescription agent : result) {
              ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
              msg.addReceiver(new AID(agent.getName().getLocalName(), AID.ISLOCALNAME));
              msg.setContentObject(new ReadyMessage());
              send(msg);
            }
          } catch (FIPAException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    };
  }
}

class FloatFormatter {
  public String format(float value) {
    return String.format("%.3f", value).replace(",", ".");
  }
}
