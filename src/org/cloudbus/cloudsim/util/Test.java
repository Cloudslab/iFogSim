package org.cloudbus.cloudsim.util;

public class Test {
	public String s1;
	public int i1;
	
	public Test(int i){
		s1 = new String("chas");
		i1=i;
	}
	
	public boolean equals(Test t){
		if(t.s1.equals(s1) && t.i1==(i1) )
			return true;
		return false;
	}
	
}
