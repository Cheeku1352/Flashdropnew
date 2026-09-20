package com.flashdrop.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transfer_history")
data class TransferHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val connectionDurationMillis: Long,
    val totalFilesCount: Int,
    val totalBytesTransferred: Long,
    val peerDeviceName: String,
    val direction: String = "Transfer"
)

@Dao
interface TransferHistoryDao {
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransferHistory>>

    @Insert
    suspend fun insert(item: TransferHistory)
}

@Database(entities = [TransferHistory::class], version = 2, exportSchema = false)
abstract class FlashDropDatabase : RoomDatabase() {
    abstract fun transferHistoryDao(): TransferHistoryDao

    companion object {
        @Volatile private var instance: FlashDropDatabase? = null

        fun get(context: Context): FlashDropDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlashDropDatabase::class.java,
                    "flashdrop.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transfer_history ADD COLUMN direction TEXT NOT NULL DEFAULT 'Transfer'")
            }
        }
    }
}

class TransferHistoryRepository(context: Context) {
    private val dao = FlashDropDatabase.get(context).transferHistoryDao()
    val history: Flow<List<TransferHistory>> = dao.observeAll()

    suspend fun record(
        progress: FileTransferProgress,
        durationMillis: Long,
        peerName: String,
        direction: String
    ) {
        dao.insert(
            TransferHistory(
                timestamp = System.currentTimeMillis(),
                connectionDurationMillis = durationMillis,
                totalFilesCount = progress.filesSelected,
                totalBytesTransferred = progress.bytesTransferred,
                peerDeviceName = peerName,
                direction = direction
            )
        )
    }
}
