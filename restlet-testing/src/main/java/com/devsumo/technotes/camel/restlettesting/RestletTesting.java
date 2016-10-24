package com.devsumo.technotes.camel.restlettesting;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.apache.http.HttpStatus;

/**
 * Simplest possible Camel booter suitable for starting a REST front end to a
 * filesystem folder, either at the command line or from a unit test (using port 0)
 * 
 * Illustrates how to use ephemeral ports with restlets
 */
public class RestletTesting {
	
	/** REST API port */
	private int restPort = 0;
	
	/** Root directory to search for requested files */
	private String filesystemPath = null;
	
	/** Camel Lifecycle manager - package scope so we can (lazily) get the Camel context from our tests */
	Main main = null;
	
	/** 
	 * Fully populating constructor
	 * 
	 * @param filesystemPath root filesystem folder to front with a REST API
	 * @param restPort port to host the REST service on
	 */
	public RestletTesting(String filesystemPath, int restPort) {
		this.filesystemPath = filesystemPath;
		this.restPort = restPort;
	}
	
	/**
	 * Start the service; boots the Camel context either from an associated test class
	 * or from the command line (where Camel itself will handle shutdown)
	 * 
	 * Creates a single REST API route with a filename in the URI. Uses <code>pollEnrich</code>
	 * to read that file into the message body and then return an appropriate response.
	 * 
	 * @param standalone whether to expect a programmatic shutdown or intercept an 
	 *     termination signal
	 * @throws Exception if the Camel context cannot be booted
	 */
	public void start(boolean standalone) throws Exception {
    	main = new Main();
    	
    	main.addRouteBuilder(new RouteBuilder(){
			@Override
			public void configure() throws Exception {
				from("restlet:http://localhost:" + restPort + "/files/{fileName}?restletMethods=get&port=" + restPort)
					.pollEnrich(simple("file:" + filesystemPath + "/?fileName=${in.headers.fileName}&noop=true&sendEmptyMessageWhenIdle=true&probeContentType=true&idempotent=false"), -1, null, false)
        			.choice()
        				.when(body().isNull())
        				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.SC_NOT_FOUND));
			}
    	});

        if(standalone) {
        	main.run();
        } else {
        	main.disableHangupSupport();
        	main.start();
        }
	}
	
	/**
	 * Programmatically shutdown the Camel context
	 * @throws Exception if the shutdown fails
	 */
	public void stop() throws Exception {
		main.stop();
	}
	
	/**
	 * Command line booter. Expects the filesystem path as the first argument and
	 * the rest port as the second. Will log an exception if incorrect or incomplete
	 * arguments are supplied
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		try {
			new RestletTesting(args[0], Integer.valueOf(args[1])).start(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
