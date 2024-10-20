package org.isa.ipc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test.
 */
@DisplayName("Starting and stopping a JamnServer")
public class JamnServerTest {
	@Test
	public void testStartStop()  {
		JamnServer lServer = new JamnServer();
		
		try {
			lServer.start();
			assertTrue(lServer.isRunning());
			
			lServer.stop();
			assertFalse(lServer.isRunning());
		}finally {
			if(lServer.isRunning()) {
				lServer.stop();
			}
		}
	}
}
