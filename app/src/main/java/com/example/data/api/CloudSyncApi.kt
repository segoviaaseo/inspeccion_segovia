package com.example.data.api

import android.util.Log
import com.example.data.model.Vehicle
import com.example.data.model.VehicleInspection
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface InspectionSyncService {
    // Dynamic URL post: uploads a single inspection to a custom server
    @POST
    suspend fun uploadInspectionToUrl(
        @Url url: String,
        @Body inspection: VehicleInspection
    ): Response<ResponseBody>

    @POST
    suspend fun uploadVehicleToUrl(
        @Url url: String,
        @Body vehicle: Vehicle
    ): Response<ResponseBody>
}

object CloudSyncApi {
    private const val TAG = "CloudSyncApi"
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/") // Placeholder, using dynamic full URLs in @Url
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(InspectionSyncService::class.java)

    /**
     * Uploads the inspection to a remote server.
     * If [customUrl] is specified (HTTP/HTTPS), perform a real POST.
     * If empty or in demo-mode, perform a simulated upload with virtual lag.
     */
    suspend fun uploadInspection(
        inspection: VehicleInspection,
        customUrl: String?
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (customUrl.isNullOrEmpty() || !customUrl.startsWith("http")) {
            // Simulated cloud sync mode
            Log.d(TAG, "Simulating cloud upload of Inspection ID: ${inspection.id} (No URL/Demo Mode)")
            delay(1000) // Virtual network roundtrip
            return@withContext true
        }

        try {
            Log.d(TAG, "Uploading inspection to cloud endpoint: $customUrl")
            val response = service.uploadInspectionToUrl(customUrl, inspection)
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully synced inspection with server: ${response.code()}")
                true
            } else {
                Log.e(TAG, "Server returned error: ${response.code()} - ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error syncing inspection: ${e.message}", e)
            false
        }
    }

    /**
     * Uploads a Vehicle profile to a remote server
     */
    suspend fun uploadVehicle(
        vehicle: Vehicle,
        customUrl: String?
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (customUrl.isNullOrEmpty() || !customUrl.startsWith("http")) {
            Log.d(TAG, "Simulating cloud vehicle sync (Demo Mode): ${vehicle.licensePlate}")
            delay(600)
            return@withContext true
        }

        try {
            // Build direct POST to the vehicle endpoint
            val vehicleUrl = if (customUrl.endsWith("/")) "${customUrl}vehicles" else "$customUrl/vehicles"
            Log.d(TAG, "Uploading vehicle to endpoint: $vehicleUrl")
            val response = service.uploadVehicleToUrl(vehicleUrl, vehicle)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Network error syncing vehicle: ${e.message}")
            false
        }
    }
}
