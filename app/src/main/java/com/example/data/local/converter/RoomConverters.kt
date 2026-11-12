package com.example.data.local.converter

import androidx.room.TypeConverter
import com.example.data.local.entity.MediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class RoomConverters {

    companion object {
        private val LIST_ITEM_REGEX = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
        private val MAP_PAIR_REGEX = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        private val listParseCache = ConcurrentHashMap<String, List<String>>()
        private val mapParseCache = ConcurrentHashMap<String, Map<String, String>>()
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return "[]"
        return try {
            val array = JSONArray()
            list.forEach { array.put(it) }
            val out = array.toString()
            if (!out.isNullOrBlank() && out != "[]") out else buildJsonListString(list)
        } catch (_: Throwable) {
            buildJsonListString(list)
        }
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        val trimmed = json?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        listParseCache[trimmed]?.let { return it }
        try {
            val array = JSONArray(trimmed)
            if (array.length() > 0) {
                val result = ArrayList<String>(array.length())
                for (i in 0 until array.length()) {
                    result.add(array.getString(i))
                }
                cacheList(trimmed, result)
                return result
            }
        } catch (_: Throwable) {
        }
        val fallback = parseJsonListFallback(trimmed)
        cacheList(trimmed, fallback)
        return fallback
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        if (map.isNullOrEmpty()) return "{}"
        return try {
            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            val out = obj.toString()
            if (!out.isNullOrBlank() && out != "{}") out else buildJsonMapString(map)
        } catch (_: Throwable) {
            buildJsonMapString(map)
        }
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String> {
        val trimmed = json?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == "{}") return emptyMap()
        mapParseCache[trimmed]?.let { return it }
        try {
            val obj = JSONObject(trimmed)
            val keys = obj.keys()
            if (keys != null) {
                val result = LinkedHashMap<String, String>()
                while (keys.hasNext()) {
                    val key = keys.next()
                    result[key] = obj.getString(key)
                }
                if (result.isNotEmpty()) {
                    cacheMap(trimmed, result)
                    return result
                }
            }
        } catch (_: Throwable) {
        }
        val fallback = parseJsonMapFallback(trimmed)
        cacheMap(trimmed, fallback)
        return fallback
    }

    private fun cacheList(key: String, value: List<String>) {
        if (listParseCache.size > 512) listParseCache.clear()
        listParseCache[key] = value
    }

    private fun cacheMap(key: String, value: Map<String, String>) {
        if (mapParseCache.size > 512) mapParseCache.clear()
        mapParseCache[key] = value
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun unescapeJson(s: String): String =
        s.replace("\\\"", "\"").replace("\\\\", "\\")

    private fun buildJsonListString(list: List<String>): String =
        list.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${escapeJson(it)}\"" }

    private fun parseJsonListFallback(trimmed: String): List<String> {
        return LIST_ITEM_REGEX.findAll(trimmed).map { unescapeJson(it.groupValues[1]) }.toList()
    }

    private fun buildJsonMapString(map: Map<String, String>): String =
        map.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }

    private fun parseJsonMapFallback(trimmed: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (match in MAP_PAIR_REGEX.findAll(trimmed)) {
            val k = unescapeJson(match.groupValues[1])
            val v = unescapeJson(match.groupValues[2])
            result[k] = v
        }
        return result
    }

    @TypeConverter
    fun fromMediaType(type: MediaType?): String {
        return type?.name ?: MediaType.IMAGE.name
    }

    @TypeConverter
    fun toMediaType(value: String?): MediaType {
        return try {
            if (value != null) MediaType.valueOf(value) else MediaType.IMAGE
        } catch (e: Exception) {
            MediaType.IMAGE
        }
    }
}

