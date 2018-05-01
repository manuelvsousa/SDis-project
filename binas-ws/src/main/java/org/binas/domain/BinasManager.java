package org.binas.domain;

import org.binas.exceptions.ExceptionManager;
import org.binas.exceptions.StationsUnavailableException;
import org.binas.station.ws.BalanceView;
import org.binas.station.ws.GetBalanceResponse;
import org.binas.station.ws.NoSlotAvail_Exception;
import org.binas.station.ws.SetBalanceResponse;
import org.binas.station.ws.cli.StationClient;
import org.binas.ws.*;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class BinasManager {

	private HashMap<String, StationClient> connectedStations = new HashMap<String, StationClient>();
	private HashMap<String, User> users = new HashMap<String,User>();

    private HashMap<String,Integer> cachedCredits =  new HashMap<String,Integer>();
    private HashMap<String,Timestamp> cachedTimestamps =  new HashMap<String,Timestamp>();

    static int finished = 0;
    static Integer exceptionCount = 0;

    static Timestamp mostUpToDate = null;
    static Integer credit = -1;


    private BinasManager() {
	}

	private static class SingletonHolder {
		private static final BinasManager INSTANCE = new BinasManager();
	}

	public static synchronized BinasManager getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private User getUserByEmail(String email) throws UserNotExists_Exception {

		User user = users.get(email);
		if(user==null) {
			ExceptionManager.userNotFound(email);
		}
		return user;
	}

	private StationClient getStation(String stationId) throws InvalidStation_Exception {
		StationClient station = this.connectedStations.get(stationId);
		if(station==null) {
			ExceptionManager.stationNotFound(stationId);
		}
		return station;
	}

	public StationView getInfoStation(String stationId) throws InvalidStation_Exception{
		StationClient station = getStation(stationId);
		StationView out = new StationView();
		out.setAvailableBinas(station.getInfo().getAvailableBinas());
		out.setFreeDocks(station.getInfo().getFreeDocks());
		out.setCapacity(station.getInfo().getCapacity());
		CoordinatesView coordinates = new CoordinatesView();
		coordinates.setX(station.getInfo().getCoordinate().getX());
		coordinates.setY(station.getInfo().getCoordinate().getY());
		out.setCoordinate(coordinates);
		out.setId(stationId);
		out.setTotalGets(station.getInfo().getTotalGets());
		out.setTotalReturns(station.getInfo().getTotalReturns());
		return out;
	}

	private Boolean checkEmail(String email){
		if(email.split("@").length != 2){ //Check if email contains only 1 @
			return false;
		}
		String[] emailList = email.split("@");
		if(emailList[0].length() == 0 || emailList[1].length() == 0){ //Check if email is not like @binas or binas@ ...
			return false;
		}
		if(emailList[0].charAt(emailList[0].length() - 1) == '.' || emailList[1].charAt(emailList[1].length() - 1) == '.'){  //Check if email is not like binas.@binas or binas@.binas ...
			return false;
		}
		if(emailList[0].charAt(0) == '.' || emailList[1].charAt(0) == '.'){  //Check if email is not like binas.@binas or binas@.binas ...
			return false;
		}
		if(!emailList[0].replace(".","").matches("^[a-zA-Z0-9]*$") || !emailList[1].replace(".","").matches("^[a-zA-Z0-9]*$")){  //Check if email is not like binas@b!na$ ...
			return false;
		}
		if(emailList[0].contains("..") || emailList[1].contains("..")){ //Check if email is binas@binas..binas ...
			return false;
		}

		return true;
	}

	public synchronized UserView activateUser(String email) throws EmailExists_Exception, InvalidEmail_Exception{
		if(email == null){
			ExceptionManager.invalidEmail(email);
		} else {
			if(!checkEmail(email)){
				ExceptionManager.invalidEmail(email);
			}
			if(users.containsKey(email)){
				ExceptionManager.emailExists(email);
			}
		}

		User user = new User(email,10);
		setBalance(email,10);
		this.users.put(email,user);
		UserView uv = new UserView();
		uv.setHasBina(user.hasBina());
		uv.setCredit(user.getCredit());
		uv.setEmail(user.getEmail());
		return uv;
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
			} catch (Exception se) {
				hasMore = false;
			}
			currentStation += 1;
		}
	}

    public List<StationView> listStations(Integer k, CoordinatesView coordinates) {
        ArrayList<StationView> Stations = new ArrayList<StationView>();
        if (coordinates == null || k == 0) return Stations;
        SortedSet<StationView> distances = new TreeSet<StationView>(new Comparator<StationView>() {
			@Override
			public int compare(StationView o1, StationView o2) {
				CoordinatesView coord1 = o1.getCoordinate();
				CoordinatesView coord2 = o2.getCoordinate();
				float distanceX1 = Math.abs(coord1.getX() - coordinates.getX());
				float distanceY1 = Math.abs(coord1.getY() - coordinates.getY());
				float distanceX2 = Math.abs(coord2.getX() - coordinates.getX());
				float distanceY2 = Math.abs(coord2.getY() - coordinates.getY());

				float distance1 = (float)Math.sqrt(distanceX1*distanceX1 + distanceY1*distanceY1);
				float distance2 = (float)Math.sqrt(distanceX2*distanceX2 + distanceY2*distanceY2);

				if (distance1 == distance2) return 1;
				if (distance1 > distance2) return 1;
				return -1;
			}
		});

        for (String station : connectedStations.keySet()) {
            try {
				distances.add(getInfoStation(station));
            } catch (InvalidStation_Exception ise) {
                //Station is invalid
            }
        }
        for (StationView station : distances) {
            Stations.add(station);
            if (Stations.size() >= k) {
                break;
            }
        }
        return Stations;
    }

	public int getUserCredit(String email) throws UserNotExists_Exception {
		if(!validString(email)) ExceptionManager.userNotFound(email);
		return getBalance(email);
	}

    public void getBina(String stationId, String email) throws AlreadyHasBina_Exception, InvalidStation_Exception, NoCredit_Exception, UserNotExists_Exception,NoBinaAvail_Exception {

		StationClient station = getStation(stationId);
		User user = getUserByEmail(email);

		//Check if credits stored in cache
		Integer savedCredits = cachedCredits.get(email);
        int userCredit = savedCredits != null ? savedCredits.intValue() : getBalance(email);

		if (user.hasBina()) {
			ExceptionManager.alreadyHasBina();
		}

		if (userCredit <= 0) {
			ExceptionManager.noCreditException();
		}

		try {
			station.getBina();
		} catch (org.binas.station.ws.NoBinaAvail_Exception e) {
			ExceptionManager.noBinaAvail();
		}
		user.setHasBina(true);

        //Save new credit in cache
        try {
            //Search cachedTimestamps for least up-to-date user balance
            while (cachedCredits.size() >= 6) {
                removeOutdatedFromCache();
            }
            cachedCredits.put(email, userCredit-1);
            cachedTimestamps.put(email, new Timestamp((new Date()).getTime()));
        } catch (Exception e) {
            //TODO: Verify this
            throw new StationsUnavailableException("[ERROR] Couldn't parse date");
        }

		setBalance(email,userCredit-1);
    }
	
	public void returnBina(String stationId,String email) throws InvalidStation_Exception, UserNotExists_Exception, NoBinaRented_Exception, FullStation_Exception {
		
		if(!validString(stationId)) ExceptionManager.stationNotFound(stationId);
		if(!validString(email)) ExceptionManager.userNotFound(email);
		
		StationClient station = getStation(stationId);
		User user = getUserByEmail(email);
		if (!user.hasBina()) {
			ExceptionManager.noBinaRented();
		}
		try {
			int bonus = station.returnBina();
			user.addBonus(bonus);

			//Look for this user's credits in cache
            Integer savedCredits = cachedCredits.get(email);
            int credits = savedCredits != null ? savedCredits.intValue() + bonus : getBalance(email) + bonus;

            //Save new credit in cache
            try {
                //Search cachedTimestamps for least up-to-date user balance
                while (cachedCredits.size() >= 6) {
                    removeOutdatedFromCache();
                }
                cachedCredits.put(email, credits);
                cachedTimestamps.put(email, new Timestamp((new Date()).getTime()));
            } catch (Exception e) {
                //TODO: Verify this
                throw new StationsUnavailableException("[ERROR] Couldn't parse date");
            }

			setBalance(email, credits);
			user.setHasBina(false);
		} catch (NoSlotAvail_Exception e) {
			ExceptionManager.fullStation();
		}
	}

	public synchronized void testClear() {

		for (StationClient station : connectedStations.values()) {
			station.testClear();
		}
		users = new HashMap<String,User>();
        cachedCredits =  new HashMap<String,Integer>();
        cachedTimestamps =  new HashMap<String,Timestamp>();

        finished = 0;
        exceptionCount = 0;
        mostUpToDate = null;
        credit = -1;
	}

	public synchronized void usersInit(int userInitialPoints) throws BadInit_Exception {
		if(userInitialPoints<0) ExceptionManager.badInit();
		String userEmail1 = "testing1@text.com";
		String userEmail2 = "testing2@text.com";
		String userEmail3 = "testing3@text.com";
		User user1 = new User(userEmail1,userInitialPoints);
		setBalance(userEmail1,userInitialPoints);
		User user2 = new User(userEmail2,userInitialPoints);
		setBalance(userEmail2,userInitialPoints);
		User user3 = new User(userEmail3,userInitialPoints);
		setBalance(userEmail3,userInitialPoints);
		users.put(userEmail1, user1);
		users.put(userEmail2,user2);
		users.put(userEmail3,user3);
	}

	public void stationInit(String stationId, int x, int y, int capacity, int returnPrize) throws BadInit_Exception{
		try {
			connectedStations.get(stationId).testInit(x, y, capacity, returnPrize);
		} catch (org.binas.station.ws.BadInit_Exception e) {
			ExceptionManager.badInit();
		}
	}

	private boolean validString(String input) {
		if(input==null) return false;
		if(input.trim().equals("")) return false;
		else return true;
	}

	
	//This method consults all the stations(replicas) and chooses the most up-to-date copy of the user's credit
	public synchronized int getBalance(String email)throws UserNotExists_Exception,StationsUnavailableException{

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
		Integer nStations = connectedStations.values().size();
		Integer errorCount = 0;
        finished = 0;

        Integer savedCredits = cachedCredits.get(email);
        if (savedCredits != null) {
            return savedCredits.intValue();
        }

        AsyncHandler<GetBalanceResponse> handler = new AsyncHandler<GetBalanceResponse>() {
            @Override
            public void handleResponse(Response<GetBalanceResponse> response) {
                try {
                    System.out.println();
                    System.out.println("Getbalance call result arrived!");
                    finished++;
                    Date parsedDate = dateFormat.parse(response.get().getBalanceInfo().getTimeStamp());
                    Timestamp readTimestamp = new Timestamp(parsedDate.getTime());
                    if(mostUpToDate == null || readTimestamp.after(mostUpToDate)) {
                        credit = response.get().getBalanceInfo().getNewBalance();
                        mostUpToDate = readTimestamp;
                    }
                } catch (InterruptedException e) {
                    System.out.println("Caught interrupted exception.");
                    System.out.print("Cause: ");
                    System.out.println(e.getCause());
                } catch (ExecutionException e) {
                    System.out.println("Caught execution exception.");
                    System.out.println(e.getCause());
                    exceptionCount+=1;
                } catch (ParseException pe) {
                    pe.printStackTrace();
                }
            }
        };
		
		for(StationClient station : connectedStations.values()) {
			try {
                station.getBalanceAsync(email, handler);
                /*
				BalanceView read = station.getBalance(email);
				Date parsedDate = dateFormat.parse(read.getTimeStamp());
				Timestamp readTimestamp = new Timestamp(parsedDate.getTime());

				if(mostUpToDate == null || readTimestamp.after(mostUpToDate)) {
					//Choosing the most recent copy
					credit = read.getNewBalance();
					mostUpToDate = readTimestamp;
				}
				*/
			}
			catch(Exception e) {
				errorCount+=1;
				if(errorCount >= nStations/2 +1) {
					//should NEVER happen, but just in case it does
					throw new StationsUnavailableException("[ERROR] Not enough stations for Quorum Consensus.");
				}
			}
		}

		try {
            while (finished < (nStations/2 +1)) {
                Thread.sleep(50);

                if (exceptionCount >= (nStations/2 +1)) {
                    //Only if it does not exist in the majority of the stations
                    ExceptionManager.userNotFound(email);
                }

                System.out.print("."); // do something usefull while waiting...
                System.out.flush();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        System.out.println("|"); System.out.println("|");

        //Search cachedTimestamps for least up-to-date user balance
        while (cachedCredits.size() >= 6) {
            removeOutdatedFromCache();
        }
        cachedCredits.put(email, credit);
        cachedTimestamps.put(email, mostUpToDate);
		
		return credit;
	}

	public synchronized void setBalance(String email,int newBalance)throws StationsUnavailableException{
		Integer nStations = connectedStations.values().size();
		Integer errorCount = 0;

		for(StationClient station : connectedStations.values()) {
			try {
				String nowDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(new Date());
				BalanceView bv = new BalanceView();
				bv.setTimeStamp(nowDate);
				bv.setNewBalance(newBalance);
                Response<SetBalanceResponse> balanceResponse = station.setBalanceAsync(email,bv);
			} catch(Exception e) {
				errorCount+=1;
				if(errorCount >= nStations/2 +1) {
					//should NEVER happen, but just in case it does
					throw new StationsUnavailableException("[ERROR] Not enough stations for Quorum Consensus.");
				}
			}
		}
	}

	private void removeOutdatedFromCache() {
        Timestamp oldest = null;
        String toRemove = null;
        for (Map.Entry<String, Timestamp> entry: cachedTimestamps.entrySet()) {
            if (oldest == null || oldest.before(entry.getValue())) {
                oldest = entry.getValue();
                toRemove = entry.getKey();
            }
        }
        cachedCredits.remove(toRemove);
        cachedTimestamps.remove(toRemove);
    }

}
