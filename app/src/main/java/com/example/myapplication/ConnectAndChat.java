package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class ConnectAndChat extends AppCompatActivity {
    private String deviceName, macAddress;
    private TextView nameOfTheDevice;
    private BluetoothAdapter bluetoothAdapter;
    Context context;

    private static final UUID UUID_CODE = UUID.fromString("41b1a5e3-320b-40ba-8bcf-7b1c1d7fe8ce");
    private static final String NAME = "BTChatApp";

    private AcceptThread acceptThread;
    private ConnectThread connectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_and_chat);
        context = this;
        init();
    }

    private void init() {
        deviceName = getIntent().getStringExtra("userName");
        macAddress = getIntent().getStringExtra("MACAddress");
        nameOfTheDevice = findViewById(R.id.nameOfDeviceInChat);
        nameOfTheDevice.setText(deviceName);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        connectThread = new ConnectThread(device);
        connectThread.start();

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {

            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    System.out.print("ERROR 58 LINE ******************");
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID_CODE);
            } catch (IOException e) {
                System.out.print("Socket's listen() method failed: " + e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    System.out.print("Socket's accept() method failed" + e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket);
                    System.out.println("CONNECTED *****************************");
                    Toast.makeText(context, "device connected", Toast.LENGTH_SHORT).show();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        System.out.print("Socket's accept() method failed" + e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                System.out.print("Could not close the connect socket: " + e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    System.out.println("********** LINE 120 *************");
                }
                tmp = device.createRfcommSocketToServiceRecord(UUID_CODE);
            } catch (IOException e) {
                System.out.print("Socket's create() method failed" + e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    System.out.print("Could not close the client socket" + closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                System.out.print("Could not close the client socket" + e);
            }
        }
    }

}