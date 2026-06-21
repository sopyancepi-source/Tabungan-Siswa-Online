package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.TabunganDao
import com.example.data.model.*

@Database(
    entities = [
        Siswa::class,
        Transaksi::class,
        PinjamanSekolah::class,
        PinjamanGuru::class,
        AdminKas::class,
        SetorKoperasi::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TabunganDatabase : RoomDatabase() {
    abstract fun dao(): TabunganDao

    companion object {
        @Volatile
        private var INSTANCE: TabunganDatabase? = null

        fun getDatabase(context: Context): TabunganDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TabunganDatabase::class.java,
                    "tabungan_siswa_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
