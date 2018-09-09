package android.mdp.android_3004;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

	final int MAZE_C = 15;
	final int MAZE_R = 20;
	final int ROBOT_SIZE = 3;
	final int START_COL = 0;
	final int START_ROW = MAZE_R - ROBOT_SIZE;

	GridLayout grid_maze;
	ImageView robot;
	int robot_location;
	int way_point;
	List<Integer> obstacle_list = new ArrayList<>(), void_list = new ArrayList<>();

	Menu menu;
	LayoutInflater inflater;

	Intent bt_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
	ArrayList<BluetoothDevice> bt_newlist = new ArrayList<>(), bt_pairedlist = new ArrayList<>();
	ListView bt_lv_devices;
	DeviceListAdapter bt_listadapter;

	private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	BluetoothConnectionService bt_connection;
	BluetoothDevice bt_device;

	Handler time_handler = new Handler();
	long time_start;
	TextView time_textview;

	SensorManager sensor_manager;
	Sensor accelerometer_sensor;
	SensorEventListener accelerometer_sensor_listener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//		========== OTHERS ==========
		inflater = LayoutInflater.from(this);

		bt_getpaired();
		reg_bt_intentfilter();

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.tilt_swt_isoff,
			R.id.direction_btn_up, R.id.direction_btn_down, R.id.direction_btn_left, R.id.direction_btn_right,
			R.id.time_btn_stopwatch, R.id.time_btn_reset, R.id.time_swt_isfast,
			R.id.point_btn_origin, R.id.point_btn_way,
			R.id.config_btn_reconfig,
			R.id.display_swt_ismanual, R.id.display_btn_update,
			R.id.message_btn_toggle};
		for (int onclick : list_onclick) {
			findViewById(onclick).setOnClickListener(onClickListener);
		}

//		========== MAZE ==========
		grid_maze = findViewById(R.id.grid_maze);
		grid_maze.setColumnCount(MAZE_C);
		grid_maze.setRowCount(MAZE_R);

		Drawable box = new_drawable(R.drawable.d_box, Cell.DEFAULT.getColor());
		for (int i = 0; i < MAZE_C * MAZE_R; i++) {
			TextView tv = new TextView(this);
			tv.setGravity(Gravity.CENTER);
			tv.setBackground(box);
			tv.setText(String.valueOf(Cell.DEFAULT.get()));

			grid_maze.addView(tv);
		}
		init();

		obstacle_create();
		obstacle_arrow(obstacle_list.get(0) % MAZE_C, obstacle_list.get(0) / MAZE_C, Direction.UP.get());

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

