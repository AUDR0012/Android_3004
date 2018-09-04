package android.mdp.android_3004;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class BluetoothActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) { //TODO: UNKNOWN - MERGE WITH WEEKIAT PART
		//String title = item.getTitle().toString();
		//((TextView) findViewById(R.id.txt_status)).setText(title + " selected");
		switch (item.getItemId()) {
			case R.id.menu_bluetooth:
				return true;
			case R.id.menu_maze:
				startActivity(new Intent(BluetoothActivity.this, MainActivity.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
