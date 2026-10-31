package com.example.allowancetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.allowancetracker.data.model.CategoryEntity
import com.example.allowancetracker.data.model.KidProfile
import com.example.allowancetracker.data.model.Transaction

/**
 * Schema changes MUST bump [version] and add a Migration below; the exported
 * schemas in app/schemas/ record each version. Never fall back to a
 * destructive migration for versions that real users may have installed.
 */
@Database(
    entities = [Transaction::class, KidProfile::class, CategoryEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "allowance_tracker_db"

        /** v3 -> v4: store amounts as whole cents (INTEGER) instead of dollars (REAL). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kidName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transactions_new`
                        (`id`, `kidName`, `title`, `amountCents`, `type`, `category`, `timestamp`)
                    SELECT `id`, `kidName`, `title`, CAST(ROUND(`amount` * 100) AS INTEGER),
                        `type`, `category`, `timestamp`
                    FROM `transactions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_3_4)

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                // Versions 1-2 only existed during early development, before
                // schemas were exported, so they cannot be migrated.
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
        }
    }
}
