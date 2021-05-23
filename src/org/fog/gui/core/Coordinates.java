package org.fog.gui.core;

public class Coordinates {

	private int x;
	private int y;

	public Coordinates() {
		this.x = 0;
		this.y = 0;
	}
	public Coordinates(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}

	@Override
	public String toString() {
		return "Coordinates [abscissa=" + x + ", ordinate=" + y + "]";
	}

}
