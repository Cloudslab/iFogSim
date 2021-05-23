package org.cloudbus.cloudsim.sdn.request;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.random.Well19937c;

/* Generate VM requests, for example:
 *  
{
  "nodes" : [
    {
      "name" : "vm01",
      "type" : "vm",
      "size" : 1000,
      "pes": 1,
      "mips" : 30000000,
      "ram"  : 512,
      "bw"  : 100000,
      "starttime": 1.3,
      "endtime"  : 20.5,
    },
   ],
  "links" : [
    { 
    	"name": "l32", 
    	"source" : "vm03" , 
    	"destination" : "vm02" , 
    	"bandwidth" : 66000000
    },
   ],
 */
public class VMRequestGenerator {
	
	public static void main(String [] argv) {
		int numVms = 100;
		String jsonFileName = "virtual2.json";
		
		VMRequestGenerator reqg = new VMRequestGenerator();
		
		List<VMSpec> vms = reqg.generateVMs(numVms);
		List<LinkSpec> links = reqg.generateLinks();
		
		reqg.wrtieJSON(vms, links, jsonFileName);

	}

	class VMSpec {
		String name;
		String type;
		long size;
		int pe;
		long mips;
		int ram;
		long bw;
		double starttime;
		double endtime;

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
			obj.put("starttime", vm.starttime);
			obj.put("endtime", vm.endtime);

			return obj;
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
				obj.put("bw", link.bw);
			return obj;
		}
	}
	
	int vmId = 0;
	
	@SuppressWarnings("unchecked")
	public void wrtieJSON(List<VMSpec> vms, List<LinkSpec> links, String jsonFileName) {
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
			file.write(obj.toJSONString());
			file.flush();
			file.close();
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
	 
		System.out.println(obj);
	}
	
	enum VMtype {
		WebServer,
		AppServer,
		DBServer,
		Proxy,
		Firewall
	}
	
	public VMSpec generateVM(long vmSize, int pes, long mips, int vmRam, long bw, double startTime, double endTime) {
		VMSpec vm = new VMSpec();
		
		vm.name = "vm"+ String.format("%02d", vmId++);
		vm.type = "vm";
		vm.size = vmSize;
		vm.pe = pes;
		vm.mips = mips;
		vm.ram = vmRam;
		vm.bw = bw;
		vm.starttime = startTime;
		vm.endtime = endTime;
		
		return vm;
	}
	
	public VMSpec generateVM(VMtype vmtype, double startTime, double endTime) {
		int pes = 1;
		long vmSize = 1000;
		long mips=2000;
		int vmRam = 512;
		long bw=1000000000/10;

		switch(vmtype) {
		case WebServer:
			//m1.large
			mips=2000;
			pes=2;
			break;
		case AppServer:
			//m2.xlarge
			mips=3000L;
			pes=8;
			break;
		case DBServer:
			//c1.xlarge
			mips=2400L;
			pes=8;
			break;
		case Proxy:
			mips=2000;
			pes=8;
			bw=500000000;
			break;
		case Firewall:
			mips=3000L;
			pes=8;
			bw=500000000;
			break;
		}
		return generateVM(vmSize, pes, mips, vmRam, bw, startTime, endTime);
	}
	
	public List<VMSpec> generateVMGroup(int numVMsInGroup, double startTime, double endTime) {
		System.out.printf("Generating VM Group(%d): %f - %f\n", numVMsInGroup, startTime, endTime);
		
		List<VMSpec> vms = new ArrayList<VMSpec>();
		
		switch(numVMsInGroup) {
		case 2:
			vms.add(this.generateVM(VMtype.WebServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.AppServer, startTime, endTime));
			break;
		case 3:
			vms.add(this.generateVM(VMtype.WebServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.AppServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.DBServer, startTime, endTime));
			break;
		case 4:
			vms.add(this.generateVM(VMtype.WebServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.AppServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.DBServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.Proxy, startTime, endTime));
			break;
		case 5:
			vms.add(this.generateVM(VMtype.WebServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.AppServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.DBServer, startTime, endTime));
			vms.add(this.generateVM(VMtype.Proxy, startTime, endTime));
			vms.add(this.generateVM(VMtype.Firewall, startTime, endTime));
			break;
		default:
			System.err.println("Unknown group number"+numVMsInGroup);
			break;
		}
		
		return vms;
	}
	
	private static long seed = 10;
	
	public List<VMSpec> generateVMs(int totalVmNum) {
		double lastStartTime = 0;
		
		double startMean = 1800; // sec = 30min
		double durScale=14400; // sec = 4 hours
		double durShape=1.2;
		
		Random rVmNum = new Random(seed);
		ExponentialDistribution rStartTime = new ExponentialDistribution(new Well19937c(seed), startMean, ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);	
		ParetoDistribution rDuration = new ParetoDistribution(new Well19937c(seed), durScale, durShape);
		
		List<VMSpec> vms = new ArrayList<VMSpec>();

		while(this.vmId < totalVmNum) {
			int vmsInGroup = rVmNum.nextInt(4)+2;
			double duration = Math.floor(rDuration.sample());
			
			vms.addAll(generateVMGroup(vmsInGroup, lastStartTime, lastStartTime+duration));
			lastStartTime += Math.floor(rStartTime.sample());
		}
		
		return vms;
	}
	
	public List<LinkSpec> generateLinks() {
		// Dummy links 
		List<LinkSpec> links = new ArrayList<LinkSpec>();

		links.add(new LinkSpec("default", "vm01","vm02", null));
		links.add(new LinkSpec("default", "vm02","vm01", null));
		links.add(new LinkSpec("default", "vm02","vm03", null));
		links.add(new LinkSpec("default", "vm03","vm02", null));
		
		return links;
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	    //return value;
	}
	
	
}
