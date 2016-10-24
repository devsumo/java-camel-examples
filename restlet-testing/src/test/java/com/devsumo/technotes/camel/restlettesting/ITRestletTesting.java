package com.devsumo.technotes.camel.restlettesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.camel.component.restlet.RestletComponent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Server;

/** 
 * Standalone integration test for a Camel Restlet API
 * Illustrates how to extract an ephemeral port from the Camel context 
 */
public class ITRestletTesting {
	
	/** Root directory (under Maven target folder) for test files */
	private static final File TEST_FILE_STORE_ROOT = 
			new File("target/testFileStore");
	
	/** Class under test */
	private RestletTesting cut = null;
	
	/** REST port to call for API tests */
	private int restPort = Integer.MAX_VALUE;
	
	/** Before each fixture; cleanly boot the Camel context and determine the REST port to call */
	@Before
	public void startServices() throws Exception {
		TEST_FILE_STORE_ROOT.mkdirs();
		cut = new RestletTesting(TEST_FILE_STORE_ROOT.getAbsolutePath(), 0);
		cut.start(false);
		restPort = getActualRestPort();
	}

	/** After each fixture, stop the Camel context and cleanout the file-store */
	@After
	public void stopServices() throws Exception {
		if(cut != null) {
			cut.stop();
		}
		FileUtils.deleteDirectory(TEST_FILE_STORE_ROOT);
	}

	/** If we request a file which exists we get an HTTP 200 response with the contents of that file */
	@Test
	public void testRequestedFileExists() throws Exception {
		final String testContent = "Some content";
		File testFile = new File(TEST_FILE_STORE_ROOT.getAbsolutePath() + File.separatorChar + "exists.txt");
		FileUtils.writeStringToFile(testFile, testContent, Charset.defaultCharset());
		
		String retrievedContent = IOUtils.toString(makeRequestURL("exists.txt"), Charset.defaultCharset());
		assertEquals(testContent, retrievedContent);
		assertTrue(testFile.exists());
	}
	
	/** If we request a file which does not exist we get an HTTP 404 response */
	@Test(expected=FileNotFoundException.class)
	public void testRequestedFileDoesNotExist() throws Exception {
		File testFile = new File(TEST_FILE_STORE_ROOT.getAbsolutePath() + File.separatorChar + "exists.txt");
		FileUtils.writeStringToFile(testFile, "stuff", Charset.defaultCharset());
		IOUtils.toString(makeRequestURL("doesNotExist.txt"), Charset.defaultCharset());
	}
	
	/**
	 * Create a local request URL for the specified file
	 * 
	 * @param fileName name of the file to download
	 * 
	 * @return URL to request the file
	 */
	private URI makeRequestURL(String fileName) {
		return URI.create("http://localhost:" + restPort + "/files/" + fileName);
	}

	/**
	 * Determine the ephemeral port allocated to our REST service.
	 * 
	 * This mechanism is lightweight but heavily tied to the Camel/Restlet versions in use, so
	 * sadly more brittle than one would ideally like.
	 * 
	 * We make some presumptions here about there being one Camel Context, one Restlet component
	 * which itself has one server. A more complex deployment would need these assumptions reworked.
	 * 
	 * @return allocated Rest API port
	 * 
	 * @throws Exception if we are unable to determine the rest port for any reason
	 */
	@SuppressWarnings("unchecked")
	private int getActualRestPort() throws Exception {
		// Make the private map of HTTP servers accessible
		Field serverMapField = RestletComponent.class.getDeclaredField("servers");
		serverMapField.setAccessible(true);
		
		// Get the actual port of the first server in the restlet component of the first Camel context
		RestletComponent component = cut.main.getCamelContexts().get(0).getComponent("restlet", RestletComponent.class);
		Map<String, Server> servers = (Map<String, Server>)serverMapField.get(component); 
		return servers.values().iterator().next().getActualPort();
	}
}
