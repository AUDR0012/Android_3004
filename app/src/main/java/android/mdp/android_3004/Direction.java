package android.mdp.android_3004;

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