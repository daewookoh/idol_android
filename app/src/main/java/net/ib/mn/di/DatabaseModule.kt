package net.ib.mn.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.ib.mn.data.local.IdolDatabase
import net.ib.mn.data.local.dao.ChartRankingDao
import net.ib.mn.data.local.dao.IdolDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migration from version 5 to 6: Add chart_rankings table
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create chart_rankings table (old version)
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chart_rankings (
                    chartCode TEXT NOT NULL,
                    idolId INTEGER NOT NULL,
                    rank INTEGER NOT NULL,
                    heartCount INTEGER NOT NULL,
                    maxHeartCount INTEGER NOT NULL,
                    minHeartCount INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(chartCode, idolId)
                )
                """.trimIndent()
            )

            // Create index on chartCode for faster queries
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_chart_rankings_chartCode ON chart_rankings(chartCode)"
            )
        }
    }

    /**
     * Migration from version 6 to 7: Expand chart_rankings table with all RankingItemData fields
     *
     * Drop and recreate table to add new columns
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop existing table
            database.execSQL("DROP TABLE IF EXISTS chart_rankings")

            // Create new table with all fields
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chart_rankings (
                    chartCode TEXT NOT NULL,
                    idolId INTEGER NOT NULL,
                    rank INTEGER NOT NULL,
                    heartCount INTEGER NOT NULL,
                    maxHeartCount INTEGER NOT NULL,
                    minHeartCount INTEGER NOT NULL,
                    voteCount TEXT NOT NULL,
                    name TEXT NOT NULL,
                    photoUrl TEXT,
                    miracleCount INTEGER NOT NULL DEFAULT 0,
                    fairyCount INTEGER NOT NULL DEFAULT 0,
                    angelCount INTEGER NOT NULL DEFAULT 0,
                    rookieCount INTEGER NOT NULL DEFAULT 0,
                    superRookieCount INTEGER NOT NULL DEFAULT 0,
                    anniversary TEXT,
                    anniversaryDays INTEGER NOT NULL DEFAULT 0,
                    top3Image1 TEXT,
                    top3Image2 TEXT,
                    top3Image3 TEXT,
                    top3Video1 TEXT,
                    top3Video2 TEXT,
                    top3Video3 TEXT,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(chartCode, idolId)
                )
                """.trimIndent()
            )

            // Create index on chartCode
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_chart_rankings_chartCode ON chart_rankings(chartCode)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideIdolDatabase(
        @ApplicationContext context: Context
    ): IdolDatabase {
        return Room.databaseBuilder(
            context,
            IdolDatabase::class.java,
            IdolDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration() // For development, recreate DB on schema changes
            .build()
    }

    @Provides
    @Singleton
    fun provideIdolDao(database: IdolDatabase): IdolDao {
        return database.idolDao()
    }

    @Provides
    @Singleton
    fun provideChartRankingDao(database: IdolDatabase): ChartRankingDao {
        return database.chartRankingDao()
    }
}
