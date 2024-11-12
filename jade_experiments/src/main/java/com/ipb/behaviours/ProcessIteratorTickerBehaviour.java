package com.ipb.behaviours;

import java.util.Iterator;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public abstract class ProcessIteratorTickerBehaviour<T> extends TickerBehaviour {
    private final Iterator<T> iterator;

    public ProcessIteratorTickerBehaviour(Agent agent, long period, Iterator<T> iterator) {
        super(agent, period);
        this.iterator = iterator;
    }

    @Override
    public void onTick() {
        if (iterator.hasNext()) {
            processItem(iterator.next());
        } else {
            onDone();
            stop();
        }
    }

    protected abstract void processItem(T item);

    protected void onDone() {}
}
