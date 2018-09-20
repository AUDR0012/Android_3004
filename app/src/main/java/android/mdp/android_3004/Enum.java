package android.mdp.android_3004;

import android.graphics.Color;

public class Enum {

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

	public enum Direction {
		UP(0),
		RIGHT(1),
		DOWN(2),
		LEFT(3);

		Direction(int id) {
			this.id = id;
		}

		private int id;

		public int get() {
			return id;
		}
	}

	public enum Instruction {
		FORWARD("Forward", "f"),
		REVERSE("Reverse", "r"),
		ROTATE_LEFT("Rotate Left", "tl"),
		ROTATE_RIGHT("Rotate Right", "tr"),
		STRAFE_LEFT("Strafe Left", "sl"),
		STRAFE_RIGHT("Strafe Right", "sr"),
		BEGIN_EXPLORATION("Begin Exploration", "beginExplore"),
		BEGIN_FASTEST_PATH("Begin Fastest Path", "beginFastest"),
		SEND_ARENA_INFO("Send Arena Info", "sendArena"),

		ORIGIN("Origin point", "origin"),//origin{x,y}
		WAY("Way point", "way"),//way{x,y}
		ARROW("Obstacle with Up Arrow", "arrow");//arrow{x,y,direction}

		Instruction(String description, String arduino) {
			this.description = description;
			this.arduino = arduino;
		}

		private String description;
		private String arduino;

		public String getDescription() {
			return description;
		}

		public String getArduino() {
			return arduino;
		}

		public void setArduino(String text) {
			arduino = text;
		}
	}
}
