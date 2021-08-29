package org.fog.placement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.mobilitydata.Location;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.References;
import org.fog.utils.Config;

public class LocationHandler {
	
	public DataParser dataObject;
	public Map<Integer, String> instanceToDataId;
	

	public LocationHandler(DataParser dataObject) {
		// TODO Auto-generated constructor stub
		this.dataObject = dataObject;
		instanceToDataId = new HashMap<Integer, String>();
		
	}

	public LocationHandler() {
		// TODO Auto-generated constructor stub

	}
	
	public DataParser getDataObject(){
		return dataObject;
	}
	
	public static double calculateDistance(Location loc1, Location loc2) {

	    final int R = 6371; // Radius of the earth in Kilometers

	    double latDistance = Math.toRadians(loc1.latitude - loc2.latitude);
	    double lonDistance = Math.toRadians(loc1.longitude - loc2.longitude);
	    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	            + Math.cos(Math.toRadians(loc1.latitude)) * Math.cos(Math.toRadians(loc2.latitude))
	            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = R * c; // kms


	    distance = Math.pow(distance, 2);

	    return Math.sqrt(distance);
	}
	

	public int determineParent(int resourceId, double time) {
		// TODO Auto-generated method stub
		String dataId = getDataIdByInstanceID(resourceId);
		int resourceLevel=getDataObject().resourceAndUserToLevel.get(dataId);
		int parentLevel = resourceLevel-1;
		Location resourceLoc;
		if(resourceLevel!=getDataObject().levelID.get("User"))
			resourceLoc = getResourceLocationInfo(dataId);
		else
			resourceLoc = getUserLocationInfo(dataId,time);
		
		int parentInstanceId = References.NOT_SET;	
		String parentDataId = "";
				
	
		if(time<References.INIT_TIME){
			for(int i=0; i<getLevelWiseResources(parentLevel).size();i++){
				Location potentialParentLoc = getResourceLocationInfo(getLevelWiseResources(parentLevel).get(i));
				if(potentialParentLoc.block==resourceLoc.block) {
					parentDataId = getLevelWiseResources(parentLevel).get(i);
					for(int parentIdIterator: instanceToDataId.keySet())
					{
						if(instanceToDataId.get(parentIdIterator).equals(parentDataId))
						{
							parentInstanceId = parentIdIterator;
						}
					}
				}	
			}
		}
		else
		{
			double minmumDistance = Config.MAX_VALUE;
			for(int i=0; i<getLevelWiseResources(parentLevel).size();i++){
				Location potentialParentLoc = getResourceLocationInfo(getLevelWiseResources(parentLevel).get(i));
				
				double distance = calculateDistance(resourceLoc, potentialParentLoc);
					if(distance<minmumDistance){
						parentDataId = getLevelWiseResources(parentLevel).get(i);
						minmumDistance = distance;
					}
			}
			
			for(int parentIdIterator: instanceToDataId.keySet())
			{
				if(instanceToDataId.get(parentIdIterator).equals(parentDataId))
				{
					parentInstanceId = parentIdIterator;
				}
			}
			
		}
		
		return parentInstanceId;	
	}	

	private Location getUserLocationInfo(String dataId, double time) {
		// TODO Auto-generated method stub
		return getDataObject().usersLocation.get(dataId).get(time);
	}

	private Location getResourceLocationInfo(String dataId) {
		// TODO Auto-generated method stub
		return getDataObject().resourceLocationData.get(dataId);
	}

	
	public List<Double> getTimeSheet(int instanceId) {
		
		String dataId = getDataIdByInstanceID(instanceId);
		List<Double>timeSheet = new ArrayList<Double>(getDataObject().usersLocation.get(dataId).keySet());
		return timeSheet;
	}

	public void linkDataWithInstance(int instanceId, String dataID) {
		// TODO Auto-generated method stub
		instanceToDataId.put(instanceId, dataID);
	}

	public int getLevelID(String resourceType) {
		// TODO Auto-generated method stub
		return dataObject.levelID.get(resourceType);
	}
	
	public ArrayList<String> getLevelWiseResources(int levelNo) {
		// TODO Auto-generated method stub
		return getDataObject().levelwiseResources.get(levelNo);
	}

	public void parseUserInfo(Map<Integer, Integer> userMobilityPattern, String datasetReference) throws IOException {
		// TODO Auto-generated method stub
		getDataObject().parseUserData(userMobilityPattern, datasetReference);
	}

	public void parseResourceInfo() throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		getDataObject().parseResourceData();
	}

	public List<String> getMobileUserDataId() {
		// TODO Auto-generated method stub
		List<String> userDataIds = new ArrayList<>(getDataObject().usersLocation.keySet());
		return userDataIds;
		
	}

	public Map<String, Integer> getDataIdsLevelReferences() {
		// TODO Auto-generated method stub
		return getDataObject().resourceAndUserToLevel;
	}
	
	public boolean isCloud(int instanceID) {
		// TODO Auto-generated method stub
		String dataId = getDataIdByInstanceID(instanceID);
		int instenceLevel=getDataObject().resourceAndUserToLevel.get(dataId);
		if(instenceLevel==getDataObject().levelID.get("Cloud"))
			return true;
		else
			return false;
	}
	
	public String getDataIdByInstanceID(int instanceID) {
		// TODO Auto-generated method stub
		return instanceToDataId.get(instanceID);
	}
	
	public Map<Integer, String> getInstenceDataIdReferences() {
		// TODO Auto-generated method stub
		return instanceToDataId;
	}

	public boolean isAMobileDevice(int instanceId) {
		// TODO Auto-generated method stub
		String dataId = getDataIdByInstanceID(instanceId);
		int instenceLevel=getDataObject().resourceAndUserToLevel.get(dataId);
		if(instenceLevel==getDataObject().levelID.get("User"))
			return true;
		else
			return false;
	}
}
