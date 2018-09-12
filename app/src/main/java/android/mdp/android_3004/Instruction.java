package android.mdp.android_3004;

public enum Instruction {
	FORWARD("Forward", "Up"),
	REVERSE("Reverse", "Down"),
	ROTATE_LEFT("Rotate Left", "Left"),
	ROTATE_RIGHT("Rotate Right", "Right"),
	STRAFE_LEFT("Strafe Left", "sl"),
	STRAFE_RIGHT("Strafe Right", "sr"),
	BEGIN_EXPLORATION("Begin Exploration", "beginExplore"),
	BEGIN_FASTEST_PATH("Begin Fastest Path", "beginFastest"),
	SEND_ARENA_INFO("Send Arena Info", "sendArena");

	Instruction(String description, String write_text) {
		this.description = description;
		this.write_text = write_text;
	}

	private String description;
	private String write_text;

	public String get() {
		return description;
	}

	public String getWriteText() {
		return write_text;
	}

	public void setWriteText(String text) {
		write_text = text;
	}
}