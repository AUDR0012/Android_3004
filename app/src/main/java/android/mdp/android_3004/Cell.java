package android.mdp.android_3004;

import android.graphics.Color;

public enum Cell {
	DEFAULT(0, Color.GREEN),
	PASSED(1, Color.YELLOW),
	OBSTACLE(2, Color.BLACK),
	WAYPOINT(3, Color.RED);

	Cell(int id, int color) {
		this.id = id;
		this.color = color;
	}

	private int id;
	private int color;

	public int get() {
		return id;
	}

	public int getColor() {
		return color;
	}
}