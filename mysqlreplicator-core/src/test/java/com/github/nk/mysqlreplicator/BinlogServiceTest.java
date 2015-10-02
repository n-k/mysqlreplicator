package com.github.nk.mysqlreplicator;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@TransactionConfiguration(defaultRollback = true)
public class BinlogServiceTest {

	@Before
	public void setUp() throws IOException {
	}

	@Test
	public void testEmpty() throws Exception {
	}

}
