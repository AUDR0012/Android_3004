package android.mdp.android_3004;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

	final int MAZE_C = 15;
	final int MAZE_R = 20;
	final int ROBOT_SIZE = 3;

	GridLayout grid_maze;
	ImageView robot;
	int robot_location = 0;
	int way_point = -1;
	List<Integer> obstacle_list;

	Handler handler;
	long time_start;
	TextView time_textview;

	AlertDialog.Builder dialog;
	SensorManager sensor_manager;
	Sensor accelerometer_sensor;
	SensorEventListener accelerometer_sensor_listener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//		========== OTHERS ==========
		dialog = new AlertDialog.Builder(this, R.style.DialogStyle);
		obstacle_list = new ArrayList<>();

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.tilt_swt_isoff,
			R.id.direction_btn_up, R.id.direction_btn_down, R.id.direction_btn_left, R.id.direction_btn_right,
			R.id.time_btn_stopwatch, R.id.time_btn_reset, R.id.time_swt_isfast,
			R.id.point_btn_origin, R.id.point_btn_way,
			R.id.config_btn_f1, R.id.config_btn_f2, R.id.config_btn_reconfig,
			R.id.display_swt_ismanual, R.id.display_btn_update};
		for (int onclick : list_onclick) {
			findViewById(onclick).setOnClickListener(onClickListener);
		}

//		========== ACCELEROMETER ==========
		sensor_manager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer_sensor = sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		accelerometer_sensor_listener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				tilt_move(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {
			}
		};
		tilt_option();

//		========== MAZE ==========
		grid_maze = findViewById(R.id.grid_maze);
		grid_maze.setColumnCount(MAZE_C);
		grid_maze.setRowCount(MAZE_R);

		Drawable box = create_drawable(R.drawable.d_box, Cell.DEFAULT.getColor());
		for (int i = 0; i < MAZE_C * MAZE_R; i++) {
			TextView tv = new TextView(this);
			tv.setGravity(Gravity.CENTER);
			tv.setBackground(box);
			tv.setText(String.valueOf(Cell.DEFAULT.get()));

			grid_maze.addView(tv);
		}
		robot_create();
		robot_go();

		obstacle_create();
		obstacle_arrow(obstacle_list.get(0) % MAZE_C, obstacle_list.get(0) / MAZE_C, Direction.UP.get());

//		========== STOPWATCH ==========
		handler = new Handler();
		time_option();

//		========== DISPLAY GRAPHICS ==========
		display_option();

