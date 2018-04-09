package org.binas.domain;

import java.util.HashMap;

import org.binas.station.ws.CoordinatesView;
import org.binas.station.ws.cli.StationClient;

public class BinasManager {
	
	HashMap<String, StationClient> connectedStations = new HashMap<String, StationClient>();

	private BinasManager() {
	}

	private static class SingletonHolder {
		private static final BinasManager INSTANCE = new BinasManager();
	}

	public static synchronized BinasManager getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	public void PopulateStations(String uddiUrl,String stationPrefix) {
		Boolean hasMore = true;
		int currentStation = 1;
		while(hasMore) {
			StationClient station = null;
			String stationName = stationPrefix + currentStation;
			try {
				station = new StationClient(uddiUrl, stationName);
				System.out.printf("[INFO] Created client using UDDI at %s for server with name %s%n", uddiUrl, stationName);
				String stationId = station.getInfo().getId();
				connectedStations.put(stationId, station);
			}catch (Exception se) {
				hasMore = false;
			}
			currentStation += 1;
		}
	}
	
	// TODO

}
