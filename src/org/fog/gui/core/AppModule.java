package org.fog.gui.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class AppModule extends Node {
	private static final long serialVersionUID = 804858850147477656L;
	
	

	public AppModule() {
	}

	public AppModule(String name) {
		super(name, "APP_MODULE");
	}

	
	@Override
	public String toString() {
		return "Node []";
	}

}
