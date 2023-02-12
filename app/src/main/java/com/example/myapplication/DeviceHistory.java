package com.example.myapplication;

import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
//wrf
@Entity
public class DeviceHistory extends AppCompatActivity  {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "device_name")
    public String deviceName;

    @ColumnInfo(name = "mac_address")
    public String macAddress;

    @ColumnInfo(name = "chat_history")
    public ArrayAdapter<String> chat_history;

    public DeviceHistory(String deviceName, String macAddress) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.chat_history = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1);
    }
}