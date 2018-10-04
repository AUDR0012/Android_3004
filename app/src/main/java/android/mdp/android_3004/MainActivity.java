package android.mdp.android_3004;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

	final int MAZE_C = 15;
	final int MAZE_R = 20;
	final int ROBOT_SIZE = 3;

	final int OBSTACLE_ADD = 1000;

	int origin_col = 0,
		origin_row = MAZE_R - ROBOT_SIZE;

	Menu menu;
	LayoutInflater inflater;
	boolean msg_showchat = true;

	String mdf_string1, mdf_string2;

	GridLayout grid_maze;
	ImageView robot;
	int point_robot, point_way;
	boolean point_isset;
	ArrayMap<Integer, Boolean> obst_list = new ArrayMap<>();

	AlertDialog dialog_bt;
	BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
	ArrayList<BluetoothDevice> bt_newlist = new ArrayList<>(), bt_pairedlist = new ArrayList<>();
	ListView bt_lv_device;
	DeviceListAdapter bt_listadapter;
	BluetoothConnectionService bt_connection = null;
	BluetoothDevice bt_device, bt_prev = null;
	boolean bt_display_isfind, bt_new_finding = false, bt_robust;

	AlertDialog dialog_msg;
	ListView msg_lv_chat, msg_lv_preview;
	ArrayList<MessageText> msg_chatlist = new ArrayList<>();
	ArrayAdapter msg_listadapter;

	SensorManager tilt_manager;
	Sensor tilt_sensor;
	SensorEventListener tilt_listener;
	Handler tilt_handler = new Handler();
	int tilt_delay = 1;
	Boolean tilt_canmove = true;

	Handler time_handler = new Handler();
	long time_start;
	TextView time_tv;

	boolean display_ismanual = false;
	ArrayList<Enum.Instruction> display_manuallist = new ArrayList<>();

	AlertDialog dialog_config;
	SharedPreferences config_pref;

	ClipboardManager copy_board;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//		========== OTHERS ==========
		inflater = LayoutInflater.from(this);

		reg_bt_intentfilter();
		LocalBroadcastManager.getInstance(this).registerReceiver(msg_receiver, new IntentFilter("messaging"));
		msg_lv_preview = findViewById(R.id.msg_lv_preview);

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.bt_swt_isrobust, R.id.tilt_swt_ison,
			R.id.direction_btn_up, R.id.direction_btn_down, R.id.direction_btn_left, R.id.direction_btn_right,
			R.id.time_btn_stopwatch, R.id.time_swt_isfastest,
//			R.id.point_btn_origin, R.id.point_btn_way, //MANUAL KEY-IN
			R.id.point_swt_isway, R.id.point_btn_set,
			R.id.display_swt_ismanual, R.id.display_btn_update,
			R.id.config_btn_f1, R.id.config_btn_f2, R.id.config_btn_reconfig};
		for (int onclick : list_onclick) {
			findViewById(onclick).setOnClickListener(onClickListener);
		}

		findViewById(R.id.msg_temp).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog_msg = pop_message().show();
			}
		});

		msg_lv_preview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				dialog_msg = pop_message().show();
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
			tv.setId(i);
			tv.setOnClickListener(tv_onClickListener);

			grid_maze.addView(tv);
		}

//		========== ACCELEROMETER ==========
		tilt_manager = (SensorManager) getSystemService(SENSOR_SERVICE);
		tilt_sensor = tilt_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		tilt_listener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				tilt_move(sensorEvent.values[0], sensorEvent.values[1]);
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {
			}
		};
		tilt_handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				tilt_canmove = true;
				tilt_handler.postDelayed(this, tilt_delay * 1000);
			}
		}, tilt_delay * 1000);

		//MANUAL KEY-IN
