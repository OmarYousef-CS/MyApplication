package com.example.myapplication;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Messages {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "mac_address")
    public String mac_address;

    @ColumnInfo(name = "message")
    public String message;

    public  Messages(String mac_address, String message) {
        this.mac_address = mac_address;
        this.message = message;
    }

    public String getMessageText () {
        return message;
    }
}
