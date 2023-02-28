package org.fog.utils;

/**
 * 地图覆盖范围 形状为一个矩形 lat_l 代表低纬度的边 lat_u 代表高纬度的边，long_l代表高经度的边，long_u代表低经度的边
 */
public class GeoCoverage {

	private double lat_l;
	private double lat_u;
	private double long_l;
	private double long_u;
	
	public GeoCoverage(double lat_l, double lat_u, double long_l, double long_u){
		this.lat_l = lat_l;
		this.lat_u = lat_u;
		this.long_l = long_l;
		this.long_u = long_u;
	}

	public boolean covers(GeoCoverage geo){
		if(this.lat_l <= geo.lat_l && this.lat_u >= geo.lat_u && this.long_l <= geo.long_l && this.long_u >= geo.long_u)
			return true;
		return false;
	}
	
	public double getLat_l() {
		return lat_l;
	}

	public void setLat_l(double lat_l) {
		this.lat_l = lat_l;
	}

	public double getLat_u() {
		return lat_u;
	}

	public void setLat_u(double lat_u) {
		this.lat_u = lat_u;
	}

	public double getLong_l() {
		return long_l;
	}

	public void setLong_l(double long_l) {
		this.long_l = long_l;
	}

	public double getLong_u() {
		return long_u;
	}

	public void setLong_u(double long_u) {
		this.long_u = long_u;
	}
	
	
}