//		========== SET POINTS ==========
		((EditText) findViewById(R.id.point_txt_x)).setHint(String.valueOf(MAZE_C - 1));
		((EditText) findViewById(R.id.point_txt_y)).setHint(String.valueOf(MAZE_R - 1));
		((EditText) findViewById(R.id.point_txt_x)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("0", String.valueOf(MAZE_C - 1))});
		((EditText) findViewById(R.id.point_txt_y)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("0", String.valueOf(MAZE_R - 1))});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu_atmaze, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String title = item.getTitle().toString();
		((TextView) findViewById(R.id.txt_status)).setText(title + " selected");
		switch (item.getItemId()) {
			case R.id.menu_bluetooth:
				startActivity(new Intent(MainActivity.this, SecondaryActivity.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			String title; //TODO: REMOVABLE
			if (v instanceof Button) title = ((Button) v).getText().toString();
			else title = ((SwitchCompat) v).getText().toString();
			((TextView) findViewById(R.id.txt_status)).setText(title + " selected");

			switch (v.getId()) {
				//ACCELEROMETER
				case R.id.tilt_swt_isoff:
					tilt_option();
					break;

				//MAZE
				case R.id.direction_btn_up:
					robot_move(directionof_robot((((int) robot.getRotation()) % 360) / 90));
					break;
				case R.id.direction_btn_down:
					robot_rotate(Direction.DOWN.get());
					break;
				case R.id.direction_btn_left:
					robot_rotate(Direction.LEFT.get());
					break;
				case R.id.direction_btn_right:
					robot_rotate(Direction.RIGHT.get());
					break;

				//STOPWATCH
				case R.id.time_swt_isfast:
					time_option();
					break;
				case R.id.time_btn_stopwatch:
					time_stopwatch(v);
					break;
				case R.id.time_btn_reset:
					time_reset();
					break;

				//SET POINTS
				case R.id.point_btn_origin:
					point_set(true);
					break;
				case R.id.point_btn_way:
					point_set(false);
					break;

				//CONFIGURATIONS //TODO: UNKNOWN - WHAT TO DO?
				case R.id.config_btn_f1:
					break;
				case R.id.config_btn_f2:
					break;
				case R.id.config_btn_reconfig:
					break;

				//DISPLAY GRAPHICS //TODO: UNKNOWN - WHAT TO DO?
				case R.id.display_swt_ismanual:
					display_option();
					break;
				case R.id.display_btn_update:
					display_manual();
					break;
			}
		}
	};

	protected void create_dialog(String title, String message) {
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//NOTHING
			}
		});
		dialog.create();
		dialog.show();
	}

	protected Drawable create_drawable(int image, int color) {
		Drawable box = getResources().getDrawable(image, null);
		box.setColorFilter(color, PorterDuff.Mode.OVERLAY);
		return box;
	}

	protected int colorof_textview(String id) {
		for (Cell c : Cell.values()) {
			if (Integer.valueOf(id) == c.get()) {
				return c.getColor();
			}
		}
		return -1;
	}

	protected Direction directionof_robot(int direction) {
		for (Direction d : Direction.values()) {
			if (direction == d.get()) {
				return d;
			}
		}
		return null;
	}

	protected void obstacle_arrow(int col, int row, int direction) {
		ImageView obstacle = new ImageView(this);
		obstacle.setImageDrawable(create_drawable(R.drawable.d_arrow, Color.TRANSPARENT));
		obstacle.setBackground(create_drawable(R.drawable.d_box, Cell.OBSTACLE.getColor()));
		obstacle.setRotation(direction * 90);
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
		lp.columnSpec = GridLayout.spec(col, 1);
		lp.rowSpec = GridLayout.spec(row, 1);
		obstacle.setLayoutParams(lp);
		grid_maze.addView(obstacle);
	}

	protected void obstacle_create() { //TODO: REMOVABLE
		int cell,
			count = (new Random()).nextInt(10) + 10;

		Drawable box = create_drawable(R.drawable.d_box, Cell.OBSTACLE.getColor());
		TextView tv;
		for (int i = 0; i < count; i++) {
			cell = (new Random()).nextInt(266);
			tv = (TextView) grid_maze.getChildAt(cell);
			tv.setBackground(box);
			tv.setText(String.valueOf(Cell.OBSTACLE.get()));
			obstacle_list.add(cell);
		}
	}

	protected void tilt_option() {
		SwitchCompat s = findViewById(R.id.tilt_swt_isoff);
		if (s.isChecked()) {
			s.setText("Off");
			sensor_manager.unregisterListener(accelerometer_sensor_listener);
		} else {
			s.setText("On");
			sensor_manager.registerListener(accelerometer_sensor_listener, accelerometer_sensor, 3);
		}
	}

	protected void tilt_move(float x, float y, float z) {
		if (x > 0.5f) robot_move(Direction.LEFT);
		if (x < -0.5f) robot_move(Direction.RIGHT);
		if (y > 0.5f) robot_move(Direction.DOWN);
		if (y < -0.5f) robot_move(Direction.UP);
	}

	protected void robot_create() {
		int col = MAZE_C - ROBOT_SIZE, row = MAZE_R - ROBOT_SIZE;

		robot = new ImageView(this);
		robot.setImageDrawable(create_drawable(R.drawable.d_robot, Color.TRANSPARENT));
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
		lp.columnSpec = GridLayout.spec(col, ROBOT_SIZE);
		lp.rowSpec = GridLayout.spec(row, ROBOT_SIZE);
		lp.height = 40 * ROBOT_SIZE;
		lp.width = 40 * ROBOT_SIZE;
		robot.setLayoutParams(lp);
		grid_maze.addView(robot);
		robot_location = grid_cell(col, row);
	}

	protected void robot_rotate(int direction) {
		robot.setRotation(robot.getRotation() + (direction * 90));
	}

	protected void robot_move(Direction direction) {
		int temp_location = robot_location,
			col = robot_location % MAZE_C,
			row = robot_location / MAZE_C;

		switch (direction) {
			case UP:
				if (row > 0)
					temp_location -= MAZE_C;
				break;
			case DOWN:
				if (row < (MAZE_R - ROBOT_SIZE))
					temp_location += MAZE_C;
				break;
			case LEFT:
				if (col > 0)
					temp_location -= 1;
				break;
			case RIGHT:
				if (col < (MAZE_C - ROBOT_SIZE))
					temp_location += 1;
				break;
		}
		if (!robot_hit(temp_location)) {
			robot_location = temp_location;
			robot_go();
		}
	}

	protected void robot_go() {
		int row = robot_location / MAZE_C,
			col = robot_location % MAZE_C;

		GridLayout.LayoutParams lp = (GridLayout.LayoutParams) robot.getLayoutParams();
		lp.rowSpec = GridLayout.spec(row, ROBOT_SIZE);
		lp.columnSpec = GridLayout.spec(col, ROBOT_SIZE);
		robot.setLayoutParams(lp);

		robot_pass();
	}

	protected void robot_pass() {
		int cell,
			row = robot_location / MAZE_C,
			col = robot_location % MAZE_C;
		Drawable box = create_drawable(R.drawable.d_box, Cell.PASSED.getColor());
		TextView tv;

		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				cell = grid_cell(c, r);
				tv = (TextView) grid_maze.getChildAt(cell);
				if (cell != way_point) {
					tv.setBackground(box);
				}
				tv.setText(String.valueOf(Cell.PASSED.get()));
			}
		}
	}

	protected boolean robot_hit(int cell) {
		int row = cell / MAZE_C,
			col = cell % MAZE_C;

		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				if (obstacle_list.contains(grid_cell(c, r)))
					return true;
			}
		}
		return false;
	}

	protected int grid_cell(int col, int row) {
		return (row * MAZE_C) + col;
	}

	protected void time_option() {
		SwitchCompat s = findViewById(R.id.time_swt_isfast);
		if (s.isChecked()) {
			s.setText("Fastest");
			time_textview = findViewById(R.id.time_txt_fast);
		} else {
			s.setText("Explore");
			time_textview = findViewById(R.id.time_txt_explore);
		}
		findViewById(R.id.time_btn_stopwatch).setEnabled(("0:00:000").equalsIgnoreCase(time_textview.getText().toString()));
	}

	protected void time_stopwatch(View v) {
		Button b = (Button) v;
		if (("Start").equalsIgnoreCase(b.getText().toString())) {
			time_start = SystemClock.uptimeMillis();
			handler.postDelayed(time_stopwatch, 0);

			findViewById(R.id.time_swt_isfast).setEnabled(false);
			findViewById(R.id.time_btn_reset).setEnabled(false);
			b.setText("Pause");
		} else {
			handler.removeCallbacks(time_stopwatch);

			findViewById(R.id.time_swt_isfast).setEnabled(true);
			findViewById(R.id.time_btn_reset).setEnabled(true);
			findViewById(R.id.time_btn_stopwatch).setEnabled(false);
			b.setText("Start");
		}
	}

	protected void time_reset() {
		time_start = 0L;
		time_set(0, 0, 0);
		findViewById(R.id.time_btn_stopwatch).setEnabled(true);
	}

	protected void time_set(int min, int sec, int millisec) {
		time_textview.setText("" + min + ":"
			+ String.format("%02d", sec) + ":"
			+ String.format("%03d", millisec));
	}

	public Runnable time_stopwatch = new Runnable() {
		public void run() {
			long time_count_ms = SystemClock.uptimeMillis() - time_start;
			int time_s = (int) (time_count_ms / 1000);

			time_set(time_s / 60, time_s % 60, (int) (time_count_ms % 1000));
			handler.postDelayed(this, 0);
		}
	};

	protected void point_set(boolean isorigin) {
		String point_x = ((TextView) findViewById(R.id.point_txt_x)).getText().toString(),
			point_y = ((TextView) findViewById(R.id.point_txt_y)).getText().toString();
		if (point_x.equalsIgnoreCase("") || point_y.equalsIgnoreCase("")) {
			create_dialog("Setting Points", "Please fill in the text fields.");
			return;
		}

		int col = Integer.valueOf(point_x),
			row = Integer.valueOf(point_y),
			cell = grid_cell(col, row);

		if (robot_hit(cell)) {
			create_dialog("Setting Points", "Robot will collide with obstacle.");
			return;
		}

		if (isorigin) {
			if ((col > (MAZE_C - ROBOT_SIZE)) || (row > (MAZE_R - ROBOT_SIZE))) {
				create_dialog("Setting Points", "Robot cannot move to that point.");
			} else {
				robot_location = cell;
				robot_go();
			}
		} else {
			Drawable box;
			if (way_point > -1) {
				box = create_drawable(R.drawable.d_box, colorof_textview(((TextView) grid_maze.getChildAt(way_point)).getText().toString()));
				grid_maze.getChildAt(way_point).setBackground(box);
			}
			box = create_drawable(R.drawable.d_box, Cell.WAYPOINT.getColor());
			grid_maze.getChildAt(cell).setBackground(box);

			way_point = cell;
		}
	}

	protected void display_option() { //TODO: UNKNOWN - HOW TO AUTO?
		SwitchCompat s = findViewById(R.id.display_swt_ismanual);
		Button b = findViewById(R.id.display_btn_update);
		if (s.isChecked()) {
			s.setText("Manual");
			b.setVisibility(View.VISIBLE);
		} else {
			s.setText("Auto");
			b.setVisibility(View.INVISIBLE);
		}
	}

	protected void display_manual() { //TODO: UNKNOWN - HOW TO MANUAL?

	}
}
