package com.github.nk.mysqlreplicator.events;

public abstract class BinLogEvent {

    public enum Type {
        insert, update, delete
    }

    private final String db;
    private final String table;
    private final Type event;

    public BinLogEvent(String db, String table, Type event) {
        this.db = db;
        this.table = table;
        this.event = event;
    }

    public String getDb() {
        return db;
    }

    public String getTable() {
        return table;
    }

    public Type getEvent() {
        return event;
    }

}
