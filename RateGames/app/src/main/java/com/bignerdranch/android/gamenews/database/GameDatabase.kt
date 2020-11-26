package com.bignerdranch.android.gamenews.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bignerdranch.android.gamenews.Game

@Database(entities = [ Game::class ], version=2)
@TypeConverters(GameTypeConverters::class)
abstract class GameDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
}

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE Game ADD COLUMN suspect TEXT NOT NULL DEFAULT ''"
        )
    }
}