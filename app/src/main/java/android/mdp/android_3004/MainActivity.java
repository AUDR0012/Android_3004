package android.mdp.android_3004;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
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

	BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
	ArrayList<BluetoothDevice> bt_newlist = new ArrayList<>(), bt_pairedlist = new ArrayList<>();
	ListView bt_lv_device;
	DeviceListAdapter bt_listadapter;
	BluetoothConnectionService bt_connection = null;
	BluetoothDevice bt_device;
	String bt_prev_addr;
	boolean bt_display_isfind;

	ListView msg_lv_chat, msg_lv_preview;
	ArrayList<Message> msg_chatlist = new ArrayList<>();
	ArrayAdapter msg_listadapter;

	SensorManager sensor_manager;
	Sensor accelerometer_sensor;
	SensorEventListener accelerometer_sensor_listener;

	Handler time_handler = new Handler();
	long time_start;
	TextView time_textview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//		========== OTHERS ==========
		inflater = LayoutInflater.from(this);

		bt_getpaired();
		reg_bt_intentfilter();
		LocalBroadcastManager.getInstance(this).registerReceiver(bt_msg_receiver, new IntentFilter("messaging"));
		msg_lv_preview = findViewById(R.id.msg_lv_preview);
		//msg_lv_chat = findViewById(R.id.msg_lv_chat);

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.tilt_swt_isoff,
			R.id.direction_btn_up, R.id.direction_btn_down, R.id.direction_btn_left, R.id.direction_btn_right,
			R.id.time_btn_stopwatch, R.id.time_btn_reset, R.id.time_swt_isfastest,
			R.id.point_btn_origin, R.id.point_btn_way,
			R.id.config_btn_f1, R.id.config_btn_f2, R.id.config_btn_reconfig,
			R.id.display_swt_ismanual, R.id.display_btn_update};
		for (int onclick : list_onclick) {
			findViewById(onclick).setOnClickListener(onClickListener);
		}

		findViewById(R.id.msg_temp).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pop_message().show();
			}
		});

		msg_lv_preview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				pop_message().show();
			}
		});

		findViewById(R.id.bt_txt_connected).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bt_checkpaired();
			}
		});

//		========== MAZE ==========
		grid_maze = findViewById(R.id.grid_maze);
		grid_maze.setColumnCount(MAZE_C);
		grid_maze.setRowCount(MAZE_R);

		Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.DEFAULT.getColor());
		for (int i = 0; i < MAZE_C * MAZE_R; i++) {
			TextView tv = new TextView(this);
			tv.setGravity(Gravity.CENTER);
			tv.setBackground(box);
			tv.setText(String.valueOf(Enum.Cell.DEFAULT.get()));

			grid_maze.addView(tv);
		}

//		obstacle_create(); //TODO:REMOVABLE
//		obstacle_arrow(obstacle_list.get(0) % MAZE_C, obstacle_list.get(0) / MAZE_C, Enum.Direction.UP.get()); //TODO:REMOVABLE

