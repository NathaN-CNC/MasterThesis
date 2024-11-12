package com.ipb.behaviours;

import java.util.Iterator;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;

public abstract class ProcessIteratorBehaviour<T> extends Behaviour {
    private final Iterator<T> iterator;

    public ProcessIteratorBehaviour(Agent agent, Iterator<T> iterator) {
        super(agent);
        this.iterator = iterator;
    }

    @Override
    public void action() {
        if (iterator.hasNext()) {
            processItem(iterator.next());
        }
    }

    @Override
    public boolean done() {
        if (!iterator.hasNext()) {
            onDone();
            return true;
        }
        return false;
    }

    protected abstract void processItem(T item);

    protected void onDone() {}
}
