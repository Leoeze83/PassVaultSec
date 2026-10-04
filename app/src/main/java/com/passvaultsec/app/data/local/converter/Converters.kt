package com.passvaultsec.app.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.passvaultsec.app.domain.model.ChecklistItem
import com.passvaultsec.app.domain.model.Collaborator

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromChecklistItems(items: List<ChecklistItem>?): String {
        return gson.toJson(items ?: emptyList<ChecklistItem>())
    }

    @TypeConverter
    fun toChecklistItems(json: String?): List<ChecklistItem> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<ChecklistItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(strings: List<String>?): String {
        return gson.toJson(strings ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromCollaboratorsMap(map: Map<String, Collaborator>?): String {
        return gson.toJson(map ?: emptyMap<String, Collaborator>())
    }

    @TypeConverter
    fun toCollaboratorsMap(json: String?): Map<String, Collaborator> {
        if (json.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, Collaborator>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromUrlPreviewList(previews: List<com.passvaultsec.app.domain.model.UrlPreview>?): String {
        return gson.toJson(previews ?: emptyList<com.passvaultsec.app.domain.model.UrlPreview>())
    }

    @TypeConverter
    fun toUrlPreviewList(json: String?): List<com.passvaultsec.app.domain.model.UrlPreview> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<com.passvaultsec.app.domain.model.UrlPreview>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromNoteLocation(location: com.passvaultsec.app.domain.model.NoteLocation?): String? {
        if (location == null) return null
        return gson.toJson(location)
    }

    @TypeConverter
    fun toNoteLocation(json: String?): com.passvaultsec.app.domain.model.NoteLocation? {
        if (json.isNullOrEmpty()) return null
        return gson.fromJson(json, com.passvaultsec.app.domain.model.NoteLocation::class.java)
    }
}
