package com.ipb.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

import com.ipb.message.ReadyMessage;
import com.ipb.utils.Constants;

public class AgentSendReady extends Agent {
    protected void setup() {
        for (String skill : Constants.SKILLS) {
            DFAgentDescription desc = new DFAgentDescription();
            ServiceDescription service = new ServiceDescription();
            service.setType(skill);
            desc.addServices(service);

            try {
                DFAgentDescription[] result = DFService.search(this, desc);
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
}
