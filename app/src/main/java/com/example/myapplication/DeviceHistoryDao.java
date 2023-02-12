package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DeviceHistoryDao {
    @Query("SELECT * FROM devicehistory")
    List<DeviceHistory> getAll();

    @Query("SELECT * FROM devicehistory WHERE mac_address LIKE :macAddressToFind")
    DeviceHistory findByMacAddress(String macAddressToFind);

    @Insert
    void insertAll(DeviceHistory... deviceHistory);

    @Delete
    void delete(DeviceHistory deviceHistory);
}
