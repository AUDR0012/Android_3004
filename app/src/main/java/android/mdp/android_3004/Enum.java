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

		WAY("Way point", "way{x,y}"),
		ARROW("Obstacle with Up Arrow", "arrow{x,y,direction}");

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

	public enum State {
		NONE(0),
		LISTEN(1),
		CONNECTING(2),
		CONNECTED(3);

		State(int id) {
			this.id = id;
		}

		private int id;

		public int get() {
			return id;
		}
	}

	public enum Handling {
		STATE_CHANGE(0, "STATE_CHANGE"),
		READ_MSG(1, "READ_MSG"),
		WRITE_MSG(2, "WRITE_MSG"),
		BT_NAME(3, "BT_NAME"),
		TOAST(4, "TOAST");

		Handling(int id, String desc) {
			this.id = id;
			this.desc = desc;
		}

		private int id;
		private String desc;

		public int get() {
			return id;
		}

		public String getDesc() {
			return desc;
		}
	}
}
