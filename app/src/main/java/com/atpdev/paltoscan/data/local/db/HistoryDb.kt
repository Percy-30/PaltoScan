package com.atpdev.paltoscan.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.atpdev.paltoscan.data.local.dao.HistoryDao
import com.atpdev.paltoscan.data.local.entity.History

// Definimos la migración de la versión 1 a la 2
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Aquí agregamos la nueva columna a la tabla 'History'
            // Cambiar el esquema de la tabla, eliminando la columna `section`
            // Actualiza todos los registros donde section = "main"
            database.execSQL("UPDATE History SET section = 'paltoscan' WHERE section = 'main'")
            // database.execSQL("CREATE TABLE IF NOT EXISTS history_new (id INTEGER PRIMARY KEY AUTOINCREMENT, diseaseName TEXT, description TEXT, prevention TEXT, treatment TEXT, imagePath TEXT)")
            // database.execSQL("INSERT INTO history_new (id, diseaseName, description, prevention, treatment, imagePath) SELECT id, diseaseName, description, prevention, treatment, imagePath FROM history")
            // database.execSQL("DROP TABLE History_database")
            // database.execSQL("ALTER TABLE history_new RENAME TO history")
        }
    }

// Migración de la versión 3 a la 4 (Geolocalización)
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE History ADD COLUMN latitude REAL DEFAULT NULL")
        database.execSQL("ALTER TABLE History ADD COLUMN longitude REAL DEFAULT NULL")
    }
}

@Database(
    entities = [History::class],
    version = 4,
    exportSchema = false, // Allows us to modify the database schema without needing to recreate the database each time.
)
abstract class HistoryDb : RoomDatabase() {
    abstract fun HistoryDao(): HistoryDao
}
