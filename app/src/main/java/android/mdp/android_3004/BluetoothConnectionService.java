package android.mdp.android_3004;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnectionService {

	private final UUID THIS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final BluetoothAdapter bt_adapter;
	private final Handler bt_handler;
	private Context context;

	private AcceptThread btt_accept;
	private ConnectThread btt_connect;
	private ConnectedThread btt_connected;

	private Enum.State state_cur;

	BluetoothConnectionService(Context context, Handler handler) {
		this.context = context;
		this.bt_handler = handler;
		this.bt_adapter = BluetoothAdapter.getDefaultAdapter();

		state_cur = Enum.State.NONE;
	}

	private synchronized void updateTitle() {
		bt_handler.obtainMessage(Enum.Handling.STATE_CHANGE.get(), state_cur.get(), -1).sendToTarget();
	}

	public synchronized void start() {
		if (btt_connect != null) {
			btt_connect.cancel();
			btt_connect = null;
		}
		if (btt_connected != null) {
			btt_connected.cancel();
			btt_connected = null;
		}
		if (btt_accept == null) {
			btt_accept = new AcceptThread();
			btt_accept.start();
		}

		updateTitle();
	}

	public synchronized void connect(BluetoothDevice device) {
		if (state_cur == Enum.State.CONNECTING && btt_connect != null) {
			btt_connect.cancel();
			btt_connect = null;
		}
		if (btt_connected != null) {
			btt_connected.cancel();
			btt_connected = null;
		}

		btt_connect = new ConnectThread(device);
		btt_connect.start();
		updateTitle();
	}

	private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (btt_connect != null) {
			btt_connect.cancel();
			btt_connect = null;
		}
		if (btt_connected != null) {
			btt_connected.cancel();
			btt_connected = null;
		}
		if (btt_accept == null) {
			btt_accept.cancel();
			btt_accept.start();
		}

		btt_connected = new ConnectedThread(socket);
		btt_connected.start();

		Message msg = bt_handler.obtainMessage(Enum.Handling.BT_NAME.get());
		Bundle bundle = new Bundle();
		bundle.putString(Enum.Handling.BT_NAME.getDesc(), device.getName());
		msg.setData(bundle);
		bt_handler.sendMessage(msg);

		updateTitle();
	}

	public synchronized void stop() {
		if (btt_connect != null) {
			btt_connect.cancel();
			btt_connect = null;
		}
		if (btt_connected != null) {
			btt_connected.cancel();
			btt_connected = null;
		}
		if (btt_accept == null) {
			btt_accept.cancel();
			btt_accept = null;
		}

		state_cur = Enum.State.NONE;
		updateTitle();
	}

	public void write(byte[] message_out) {
		ConnectedThread r;
		synchronized (this) {
			if (state_cur != Enum.State.CONNECTED) return;
			r = btt_connected;
		}
		r.write(message_out);
	}

	private void failed(boolean hasfailed) {
		Message msg = bt_handler.obtainMessage(Enum.Handling.TOAST.get());
		Bundle bundle = new Bundle();
		bundle.putString(Enum.Handling.TOAST.getDesc(), hasfailed ? "Unable to connect device" : "Device connection was lost");
		msg.setData(bundle);
		bt_handler.sendMessage(msg);

		state_cur = Enum.State.NONE;

		updateTitle();
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
			state_cur = Enum.State.LISTEN;
		}

		public void run() {
			BluetoothSocket socket = null;

			while (state_cur != Enum.State.CONNECTED) {
				try {
					socket = server_socket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}

				if (socket != null) {
					synchronized (this) {
						switch (state_cur) {
							case LISTEN:
							case CONNECTING:
								connected(socket, socket.getRemoteDevice());
								break;
							case NONE:
							case CONNECTED:
								try {
									socket.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
								break;
						}
					}
				}
			}
		}

		private void cancel() {
			try {
				server_socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket socket;
		private final BluetoothDevice device;

		ConnectThread(BluetoothDevice bd) {
			device = bd;
			BluetoothSocket tmp = null;

			try {
				tmp = device.createRfcommSocketToServiceRecord(THIS_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = tmp;
			state_cur = Enum.State.CONNECTING;
		}

		public void run() {
			bt_adapter.cancelDiscovery();

			try {
				socket.connect();
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				failed(true);
				return;
			}

			synchronized (this) {
				btt_connect = null;
			}

			connected(socket, device);
		}

		private void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public class ConnectedThread extends Thread {
		private final BluetoothSocket socket;
		private final InputStream stream_in;
		private final OutputStream stream_out;

		ConnectedThread(BluetoothSocket bs) {
			socket = bs;
			InputStream tmp_in = null;
			OutputStream tmp_out = null;

			try {
				tmp_in = socket.getInputStream();
				tmp_out = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stream_in = tmp_in;
			stream_out = tmp_out;

			state_cur = Enum.State.CONNECTED;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;

			while (state_cur == Enum.State.CONNECTED) {
				try {
					bytes = stream_in.read(buffer);
					bt_handler.obtainMessage(Enum.Handling.READ_MSG.get(), bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					e.printStackTrace();
					failed(false);
					break;
				}
			}
		}

		void write(byte[] bytes) {
			try {
				stream_out.write(bytes);
				bt_handler.obtainMessage(Enum.Handling.WRITE_MSG.get(), -1, -1, bytes).sendToTarget();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
