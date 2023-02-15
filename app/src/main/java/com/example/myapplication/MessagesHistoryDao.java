package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessagesHistoryDao {
    @Query("SELECT * FROM messages")
    List<Messages> getAll();

    @Insert
    void insertAll(Messages... deviceHistory);

    @Delete
    void delete(Messages deviceHistory);

    @Query("SELECT * FROM messages WHERE mac_address LIKE :macAddress")
    List<Messages> messagesForMacAddress(String macAddress);
}
