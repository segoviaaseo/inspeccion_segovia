package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InspectionItem(
    val id: String,
    val name: String,
    val status: String, // "pass", "fail", "not-checked"
    val notes: String? = null,
    val category: String // "technical", "safety", "legal"
)

@Entity(tableName = "vehicles")
@JsonClass(generateAdapter = true)
data class Vehicle(
    @PrimaryKey val id: String,
    val name: String,
    val licensePlate: String,
    val type: String,
    val soatExpiry: String? = null, // YYYY-MM-DD
    val rtmExpiry: String? = null    // YYYY-MM-DD
)

@Entity(tableName = "inspections")
@JsonClass(generateAdapter = true)
data class VehicleInspection(
    @PrimaryKey val id: String,
    val date: String,            // YYYY-MM-DD
    val vehicleId: String,
    val inspector: String,
    val startTime: String,
    val endTime: String? = null,
    val items: List<InspectionItem>,
    val notes: String? = null,
    val completed: Boolean = false,
    val firmaInspector: String? = null, // Base64 signature image or URI
    val firmaAsistente: String? = null, // Base64 signature image or URI
    val nombreAsistente: String? = null,
    val cargoAsistente: String? = null,
    val isSynced: Boolean = false       // Tracking Cloud/Server sync status
)
