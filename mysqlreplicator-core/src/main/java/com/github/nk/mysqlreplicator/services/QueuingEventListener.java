package com.github.nk.mysqlreplicator.services;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.events.DeleteEvent;
import com.github.nk.mysqlreplicator.events.InsertEvent;
import com.github.nk.mysqlreplicator.events.UpdateEvent;
import com.github.shyiko.mysql.binlog.BinaryLogClient.EventListener;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;

public class QueuingEventListener implements EventListener {

	private static final Logger log = LoggerFactory.getLogger(QueuingEventListener.class);

	private final Map<Long, TableInfo> tableInfoMap;
	private final BlockingQueue<BinLogEvent> eventQueue;
	private final StateStore stateStore;
	private final BinlogPositionProvider positionProvider;
	private final TableInfoProvider tableInfoProvider;

	public QueuingEventListener(StateStore stateStore, 
			BlockingQueue<BinLogEvent> eventQueue,
			BinlogPositionProvider positionProvider, 
			TableInfoProvider tableInfoProvider) {
		this.positionProvider = positionProvider;
		this.tableInfoProvider = tableInfoProvider;
		this.stateStore = stateStore;
		this.eventQueue = eventQueue;

		tableInfoMap = new HashMap<>();
	}

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
			stateStore.saveState(positionProvider.getFilename(), positionProvider.getPosition());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void handleRotate(RotateEventData red) {
		try {
			stateStore.saveState(positionProvider.getFilename(), positionProvider.getPosition());
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
			List<ColumnInfo> colInfos = tInfo.getColInfos();
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
			List<ColumnInfo> cols = tInfo.getColInfos();
			for (Entry<Serializable[], Serializable[]> row : rows) {
				Map<String, Object> before = convertRow(cols, ured.getIncludedColumnsBeforeUpdate(),
						row.getKey());
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
			List<ColumnInfo> cols = tInfo.getColInfos();
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
				tableInfoMap.put(tableId, tableInfoProvider.getTableInfo(tmed));
			} catch (Exception e) {
				log.error("Couldn't refresh table info", e);
			}
		}
	}
	
	private static Map<String, Object> convertRow(List<ColumnInfo> cols, 
			BitSet includedCols, 
			Serializable[] values) {
		Map<String, Object> map = new HashMap<>();

		int size = cols.size();
		for (int i = 0; i < size; i++) {
			ColumnInfo colInfo = cols.get(i);
			if (includedCols.get(i)) {
				map.put(colInfo.getName(), concertObject(colInfo, values[i]));
			} else {
				map.put(colInfo.getName(), null);
			}
		}
		return map;
	}

	private static Object concertObject(ColumnInfo colInfo, Serializable value) {
		String typeLowerCase = colInfo.getTypeLowerCase();
		if ("enum".equals(typeLowerCase)) {
			int idx = (int) value;
			return idx <= 0 ? null : colInfo.getEnumValues().get(idx - 1);
		}
		return value;
	}
}