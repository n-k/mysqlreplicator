package com.github.nk.mysqlreplicator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.events.InsertEvent;
import com.github.nk.mysqlreplicator.services.ColumnInfo;
import com.github.nk.mysqlreplicator.services.MemoryStateStore;
import com.github.nk.mysqlreplicator.services.QueuingEventListener;
import com.github.nk.mysqlreplicator.services.TableInfo;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;

public class QueuingEventListenerTest {

	private MemoryStateStore stateStore;
	private ArrayBlockingQueue<BinLogEvent> eventQueue;
	private MockTableInfoProvider tableInfoProvider;
	private MockBinlogPositionProvider positionProvider;

	private QueuingEventListener listener;

	@Before
	public void setUp() throws IOException {
		stateStore = new MemoryStateStore();
		stateStore.setWriteNth(1);
		eventQueue = new ArrayBlockingQueue<>(1);
		tableInfoProvider = new MockTableInfoProvider();
		positionProvider = new MockBinlogPositionProvider();
		positionProvider.setFilename("test");
		positionProvider.setPosition(0);

		listener = new QueuingEventListener(stateStore, eventQueue, positionProvider, tableInfoProvider);
	}

	@Test
	public void testEmpty() throws Exception {
		Assert.assertTrue(eventQueue.size() == 0);
		// set a table info in provider which will be fetched
		List<ColumnInfo> colInfos = new ArrayList<>();
		colInfos.add(new ColumnInfo("id", "bigint", new ArrayList<>()));
		colInfos.add(new ColumnInfo("name", "string", new ArrayList<>()));
		TableMapEventData med = new TableMapEventData();
		med.setTableId(1);
		TableInfo ti = new TableInfo(med, colInfos, "id");
		tableInfoProvider.setTableInfo(ti);

		Event e = new Event(null, med);
		listener.onEvent(e);
		// now send a row added event and check event queue

		WriteRowsEventData wed = new WriteRowsEventData();
		wed.setTableId(1);
		BitSet columns = new BitSet();
		columns.set(0);
		columns.set(1);
		wed.setIncludedColumns(columns);
		Serializable[] row = new Serializable[] { 1, "test" };
		List<Serializable[]> rows = new ArrayList<>();
		rows.add(row);
		wed.setRows(rows);
		listener.onEvent(new Event(null, wed));

		Assert.assertEquals(1, eventQueue.size());
		BinLogEvent ble = eventQueue.take();
		Assert.assertNotNull(ble);
		Assert.assertTrue(ble instanceof InsertEvent);
		System.out.println(new ObjectMapper().writeValueAsString(ble));
		InsertEvent ie = (InsertEvent) ble;
		Map<String, Object> data = ie.getData();
		Assert.assertNotNull(data);
		
		Assert.assertTrue(data.containsKey("id"));
		Assert.assertEquals(1, data.get("id"));
		Assert.assertTrue(data.containsKey("name"));
		Assert.assertEquals("test", data.get("name"));
	}

}
