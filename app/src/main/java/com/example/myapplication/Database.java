package com.example.myapplication;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Messages.class}, version = 1)
public abstract class Database extends RoomDatabase {
    public abstract MessagesHistoryDao deviceHistoryDao();
}
