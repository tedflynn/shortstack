package com.vivificavit.shortstack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vivificavit.shortstack.data.model.CategoryEntity
import com.vivificavit.shortstack.data.model.Goal
import com.vivificavit.shortstack.data.model.GoalMove
import com.vivificavit.shortstack.data.model.KidProfile
import com.vivificavit.shortstack.data.model.RecurringEntry
import com.vivificavit.shortstack.data.model.Transaction

/**
 * Schema changes MUST bump [version] and add a Migration below; the exported
 * schemas in app/schemas/ record each version. Never fall back to a
 * destructive migration for versions that real users may have installed.
 */
@Database(
    entities = [
        Transaction::class, KidProfile::class, CategoryEntity::class, Goal::class, GoalMove::class,
        RecurringEntry::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun goalDao(): GoalDao

    abstract fun recurringDao(): RecurringDao

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

        /** v4 -> v5: categories can store an icon the user picked. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `icon` TEXT")
            }
        }

        /** v5 -> v6: savings goals, and the money set aside for them. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kidName` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetCents` INTEGER NOT NULL,
                        `icon` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `targetDay` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_kidName` ON `goals` (`kidName`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal_moves` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kidName` TEXT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `transactionId` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_moves_kidName` ON `goal_moves` (`kidName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_moves_goalId` ON `goal_moves` (`goalId`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_goal_moves_transactionId` ON `goal_moves` (`transactionId`)"
                )
            }
        }

        /** v6 -> v7: repeating entries, and a link from each entry to the one that added it. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurringId` INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kidName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `startTimestamp` INTEGER NOT NULL,
                        `occurrences` INTEGER NOT NULL,
                        `goalId` INTEGER,
                        `goalCents` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_entries_kidName` ON `recurring_entries` (`kidName`)"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

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
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
                .build()
        }
    }
}
