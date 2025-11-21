/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.util.LinkedList;
import java.util.List;


/**
 * Request class represents a message submitted to VM. Each request has a list of activities
 * that should be performed at the VM. (Processing and Transmission)
 *   
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class Request {
	
	long requestId;
	int userId;
	LinkedList<Activity> activities;
	
	private LinkedList<Activity> removedActivites;	//Logging purpose only

	public Request(long requestId, int userId){
		this.requestId=requestId;
		this.userId=userId;
		this.activities = new LinkedList<Activity>();
		
		this.removedActivites = new LinkedList<Activity>();
		
	}
	
	public long getRequestId(){
		return requestId;
	}
	
	public int getUserId(){
		return userId;
	}
		
	public boolean isFinished(){
		return activities.size()==0;
	}
	
	public void addActivity(Activity act){
		activities.add(act);
	}
	
	public Activity getNextActivity(){
		Activity act = activities.get(0);
		return act;
	}
	
	public Transmission getNextTransmission() {
		for(Activity act:activities) {
			if(act instanceof Transmission)
				return (Transmission) act;
		}
		return null;
	}
	
	public Activity removeNextActivity(){
		Activity act = activities.remove(0);
		
		this.removedActivites.add(act);

		return act;
	}
	public String toString() {
		return "Request. UserID:"+ this.userId + ",Req ID:"+this.requestId;
	}
	
	public List<Activity> getRemovedActivities() {
		return this.removedActivites;
	}
}
