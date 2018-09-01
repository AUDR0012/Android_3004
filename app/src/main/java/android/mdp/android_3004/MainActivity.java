package android.mdp.android_3004;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.Gravity;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		int[] list_color = {Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.BLUE}; //TODO: REMOVABLE

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.direction_btn_up, R.id.direction_btn_down, R.id.direction_btn_left, R.id.direction_btn_right,
				R.id.time_btn_timer, R.id.time_btn_reset, R.id.time_swt_isfast,
				R.id.point_btn_start, R.id.point_btn_way,
				R.id.display_swt_isauto, R.id.display_btn_update};
		for (int onclick : list_onclick) {
			findViewById(onclick).setOnClickListener(this);
		}
		onclick_time_option();
		onclick_display_option();

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
			tv.setText(Integer.toString(i));

			grid_maze.addView(tv);
		}
		robot_create();

//		========== SET POINTS ==========
		((EditText) findViewById(R.id.point_txt_x)).setHint(Integer.toString(MAZE_C));
		((EditText) findViewById(R.id.point_txt_y)).setHint(Integer.toString(MAZE_R));
		((EditText) findViewById(R.id.point_txt_x)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("1", Integer.toString(MAZE_C))});
		((EditText) findViewById(R.id.point_txt_y)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("1", Integer.toString(MAZE_R))});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
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
			case R.id.time_swt_isfast:
				onclick_time_option();
				break;
			case R.id.time_btn_timer:
				onclick_time_timer(v);
				break;
			case R.id.time_btn_reset:
				onclick_time_reset();
				break;
			case R.id.point_btn_start:
				onclick_point_set(true);
				break;
			case R.id.point_btn_way:
				onclick_point_set(false);
				break;
			case R.id.display_swt_isauto:
				onclick_display_option();
				break;
			case R.id.display_btn_update:
				onclick_display_manual();
				break;
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

	protected void onclick_time_option() {
		SwitchCompat s = findViewById(R.id.time_swt_isfast);
		if (s.isChecked()) {
			s.setText("Fastest");
		} else {
			s.setText("Explore");
		}
	}

	protected void onclick_time_timer(View v) { //TODO: TIMER ITSELF
		Button b = (Button) v;
		if (("Start").equalsIgnoreCase(b.getText().toString())) {
			b.setText("Stop");
		} else {
			b.setText("Start");
		}
	}

	protected void onclick_time_reset() { //TODO: CLEAR TEXT
		SwitchCompat s = findViewById(R.id.time_swt_isfast);
		TextView tv = (TextView) (s.isChecked() ? findViewById(R.id.time_txt_fast) : findViewById(R.id.time_txt_explore));
		tv.setText("00.00.00");
	}

	protected void onclick_point_set(boolean isstart) { //TODO: SETTING POINTS
		int x = Integer.valueOf(((EditText) findViewById(R.id.point_txt_x)).getText().toString()),
				y = Integer.valueOf(((EditText) findViewById(R.id.point_txt_y)).getText().toString());
		//if (isstart &&)
	}

	protected void onclick_display_option() {
		SwitchCompat s = findViewById(R.id.display_swt_isauto);
		Button b = findViewById(R.id.display_btn_update);
		if (s.isChecked()) {
			s.setText("Auto");
			b.setVisibility(View.INVISIBLE);
		} else {
			s.setText("Manual");
			b.setVisibility(View.VISIBLE);
		}
	}

	protected void onclick_display_manual() { //TODO: UPDATE MANUAL

	}
}
