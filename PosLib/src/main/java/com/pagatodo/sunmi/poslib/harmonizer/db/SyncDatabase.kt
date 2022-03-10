package com.pagatodo.sunmi.poslib.harmonizer.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Sync::class], version = Config.VERSION_DATA_BASE)
@TypeConverters(DateTypeConverter::class)
abstract class SyncDatabase : RoomDatabase() {

    abstract fun databaseDao(): SyncDao

    companion object {

        @Volatile
        private var INSTANCE: SyncDatabase? = null

        fun getDatabase(context: Context): SyncDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, SyncDatabase::class.java, "sync.db")
                    //.addMigrations(Config.MIGRATION_10)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}