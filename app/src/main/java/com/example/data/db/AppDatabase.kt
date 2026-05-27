package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.Vehicle
import com.example.data.model.VehicleInspection
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY licensePlate ASC")
    fun getAllVehiclesFlow(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehicles(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: String): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
}

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY date DESC, startTime DESC")
    fun getAllInspectionsFlow(): Flow<List<VehicleInspection>>

    @Query("SELECT * FROM inspections")
    suspend fun getAllInspections(): List<VehicleInspection>

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    suspend fun getInspectionById(id: String): VehicleInspection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: VehicleInspection)

    @Delete
    suspend fun deleteInspection(inspection: VehicleInspection)

    @Query("SELECT * FROM inspections WHERE isSynced = 0")
    suspend fun getUnsyncedInspections(): List<VehicleInspection>

    @Query("UPDATE inspections SET isSynced = 1 WHERE id = :id")
    suspend fun markInspectionAsSynced(id: String)
}

@Database(
    entities = [Vehicle::class, VehicleInspection::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun inspectionDao(): InspectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "segovia_aseo_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
