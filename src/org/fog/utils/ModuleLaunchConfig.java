package org.fog.utils;

import org.fog.application.AppModule;

public class ModuleLaunchConfig {

	private AppModule module;
	private int instanceCount;
	
	public ModuleLaunchConfig(AppModule module, int instanceCount){
		setModule(module);
		setInstanceCount(instanceCount);
	}
	
	public AppModule getModule() {
		return module;
	}
	public void setModule(AppModule module) {
		this.module = module;
	}
	public int getInstanceCount() {
		return instanceCount;
	}
	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}
	
}