//		========== SET POINTS ==========
		((EditText) findViewById(R.id.point_txt_x)).setHint(String.valueOf(MAZE_C - 1));
		((EditText) findViewById(R.id.point_txt_y)).setHint(String.valueOf(MAZE_R - 1));
		((EditText) findViewById(R.id.point_txt_x)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("0", String.valueOf(MAZE_C - 1))});
		((EditText) findViewById(R.id.point_txt_y)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("0", String.valueOf(MAZE_R - 1))});
	}

	@Override
	public void onPause() {
		bt_adapter.cancelDiscovery();
		super.onPause();
	}

	@Override
	public void onResume() {
		bt_update(-1);
		super.onResume();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(bt_receiver);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu_atmaze, menu);

		this.menu = menu;
		reset_app();
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String title = item.getTitle().toString();
		((TextView) findViewById(R.id.txt_status)).setText(title + " selected");
		switch (item.getItemId()) {
			//RESTART
			case R.id.menu_restart:
				reset_app();
				return true;

			//BLUETOOTH
			case R.id.menu_bt_on:
				bt_update(1);
				return true;
			case R.id.menu_bt_off:
				bt_update(0);
				return true;
			case R.id.menu_bt_discoverable:
				bt_discover();
				return true;
			case R.id.menu_bt_finddevices:
				AlertDialog.Builder bt_dialog = pop_bluetooth();
				bt_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						bt_adapter.cancelDiscovery();
					}
				});
				bt_dialog.show();
				return true;

			//TODO:REMOVABLE??
			case R.id.menu_bt_move:
				startActivity(new Intent(this, SecondaryActivity.class));
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void new_message(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	protected GridLayout.LayoutParams new_layoutparams(int col, int row, int size) {
		GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
		lp.columnSpec = GridLayout.spec(col, size);
		lp.rowSpec = GridLayout.spec(row, size);
		lp.height = 40 * size;
		lp.width = 40 * size;

		return lp;
	}

	protected Drawable new_drawable(int image, int color) {
		Drawable box = getResources().getDrawable(image, null);
		box.setColorFilter(color, PorterDuff.Mode.OVERLAY);
		return box;
	}

	protected int enum_getcolor(String id) {
		for (Cell c : Cell.values()) {
			if (Integer.valueOf(id) == c.get()) {
				return c.getColor();
			}
		}
		return -1;
	}

	protected Direction enum_getdirection(int direction) {
		for (Direction d : Direction.values()) {
			if (direction == d.get()) {
				return d;
			}
		}
		return null;
	}

	protected int cell_id(int col, int row) {
		return (row * MAZE_C) + col;
	}

	protected void init() {
		int col, row;
		//ORIGIN
		col = 0;
		row = MAZE_R - ROBOT_SIZE;
		TextView tv_origin = new TextView(this);
		tv_origin.setGravity(Gravity.CENTER);
		tv_origin.setAllCaps(true);
		tv_origin.setTypeface(Typeface.DEFAULT_BOLD);
		tv_origin.setBackground(new_drawable(R.drawable.d_box, Color.TRANSPARENT));
		tv_origin.setText("Start");
		tv_origin.setLayoutParams(new_layoutparams(col, row, ROBOT_SIZE));
		grid_maze.addView(tv_origin);
		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				void_list.add(cell_id(c, r));
			}
		}

		//GOAL
		col = MAZE_C - ROBOT_SIZE;
		row = 0;
		TextView tv_goal = new TextView(this);
		tv_goal.setGravity(Gravity.CENTER);
		tv_goal.setAllCaps(true);
		tv_goal.setTypeface(Typeface.DEFAULT_BOLD);
		tv_goal.setBackground(new_drawable(R.drawable.d_box, Color.TRANSPARENT));
		tv_goal.setText("Goal");
		tv_goal.setLayoutParams(new_layoutparams(col, row, ROBOT_SIZE));
		grid_maze.addView(tv_goal);
		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				void_list.add(cell_id(c, r));
			}
		}
	}

	protected void reset_app() {
		reset_cells();

		way_point = -1;
		robot_location = cell_id(START_COL, START_ROW);
		robot_go();

		bt_update(-1);

		tilt_option();

		((TextView) findViewById(R.id.time_txt_explore)).setText("0:00:000");
		((TextView) findViewById(R.id.time_txt_fast)).setText("0:00:000");
		time_option();

		((EditText) findViewById(R.id.point_txt_x)).setText("");
		((EditText) findViewById(R.id.point_txt_y)).setText("");

		display_option();
	}

	protected void reset_cells() {
		Drawable box = new_drawable(R.drawable.d_box, Cell.DEFAULT.getColor());

		for (int i = 0; i < MAZE_C * MAZE_R; i++) {
			TextView tv = (TextView) grid_maze.getChildAt(i);
			if (Integer.valueOf(tv.getText().toString()) == Cell.PASSED.get()) {
				tv.setText(String.valueOf(Cell.DEFAULT.get()));
				tv.setBackground(box);
			}
		}
	}

	protected void reg_bt_intentfilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		registerReceiver(bt_receiver, filter);
	}

	protected void obstacle_arrow(int col, int row, int direction) {
		ImageView obstacle = new ImageView(this);
		obstacle.setImageDrawable(new_drawable(R.drawable.d_arrow, Color.TRANSPARENT));
		obstacle.setBackground(new_drawable(R.drawable.d_box, Cell.OBSTACLE.getColor()));
		obstacle.setRotation(direction * 90);
		obstacle.setLayoutParams(new_layoutparams(col, row, 1));
		grid_maze.addView(obstacle);
	}

	protected void obstacle_create() { //TODO:REMOVABLE
		int cell, count = (new Random()).nextInt(10) + 10;

		Drawable box = new_drawable(R.drawable.d_box, Cell.OBSTACLE.getColor());
		TextView tv;
		for (int i = 0; i < count; i++) {
			cell = (new Random()).nextInt(300);
			if (void_list.contains(cell)) {
				i--;
				continue;
			}
			tv = (TextView) grid_maze.getChildAt(cell);
			tv.setBackground(box);
			tv.setText(String.valueOf(Cell.OBSTACLE.get()));
			obstacle_list.add(cell);
		}
	}

	protected void robot_go() {
		if (robot == null) {
			robot = new ImageView(this);
			robot.setImageDrawable(new_drawable(R.drawable.d_robot, Color.TRANSPARENT));
			grid_maze.addView(robot);
		}

		int row = robot_location / MAZE_C,
			col = robot_location % MAZE_C;
		robot.setLayoutParams(new_layoutparams(col, row, ROBOT_SIZE));

		Drawable box = new_drawable(R.drawable.d_box, Cell.PASSED.getColor());
		TextView tv;
		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				int cell = cell_id(c, r);
				tv = (TextView) grid_maze.getChildAt(cell);
				if (cell != way_point) {
					tv.setBackground(box);
				}
				tv.setText(String.valueOf(Cell.PASSED.get()));
			}
		}
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

	protected boolean robot_hit(int cell) {
		int row = cell / MAZE_C,
			col = cell % MAZE_C;

		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				if (obstacle_list.contains(cell_id(c, r)))
					return true;
			}
		}
		return false;
	}

	protected AlertDialog.Builder pop_bluetooth() {
		View v = inflater.inflate(R.layout.activity_bluetooth, null);
		bt_lv_devices = v.findViewById(R.id.bt_lv_devices);
		bt_lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				bt_adapter.cancelDiscovery();

				new_message(view.getContext(), bt_newlist.get(position).getName());
				bt_newlist.get(position).createBond();

				bt_device = bt_newlist.get(position);
				bt_connection = new BluetoothConnectionService(MainActivity.this);
			}
		});
		v.findViewById(R.id.bt_btn_find).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (bt_adapter.isDiscovering()) bt_adapter.cancelDiscovery();
				bt_adapter.startDiscovery();
				new_message(v.getContext(), "Find Devices");
			}
		});
		v.findViewById(R.id.bt_btn_find).callOnClick();
		return new AlertDialog.Builder(this).setView(v);
	}

	protected void bt_getpaired() {
		Set<BluetoothDevice> set = bt_adapter.getBondedDevices();
		bt_pairedlist.clear();
		for (BluetoothDevice bd : set) {
			bt_pairedlist.add(bd);
		}
	}

	protected void bt_update(int toggle) {
		if (bt_adapter == null || menu == null) return;

		boolean on = bt_adapter.isEnabled();
		if (toggle == 1) {
			startActivityForResult(bt_intent, 1);
			on = true;
		} else if (toggle == 0) {
			bt_adapter.disable();
			on = false;
		}

		if (on) {
			menu.findItem(R.id.menu_bt).setIcon(new_drawable(R.drawable.d_bt_on, Color.TRANSPARENT));
			menu.findItem(R.id.menu_bt_on).setVisible(false);
			menu.findItem(R.id.menu_bt_off).setVisible(true);
			menu.findItem(R.id.menu_bt_discoverable).setVisible(true);
			menu.findItem(R.id.menu_bt_finddevices).setVisible(true);
			findViewById(R.id.message_btn_toggle).setVisibility(View.VISIBLE);
		} else {
			menu.findItem(R.id.menu_bt).setIcon(new_drawable(R.drawable.d_bt_off, Color.TRANSPARENT));
			menu.findItem(R.id.menu_bt_on).setVisible(true);
			menu.findItem(R.id.menu_bt_off).setVisible(false);
			menu.findItem(R.id.menu_bt_discoverable).setVisible(false);
			menu.findItem(R.id.menu_bt_finddevices).setVisible(false);
			findViewById(R.id.message_btn_toggle).setVisibility(View.INVISIBLE);
		}
	}

	protected void bt_discover() {//TODO:TEST
		if (bt_adapter == null) return;

		Intent discoverable_indent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverable_indent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivity(discoverable_indent);
	}

	protected void bt_startconnection(BluetoothDevice device, UUID uuid) {
		bt_connection.start_client(device, uuid);
	}

	private final BroadcastReceiver bt_receiver = new BroadcastReceiver() { //TODO:TEST
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			new_message(getApplicationContext(), action);

			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

				//

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

				//

			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (!bt_newlist.contains(device) && !bt_pairedlist.contains(device)) {
					bt_newlist.add(device);
					bt_listadapter = new DeviceListAdapter(context, R.layout.device_adapter_view, bt_newlist);
					bt_lv_devices.setAdapter(bt_listadapter);
				}

			} else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {

				//

			} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

				//

			} else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {

				//

			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

				//

			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch (device.getBondState()) {
					case BluetoothDevice.BOND_NONE:
						new_message(getApplicationContext(), "BluetoothDevice.BOND_NONE");
						((TextView) findViewById(R.id.bt_lbl_connected)).setText("");
						bt_device = device;
						break;
					case BluetoothDevice.BOND_BONDING:
						new_message(getApplicationContext(), "BluetoothDevice.BOND_BONDING");
						break;
					case BluetoothDevice.BOND_BONDED:
						new_message(getApplicationContext(), "BluetoothDevice.BOND_BONDED");
						((TextView) findViewById(R.id.bt_lbl_connected)).setText(device.getName());
						break;
					default:
						new_message(getApplicationContext(), "" + device.getBondState());
						break;
				}
			}
		}
	};

	protected AlertDialog.Builder pop_message() {
		final View v = inflater.inflate(R.layout.activity_message, null);
		v.findViewById(R.id.data_btn_send).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				byte[] bytes = ((TextView) findViewById(R.id.data_txt_message)).getText().toString().getBytes(Charset.defaultCharset());
				bt_connection.write(bytes);
				Toast.makeText(v.getContext(), "Send", Toast.LENGTH_SHORT).show();
			}
		});
		v.findViewById(R.id.data_btn_clear).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(), "Clear", Toast.LENGTH_SHORT).show();
			}
		});
		return new AlertDialog.Builder(this).setView(v);
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
			time_handler.postDelayed(time_stopwatch, 0);

			findViewById(R.id.time_swt_isfast).setEnabled(false);
			findViewById(R.id.time_btn_reset).setEnabled(false);
			b.setText("Pause");
		} else {
			time_handler.removeCallbacks(time_stopwatch);

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
			time_handler.postDelayed(this, 0);
		}
	};

	protected void point_set(boolean origin) {
		//COMMON VALIDITY CHECK (1)
		String point_x = ((TextView) findViewById(R.id.point_txt_x)).getText().toString(),
			point_y = ((TextView) findViewById(R.id.point_txt_y)).getText().toString();
		if (point_x.equalsIgnoreCase("") || point_y.equalsIgnoreCase("")) {
			new_message(this, "Please fill in the text fields.");
			return;
		}
		//COMMON VALIDITY CHECK (2)
		int col = Integer.valueOf(point_x),
			row = Integer.valueOf(point_y),
			cell = cell_id(col, row);
		if (robot_hit(cell)) {
			new_message(this, "Robot will collide with obstacle.");
			return;
		}

		//SPECIFIC CHECK
		if (origin) {
			if ((col > (MAZE_C - ROBOT_SIZE)) || (row > (MAZE_R - ROBOT_SIZE))) {
				new_message(this, "Robot cannot move to that point.");
			} else {
				robot_location = cell;
				reset_cells();
				robot_go();
			}
		} else {
			Drawable box;
			if (void_list.contains(way_point)) {
				new_message(this, "Way point is unable to place at that point.");
				return;
			}

			if (way_point > -1) {
				box = new_drawable(R.drawable.d_box, enum_getcolor(((TextView) grid_maze.getChildAt(way_point)).getText().toString()));
				grid_maze.getChildAt(way_point).setBackground(box);
			}
			box = new_drawable(R.drawable.d_box, Cell.WAYPOINT.getColor());
			grid_maze.getChildAt(cell).setBackground(box);

			way_point = cell;
		}
	}

	protected void display_option() { //TODO:UNKNOWN - HOW TO AUTO?
		SwitchCompat s = findViewById(R.id.display_swt_ismanual);
		Button b = findViewById(R.id.display_btn_update);
		if (s.isChecked()) {
			s.setText("Manual");
			b.setEnabled(true);
		} else {
			s.setText("Auto");
			b.setEnabled(false);
		}
	}

	protected void display_manual() { //TODO:UNKNOWN - HOW TO MANUAL?

	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			String title; //TODO:REMOVABLE
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
					robot_move(enum_getdirection((((int) robot.getRotation()) % 360) / 90));
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

				//CONFIGURATIONS //TODO:UNKNOWN - WHAT TO DO?
				case R.id.config_btn_reconfig:
					break;

				//DISPLAY GRAPHICS //TODO:UNKNOWN - WHAT TO DO?
				case R.id.display_swt_ismanual:
					display_option();
					break;
				case R.id.display_btn_update:
					display_manual();
					break;

				//MESSAGE
				case R.id.message_btn_toggle:
					bt_startconnection(bt_device, BT_UUID);
					pop_message().show();
					break;
			}
		}
	};
}
