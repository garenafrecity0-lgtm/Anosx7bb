package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface BoostLogDao {
    @Query("SELECT * FROM boost_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<BoostLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BoostLogEntity): Long

    @Query("SELECT SUM(ramFreedMb) FROM boost_logs")
    fun getTotalRamFreedMb(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM boost_logs")
    fun getTotalBoostCount(): Flow<Int>

    @Query("DELETE FROM boost_logs")
    suspend fun clearAllLogs()
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastBoosted DESC, boostCount DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGame(game: GameEntity)

    @Query("UPDATE games SET lastBoosted = :timestamp, boostCount = boostCount + 1 WHERE packageName = :packageName")
    suspend fun recordGameBoost(packageName: String, timestamp: Long)

    @Query("DELETE FROM games WHERE packageName = :packageName")
    suspend fun deleteGame(packageName: String)
}

@Dao
interface LicenseDao {
    @Query("SELECT * FROM licenses ORDER BY createdAt DESC")
    fun getAllLicenses(): Flow<List<LicenseKeyEntity>>

    @Query("SELECT * FROM licenses ORDER BY createdAt DESC")
    suspend fun getAllLicensesList(): List<LicenseKeyEntity>

    @Query("SELECT * FROM licenses WHERE UPPER(keyString) = UPPER(:keyString) LIMIT 1")
    suspend fun getLicense(keyString: String): LicenseKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicense(license: LicenseKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicenses(licenses: List<LicenseKeyEntity>)

    @Query("DELETE FROM licenses WHERE UPPER(keyString) = UPPER(:keyString)")
    suspend fun deleteLicense(keyString: String)

    @Query("UPDATE licenses SET isActive = :isActive WHERE UPPER(keyString) = UPPER(:keyString)")
    suspend fun updateStatus(keyString: String, isActive: Boolean)
}

@Dao
interface ActiveSessionDao {
    @Query("SELECT * FROM active_sessions ORDER BY lastPingAt DESC")
    fun getAllSessions(): Flow<List<ActiveSessionEntity>>

    @Query("SELECT * FROM active_sessions WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getSession(deviceId: String): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: ActiveSessionEntity)

    @Query("UPDATE active_sessions SET lastPingAt = :timestamp, isOnline = :isOnline, currentActivity = :activity WHERE deviceId = :deviceId")
    suspend fun updatePing(deviceId: String, timestamp: Long, isOnline: Boolean, activity: String)

    @Query("DELETE FROM active_sessions WHERE deviceId = :deviceId")
    suspend fun deleteSession(deviceId: String)

    @Query("DELETE FROM active_sessions")
    suspend fun clearAllSessions()
}

@Dao
interface BroadcastDao {
    @Query("SELECT * FROM broadcast_messages ORDER BY timestamp DESC")
    fun getAllBroadcasts(): Flow<List<BroadcastMessageEntity>>

    @Query("SELECT * FROM broadcast_messages ORDER BY timestamp DESC")
    suspend fun getAllBroadcastsList(): List<BroadcastMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcast(message: BroadcastMessageEntity): Long

    @Query("UPDATE broadcast_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE broadcast_messages SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM broadcast_messages WHERE id = :id")
    suspend fun deleteBroadcast(id: Long)

    @Query("DELETE FROM broadcast_messages")
    suspend fun clearAllBroadcasts()
}

@Dao
interface ProtectedAppDao {
    @Query("SELECT * FROM protected_app WHERE id = 1 LIMIT 1")
    fun getProtectedApp(): Flow<ProtectedAppEntity?>

    @Query("SELECT * FROM protected_app WHERE id = 1 LIMIT 1")
    suspend fun getProtectedAppDirect(): ProtectedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProtectedApp(app: ProtectedAppEntity)

    @Query("UPDATE protected_app SET launchCount = launchCount + 1, lastUpdated = :timestamp WHERE id = 1")
    suspend fun recordLaunch(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE protected_app SET isLocked = :isLocked WHERE id = 1")
    suspend fun setLocked(isLocked: Boolean)
}

@Database(
    entities = [
        BoostLogEntity::class,
        GameEntity::class,
        LicenseKeyEntity::class,
        ActiveSessionEntity::class,
        BroadcastMessageEntity::class,
        ProtectedAppEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boostLogDao(): BoostLogDao
    abstract fun gameDao(): GameDao
    abstract fun licenseDao(): LicenseDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun broadcastDao(): BroadcastDao
    abstract fun protectedAppDao(): ProtectedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anos_x7_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
