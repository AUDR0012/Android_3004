package android.mdp.android_3004;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SecondaryActivity extends AppCompatActivity {

	ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_secondary);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//		========== CLICKABLE CONTROLS ==========
		int[] list_onclick = {R.id.bluetooth_swt_ison, R.id.bluetooth_swt_isdiscoverable, R.id.bluetooth_btn_find,
			R.id.data_btn_clear, R.id.data_btn_send};
		for (int onclick : list_onclick) {
			this.findViewById(onclick).setOnClickListener(onClickListener);
		}

		bluetooth_isoff();
		bluetooth_isundiscoverable();


		String[] textlist = {"abc", "def12345678901234567890123456789012345678901234567890123456789", "ghi", "jkl12345678901234567890123456789012345678901234567890", "mno", "pqr12345678901234567890123456789012345678901234567890", "stu", "vwx12345678901234567890123456789012345678901234567890", "yz"};
//		randomtext1(textlist);
//		randomtext2(textlist);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu_atbluetooth, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_maze:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
				//BLUETOOTH
				case R.id.bluetooth_swt_ison:
					bluetooth_isoff();
					break;
				case R.id.bluetooth_swt_isdiscoverable:
					bluetooth_isundiscoverable();
					break;
				case R.id.bluetooth_btn_find:
					break;
				case R.id.data_btn_send:
					break;
			}
		}
	};

	protected void bluetooth_isoff() {
		SwitchCompat s = findViewById(R.id.bluetooth_swt_ison);
		if (s.isChecked()) {
			s.setText("On");
		} else {
			s.setText("Off");
		}
	}

	protected void bluetooth_isundiscoverable() {
		SwitchCompat s = findViewById(R.id.bluetooth_swt_isdiscoverable);
		if (s.isChecked()) {
			s.setText("Discoverable");
		} else {
			s.setText("Not Discoverable");
		}
	}

	protected void randomtext1(String[] textlist) {
		int[] lv = {R.id.bluetooth_lv_devices};

		for (int i = 0; i < lv.length; i++) {
			adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, textlist) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					View view = super.getView(position, convertView, parent);

					TextView tv = (TextView) view;
					tv.setTextColor(getResources().getColor(R.color.midnight_blue));
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

					return view;
				}
			};
			((ListView) findViewById(lv[i])).setAdapter(adapter);
		}
	}

	protected void randomtext2(String[] textlist) {
		int[] lv = {R.id.receive_lv_display, R.id.send_lv_display};
		String text = "";
		for (int j = 0; j < textlist.length; j++) {
			if (j != 0) text += "\n\n";
			text += textlist[j];
		}

		for (int i = 0; i < lv.length; i++) {
			TextView tv = (TextView) findViewById(lv[i]);
			tv.setText(text);
		}
	}
}
