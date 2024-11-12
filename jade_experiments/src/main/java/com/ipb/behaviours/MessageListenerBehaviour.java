package com.ipb.behaviours;

import java.io.Serializable;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public abstract class MessageListenerBehaviour extends CyclicBehaviour {
    private final MessageTemplate messageTemplate;

    public MessageListenerBehaviour(Agent agent, MessageTemplate messageTemplate) {
        super(agent);
        this.messageTemplate = messageTemplate;
    }

    @Override
    public void action() {
        ACLMessage message = getAgent().receive(this.messageTemplate);

        if (message == null) {
            block();
            return;
        }

        Serializable contentObject = null;

        try {
            contentObject = message.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }

        if (contentObject == null) {
            block();
            return;
        }

        dispatchObjectMessage(message, contentObject);
        block();
    }

    protected abstract void dispatchObjectMessage(ACLMessage message, Serializable contentObject);
}