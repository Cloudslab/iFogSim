package org.fog.gui.core;

public class PlaceHolder {

	protected Coordinates coordinates;
	protected boolean isOccupied;
	protected Node node;
	
	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public boolean isOccupied() {
		return isOccupied;
	}

	public void setOccupied(boolean isOccupied) {
		this.isOccupied = isOccupied;
	}

	public PlaceHolder(Coordinates coordinates){
		setCoordinates(coordinates);
		setOccupied(false);
	}
	
	public PlaceHolder(){
		setCoordinates(new Coordinates());
		setOccupied(false);
	}
	
	public PlaceHolder(int x, int y){
		setCoordinates(new Coordinates(x, y));
		setOccupied(false);
	}

	public Coordinates getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}
}
