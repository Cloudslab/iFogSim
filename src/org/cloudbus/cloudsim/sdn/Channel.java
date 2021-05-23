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

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

/** 
 * This class represents a channel for transmission of data between switches.
 * It controls sharing of available bandwidth. Relation between
 * Transmission and Channel is the same as Cloudlet and CloudletScheduler,
 * but here we consider only the time shared case, representing a shared
 * channel among different simultaneous package transmissions.
 *
 * This is logical channel. One physical link (class Link) can hold more than one logical channels (class Channel).
 * Channel is directional. It is one way.
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class Channel {
	private List<Node> nodes;
	private List<Link> links;

	private double allocatedBandwidth; // Actual bandwidth allocated to the channel
	private double previousTime;

	private LinkedList<Transmission> inTransmission;
	private LinkedList<Transmission> completed;
	
	private final int srcId;
	private final int dstId;
	private final int chId;
	private final double requestedBandwidth;	// Requested by user
	
	public Channel(int chId, int srcId, int dstId, List<Node> nodes, List<Link> links, double bandwidth) {
		this.chId = chId;
		this.srcId = srcId;
		this.dstId = dstId;
		this.nodes = nodes;
		this.links = links;
		this.allocatedBandwidth = bandwidth;
		this.requestedBandwidth = bandwidth;
		this.inTransmission = new LinkedList<Transmission>();
		this.completed = new LinkedList<Transmission>();
	}
	
	public void initialize() {
		// Assign BW to all links
		for(int i=0; i<nodes.size()-1; i++) {
			Node from = nodes.get(i);
			Link link = links.get(i);
			
			link.addChannel(from, this);
			
			from.updateNetworkUtilization();
		}
		nodes.get(nodes.size()-1).updateNetworkUtilization();
	}
	
	public void terminate() {
		// Assign BW to all links
		for(int i=0; i<nodes.size()-1; i++) {
			Link link = links.get(i);
			
			link.removeChannel(this);
			
			Node node = nodes.get(i);
			node.updateNetworkUtilization();
		}
		nodes.get(nodes.size()-1).updateNetworkUtilization();
	}
	
	private double getLowestSharedBandwidth() {
		// Get the lowest bandwidth along links in the channel
		double lowestSharedBw = Double.POSITIVE_INFINITY;

		for(int i=0; i<nodes.size()-1; i++) {
			Node from = nodes.get(i);
			Node to = nodes.get(i+1);
			Link link = links.get(i);
			
			if(lowestSharedBw > link.getSharedBandwidthPerChannel(from, to))
				lowestSharedBw = link.getSharedBandwidthPerChannel(from, to);
		}
		return lowestSharedBw;
		
	}
	
	public double getAdjustedRequestedBandwidth() {
		double lowest_factor = 1.0;
		
		// Find the slowest link (low bw) among all links where this channel is passing through
		for(int i=0; i<nodes.size()-1; i++) {
			Node from = nodes.get(i);
			Link link = links.get(i);
			
			double factor = link.getDedicatedChannelAdjustFactor(from);
			if(lowest_factor > factor)
				lowest_factor = factor;
		}
		
		return lowest_factor;

	}
	public boolean adjustDedicatedBandwidthAlongLink() {
		if(chId == -1) 
			return false;
		
		double lowestLinkBwShared = Double.POSITIVE_INFINITY;
		double factor = this.getAdjustedRequestedBandwidth(); 
		double adjustedBandwidth = this.getRequestedBandwidth() * factor;
		if(factor < 1.0) {
			Log.printLine("Link.adjustDedicatedBandwidthAlongLink(): Cannot allocate requested amount of BW"
					+adjustedBandwidth+"/"+this.getRequestedBandwidth());
		}			

		// Find the slowest link (low bw) among all links where this channel is passing through
		for(int i=0; i<nodes.size()-1; i++) {
			Node from = nodes.get(i);
			Link link = links.get(i);
			
			double link_bw = link.getBw();
			int numChannels = link.getChannelCount(from);
			
			double link_bw_per_channel = link_bw / numChannels;
			
			if(lowestLinkBwShared > link_bw_per_channel)
				lowestLinkBwShared = link_bw_per_channel;
		}
		
		// Dedicated channel.
		if(adjustedBandwidth < lowestLinkBwShared) {
			changeBandwidth(lowestLinkBwShared);
			return true;				
		}
		else if(this.allocatedBandwidth != adjustedBandwidth) {
			changeBandwidth(adjustedBandwidth);
			return true;
		}
		
		return false;
	}
	public boolean adjustSharedBandwidthAlongLink() {
		if(chId != -1) 
			return false;

		// Get the lowest bandwidth along links in the channel
		double lowestLinkBw = getLowestSharedBandwidth();

		if(this.allocatedBandwidth != lowestLinkBw) {
			changeBandwidth(lowestLinkBw);
			return true;
		}
		return false;
	}
	
	public boolean changeBandwidth(double newBandwidth){
		if (newBandwidth == allocatedBandwidth)
			return false; //nothing changed
		
		boolean isChanged = this.updatePackageProcessing();
		this.allocatedBandwidth=newBandwidth;
		
		return isChanged;
	}
	
	public double getAllocatedBandwidth() {
		return allocatedBandwidth;
	}
	
	private double getAllocatedBandwidthPerTransmission() {
		// If this channel shares a link with another channel, this channel might not get the full BW from link.
		if(inTransmission.size() == 0) {
			return getAllocatedBandwidth();
		}
		
		return getAllocatedBandwidth()/inTransmission.size();
	}
	
	public int getActiveTransmissionNum() {
		return inTransmission.size();
	}
	/**
	 * Updates processing of transmissions taking place in this Channel.
	 * @param currentTime current simulation time (in seconds)
	 * @return delay to next transmission completion or
	 *         Double.POSITIVE_INFINITY if there is no pending transmissions
	 */
	public boolean updatePackageProcessing(){
		double currentTime = CloudSim.clock();
		double timeSpent = NetworkOperatingSystem.round(currentTime - this.previousTime);
		
		if(timeSpent <= 0 || inTransmission.size() == 0)
			return false;	// Nothing changed

		//update the amount of transmission 
		long processedThisRound =  Math.round(timeSpent*getAllocatedBandwidthPerTransmission());
		
		//update transmission table; remove finished transmission
		LinkedList<Transmission> completedTransmissions = new LinkedList<Transmission>();
		for(Transmission transmission: inTransmission){
			transmission.addCompletedLength(processedThisRound);
			
			if (transmission.isCompleted()){
				completedTransmissions.add(transmission);
				this.completed.add(transmission);
			}	
		}
		
		this.inTransmission.removeAll(completedTransmissions);
		previousTime=currentTime;

		Log.printLine(CloudSim.clock() + ": Channel.updatePackageProcessing() ("+this.toString()+"):Time spent:"+timeSpent+
				", BW/host:"+getAllocatedBandwidthPerTransmission()+", Processed:"+processedThisRound);
		
		if(completedTransmissions.isEmpty())
			return false;	// Nothing changed
		return true;
	}
	
	// Estimated finish time of one transmission
	private double estimateFinishTime(Transmission t) {
		double bw = getAllocatedBandwidthPerTransmission();
		
		if(bw == 0) {
			return Double.POSITIVE_INFINITY;
		}
		
		double eft= (double)t.getSize()/bw;
		return eft;
	}
	
	// The earliest finish time among all transmissions in this channel 
	public double nextFinishTime() {
		//now, predicts delay to next transmission completion
		double delay = Double.POSITIVE_INFINITY;

		for (Transmission transmission:this.inTransmission){
			double eft = estimateFinishTime(transmission);
			if (eft<delay)
				delay = eft;
		}
		
		if(delay == Double.POSITIVE_INFINITY) {
			return delay;
		}
		else if(delay < 0) {
			throw new IllegalArgumentException("Channel.nextFinishTime: delay"+delay);
		}

		delay=NetworkOperatingSystem.round(delay);

		if (delay < NetworkOperatingSystem.getMinTimeBetweenNetworkEvents()) { 
			//Log.printLine(CloudSim.clock() + ":Channel: delay is too short: "+ delay);
			delay = NetworkOperatingSystem.getMinTimeBetweenNetworkEvents();
		}
		
		return delay;
	}

	/**
	 * Adds a new Transmission to be submitted via this Channel
	 * @param transmission transmission initiating
	 * @return estimated delay to complete this transmission
	 * 
	 */
	public double addTransmission(Transmission transmission){
		if (this.inTransmission.isEmpty()) 
			previousTime=CloudSim.clock();
		
		this.inTransmission.add(transmission);
		double eft = estimateFinishTime(transmission);

		return eft;
	}

	/**
	 * Remove a transmission submitted to this Channel
	 * @param transmission to be removed
	 * 
	 */
	public void removeTransmission(Transmission transmission){
		inTransmission.remove(transmission);
	}

	/**
	 * @return list of Packages whose transmission finished, or empty
	 *         list if no package arrived.
	 */
	public LinkedList<Transmission> getArrivedPackages(){
		LinkedList<Transmission> returnList = new LinkedList<Transmission>();

		if (!completed.isEmpty()){
			returnList.addAll(completed);
		}
		completed.removeAll(returnList);

		return returnList;
	}
	
	public int getChId() {
		return chId;
	}

	public double getLastUpdateTime(){
		return previousTime;
	}
	
	public String toString() {
		return "Channel("+this.srcId+"->"+this.dstId+"|"+this.chId
				+"): BW:"+allocatedBandwidth+", Transmissions:"+inTransmission.size();
	}

	public Node getLastNode() {
		Node node = this.nodes.get(this.nodes.size()-1);
		return node;
	}

	public int getSrcId() {
		return srcId;
	}

	public int getDstId() {
		return dstId;
	}

	public double getRequestedBandwidth() {
		return requestedBandwidth;
	}
}
