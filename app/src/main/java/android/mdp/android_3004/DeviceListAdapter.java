package android.mdp.android_3004;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

	private LayoutInflater inflater;
	private ArrayList<BluetoothDevice> device_list;
	private int resource_id;

	public DeviceListAdapter(Context context, int resource_id, ArrayList<BluetoothDevice> device_list) {
		super(context, resource_id, device_list);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = inflater.inflate(resource_id, null);

		BluetoothDevice device = device_list.get(position);

		if (device != null) {
			TextView name = convertView.findViewById(R.id.device_txt_name);
			TextView address = convertView.findViewById(R.id.device_txt_address);

			if (name != null) name.setText(device.getName());
			if (address != null) address.setText(device.getAddress());
		}
		return convertView;
	}
}
