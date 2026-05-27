package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.InspectionItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val listType = Types.newParameterizedType(List::class.java, InspectionItem::class.java)
    private val adapter = moshi.adapter<List<InspectionItem>>(listType)

    @TypeConverter
    fun fromInspectionItemList(items: List<InspectionItem>?): String {
        return items?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toInspectionItemList(jsonData: String?): List<InspectionItem> {
        if (jsonData.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(jsonData) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
