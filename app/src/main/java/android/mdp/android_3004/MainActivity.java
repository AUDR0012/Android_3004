package android.mdp.android_3004;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	final int MAZE_C = 15;
	final int MAZE_R = 20;
	final int ROBOT_SIZE = 2;
	GridLayout grid_maze;
	ImageView robot;
	int robot_location = 0;

	Handler handler;
	long time_start;
	TextView time_textview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		int[] list_color = {Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.BLUE}; //TODO: REMOVABLE

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.direction_btn_up, R.id.direction_btn_down, R.id.direction_btn_left, R.id.direction_btn_right,
				R.id.time_btn_stopwatch, R.id.time_btn_reset, R.id.time_swt_isfast,
				R.id.point_btn_origin, R.id.point_btn_way,
				R.id.config_btn_f1, R.id.config_btn_f2, R.id.config_btn_reconfig,
				R.id.display_swt_ismanual, R.id.display_btn_update};
		for (int onclick : list_onclick) {
			findViewById(onclick).setOnClickListener(this);
		}
		time_option();
		display_option();

//		========== MAZE ==========
		grid_maze = findViewById(R.id.grid_maze);
		grid_maze.setColumnCount(MAZE_C);
		grid_maze.setRowCount(MAZE_R);

		Drawable d_box = this.getResources().getDrawable(R.drawable.d_box, null);
		d_box.setColorFilter(list_color[new Random().nextInt(list_color.length)], PorterDuff.Mode.ADD); //TODO: REMOVABLE
		for (int i = 0; i < MAZE_C * MAZE_R; i++) {
			TextView tv = new TextView(this);
			tv.setGravity(Gravity.CENTER);
			tv.setBackground(d_box);
			tv.setText(String.valueOf(i));

			grid_maze.addView(tv);
		}
		robot_create();

//		========== STOPWATCH ==========
		handler = new Handler();

//		========== SET POINTS ==========
		((EditText) findViewById(R.id.point_txt_x)).setHint(Integer.toString(MAZE_C));
		((EditText) findViewById(R.id.point_txt_y)).setHint(Integer.toString(MAZE_R));
		((EditText) findViewById(R.id.point_txt_x)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("1", Integer.toString(MAZE_C))});
		((EditText) findViewById(R.id.point_txt_y)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("1", Integer.toString(MAZE_R))});
	}

	@Override
	public void onClick(View v) {
		String title; //TODO: REMOVABLE
		if (v instanceof Button) title = ((Button) v).getText().toString();
		else title = ((SwitchCompat) v).getText().toString();
		((TextView) findViewById(R.id.txt_status)).setText(title + " selected");

		switch (v.getId()) {
			//MAZE
			case R.id.direction_btn_up:
				robot_move();
				break;
			case R.id.direction_btn_down:
				robot_rotate(2);
				break;
			case R.id.direction_btn_left:
				robot_rotate(3);
				break;
			case R.id.direction_btn_right:
				robot_rotate(1);
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

			//SET POINT
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

			//DISPLAY GRAPHICS
			case R.id.display_swt_ismanual:
				display_option();
				break;
			case R.id.display_btn_update:
				display_manual();
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) { //TODO: UNKNOWN - MERGE WITH WEEKIAT PART
		String title = item.getTitle().toString();
		((TextView) findViewById(R.id.txt_status)).setText(title + " selected");
		switch (item.getItemId()) {
			case R.id.menu_bluetooth:
				return true;
			case R.id.menu_messenger:
				return true;
			case R.id.menu_maze:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void robot_create() {
		int c = MAZE_C - ROBOT_SIZE, r = MAZE_R - ROBOT_SIZE;

		Drawable d_robot = this.getResources().getDrawable(R.drawable.d_robot2, null);
		robot = new ImageView(this);
		robot.setImageDrawable(d_robot);
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
		lp.columnSpec = GridLayout.spec(c, ROBOT_SIZE);
		lp.rowSpec = GridLayout.spec(r, ROBOT_SIZE);
		lp.height = 40 * ROBOT_SIZE;
		lp.width = 40 * ROBOT_SIZE;
		robot.setLayoutParams(lp);
		grid_maze.addView(robot);
		robot_location = (r * MAZE_C) + c;
	}

	protected void robot_rotate(int direction) {
		robot.setRotation(robot.getRotation() + (direction * 90));
	}

	protected void robot_move() { // 0: UP | 1: RIGHT | 2: DOWN | 3: LEFT
		int col = robot_location % MAZE_C,
				row = robot_location / MAZE_C,
				direction = (((int) robot.getRotation()) % 360) / 90;

		if ((direction == 0 && row > 0) ||
				(direction == 1 && col < (MAZE_C - ROBOT_SIZE)) ||
				(direction == 2 && row < (MAZE_R - ROBOT_SIZE)) ||
				(direction == 3 && col > 0)) {
			int addon = 1;
			if (direction == 0 || direction == 2) addon = MAZE_C;
			if (direction == 0 || direction == 3) addon *= -1;
			robot_location += addon;

			GridLayout.LayoutParams lp = (GridLayout.LayoutParams) robot.getLayoutParams();
			if (direction == 0 || direction == 2) {
				lp.rowSpec = GridLayout.spec(row + ((direction == 0) ? -1 : 1), ROBOT_SIZE);
			} else {
				lp.columnSpec = GridLayout.spec(col + ((direction == 3) ? -1 : 1), ROBOT_SIZE);
			}
			robot.setLayoutParams(lp);
		}
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
			handler.postDelayed(runnable, 0);

			findViewById(R.id.time_swt_isfast).setEnabled(false);
			findViewById(R.id.time_btn_reset).setEnabled(false);
			b.setText("Pause");
		} else {
			handler.removeCallbacks(runnable);

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

	public Runnable runnable = new Runnable() {
		public void run() {
			long time_count_ms = SystemClock.uptimeMillis() - time_start;
			int time_s = (int) (time_count_ms / 1000);

			time_set(time_s / 60, time_s % 60, (int) (time_count_ms % 1000));
			handler.postDelayed(this, 0);
		}

	};

	protected void point_set(boolean isorigin) { //TODO: SETTING POINTS
//		int cell,
//		point_x = Integer.valueOf(((TextView)findViewById(R.id.point_txt_x));
//		if (isorigin) {
//
//		}else{
//		}
	}

	protected void display_option() { //TODO: HOW TO AUTO?
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

	protected void display_manual() { //TODO: UNKNOWN - UPDATE MANUAL

	}
}
