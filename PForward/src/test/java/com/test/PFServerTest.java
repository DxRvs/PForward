package com.test;
import org.junit.Test;

import com.tl.pf.PFServer;

import static org.junit.Assert.*;


public class PFServerTest {

	@Test public void testAppHasAVersion() {
        assertNotNull("The version must be", PFServer.Version);
    }	
}