//		========== ACCELEROMETER ==========
		sensor_manager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer_sensor = sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		accelerometer_sensor_listener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				tilt_move(sensorEvent.values[0], sensorEvent.values[1]);
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
		if (bt_adapter.isDiscovering()) bt_adapter.cancelDiscovery();
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
		mi.inflate(R.menu.menu, menu);

		this.menu = menu;
		reset_app();
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
			case R.id.menu_bt_discover:
				bt_discover();
				return true;
			case R.id.menu_bt_find:
				AlertDialog.Builder bt_dialog = pop_bluetooth();
				bt_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (bt_adapter.isDiscovering()) bt_adapter.cancelDiscovery();
					}
				});
				bt_dialog.show();
				return true;
			case R.id.menu_bt_reconnect:
				if (!r_string(R.string._null).equalsIgnoreCase(bt_prev_addr))
					bt_device = bt_adapter.getRemoteDevice(bt_prev_addr);
				bt_checkpaired();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected String r_string(int id) {
		return getResources().getString(id);
	}

	protected String view_string(View v) {
		if (v instanceof TextView)
			return ((TextView) v).getText().toString();
		else if (v instanceof Button)
			return ((Button) v).getText().toString();
		return null;
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

	protected int enum_getcolor(int id) {
		for (Enum.Cell c : Enum.Cell.values()) {
			if (c.get() == id) {
				return c.getColor();
			}
		}
		return -1;
	}

	protected Enum.Direction enum_getdirection(int id) {
		for (Enum.Direction d : Enum.Direction.values()) {
			if (d.get() == id) {
				return d;
			}
		}
		return null;
	}

	protected Enum.Instruction enum_getinstruction(String arduino) {
		for (Enum.Instruction i : Enum.Instruction.values()) {
			if (i.getArduino().equalsIgnoreCase(arduino)) {
				return i;
			}
		}
		return null;
	}

	protected int cell_id(int col, int row) {
		return (row * MAZE_C) + col;
	}

	protected void reset_app() {
		if (bt_connection == null)
			bt_connection = new BluetoothConnectionService(this);
		bt_update(-1);
		bt_checkpaired();

		reset_cells();

		way_point = -1;
		robot_location = cell_id(START_COL, START_ROW);
		robot_go();

		tilt_option();

		((TextView) findViewById(R.id.time_txt_explore)).setText(R.string.time_default);
		((TextView) findViewById(R.id.time_txt_fastest)).setText(R.string.time_default);
		time_option();

		((EditText) findViewById(R.id.point_txt_x)).setText(r_string(R.string._null));
		((EditText) findViewById(R.id.point_txt_y)).setText(r_string(R.string._null));

		display_option();
	}

	protected void reset_cells() {
		Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.DEFAULT.getColor());

		for (int i = 0; i < MAZE_C * MAZE_R; i++) {
			TextView tv = (TextView) grid_maze.getChildAt(i);
			if (Integer.valueOf(view_string(tv)) == Enum.Cell.PASSED.get()) {
				tv.setText(String.valueOf(Enum.Cell.DEFAULT.get()));
				tv.setBackground(box);
			}
		}
	}

	protected void reg_bt_intentfilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
		registerReceiver(bt_receiver, filter);
	}

	protected void obstacle_create() { //TODO:REMOVABLE
		int cell, count = (new Random()).nextInt(10) + 10;

		Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.OBSTACLE.getColor());
		TextView tv;
		for (int i = 0; i < count; i++) {
			cell = (new Random()).nextInt(210) + 45;
			if (void_list.contains(cell)) {
				i--;
				continue;
			}
			tv = (TextView) grid_maze.getChildAt(cell);
			tv.setBackground(box);
			tv.setText(String.valueOf(Enum.Cell.OBSTACLE.get()));
			obstacle_list.add(cell);
		}
	}

	protected void obstacle_arrow(int col, int row, int direction) {
		ImageView obstacle = new ImageView(this);
		obstacle.setImageDrawable(new_drawable(R.drawable.d_arrow, Color.TRANSPARENT));
		obstacle.setBackground(new_drawable(R.drawable.d_box, Enum.Cell.OBSTACLE.getColor()));
		obstacle.setRotation(direction * 90);
		obstacle.setLayoutParams(new_layoutparams(col, row, 1));
		grid_maze.addView(obstacle);
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

		Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.PASSED.getColor());
		TextView tv;
		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				int cell = cell_id(c, r);
				tv = (TextView) grid_maze.getChildAt(cell);
				if (cell != way_point) {
					tv.setBackground(box);
				}
				tv.setText(String.valueOf(Enum.Cell.PASSED.get()));
			}
		}
	}

	protected void robot_rotate(int direction) {
		robot.setRotation(robot.getRotation() + (direction * 90));
	}

	protected void robot_move(Enum.Direction direction) {
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
		bt_lv_device = v.findViewById(R.id.bt_lv_devices);
		bt_lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				if (bt_adapter.isDiscovering()) bt_adapter.cancelDiscovery();
				if (bt_display_isfind) {
					bt_newlist.get(position).createBond();
				} else {
					bt_device = bt_pairedlist.get(position);
					bt_checkpaired();
				}
			}
		});
		v.findViewById(R.id.bt_btn_find).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (bt_adapter.isDiscovering()) bt_adapter.cancelDiscovery();
				bt_display_isfind = true;

				bt_adapter.startDiscovery();
				bt_newlist.clear();
				bt_listview(v.getContext(), bt_newlist);
				new_message(v.getContext(), "Find New Devices...");
			}
		});
		v.findViewById(R.id.bt_btn_paired).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (bt_adapter.isDiscovering()) bt_adapter.cancelDiscovery();
				bt_display_isfind = false;

				bt_getpaired();
				bt_listview(v.getContext(), bt_pairedlist);
			}
		});
		return new AlertDialog.Builder(this).setView(v);
	}

	protected void bt_listview(Context context, ArrayList<BluetoothDevice> devicelist) {
		bt_listadapter = new DeviceListAdapter(context, R.layout.device_adapter_view, devicelist);
		bt_lv_device.setAdapter(bt_listadapter);
	}

	protected void bt_getpaired() {
		Set<BluetoothDevice> set = bt_adapter.getBondedDevices();
		bt_pairedlist.clear();
		for (BluetoothDevice bd : set) {
			bt_pairedlist.add(bd);
		}

		if (bt_pairedlist.size() == 0) {
			new_message(this, "There are no paired devices");
		}
	}

	protected void bt_checkpaired() {
		if (bt_device == null) {
			((TextView) findViewById(R.id.bt_lbl_connected)).setText(R.string.bt_connect_no);
			findViewById(R.id.bt_txt_connected).setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.bt_txt_connected)).setText(r_string(R.string._null));

			findViewById(R.id.msg_lv_preview).setVisibility(View.INVISIBLE);
			findViewById(R.id.msg_temp).setVisibility(View.INVISIBLE);
		} else {
			bt_connection.start_client(bt_device);

			((TextView) findViewById(R.id.bt_lbl_connected)).setText(R.string.bt_connect_yes);
			findViewById(R.id.bt_txt_connected).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.bt_txt_connected)).setText(bt_device.getName());

			findViewById(R.id.msg_lv_preview).setVisibility(View.VISIBLE);
			if (msg_chatlist.size() == 0) findViewById(R.id.msg_temp).setVisibility(View.VISIBLE);
		}
	}

	protected void bt_update(int toggle) {
		if (bt_adapter == null || menu == null) return;

		boolean on = bt_adapter.isEnabled();
		if (toggle == 1) {
			Intent intent_ACTION_REQUEST_ENABLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent_ACTION_REQUEST_ENABLE, 1);
			on = true;
		} else if (toggle == 0) {
			bt_adapter.disable();
			on = false;
			msg_chatlist.clear();
		}

		menu.findItem(R.id.menu_bt).setIcon(new_drawable(on ? R.drawable.d_bt_on : R.drawable.d_bt_off, Color.TRANSPARENT));
		menu.findItem(R.id.menu_bt_on).setVisible(!on);
		menu.findItem(R.id.menu_bt_off).setVisible(on);
		menu.findItem(R.id.menu_bt_discover).setVisible(on);
		menu.findItem(R.id.menu_bt_find).setVisible(on);
	}

	protected void bt_discover() {
		if (bt_adapter == null) return;

		Intent indent_ACTION_REQUEST_DISCOVERABLE = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		indent_ACTION_REQUEST_DISCOVERABLE.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivity(indent_ACTION_REQUEST_DISCOVERABLE);
	}

	private final BroadcastReceiver bt_receiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (bt_device != device) {
					bt_device = device;
					bt_checkpaired();
				}

			} else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {

				bt_prev_addr = bt_device.getAddress();
				bt_device = null;
				bt_checkpaired();
				msg_chatlist.clear();

			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

				bt_prev_addr = bt_device.getAddress();
				bt_device = null;
				bt_checkpaired();
				msg_chatlist.clear();

			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch (device.getBondState()) {
					case BluetoothDevice.BOND_NONE:
						new_message(getApplicationContext(), "BOND_NONE");
						break;
					case BluetoothDevice.BOND_BONDING:
						new_message(getApplicationContext(), "BOND_BONDING");
						break;
					case BluetoothDevice.BOND_BONDED:
						new_message(getApplicationContext(), "BOND_BONDED");
						bt_device = device;
						bt_checkpaired();
						break;
				}
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (!bt_newlist.contains(device) && !bt_pairedlist.contains(device) && (device != bt_device)) {
					bt_newlist.add(device);
					bt_listview(context, bt_newlist);
				}

			} else {
				new_message(context, action);
			}
		}
	};

	protected AlertDialog.Builder pop_message() {
		View v = inflater.inflate(R.layout.activity_message, null);
		final TextView tv = v.findViewById(R.id.data_txt_msg);
		msg_lv_chat = v.findViewById(R.id.msg_lv_chat);
		msg_listview(this);

		v.findViewById(R.id.data_btn_send).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				msg_writemsg(v.getContext(), view_string(tv));
			}
		});
		v.findViewById(R.id.data_btn_clear).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setText(r_string(R.string._null));
			}
		});
		return new AlertDialog.Builder(this).setView(v);
	}

	protected void msg_writemsg(Context context, String text) {
		byte[] bytes = text.getBytes(Charset.defaultCharset());
		bt_connection.write(bytes);

		msg_chatlist.add(new Message(false, text, getResources()));
		msg_listview(context);
	}

	protected void msg_listview(Context context) {
		msg_listadapter = new ChatListAdapter(context, R.layout.chat_adapter_view, msg_chatlist);

		if (msg_lv_preview != null) {
			if (msg_chatlist.size() != 0)
				findViewById(R.id.msg_temp).setVisibility(View.GONE);

			msg_lv_preview.setAdapter(msg_listadapter);
			msg_lv_preview.post(new Runnable() {
				@Override
				public void run() {
					msg_lv_preview.setSelection(msg_listadapter.getCount() - 1);
				}
			});
		}

		if (msg_lv_chat != null) {
			msg_lv_chat.setAdapter(msg_listadapter);
			msg_lv_chat.post(new Runnable() {
				@Override
				public void run() {
					msg_lv_chat.setSelection(msg_listadapter.getCount() - 1);
				}
			});
		}
	}

	private final BroadcastReceiver bt_msg_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String text = intent.getStringExtra("read message");
			msg_chatlist.add(new Message(true, text, getResources()));
			msg_listview(getApplicationContext());

			msg_instruction(false, enum_getinstruction(text));
		}
	};

	protected void tilt_option() {
		SwitchCompat s = findViewById(R.id.tilt_swt_isoff);
		if (s.isChecked()) {
			s.setText(R.string.tilt_off);
			sensor_manager.unregisterListener(accelerometer_sensor_listener);
		} else {
			s.setText(R.string.tilt_on);
			sensor_manager.registerListener(accelerometer_sensor_listener, accelerometer_sensor, 3);
		}
	}

	protected void tilt_move(float x, float y) { //TODO:SENDING/MOVING ISSUE
		if (x > 0.5f) { //MOVE LEFT
			robot_move(Enum.Direction.LEFT);
		}
		if (x < -0.5f) { //MOVE RIGHT
			robot_move(Enum.Direction.RIGHT);
		}
		if (y > 0.5f) { //MOVE DOWN
			robot_move(Enum.Direction.DOWN);
		}
		if (y < -0.5f) { //MOVE UP
			robot_move(Enum.Direction.UP);
		}
	}

	protected void time_option() {
		SwitchCompat s = findViewById(R.id.time_swt_isfastest);
		if (s.isChecked()) {
			s.setText(R.string.time_fastest);
			time_textview = findViewById(R.id.time_txt_fastest);
		} else {
			s.setText(R.string.time_explore);
			time_textview = findViewById(R.id.time_txt_explore);
		}
		findViewById(R.id.time_btn_stopwatch).setEnabled(view_string(time_textview).equalsIgnoreCase(r_string(R.string.time_default)));
	}

	protected void time_stopwatch(View v) {
		Button b = (Button) v;
		if (view_string(b).equalsIgnoreCase(r_string(R.string.time_start))) {
			time_start = SystemClock.uptimeMillis();
			time_handler.postDelayed(time_stopwatch, 0);

			findViewById(R.id.time_swt_isfastest).setEnabled(false);
			findViewById(R.id.time_btn_reset).setEnabled(false);
			b.setText(R.string.time_stop);
		} else {
			time_handler.removeCallbacks(time_stopwatch);

			findViewById(R.id.time_swt_isfastest).setEnabled(true);
			findViewById(R.id.time_btn_reset).setEnabled(true);
			findViewById(R.id.time_btn_stopwatch).setEnabled(false);
			b.setText(R.string.time_start);
		}
	}

	protected void time_reset() {
		time_start = 0L;
		time_set(0, 0, 0);
		findViewById(R.id.time_btn_stopwatch).setEnabled(true);
	}

	protected void time_set(int min, int sec, int millisec) {
		time_textview.setText(String.format("%d:%s:%s", min, String.format("%02d", sec), String.format("%03d", millisec)));
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
		String point_x = view_string(findViewById(R.id.point_txt_x)),
			point_y = view_string(findViewById(R.id.point_txt_y));
		if (point_x.equalsIgnoreCase(r_string(R.string._null)) || point_y.equalsIgnoreCase(r_string(R.string._null))) {
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
				box = new_drawable(R.drawable.d_box, enum_getcolor(Integer.valueOf(view_string(grid_maze.getChildAt(way_point)))));
				grid_maze.getChildAt(way_point).setBackground(box);
			}
			box = new_drawable(R.drawable.d_box, Enum.Cell.WAYPOINT.getColor());
			grid_maze.getChildAt(cell).setBackground(box);

			way_point = cell;
		}
	}

	protected void display_option() {
		SwitchCompat s = findViewById(R.id.display_swt_ismanual);
		Button b = findViewById(R.id.display_btn_update);
		if (s.isChecked()) {
			s.setText(R.string.display_manual);
			b.setEnabled(true);
		} else {
			s.setText(R.string.display_auto);
			b.setEnabled(false);
		}
	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		public void onClick(View v) {

			switch (v.getId()) {
				//ACCELEROMETER
				case R.id.tilt_swt_isoff:
					tilt_option();
					break;

				//MAZE
				case R.id.direction_btn_up:
					msg_instruction(true, Enum.Instruction.FORWARD);
					break;
				case R.id.direction_btn_down:
					msg_instruction(true, Enum.Instruction.REVERSE);
					break;
				case R.id.direction_btn_left:
					msg_instruction(true, Enum.Instruction.ROTATE_LEFT);
					break;
				case R.id.direction_btn_right:
					msg_instruction(true, Enum.Instruction.ROTATE_RIGHT);
					break;

				//STOPWATCH
				case R.id.time_swt_isfastest:
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

				//CONFIGURATIONS //TODO:UNKNOWN - WHAT IS CONFIG?
				case R.id.config_btn_f1:
					break;
				case R.id.config_btn_f2:
					break;
				case R.id.config_btn_reconfig:
					break;

				//DISPLAY GRAPHICS //TODO:UNKNOWN - HOW TO MANUAL/AUTO?
				case R.id.display_swt_ismanual:
					display_option();
					break;
				case R.id.display_btn_update:
					break;
			}
		}
	};

	protected void msg_instruction(boolean towrite, Enum.Instruction instruction) {
		if (instruction != null) {
			if (bt_device != null && towrite) {
				msg_writemsg(this, instruction.getArduino());
			}
			switch (instruction) {
				case FORWARD:
					robot_move(enum_getdirection((((int) robot.getRotation()) % 360) / 90));
					break;
				case REVERSE:
					robot_move(enum_getdirection((((int) (robot.getRotation() + 180)) % 360) / 90));
					break;
				case ROTATE_LEFT:
					robot_rotate(Enum.Direction.LEFT.get());
					break;
				case ROTATE_RIGHT:
					robot_rotate(Enum.Direction.RIGHT.get());
					break;
				case STRAFE_LEFT:
					robot_rotate(Enum.Direction.LEFT.get());
					robot_move(enum_getdirection((((int) robot.getRotation()) % 360) / 90));
					break;
				case STRAFE_RIGHT:
					robot_rotate(Enum.Direction.RIGHT.get());
					robot_move(enum_getdirection((((int) robot.getRotation()) % 360) / 90));
					break;
				case BEGIN_EXPLORATION:
					if (time_start != 0L) {
						if (findViewById(R.id.time_btn_stopwatch).isEnabled())
							findViewById(R.id.time_btn_stopwatch).callOnClick();
						findViewById(R.id.time_btn_reset).callOnClick();
					}
					((SwitchCompat) findViewById(R.id.time_swt_isfastest)).setChecked(false);
					time_option();
					findViewById(R.id.time_btn_stopwatch).callOnClick();
					break;
				case BEGIN_FASTEST_PATH:
					if (time_start != 0L) {
						if (findViewById(R.id.time_btn_stopwatch).isEnabled())
							findViewById(R.id.time_btn_stopwatch).callOnClick();
						findViewById(R.id.time_btn_reset).callOnClick();
					}
					((SwitchCompat) findViewById(R.id.time_swt_isfastest)).setChecked(true);
					time_option();
					findViewById(R.id.time_btn_stopwatch).callOnClick();
					break;
				case SEND_ARENA_INFO:
					String text = r_string(R.string._null);
					for (int i = 0; i < MAZE_C * MAZE_R; i++) {
						if (!text.equalsIgnoreCase(r_string(R.string._null)) && (i % MAZE_C) == 0) {
							text += "\n";
						}
						text += (((TextView) grid_maze.getChildAt(i)).getText() + " ");
					}
					msg_writemsg(this, text);
					break;
			}
		}
	}
}
