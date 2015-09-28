package com.github.nk.mysqlreplicator;

import java.util.concurrent.BlockingQueue;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.github.nk.mysqlreplicator.config.EventWriterFactory;
import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.services.BinlogReader;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private BinlogReader binlogService;

    @Autowired
    @Resource(name = "binlogEventQueue")
    private BlockingQueue<BinLogEvent> eventQueue;

    @Autowired
    private EventWriterFactory consumerFactory;

    @Value("${writer.threads}")
    private int writerThreads;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // TODO: don't use daemon threads, add proper start/stop
        for (int i = 0; i < writerThreads; i++) {
            Thread thread = new Thread(consumerFactory.getEventWriter());
            thread.setDaemon(true);
            thread.start();
        }
        binlogService.start();
        System.out.println("Started command line application. Press CTRL+C or send SIGINT to exit.");
        Thread.currentThread().join();
    }
}
