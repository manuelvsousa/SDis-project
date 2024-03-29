package org.binas.ws.cli;

import org.binas.ws.StationView;

public class BinasClientApp {

    public static void main(String[] args) throws Exception {
        // Check arguments
        if (args.length == 0) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: java " + BinasClientApp.class.getName()
                    + " wsURL OR uddiURL wsName");
            return;
        }
        String uddiURL = null;
        String wsName = null;
        String wsURL = null;
        if (args.length == 1) {
            wsURL = args[0];
        } else if (args.length >= 2) {
            uddiURL = args[0];
            wsName = args[1];
        }

		System.out.println(BinasClientApp.class.getSimpleName() + " running");

        // Create client
        BinasClient client = null;

        if (wsURL != null) {
            System.out.printf("Creating client for server at %s%n", wsURL);
            client = new BinasClient(wsURL);
        } else if (uddiURL != null) {
            System.out.printf("Creating client using UDDI at %s for server with name %s%n",
                uddiURL, wsName);
            client = new BinasClient(uddiURL, wsName);
        }
        System.out.println("F2 TEST INIT !!");

        /*
        client.activateUser("t06@tecnico.ulisboa.pt");
        client.rentBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Rented a bina :) !!");
        client.returnBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Returned a bina :( !!");
        System.out.println("sleeping 15 seconds.... [PLEASE TURN OFF STATION NUMBER 3]");
        Thread.sleep(15000); //15 seconds to shutdown T06_Station3 (-Dws.i=3)
        System.out.println("I am awake again!");
        client.rentBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Rented a bina :) !!");
        client.returnBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Returned a bina :( !!");
        System.out.println("sleeping 20 seconds.... [PLEASE TURN ON STATION NUMBER 3]");
        Thread.sleep(20000); //20 seconds to revive T06_Station3 (-Dws.i=3)
        System.out.println("I am awake again!");
        client.rentBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Rented a bina :) !!");
        System.out.println("\n\nF3 TEST INIT!!");
        System.out.println("sleeping 25 seconds.... [PLEASE TURN OFF BINAS-WS]");
        Thread.sleep(25000); //25 seconds to shutdown binas-ws
        System.out.println("sleeping 20 seconds.... [PLEASE TURN ON BINAS-WS AGAIN]");
        Thread.sleep(20000); //20 seconds to start binas-ws again
        client.rentBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Rented a bina :) !!");
        client.returnBina("T06_Station1","t06@tecnico.ulisboa.pt");
        System.out.println("I Returned a bina :( !!");
        System.out.println("\n\nHappy End :) :) :) !!");
        */

        //System.out.println("Invoke ping()...");
        //String result = client.testPing("client");
        //System.out.print(result);

        client.activateUser("alice@T06.binas.org");
        //client.rentBina("T06_Station1", "alice@T06.binas.org");

        //client.returnBina("T06_Station1", "alice@T06.binas.org");
	 }
}

