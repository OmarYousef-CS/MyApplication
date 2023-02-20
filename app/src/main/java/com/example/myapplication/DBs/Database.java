package com.example.myapplication.DBs;

import androidx.room.RoomDatabase;

import com.example.myapplication.DBs.Messages;
import com.example.myapplication.DBs.MessagesHistoryDao;

@androidx.room.Database(entities = {Messages.class}, version = 1)
public abstract class Database extends RoomDatabase {
    public abstract MessagesHistoryDao deviceHistoryDao();
}
