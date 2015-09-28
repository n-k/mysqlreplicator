package com.github.nk.mysqlreplicator.events;

import java.util.Map;

public class UpdateEvent extends BinLogEvent {

    private final Map<String, Object> before;
    private final Map<String, Object> after;

    public UpdateEvent(String db, String table, Map<String, Object> before, Map<String, Object> after) {
        super(db, table, Type.update);
        this.before = before;
        this.after = after;
    }

    public Map<String, Object> getBefore() {
        return before;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

}
