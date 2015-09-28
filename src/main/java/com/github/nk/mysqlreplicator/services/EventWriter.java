package com.github.nk.mysqlreplicator.services;

import java.util.concurrent.BlockingQueue;

import com.github.nk.mysqlreplicator.events.BinLogEvent;

/**
 * Interface of event writers. These are passed
 * com.github.nk.mysqlreplicator.events.* objects
 * 
 * Note that the same instance will be used by all writer threads.
 */
public abstract class EventWriter implements Runnable {

    protected final BlockingQueue<BinLogEvent> q;

    private volatile boolean stopped = false;

    public EventWriter(BlockingQueue<BinLogEvent> q) {
        this.q = q;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public final void run() {
        while (true) {
            if (stopped) {
                break;
            }
            consume();
        }
    }

    abstract public void consume();
}
