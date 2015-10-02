package com.github.nk.mysqlreplicator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mysql.management.MysqldResource;
import com.mysql.management.MysqldResourceI;

@Service
public class MysqldService {

	@Value("${mysql.port}")
	private int port;

	@Value("${mysql.user}")
	private String username;

	@Value("${mysql.password}")
	private String password;

	private MysqldResource server;

	public void start() {
		server = startDatabase(new File("./temp"), port, username, password);
	}

	@PreDestroy
	public void stop() {
		server.shutdown();
	}

	private static MysqldResource startDatabase(File databaseDir, int port, String userName, String password) {
		MysqldResource mysqldResource = new MysqldResource(databaseDir);
		Map<String, Object> database_options = new HashMap<>();
		database_options.put(MysqldResourceI.PORT, Integer.toString(port));
		database_options.put(MysqldResourceI.INITIALIZE_USER, "true");
		database_options.put(MysqldResourceI.INITIALIZE_USER_NAME, userName);
		database_options.put(MysqldResourceI.INITIALIZE_PASSWORD, password);
		mysqldResource.start("test-mysqld-thread", database_options);
		if (!mysqldResource.isRunning()) {
			throw new RuntimeException("MySQL did not start.");
		}
		System.out.println("MySQL is running.");
		return mysqldResource;
	}
}
