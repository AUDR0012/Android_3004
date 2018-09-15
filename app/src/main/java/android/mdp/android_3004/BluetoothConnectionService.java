package android.mdp.android_3004;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnectionService {
	private final UUID THIS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothAdapter bt_adapter;
	private Context context;

	private AcceptThread btt_accept;

	private ConnectThread btt_connect;
	private BluetoothDevice bt_device;
	private UUID bt_device_UUID;

	private ConnectedThread btt_connected;

	BluetoothConnectionService(Context context) {
		this.context = context;
		this.bt_adapter = BluetoothAdapter.getDefaultAdapter();
		start();
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket server_socket;

		AcceptThread() {
			BluetoothServerSocket tmp = null;

			try {
				tmp = bt_adapter.listenUsingInsecureRfcommWithServiceRecord(context.getString(R.string.app_name), THIS_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			server_socket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;
			try {
				socket = server_socket.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (socket != null) {
				connected(socket, bt_device);
			}
		}

		public void cancel() {
			try {
				server_socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ConnectThread extends Thread {
		BluetoothSocket socket = null;

		ConnectThread(BluetoothDevice bd, UUID uuid) {
			bt_device = bd;
			bt_device_UUID = uuid;
		}

		public void run() {
			BluetoothSocket tmp = null;
			try {
				tmp = bt_device.createRfcommSocketToServiceRecord(bt_device_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = tmp;

			bt_adapter.cancelDiscovery();
			try {
				socket.connect();
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
			connected(socket, bt_device);
		}

		void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
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
		btt_connect = new ConnectThread(device, device_UUID);
		btt_connect.start();
	}

	public class ConnectedThread extends Thread {
		private BluetoothSocket socket;
		private final InputStream stream_in;
		private final OutputStream stream_out;

		ConnectedThread(BluetoothSocket bs) {
			socket = bs;
			InputStream tmp_in = null;
			OutputStream tmp_out = null;

			try {
				tmp_in = socket.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				tmp_out = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
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

					Intent messaging_intent = new Intent("messaging");
					messaging_intent.putExtra("read message", message);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messaging_intent);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}

		void write(byte[] bytes) {
			try {
				stream_out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				stream_out.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
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
