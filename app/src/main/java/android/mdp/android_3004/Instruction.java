package android.mdp.android_3004;

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

	RESTART_ARENA("", ""),
	STOP_TIME("", ""),
	RESET_TIME("", ""),
	WAY_POINT("", ""),
	ORIGIN_POINT("", ""),
	DISPLAY_AUTO("", ""),
	DISPLAY_MANUAL_UPDATE("", "");

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