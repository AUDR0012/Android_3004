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
		UP(0, "U"),
		RIGHT(1, "R"),
		DOWN(2, "D"),
		LEFT(3, "L");

		Direction(int id, String chara) {
			this.id = id;
			this.chara = chara;
		}

		private int id;
		private String chara;

		public int get() {
			return id;
		}

		public String getChara() {
			return chara;
		}
	}

	public enum Instruction {
		FORWARD("Forward", "f"),
		REVERSE("Reverse", "r"),
		ROTATE_LEFT("Rotate Left", "tl"),
		ROTATE_RIGHT("Rotate Right", "tr"),
		STRAFE_LEFT("Strafe Left", "sl"),
		STRAFE_RIGHT("Strafe Right", "sr"),
		SEND_ARENA_INFO("Send Arena Info", "sendArena"),

		ORIGIN("Origin point", "origin"),//origin{x,y}
		WAY("Way point", "way"),//way{x,y}
		OBSTACLE("Obstacle", "obstacle"),//obstacle{x,y}
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
