package android.mdp.android_3004;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnectionService {
	private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final BluetoothAdapter bt_adapter;
	Context context;

	private AcceptThread btt_accept;

	private ConnectThread btt_connect;
	private BluetoothDevice bt_device;
	private UUID bt_device_UUID;

	private ConnectedThread btt_connected;

	ProgressDialog progress_dialog;

	public BluetoothConnectionService(Context context) {
		this.context = context;
		this.bt_adapter = BluetoothAdapter.getDefaultAdapter();
		start();
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket server_socket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			try {
				tmp = bt_adapter.listenUsingInsecureRfcommWithServiceRecord(context.getString(R.string.app_name), BT_UUID);
			} catch (IOException e) {
			}
			server_socket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;
			try {
				socket = server_socket.accept();
			} catch (IOException e) {
			}

			if (socket != null) {
				connected(socket, bt_device);
			}
		}

		public void cancel() {
			try {
				server_socket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectThread extends Thread {
		BluetoothSocket socket = null;

		public ConnectThread(BluetoothDevice bd, UUID uuid) {
			bt_device = bd;
			bt_device_UUID = uuid;
		}

		public void run() {
			BluetoothSocket tmp = null;
			try {
				tmp = bt_device.createRfcommSocketToServiceRecord(bt_device_UUID);
			} catch (IOException e) {
			}
			socket = tmp;

			bt_adapter.cancelDiscovery();
			try {
				socket.connect();
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e1) {
				}
			}
			connected(socket, bt_device);
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	public synchronized void start() {
		if (btt_connect != null) {
			btt_connect.cancel();
			btt_connect = null;
		}
		if (btt_accept == null) {
			btt_accept = new AcceptThread();
			btt_accept.start();
		}
	}

	public void start_client(BluetoothDevice device, UUID device_UUID) {
		progress_dialog = ProgressDialog.show(context, "Connecting Bluetooth", "Please wait...", true);

		btt_connect = new ConnectThread(device, device_UUID);
		btt_connect.start();
	}

	public class ConnectedThread extends Thread {
		private final BluetoothSocket socket;
		private final InputStream stream_in;
		private final OutputStream stream_out;

		public ConnectedThread(BluetoothSocket bs) {
			socket = bs;
			InputStream tmp_in = null;
			OutputStream tmp_out = null;

			progress_dialog.dismiss();

			try {
				tmp_in = socket.getInputStream();
				tmp_out = socket.getOutputStream();
			} catch (IOException e) {
			}

			stream_in = tmp_in;
			stream_out = tmp_out;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;

			while (true) {
				try {
					bytes = stream_in.read(buffer);
					String message = new String(buffer, 0, bytes);
				} catch (IOException e) {
					break;
				}
			}
		}

		public void write(byte[] bytes) {
			try {
				stream_out.write(bytes);
			} catch (IOException e) {
			}
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	private void connected(BluetoothSocket socket, BluetoothDevice device) {
		btt_connected = new ConnectedThread(socket);
		btt_connected.start();
	}

	public void write(byte[] message_out) {
		btt_connected.write(message_out);
	}
}
