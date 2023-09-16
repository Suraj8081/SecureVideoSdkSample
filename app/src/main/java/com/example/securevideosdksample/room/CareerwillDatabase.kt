package com.example.securevideosdksample.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.securevideosdksample.room.dao.DownloadDao
import com.example.securevideosdksample.room.table.DownloadVideoTable


@Database(entities = [DownloadVideoTable::class],version = 1, exportSchema = false)
abstract class CareerwillDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    companion object {
        @Volatile
        var instance: CareerwillDatabase? = null
        private const val DATABASE_NAME = "careerDb"
        fun getInstance(context: Context): CareerwillDatabase? {
            if (instance == null) {
                synchronized(CareerwillDatabase::class.java)
                {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context, CareerwillDatabase::class.java, DATABASE_NAME).build()
                    }
                }
            }

            return instance
        }
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

            }
        }

        @JvmStatic
        fun destroyInstance() {
            if (instance != null) {
                if (instance!!.isOpen) {
                    instance!!.close()
                }
            }
            instance = null
        }
    }

    }