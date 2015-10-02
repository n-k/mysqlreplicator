package com.github.nk.mysqlreplicator.services;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.services.StateStore.BinlogState;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogClient.LifecycleListener;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;

@Service
public class BinlogReader implements BinlogPositionProvider, TableInfoProvider {

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

	@Autowired
	@Resource(name = "binlogEventQueue")
	private BlockingQueue<BinLogEvent> eventQueue;

	@Autowired
	private StateStore stateStore;

	private boolean shouldReconnect = true;

	@PostConstruct
	public void init() throws Exception {
		client = new BinaryLogClient(host, port, username, password);
		BinlogState prevState = stateStore.getState();
		client.setBinlogFilename(prevState.getFilename());
		client.setBinlogPosition(prevState.getIndex());
		client.registerLifecycleListener(new LCListener());
		client.registerEventListener(new QueuingEventListener(stateStore, eventQueue, this, this));
	}

	@PreDestroy
	public void stop() {
		shouldReconnect = false;
		try {
			client.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() throws IOException {
		client.connect();
	}

	private class LCListener implements LifecycleListener {

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

	@Override
	public String getFilename() {
		return client.getBinlogFilename();
	}

	@Override
	public long getPosition() {
		return client.getBinlogPosition();
	}

	@Override
	public TableInfo getTableInfo(TableMapEventData tmed) throws Exception {
		return TableInfo.getInfo(host, port, username, password, tmed);
	}
}
