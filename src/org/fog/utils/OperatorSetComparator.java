package org.fog.utils;

import java.util.Comparator;
import java.util.List;

public class OperatorSetComparator implements Comparator<List<String>>{

	@Override
	public int compare(List<String> arg0, List<String> arg1) {
		return (arg1.size() - arg0.size());
	}

}
