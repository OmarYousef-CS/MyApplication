package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.DBs.Database;
import com.example.myapplication.DBs.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class ConnectAndChat extends AppCompatActivity {
    // Variables
    private String deviceName, macAddress;
    private TextView status;
    private EditText textEditForChat;
    private ListView conversation;
    private ArrayAdapter<String> conversationAdapter;
    private Button deviceListButton, sendMsgButton;
    private BluetoothAdapter bluetoothAdapter;
    private Database database;
    Context context;

    // Addresses for the BT application
    private static final UUID UUID_CODE = UUID.fromString("41b1a5e3-320b-40ba-8bcf-7b1c1d7fe8ce");
    private static final String NAME = "BTChatApp";

    // Statics for the Handler
    static final int STATE_CONNECTED = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTION_FAILED = 3;
    static final int STATE_READ_MESSAGE = 4;
    static final int STATE_WRITE_MESSAGE = 5;
    static final int STATE_LISTEN = 6;
    static final int STATE_NOT_CONNECTED = 7;
    static final int STATE_DISCONNECT = 8;

    // Bluetooth. Connect + Transfer data
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    // Handler
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case STATE_CONNECTED:
                    status.setText(deviceName);
                    try {
                        checkingDB();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting...");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_READ_MESSAGE: //Receive the message
                    try {
                        String msg = saveMsgInDB(message, deviceName);
                        conversationAdapter.add(deviceName + ": " + msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case STATE_WRITE_MESSAGE:
                    break;
                case STATE_LISTEN:
                    status.setText("Listining...");
                    break;
                case STATE_NOT_CONNECTED:
                    status.setText("not Connected");
                    break;
                case STATE_DISCONNECT:
                    status.setText("Device Disconnected");
                    connectedThread.cancel();
                    conversationAdapter.clear();
                    connectAndListen();
                    break;
            }
            return false;
        }
    });

    //===== Functions ===================================================== +
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_and_chat);
        context = this;
        initBluetooth();
        init();
    }

    private void init() {
        database = Room.databaseBuilder(getApplicationContext(),
                Database.class, "messages_database").allowMainThreadQueries().build();
        initButtons();
        conversation = (ListView) findViewById(R.id.conversation);
        status = findViewById(R.id.statusOfConnection);
        status.setText("Listining...");

        conversationAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1);
        conversation.setAdapter(conversationAdapter);

        deviceName = getIntent().getStringExtra("userName");
        macAddress = getIntent().getStringExtra("MACAddress");

        // connect as a server ot listen as a client
        connectAndListen();

    }

    private void connectAndListen () {
        if (deviceName != "" && macAddress.length() == 17) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            status.setText(deviceName);
            // try to connect to device
            connectThread = new ConnectThread(device);
            connectThread.start();
        } else {  // be available for connection
            conversationAdapter.clear();
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    private void initButtons() {
        deviceListButton = findViewById(R.id.findDeviceToConnect);
        sendMsgButton = findViewById(R.id.sendMessageButton);
        textEditForChat = findViewById(R.id.textEditForChat);

        // Click event to connect to another device
        deviceListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ConnectAndChat.this, ListOfDevices.class);
                startActivity(intent);
            }
        });

        // Click event to send message
        sendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = String.valueOf(textEditForChat.getText());
                try {
                    connectedThread.write(message.getBytes());
                    database.deviceHistoryDao().insertAll(new Messages(  // save msg to DB
                            macAddress,CipherHelper.encrypt( "Me: " + message)));
                    conversationAdapter.add("Me: " + message);
                    textEditForChat.setText("");
                } catch (Exception e) {
                    Message messageForHandler = Message.obtain();
                    messageForHandler.what = STATE_NOT_CONNECTED;
                    handler.sendMessage(messageForHandler);
                }

            }
        });
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, "Please Enable Bluetooth", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    private void checkingDB() throws Exception {
        List<Messages> chatHistory = database.deviceHistoryDao().messagesForMacAddress(macAddress);
        for (int msg = 0; msg < chatHistory.size(); msg++) {
            conversationAdapter.add(CipherHelper.decrypt(chatHistory.get(msg).getMessageText()));
        }
    }

    private String saveMsgInDB(Message message, String device) throws Exception {
        byte[] readMsg = (byte[]) message.obj;
        String tempMessage = new String(readMsg, 0, message.arg1);
        database.deviceHistoryDao().insertAll( new Messages(   //Add the encrypted message to DB
                macAddress, CipherHelper.encrypt(device + ": " + tempMessage)));
        return tempMessage;
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
                    //set the status of the handler
                    Message message = Message.obtain();
                    message.what = STATE_LISTEN;
                    handler.sendMessage(message);

                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                    System.out.print("Socket's accept() method failed" + e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket);
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    //check permition
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        System.out.println("************** LINE 207 **************");
                    }

                    //get device details
                    deviceName = socket.getRemoteDevice().getName();
                    macAddress = socket.getRemoteDevice().getAddress();

                    //open the sockets to send and receive messages
                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();

                    //set handler status on connected
                    message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

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
                }
                tmp = device.createRfcommSocketToServiceRecord(UUID_CODE);
            } catch (IOException e) {
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
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
                //start the sockets
                connectedThread = new ConnectedThread(mmSocket);
                connectedThread.start();
                //set handler status on connected
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    System.out.println("*******************************320*************************");
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

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            STATE_READ_MESSAGE, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Message message = Message.obtain();
                    message.what = STATE_DISCONNECT;
                    handler.sendMessage(message);
                    e.printStackTrace();
                    System.out.print(e.getMessage());
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        STATE_WRITE_MESSAGE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Message message = Message.obtain();
                message.what = STATE_DISCONNECT;
                handler.sendMessage(message);
                e.printStackTrace();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}