//		((EditText) findViewById(R.id.point_txt_x)).setHint(String.valueOf(MAZE_C - 1));
//		((EditText) findViewById(R.id.point_txt_y)).setHint(String.valueOf(MAZE_R - 1));
//		((EditText) findViewById(R.id.point_txt_x)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("0", String.valueOf(MAZE_C - 1))});
//		((EditText) findViewById(R.id.point_txt_y)).setFilters(new InputFilterMinMax[]{new InputFilterMinMax("0", String.valueOf(MAZE_R - 1))});
	}

	@Override
	public void onPause() {
		bt_canceldiscover();
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
			//HIDING
			case R.id.menu_restart:
				reset_app();
				return true;
			case R.id.menu_mdf:
				pop_mdf().show();
				return true;
			case R.id.menu_arrow:
				//TODO
				pop_arrow().show();
				return true;
			case R.id.menu_sensor:
				msg_writemsg("ar_c", "");
				return true;

			//BLUETOOTH
			case R.id.menu_bt:
				bt_update(-1);
				if (bt_adapter.isEnabled() && bt_connection == null) {
					bt_connection = new BluetoothConnectionService(this);
				}
				return true;
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
						bt_canceldiscover();
					}
				});
				dialog_bt = bt_dialog.show();
				return true;
			case R.id.menu_bt_reconnect:
				if (bt_prev == null) {
					new_message("You have not connected to a device previously");
				} else {
					bt_connection.start_client(bt_prev);
				}
				bt_checkpaired();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected AlertDialog.Builder pop_mdf() {
		View v = inflater.inflate(R.layout.pop_mdf, null);

		final TextView tv_s1 = v.findViewById(R.id.s1_txt_data);
		final TextView tv_s2 = v.findViewById(R.id.s2_txt_data);
		tv_s1.setText(mdf_string1);
		tv_s2.setText(mdf_string2);


		tv_s1.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copy_data(R.string.mdf_s1);
				return false;
			}
		});
		tv_s2.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copy_data(R.string.mdf_s2);
				return false;
			}
		});
		return new AlertDialog.Builder(this).setView(v);
	}

	protected void copy_data(int string) {
		String s = (string == R.string.mdf_s1) ? mdf_string1 : mdf_string2;

		ClipData copy_clip = ClipData.newPlainText("Copied text", s);
		copy_board.setPrimaryClip(copy_clip);
		new_message("Copied " + r_string(string));
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

	protected String new_message(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		return message;
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

	protected Enum.Direction enum_getdirection_chara(String chara) {
		for (Enum.Direction d : Enum.Direction.values()) {
			if (d.getChara().equalsIgnoreCase(chara)) {
				return d;
			}
		}
		return null;
	}

	protected Enum.Instruction enum_getinstruction(String text) {
		for (Enum.Instruction i : Enum.Instruction.values()) {
			if (i.getText().equalsIgnoreCase(text)) {
				return i;
			}
		}
		return null;
	}

	protected int cell_id(int col, int row) {
		return (row * MAZE_C) + col;
	}

	protected int cell_fliprow(int row) {
		return (MAZE_R - 1) - row;
	}

	protected int cell_robot(boolean iswrite, int cell) {
		return cell + ((MAZE_C + 1) * (iswrite ? -1 : 1));
	}

	protected void reset_app() {
		bt_update(-1);
		bt_checkpaired();
		bt_robust_option();

		mdf_string1 = r_string(R.string._null);
		mdf_string2 = r_string(R.string._null);

		config_pref = getPreferences(Context.MODE_PRIVATE);
		copy_board = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		reset_cells(true);
		point_robot = cell_id(origin_col, origin_row);
		robot_go();
		robot.setRotation(0);

		point_isset = false;
		point_way = -1;
		point_update(false);
		point_update(true);
		point_option();

		tilt_option();

		((TextView) findViewById(R.id.time_txt_explore)).setText(R.string.time_default);
		((TextView) findViewById(R.id.time_txt_fastest)).setText(R.string.time_default);
		time_option();

		((EditText) findViewById(R.id.point_txt_x)).setText(r_string(R.string._null));
		((EditText) findViewById(R.id.point_txt_y)).setText(r_string(R.string._null));

		display_option();
	}

	protected void reset_cells(boolean reset_all) {
		Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.DEFAULT.getColor());

		if (reset_all) {

			for (int i = 0; i < MAZE_C * MAZE_R; i++) {
				TextView tv = (TextView) grid_maze.findViewById(i);
				tv.setText(String.valueOf(Enum.Cell.DEFAULT.get()));
				tv.setBackground(box);
			}
			for (int i = 0; i < obst_list.size(); i++) {
				if (obst_list.valueAt(i)) {
					View v = findViewById(OBSTACLE_ADD + obst_list.keyAt(i));
					grid_maze.removeView(v);
				}
			}
			obst_list.clear();

		} else {

			for (int i = 0; i < MAZE_C * MAZE_R; i++) {
				TextView tv = (TextView) grid_maze.findViewById(i);
				if (Integer.valueOf(view_string(tv)) != Enum.Cell.OBSTACLE.get()) {
					tv.setText(String.valueOf(Enum.Cell.DEFAULT.get()));
					if (point_way != i) {
						tv.setBackground(box);
					}
				}
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

	protected String obst_create(int cell) {
		if (obst_list.containsKey(cell)) {
			return new_message("Obstacle is already created at that cell");
		} else {
			Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.OBSTACLE.getColor());
			TextView obstacle = grid_maze.findViewById(cell);
			obstacle.setBackground(box);
			obstacle.setText(String.valueOf(Enum.Cell.OBSTACLE.get()));
			obst_list.put(cell, false);
			return r_string(R.string._success);
		}
	}

	protected void obst_arrow(int cell, String face) {
		int col = cell % MAZE_C,
			row = cell / MAZE_C;

		TextView obst = new TextView(this);
		obst.setBackground(new_drawable(R.drawable.d_arrow, Color.TRANSPARENT));
		obst.setRotation(0);
		obst.setLayoutParams(new_layoutparams(col, row, 1));
		obst.setText(face);
		obst.setTextColor(Color.WHITE);
		obst.setGravity(Gravity.CENTER);
		obst.setId(OBSTACLE_ADD + cell);
		grid_maze.addView(obst);

		obst_list.setValueAt(obst_list.indexOfKey(cell), true);
		((TextView) grid_maze.getChildAt(cell)).setText(face);
	}

	protected AlertDialog.Builder pop_arrow() {
		View v = inflater.inflate(R.layout.pop_arrow, null);
		TextView tv_arrow = v.findViewById(R.id.arrow_txt_data);

		int count = 1;
		String text = r_string(R.string._null);
		for (int i = 0; i < obst_list.size(); i++) {
			if (obst_list.valueAt(i)) {
				int cell = obst_list.keyAt(i);
				TextView tv = findViewById(OBSTACLE_ADD + cell);
				text += String.format("%d. (%d,%d) %s", count, cell % MAZE_C, cell / MAZE_C, tv.getText());
			}
		}
		if (text == r_string(R.string._null)) {
			text = "There are no obstacles with an Up Arrow";
		}
		tv_arrow.setText(text);

		return new AlertDialog.Builder(this).setView(v);
	}

	protected void robot_go() {
		if (robot == null) {
			robot = new ImageView(this);
			robot.setImageDrawable(new_drawable(R.drawable.d_robot, Color.TRANSPARENT));
			grid_maze.addView(robot);
		}

		int col = point_robot % MAZE_C,
			row = point_robot / MAZE_C;
		robot.setLayoutParams(new_layoutparams(col, row, ROBOT_SIZE));

		Drawable box = new_drawable(R.drawable.d_box, Enum.Cell.PASSED.getColor());
		TextView tv;
		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				int cell = cell_id(c, r);
				tv = grid_maze.findViewById(cell);
				if (cell != point_way) {
					tv.setBackground(box);
				}
				tv.setText(String.valueOf(Enum.Cell.PASSED.get()));
			}
		}
	}

	protected void robot_rotate(int direction) {
		robot.setRotation(robot.getRotation() + (direction * 90));
	}

	protected boolean robot_move(Enum.Direction direction) {
		int temp_location = point_robot,
			col = point_robot % MAZE_C,
			row = point_robot / MAZE_C;

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
			point_robot = temp_location;
			robot_go();
			return true;
		}
		return false;
	}

	protected boolean robot_hit(int cell) {
		int col = cell % MAZE_C,
			row = cell / MAZE_C;

		for (int r = row; r < row + ROBOT_SIZE; r++) {
			for (int c = col; c < col + ROBOT_SIZE; c++) {
				if (obst_list.containsKey(cell_id(c, r)))
					return true;
			}
		}
		return false;
	}

	protected AlertDialog.Builder pop_bluetooth() {
		View v = inflater.inflate(R.layout.pop_bluetooth, null);
		bt_lv_device = v.findViewById(R.id.bt_lv_devices);
		bt_lv_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				bt_canceldiscover();
				if (bt_display_isfind) {
					bt_newlist.get(position).createBond();
				} else {
					bt_connection.start_client(bt_pairedlist.get(position));
					bt_checkpaired();
				}
			}
		});
		v.findViewById(R.id.bt_btn_find).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bt_canceldiscover();
				bt_display_isfind = true;

				bt_adapter.startDiscovery();
				bt_newlist.clear();
				bt_listview(v.getContext(), bt_newlist);
				bt_new_finding = true;
			}
		});
		v.findViewById(R.id.bt_btn_paired).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bt_canceldiscover();
				bt_display_isfind = false;

				bt_getpaired();
				bt_listview(v.getContext(), bt_pairedlist);
			}
		});
		return new AlertDialog.Builder(this).setView(v);
	}

	protected void bt_canceldiscover() {
		if (bt_adapter.isDiscovering()) {
			if (bt_new_finding) bt_new_finding = false;
			bt_adapter.cancelDiscovery();
		}
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
			new_message("There are no paired devices");
		}
	}

	protected void bt_checkpaired() {
		if (bt_device == null) {
			((TextView) findViewById(R.id.bt_lbl_connected)).setText(R.string.bt_connect_no);
			findViewById(R.id.bt_txt_connected).setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.bt_txt_connected)).setText(r_string(R.string._null));

			findViewById(R.id.msg_lv_preview).setVisibility(View.GONE);
			findViewById(R.id.msg_temp).setVisibility(View.GONE);
			//if (dialog_msg.isShowing()) dialog_msg.dismiss();
		} else {
			if (dialog_bt.isShowing()) dialog_bt.dismiss();

			((TextView) findViewById(R.id.bt_lbl_connected)).setText(R.string.bt_connect_yes);
			findViewById(R.id.bt_txt_connected).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.bt_txt_connected)).setText(bt_device.getName());

			if (msg_showchat) {
				findViewById(R.id.msg_lv_preview).setVisibility(View.VISIBLE);
				if (msg_chatlist.size() == 0) {
					findViewById(R.id.msg_temp).setVisibility(View.VISIBLE);
				}
			} else {
				findViewById(R.id.msg_lv_preview).setVisibility(View.GONE);
				findViewById(R.id.msg_temp).setVisibility(View.GONE);
			}
		}
	}

	protected void bt_update(int toggle) {
		// 1:: on bluetooth
		// 0:: off bluetooth
		//-1:: verify bluetooth details
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
		menu.findItem(R.id.menu_bt_reconnect).setVisible(on);
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

			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

				if (bt_new_finding) {
					new_message("Finding new devices...");
				}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

				if (bt_new_finding) {
					if (bt_newlist.size() == 0) {
						new_message("No new devices found");
					}
					bt_new_finding = false;
				}

			} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

				if (bt_connection != null) {
					bt_device = bt_connection.getDevice();
					bt_prev = bt_device;
					bt_canceldiscover();
					bt_checkpaired();
				}

			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

				if (bt_device != null) {
					bt_device = null;
					bt_checkpaired();
					msg_chatlist.clear();

					if (bt_robust) {
						bt_connection.start_client(bt_prev);
					}
				}

			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch (device.getBondState()) {
					case BluetoothDevice.BOND_NONE:
						break;
					case BluetoothDevice.BOND_BONDING:
						break;
					case BluetoothDevice.BOND_BONDED:
						bt_connection.start_client(device);
						bt_device = device;
						bt_prev = bt_device;
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
				//new_message(action);
			}
		}
	};

	protected AlertDialog.Builder pop_message() {
		View v = inflater.inflate(R.layout.pop_message, null);
		final TextView tv = v.findViewById(R.id.msg_txt_data);
		final ImageButton btn = v.findViewById(R.id.msg_btn_clear);
		msg_lv_chat = v.findViewById(R.id.msg_lv_chat);
		msg_listview();

		tv.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (view_string(tv).trim().length() > 0 && btn.getVisibility() == View.GONE) {
					btn.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setText(r_string(R.string._null));
				btn.setVisibility(View.GONE);
			}
		});

		v.findViewById(R.id.msg_btn_send).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (view_string(tv).trim().length() > 0) {
					msg_writemsg(view_string(tv).trim(), r_string(R.string._null));
				}
			}
		});
		return new AlertDialog.Builder(this).setView(v);
	}

	protected void msg_writemsg(String text, String description) {
		if (bt_device == null) {
			new_message("unable to send message");
		} else {
			text = msg_appendto(text);
			byte[] bytes = text.getBytes(Charset.defaultCharset());
			bt_connection.write(bytes);

			String new_message = text;
			if (Enum.Instruction.SEND_ARENA_INFO.getDescription().equalsIgnoreCase(description)) {
				new_message = text.replaceAll(r_string(R.string._delimiter), "\n");
			}

			msg_chatlist.add(new MessageText(false, new_message, description, getResources()));
			msg_listview();
		}
	}

	protected String msg_appendto(String text) {
		boolean contains_delimeter = text.contains(r_string(R.string._delimiter)),
			contains_bracket = text.contains(r_string(R.string._bracket_s));

		if (contains_delimeter && contains_bracket) {
			return Enum.To.ALGORITHM.getCode() + text;
		} else if (enum_getinstruction(text) != null) {
			return Enum.To.ARDUINO.getCode() + text;
		} else {
			return text;
		}
	}

	protected void msg_listview() {
		msg_listadapter = new ChatListAdapter(this, R.layout.chat_adapter_view, msg_chatlist);

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

	private final BroadcastReceiver msg_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String text = intent.getStringExtra("read message").trim(),
				description = r_string(R.string._success);

			if (text.equalsIgnoreCase(r_string(R.string._code))) {
				msg_showchat = !msg_showchat;
				bt_checkpaired();
			} else if (!text.contains(r_string(R.string._bracket_s)) && !text.contains(r_string(R.string._delimiter)) && !text.contains(r_string(R.string._bracket_e))) {

				Enum.Instruction instruction = enum_getinstruction(text);
				msg_chatlist.add(new MessageText(true, text, ((instruction == null) ? r_string(R.string._null) : instruction.getDescription()), getResources()));
				msg_listview();

				if (instruction != null) {
					switch (instruction) {
						case SEND_ARENA_INFO:
							String send_arena = r_string(R.string._null);
							for (int i = 0; i < MAZE_C * MAZE_R; i++) {
								if (!send_arena.equalsIgnoreCase(r_string(R.string._null)) && (i % MAZE_C) == 0) {
									send_arena += r_string(R.string._delimiter);
								}
								send_arena += (view_string(grid_maze.findViewById(i)) + " ");
							}
							msg_writemsg(send_arena, instruction.getDescription());
							break;

						default: //MOVEMENTS
							msg_movements(false, true, instruction);
							break;
					}
				}

			} else {

				int ch_sta = text.indexOf(r_string(R.string._bracket_s)),
					ch_mid1 = text.indexOf(r_string(R.string._delimiter)),
					ch_mid2 = text.indexOf(r_string(R.string._delimiter), ch_mid1 + 1),
					ch_end = text.indexOf(r_string(R.string._bracket_e));

				try {
					Enum.Instruction instruction = enum_getinstruction(text.substring(0, ch_sta));
					String s1 = text.substring(ch_sta + 1, ch_mid1),
						s2 = text.substring(ch_mid1 + 1, (ch_mid2 == -1) ? ch_end : ch_mid2);

					int cell;
					Enum.Direction direction = (ch_mid2 == -1) ? null : enum_getdirection_chara(text.substring(ch_mid2 + 1, ch_end));

					switch (instruction) {
						case MDF:
							if (ch_mid2 != -1) {
								description = new_message(String.format("%s requires only 2 strings", instruction.getDescription()));
							} else {
								mdf_string1 = s1;
								mdf_string2 = s2;
							}
							break;

						case WAY:
							if (ch_mid2 != -1) {
								description = new_message(String.format("%s requires only 2 input: x, y", instruction.getDescription()));
							} else {
								cell = cell_id(Integer.valueOf(s1), cell_fliprow(Integer.valueOf(s2)));
								description = point_set2(true, cell, true);
								point_update(true);
							}
							break;
						case ORIGIN:
							if (ch_mid2 != -1) {
								description = new_message(String.format("%s requires only 2 input: x, y", instruction.getDescription()));
							} else {
								cell = cell_id(Integer.valueOf(s1), cell_fliprow(Integer.valueOf(s2)));
								description = point_set2(false, cell, true);
								point_update(false);
							}
							break;
						case OBSTACLE:
							if (ch_mid2 != -1) {
								description = new_message(String.format("%s requires only 2 input: x, y", instruction.getDescription()));
							} else {
								cell = cell_id(Integer.valueOf(s1), cell_fliprow(Integer.valueOf(s2)));
								description = obst_create(cell);
							}
							break;
						case ARROW:
							if (ch_mid2 == -1) {
								description = new_message(String.format("%s requires 3 input: x, y, direction", instruction.getDescription()));
							} else {
								if (direction == null) {
									description = new_message("Invalid direction found");
								} else {
									cell = cell_id(Integer.valueOf(s1), cell_fliprow(Integer.valueOf(s2)));

									if (obst_list.containsKey(cell)) {
										if (obst_list.get(cell)) {
											description = new_message("Arrow already created");
										} else {
											obst_arrow(cell, direction.getChara());
										}
									} else {
										description = new_message("No obstacle found at that cell");
									}
								}
							}
							break;
						default:
							throw new Exception();
					}
				} catch (Exception e) {
					description = new_message("Syntax Error");
				}

				msg_chatlist.add(new MessageText(true, text, description, getResources()));
				msg_listview();
			}
		}
	};

	protected void bt_robust_option() {
		SwitchCompat s = findViewById(R.id.bt_swt_isrobust);
		if (s.isChecked()) {
			s.setText(R.string.bt_robust_on);
			bt_robust = true;
		} else {
			s.setText(R.string.bt_robust_off);
			bt_robust = false;
		}
	}

	protected void tilt_option() {
		SwitchCompat s = findViewById(R.id.tilt_swt_ison);
		if (s.isChecked()) {
			s.setText(R.string._turnoff);
			tilt_manager.registerListener(tilt_listener, tilt_sensor, 3);
		} else {
			s.setText(R.string._turnon);
			tilt_manager.unregisterListener(tilt_listener);
		}
	}

	protected void tilt_move(float x, float y) {
		if (tilt_canmove) {
			tilt_canmove = false;

			float hori = (x > 0) ? x : 0 - x,
				vert = (y > 0) ? y : 0 - y;
			boolean movehori = (hori > vert);

			if (movehori) {
				if (x > 0.5f) { //MOVE LEFT
					tilt_action(Enum.Direction.LEFT);
				}
				if (x < -0.5f) { //MOVE RIGHT
					tilt_action(Enum.Direction.RIGHT);
				}
			} else {
				if (y > 0.5f) { //MOVE DOWN
					tilt_action(Enum.Direction.DOWN);
				}
				if (y < -0.5f) { //MOVE UP
					tilt_action(Enum.Direction.UP);
				}
			}
		}
	}

	protected void tilt_action(Enum.Direction target) {
		int curr = ((((int) robot.getRotation()) % 360) / 90),
			turns = (target.get() - curr);
		switch (turns) {
			case -3:
			case 1:
				msg_movements(true, true, Enum.Instruction.ROTATE_RIGHT);
				break;
			case -2:
			case 2:
				msg_movements(true, true, Enum.Instruction.ROTATE_RIGHT);
				msg_movements(true, true, Enum.Instruction.ROTATE_RIGHT);
				break;
			case -1:
			case 3:
				msg_movements(true, true, Enum.Instruction.ROTATE_LEFT);
				break;
		}
		msg_movements(true, true, Enum.Instruction.FORWARD);
	}

	protected void time_option() {
		SwitchCompat s = findViewById(R.id.time_swt_isfastest);
		if (s.isChecked()) {
			s.setText(R.string.time_fastest);
			time_tv = findViewById(R.id.time_txt_fastest);
		} else {
			s.setText(R.string.time_explore);
			time_tv = findViewById(R.id.time_txt_explore);
		}
	}

	protected void time_reset() {
		time_start = 0L;
		time_set(0, 0, 0);
	}

	protected void time_stopwatch(View v) {
		Button b = (Button) v;
		if (view_string(b).equalsIgnoreCase(r_string(R.string.time_start))) {

			time_reset();
			if (((SwitchCompat) findViewById(R.id.time_swt_isfastest)).isChecked()) {
				msg_writemsg("al_startf", "");
			} else {
				msg_writemsg("al_starte", "");
			}

			time_start = SystemClock.uptimeMillis();
			time_handler.postDelayed(time_stopwatch, 0);

			findViewById(R.id.time_swt_isfastest).setEnabled(false);
			b.setText(R.string.time_stop);

		} else {

			time_handler.removeCallbacks(time_stopwatch);

			findViewById(R.id.time_swt_isfastest).setEnabled(true);
			b.setText(R.string.time_start);

		}
	}

	protected void time_set(int min, int sec, int millisec) {
		time_tv.setText(String.format("%d:%s:%s", min, String.format("%02d", sec), String.format("%03d", millisec)));
	}

	public Runnable time_stopwatch = new Runnable() {
		public void run() {
			long time_count_ms = SystemClock.uptimeMillis() - time_start;
			int time_s = (int) (time_count_ms / 1000);

			time_set(time_s / 60, time_s % 60, (int) (time_count_ms % 1000));
			time_handler.postDelayed(this, 0);
		}
	};

	private View.OnClickListener tv_onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (point_isset) {
				SwitchCompat s = findViewById(R.id.point_swt_isway);
				point_set2(s.isChecked(), v.getId(), true);
				point_update(s.isChecked());
			}
		}
	};

	protected void point_option() {
		SwitchCompat s = findViewById(R.id.point_swt_isway);
		if (s.isChecked()) {
			s.setText(R.string.point_way);
		} else {
			s.setText(R.string.point_origin);
		}
	}

	protected void point_toset() {
		Enum.Instruction instruction = ((SwitchCompat) findViewById(R.id.point_swt_isway)).isChecked() ? Enum.Instruction.WAY : Enum.Instruction.ORIGIN;
		if (point_isset && instruction == Enum.Instruction.WAY && point_way == -1) {
			new_message("Please select a cell");
		} else {
			if (point_isset && bt_device != null) {
				String col, row;
				if (instruction == Enum.Instruction.WAY) {
					col = view_string(findViewById(R.id.point_txt_way_x));
					row = view_string(findViewById(R.id.point_txt_way_y));
				} else {
					col = view_string(findViewById(R.id.point_txt_origin_x));
					row = view_string(findViewById(R.id.point_txt_origin_y));
				}
				msg_writemsg(String.format("%s{%s,%s}", instruction.getText(), col, row), instruction.getDescription());
			}
			point_isset = !point_isset;
			findViewById(R.id.point_swt_isway).setEnabled(!point_isset);
			((TextView) findViewById(R.id.point_btn_set)).setText(r_string(point_isset ? R.string.point_isset_true : R.string.point_isset_false));
		}
	}

	//MANUAL KEY-IN
	protected String point_set1(boolean isway) {

		String point_x = view_string(findViewById(R.id.point_txt_x)),
			point_y = view_string(findViewById(R.id.point_txt_y));
		if (point_x.equalsIgnoreCase(r_string(R.string._null)) || point_y.equalsIgnoreCase(r_string(R.string._null))) {
			return new_message("Please fill in the text fields");
		} else {
			return point_set2(isway, cell_id(Integer.valueOf(point_x), Integer.valueOf(point_y)), true);
		}
	}

	protected String point_set2(boolean isway, int cell, boolean iswrite) {
		if (isway) {
			if (obst_list.containsKey(cell)) {
				return new_message("Way point cannot be on an obstacle");
			}
			Drawable box;
			if (point_way > -1) {
				box = new_drawable(R.drawable.d_box, enum_getcolor(Integer.valueOf(view_string(grid_maze.findViewById(point_way)))));
				grid_maze.findViewById(point_way).setBackground(box);
			}
			box = new_drawable(R.drawable.d_box, Enum.Cell.WAYPOINT.getColor());
			grid_maze.findViewById(cell).setBackground(box);

			point_way = cell;
		} else {
			int new_cell = cell_robot(iswrite, cell),
				col = new_cell % MAZE_C,
				row = new_cell / MAZE_C;

			if (row < (MAZE_R / 2)) {
				return new_message("Robot cannot be placed on the second-half");
			} else if ((col > (MAZE_C - ROBOT_SIZE)) || (row > (MAZE_R - ROBOT_SIZE))) {
				return new_message("Robot cannot move to that point");
			} else if (robot_hit(new_cell)) {
				return new_message("Robot will collide with obstacle");
			} else {
				point_robot = new_cell;
				reset_cells(false);
				robot_go();

				origin_col = col;
				origin_row = row;
				robot.setRotation(0);
			}
		}
		return r_string(R.string._success);
	}

	protected void point_update(boolean isway) {
		if (isway) {
			((TextView) findViewById(R.id.point_txt_way_x)).setText((point_way == -1) ? "-" : String.valueOf(point_way % MAZE_C));
			((TextView) findViewById(R.id.point_txt_way_y)).setText((point_way == -1) ? "-" : String.valueOf(cell_fliprow(point_way / MAZE_C)));
		} else {
			int cell = cell_robot(false, cell_id(origin_col, origin_row));
			((TextView) findViewById(R.id.point_txt_origin_x)).setText(String.valueOf(cell % MAZE_C));
			((TextView) findViewById(R.id.point_txt_origin_y)).setText(String.valueOf(cell_fliprow(cell / MAZE_C)));
		}
	}

	protected void display_option() {
		SwitchCompat s = findViewById(R.id.display_swt_ismanual);
		Button b = findViewById(R.id.display_btn_update);
		if (s.isChecked()) {
			s.setText(R.string.display_manual);
			b.setEnabled(true);
			display_ismanual = true;
		} else {
			s.setText(R.string.display_auto);
			b.setEnabled(false);
			display_ismanual = false;
		}
	}

	protected void display_toupdate() {
		for (Enum.Instruction instruction : display_manuallist) {
			msg_movements(false, false, instruction);
		}
		display_manuallist.clear();
	}

	protected AlertDialog.Builder pop_config() {
		View v = inflater.inflate(R.layout.pop_config, null);
		final TextView f1_tv = v.findViewById(R.id.f1_txt_config);
		final ImageButton f1_btn = v.findViewById(R.id.f1_btn_clear);
		f1_tv.setHint(config_getpref(true));

		final TextView f2_tv = v.findViewById(R.id.f2_txt_config);
		final ImageButton f2_btn = v.findViewById(R.id.f2_btn_clear);
		f2_tv.setHint(config_getpref(false));

		f1_tv.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (view_string(f1_tv).trim().length() > 0 && f1_btn.getVisibility() == View.GONE) {
					f1_btn.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		f1_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				f1_tv.setText(r_string(R.string._null));
				f1_btn.setVisibility(View.GONE);
			}
		});

		f2_tv.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (view_string(f2_tv).trim().length() > 0 && f2_btn.getVisibility() == View.GONE) {
					f2_btn.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		f2_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				f2_tv.setText(r_string(R.string._null));
				f2_btn.setVisibility(View.GONE);
			}
		});

		v.findViewById(R.id.config_btn_save).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor config_editor = config_pref.edit();
				config_editor.putString(r_string(R.string.config_f1), config_pullpref(f1_tv, true));
				config_editor.putString(r_string(R.string.config_f2), config_pullpref(f2_tv, false));
				if (config_editor.commit()) {
					new_message("Configurations updated successfully");
					dialog_config.dismiss();
				}
			}
		});
		return new AlertDialog.Builder(this).setView(v);
	}

	protected String config_pullpref(TextView tv, boolean isf1) {
		String s = view_string(tv).trim();
		return (s.length() == 0) ? config_getpref(isf1) : s;
	}

	protected String config_getpref(boolean isf1) {
		return isf1 ?
			config_pref.getString(r_string(R.string.config_f1), r_string(R.string.config_f1_default)) :
			config_pref.getString(r_string(R.string.config_f2), r_string(R.string.config_f2_default));
	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		public void onClick(View v) {

			switch (v.getId()) {
				//BLUETOOTH
				case R.id.bt_swt_isrobust:
					bt_robust_option();
					break;

				//ACCELEROMETER
				case R.id.tilt_swt_ison:
					tilt_option();
					break;

				//MAZE
				case R.id.direction_btn_up:
					msg_movements(true, true, Enum.Instruction.FORWARD);
					break;
				case R.id.direction_btn_down:
					msg_movements(true, true, Enum.Instruction.REVERSE);
					break;
				case R.id.direction_btn_left:
					msg_movements(true, true, Enum.Instruction.ROTATE_LEFT);
					break;
				case R.id.direction_btn_right:
					msg_movements(true, true, Enum.Instruction.ROTATE_RIGHT);
					break;

				//STOPWATCH
				case R.id.time_swt_isfastest:
					time_option();
					break;
				case R.id.time_btn_stopwatch:
					time_stopwatch(v);
					break;

				//SET POINTS
				case R.id.point_swt_isway:
					point_option();
					break;
				case R.id.point_btn_set:
					point_toset();
					break;
				//MANUAL KEY-IN
//				case R.id.point_btn_origin:
//					point_set1(false);
//					break;
//				case R.id.point_btn_way:
//					point_set1(true);
//					break;

				//DISPLAY GRAPHICS
				case R.id.display_swt_ismanual:
					display_option();
					break;
				case R.id.display_btn_update:
					display_toupdate();
					break;

				//CONFIGURATIONS
				case R.id.config_btn_f1:
					if (msg_showchat) {
						msg_writemsg(config_getpref(true), r_string(R.string.config_f1));
					} else {
						new_message(config_getpref(true));
					}
					break;
				case R.id.config_btn_f2:
					if (msg_showchat) {
						msg_writemsg(config_getpref(false), r_string(R.string.config_f2));
					} else {
						new_message(config_getpref(false));
					}
					break;
				case R.id.config_btn_reconfig:
					dialog_config = pop_config().show();
					break;
			}
		}
	};

	protected void msg_movements(boolean towrite, boolean tolist, Enum.Instruction instruction) {
		if (instruction != null) {
			boolean success = true;
			if (display_ismanual && tolist) {
				display_manuallist.add(instruction);
			} else {
				switch (instruction) {
					case FORWARD:
						success = robot_move(enum_getdirection((((int) robot.getRotation()) % 360) / 90));
						break;
					case REVERSE:
						success = robot_move(enum_getdirection((((int) (robot.getRotation() + 180)) % 360) / 90));
						break;
					case ROTATE_LEFT:
						robot_rotate(Enum.Direction.LEFT.get());
						break;
					case ROTATE_RIGHT:
						robot_rotate(Enum.Direction.RIGHT.get());
						break;
					case STOP:
					case CALIBRATING:
					default:
						break;
				}
			}

			if (success) {
				((TextView) findViewById(R.id.txt_status)).setText(instruction.getStatus());
				if (bt_device != null && towrite) {
					msg_writemsg(instruction.getText(), instruction.getDescription());
				}
			}
		}
	}
}
