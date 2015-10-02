package com.github.nk.mysqlreplicator.services;

import java.io.IOException;
import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.events.DeleteEvent;
import com.github.nk.mysqlreplicator.events.InsertEvent;
import com.github.nk.mysqlreplicator.events.UpdateEvent;
import com.github.nk.mysqlreplicator.services.StateStore.BinlogState;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogClient.EventListener;
import com.github.shyiko.mysql.binlog.BinaryLogClient.LifecycleListener;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;

@Service
public class BinlogReader {

    private static final Logger log = LoggerFactory.getLogger(BinlogReader.class);
    private static final long RECONNECT_DELAY_SECS = 10;

    @Value("${mysql.host}")
    private String host;

    @Value("${mysql.port}")
    private int port;

    @Value("${mysql.user}")
    private String username;

    @Value("${mysql.password}")
    private String password;

    private BinaryLogClient client;

    private Map<Long, TableInfo> tableInfoMap;

    @Autowired
    @Resource(name = "binlogEventQueue")
    private BlockingQueue<BinLogEvent> eventQueue;

    @Autowired
    private StateStore stateStore;

    @PostConstruct
    public void init() throws Exception {
        client = new BinaryLogClient(host, port, username, password);
        BinlogState prevState = stateStore.getState();
        client.setBinlogFilename(prevState.getFilename());
        client.setBinlogPosition(prevState.getIndex());
        client.registerLifecycleListener(new LCListener());
        client.registerEventListener(new EListener());
        tableInfoMap = new HashMap<>();
    }

    public void start() throws IOException {
        client.connect();
    }

    private class EListener implements EventListener {

        @Override
        public void onEvent(Event ev) {
            EventData data = ev.getData();
            if (data instanceof WriteRowsEventData) {
                handleWriteRows((WriteRowsEventData) data);
            } else if (data instanceof UpdateRowsEventData) {
                handleUpdatedRows((UpdateRowsEventData) data);
            } else if (data instanceof DeleteRowsEventData) {
                handleDeleteRows((DeleteRowsEventData) data);
            } else if (data instanceof TableMapEventData) {
                handleTableRemap((TableMapEventData) data);
            } else if (data instanceof RotateEventData) {
                handleRotate((RotateEventData) data);
            }
            try {
                stateStore.saveState(client.getBinlogFilename(), client.getBinlogPosition());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleRotate(RotateEventData red) {
        try {
            stateStore.saveState(client.getBinlogFilename(), client.getBinlogPosition());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleWriteRows(WriteRowsEventData wed) {
        BitSet includedColumns = wed.getIncludedColumns();
        List<Serializable[]> rows = wed.getRows();
        long tableId = wed.getTableId();
        TableInfo tInfo = tableInfoMap.get(tableId);
        if (tInfo != null) {
            List<ColInfo> colInfos = tInfo.getColInfos();
            for (Serializable[] row : rows) {
                Map<String, Object> converted = convertRow(colInfos, includedColumns, row);
                InsertEvent ie = new InsertEvent(tInfo, converted);
                eventQueue.offer(ie);
            }
        }
    }

    private void handleUpdatedRows(UpdateRowsEventData ured) {
        List<Entry<Serializable[], Serializable[]>> rows = ured.getRows();
        long tableId = ured.getTableId();
        TableInfo tInfo = tableInfoMap.get(tableId);
        if (tInfo != null) {
            List<ColInfo> cols = tInfo.getColInfos();
            for (Entry<Serializable[], Serializable[]> row : rows) {
                Map<String, Object> before = convertRow(cols, ured.getIncludedColumnsBeforeUpdate(), row.getKey());
                Map<String, Object> after = convertRow(cols, ured.getIncludedColumns(), row.getValue());
                UpdateEvent ue = new UpdateEvent(tInfo, before, after);
                eventQueue.offer(ue);
            }
        }
    }

    private void handleDeleteRows(DeleteRowsEventData dred) {
        List<Serializable[]> rows = dred.getRows();
        BitSet includedColumns = dred.getIncludedColumns();
        long tableId = dred.getTableId();
        TableInfo tInfo = tableInfoMap.get(tableId);
        if (tInfo != null) {
            List<ColInfo> cols = tInfo.getColInfos();
            for (Serializable[] row : rows) {
                Map<String, Object> converted = convertRow(cols, includedColumns, row);
                DeleteEvent de = new DeleteEvent(tInfo, converted);
                eventQueue.offer(de);
            }
        }
    }

    private void handleTableRemap(TableMapEventData tmed) {
        long tableId = tmed.getTableId();
        synchronized (this) {
            if (tableInfoMap.containsKey(tableId)) {
                return;
            }
            try {
                tableInfoMap.put(tableId, TableInfo.getInfo(host, port, username, password, tmed));
            } catch (Exception e) {
                log.error("Couldn't refresh table info", e);
            }
        }
    }

    private Map<String, Object> convertRow(List<ColInfo> cols, BitSet includedCols, Serializable[] values) {
        Map<String, Object> map = new HashMap<>();

        int size = cols.size();
        for (int i = 0; i < size; i++) {
            ColInfo colInfo = cols.get(i);
            if (includedCols.get(i)) {
                map.put(colInfo.getName(), concertObject(colInfo, values[i]));
            } else {
                map.put(colInfo.getName(), null);
            }
        }
        return map;
    }

    private Object concertObject(ColInfo colInfo, Serializable value) {
        String typeLowerCase = colInfo.getTypeLowerCase();
        if ("enum".equals(typeLowerCase)) {
            int idx = (int) value;
            return idx <= 0 ? null : colInfo.getEnumValues().get(idx - 1);
        }
        return value;
    }

    private class LCListener implements LifecycleListener {

        private boolean shouldReconnect = true;

        @Override
        public void onCommunicationFailure(BinaryLogClient client, Exception e) {
            if (e.getMessage().equals("1236 - Could not find first log file name in binary log index file")) {
                log.error("Binlog {}/{} is no longer available on the master; need to rebootstrap",
                        client.getBinlogFilename(), client.getBinlogPosition());
                shouldReconnect = false;
                disconnectAndExit();
            } else {
                log.warn("Communication failure", e);
            }
        }

        @Override
        public void onConnect(BinaryLogClient client) {
        }

        @Override
        public void onDisconnect(BinaryLogClient client) {
            if (shouldReconnect) {
                log.warn("Disconnected; reconnect in {} seconds", RECONNECT_DELAY_SECS);
                connectInNewThread(RECONNECT_DELAY_SECS);
            } else {
                log.warn("Disconnected; won't reconnect");
            }
        }

        @Override
        public void onEventDeserializationFailure(BinaryLogClient client, Exception e) {
            log.warn("Event deserialization failure", e);
        }

        private void disconnectAndExit() {
            try {
                // Need to disconnect before exiting, otherwise we can't exit
                // because
                // there are still running threads
                client.disconnect();
            } catch (Exception e) {
                log.warn("Could not disconnect", e);
            } finally {
                log.info("Program should now exit");
                System.exit(-1);
            }
        }

        private void connectInNewThread(long delaySecs) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (delaySecs > 0)
                            Thread.sleep(delaySecs * 1000);
                        client.connect();
                    } catch (Exception e) {
                        long delay = (delaySecs > 0) ? delaySecs : RECONNECT_DELAY_SECS;
                        log.error("Could not connect, will reconnect in " + delay + " seconds", e);
                        connectInNewThread(delay);
                    }
                }
            }).start();
        }
    }
}
