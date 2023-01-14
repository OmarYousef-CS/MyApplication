package com.example.myapplication;

import static android.util.Log.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ListOfDevices extends AppCompatActivity {
    private ListView paierdDevicesList, availableDevicesList;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> adapterPairedDevices, adapterAvailableDevices;
    private Context context;
    private ProgressBar scanDevicesBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_devices);
        context = this;
        initBluetooth();
        init();

    }

    private void init() {

        // initialization the variables
        paierdDevicesList = (ListView) findViewById(R.id.paierdDevices);
        availableDevicesList = (ListView) findViewById(R.id.availableDevices);
        scanDevicesBar = findViewById(R.id.progresBar);

        // initialization the Adapters
        adapterPairedDevices = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, getPairedDevices());
        adapterAvailableDevices = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1);

        // UnParied devices
        getAvailableDevices();

        // set adapters for the ListViews
        paierdDevicesList.setAdapter(adapterPairedDevices);
        availableDevicesList.setAdapter(adapterAvailableDevices);

        // set the onItemClickListener
        paierdDevicesOnItemClick();
        availableDevicesOnItemClick();

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

    private void makeDeviceDiscoverAble() {
        int requestCode = 1;
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, requestCode);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                scanDevicesBar.setVisibility(View.GONE);
                Toast.makeText(context, "Scan Completed", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private ArrayList<String> getPairedDevices() {
        ArrayList<String> PairedDevicesList = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                PairedDevicesList.add(device.getName() + "\n" + device.getAddress());
            }
        }
        return PairedDevicesList;
    }

    private void getAvailableDevices() {
        makeDeviceDiscoverAble();
        bluetoothAdapter.startDiscovery();
        scanDevicesBar.setVisibility(View.VISIBLE);
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, intentFilter);
        IntentFilter intentFilterFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, intentFilterFinished);

    }

    private void paierdDevicesOnItemClick() {
        paierdDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                String device = adapterPairedDevices.getItem(position);
                Intent intent = new Intent(ListOfDevices.this, ConnectAndChat.class);
                // send the data for the new activity
                intent.putExtra("userName", device.substring(0,device.length()-18));
                intent.putExtra("MACAddress", device.substring(device.length() -17));
                startActivity(intent);
            }
        });
    }

    private void availableDevicesOnItemClick() {
        availableDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String device = adapterAvailableDevices.getItem(position);
                Intent intent = new Intent(ListOfDevices.this, ConnectAndChat.class);
                // send the data for the new activity
                intent.putExtra("userName", device.substring(0,device.length()-18));
                intent.putExtra("MACAddress", device.substring(device.length() -17));
                startActivity(intent);
            }
        });
    }

}
