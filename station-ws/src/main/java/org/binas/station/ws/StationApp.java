package org.binas.station.ws;

import org.binas.station.domain.Station;

/**
 * The application is where the service starts running. The program arguments
 * are processed here. Other configurations can also be done here.
 */
public class StationApp {

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length < 3) {
			System.err.println("Argument(s) missing!");
			return;
		}
		String wsName = args[1];
		String uddiURL = args[0];
		String wsURL = args[2];
		
		

		StationEndpointManager endpoint = new StationEndpointManager(uddiURL,wsName, wsURL);
		Station.getInstance().setId(wsName);

		System.out.println(StationApp.class.getSimpleName() + " running");

		try {
			endpoint.start();
			endpoint.awaitConnections();
		}finally {
			endpoint.stop();
		}

	}

}