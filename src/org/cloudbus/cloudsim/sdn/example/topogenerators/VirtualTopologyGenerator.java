/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example.topogenerators;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Generate virtual topology Json file from pre-configured VM type sets.
 * VM types are defined in another class - VirtualTopologyGeneratorVmTypes.
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public class VirtualTopologyGenerator {
	private List<VMSpec> vms = new ArrayList<VMSpec>();
	private List<LinkSpec> links = new ArrayList<LinkSpec>();
	private List<DummyWorkloadSpec> dummyWorkload = new ArrayList<DummyWorkloadSpec>();

	public VMSpec addVM(String name, VMSpec spec) {
		return addVM(name, spec.pe, spec.mips, spec.ram, spec.size, spec.bw, spec.starttime, spec.endtime);
	}
	public VMSpec addVM(String name, int pes, long mips, int ram, long storage, long bw, double starttime, double endtime) {
		VMSpec vm = new VMSpec(pes, mips, ram, storage, bw, starttime, endtime);
		vm.name = name;
		
		vms.add(vm);
		return vm;
	}
	
	
	public LinkSpec addLink(String linkname, VMSpec source, VMSpec dest, Long bw) {
		LinkSpec link = new LinkSpec(linkname, source.name,dest.name, bw);
		links.add(link);
		
		addWorkload(linkname, source, dest);
		return link;
	}
	
	public void addWorkload(String linkname, VMSpec source, VMSpec dest) {
		DummyWorkloadSpec wl = new DummyWorkloadSpec(source.starttime, source.name,dest.name, linkname);
		this.dummyWorkload.add(wl);
	}
	
	public VMSpec createVmSpec(int pe, long mips, int ram, long storage, long bw, double starttime, double endtime) {
		return new VMSpec(pe, mips, ram, storage, bw, starttime, endtime);
	}

	class VMSpec {
		String name;
		String type;
		long size;
		int pe;
		long mips;
		int ram;
		long bw;
		double starttime = -1;
		double endtime = -1;
		
		public VMSpec(int pe, long mips, int ram, long storage, long bw,double starttime,double endtime) {
			this.pe = pe;
			this.mips = mips;
			this.ram = ram;
			this.size = storage;
			this.bw = bw;
			this.type = "vm";
			this.starttime = starttime;
			this.endtime = endtime;
		}
		
		@SuppressWarnings("unchecked")
		JSONObject toJSON() {
			VMSpec vm = this;
			JSONObject obj = new JSONObject();
			obj.put("name", vm.name);
			obj.put("type", vm.type);
			obj.put("size", vm.size);
			obj.put("pes", vm.pe);
			obj.put("mips", vm.mips);
			obj.put("ram", new Integer(vm.ram));
			obj.put("bw", vm.bw);
			if(vm.starttime != -1)
				obj.put("starttime", vm.starttime);
			if(vm.endtime != -1)
				obj.put("endtime", vm.endtime);

			return obj;
		}
	}

	class DummyWorkloadSpec {
		double startTime;
		String source;
		String linkname;
		String destination;
		
		public DummyWorkloadSpec(double startTime, String source,String destination,String linkname) {
			this.linkname = linkname;
			this.source = source;
			this.destination = destination;
			this.startTime = startTime;
		}
		public String toString() {
			String st = startTime+ ","+ source + ",0,1,"+linkname+","+destination + ",1000000000000000,1";
			return st;
		}
	}
	class LinkSpec {
		String name;
		String source;
		String destination;
		Long bw;
		
		public LinkSpec(String name,String source,String destination,Long bw) {
			this.name = name;
			this.source = source;
			this.destination = destination;
			this.bw = bw;
		}
		@SuppressWarnings("unchecked")
		JSONObject toJSON() {
			LinkSpec link = this;
			JSONObject obj = new JSONObject();
			obj.put("name", link.name);
			obj.put("source", link.source);
			obj.put("destination", link.destination);
			if(link.bw != null)
				obj.put("bandwidth", link.bw);
			return obj;
		}
	}
	
	int vmId = 0;
	
	@SuppressWarnings("unchecked")
	public void wrtieJSON(String jsonFileName) {
		JSONObject obj = new JSONObject();

		JSONArray vmList = new JSONArray();
		JSONArray linkList = new JSONArray();
		
		for(VMSpec vm:vms) {
			vmList.add(vm.toJSON());
		}
		
		for(LinkSpec link:links) {
			linkList.add(link.toJSON());
		}
		
		obj.put("nodes", vmList);
		obj.put("links", linkList);
	 
		try {
	 
			FileWriter file = new FileWriter(jsonFileName);
			file.write(obj.toJSONString().replaceAll(",", ",\n"));
			file.flush();
			file.close();
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
	 
		System.out.println(obj);
		
		System.out.println("===============WORKLOAD=============");
		System.out.println("start, source, z, w1, link, dest, psize, w2");
		for(DummyWorkloadSpec wl:this.dummyWorkload) {
			System.out.println(wl);
		}
	}
}
