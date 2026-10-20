package com.example.vokasilearning.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class AdminLogEvent(
    val timestamp: Long,
    val type: String,
    val detail: String
)

internal object LocalAdminLog {
    private const val FILE = "vokasi_admin_audit.json"
    private const val MAX_EVENTS = 500

    fun load(context: Context): List<AdminLogEvent> = try {
        val file = context.getFileStreamPath(FILE)
        val json = if (file.exists()) JSONArray(file.readText(Charsets.UTF_8)) else JSONArray()
        (0 until json.length()).map { index ->
            val data = json.getJSONObject(index)
            AdminLogEvent(
                data.getLong("timestamp"),
                data.getString("type"),
                data.getString("detail")
            )
        }.sortedByDescending { it.timestamp }
    } catch (_: Exception) { emptyList() }

    fun append(context: Context, type: String, detail: String) {
        val safe = detail.take(160)
        val latest = (listOf(AdminLogEvent(System.currentTimeMillis(), type, safe)) +
            load(context)).take(MAX_EVENTS)
        val arr = JSONArray()
        latest.forEach { item ->
            arr.put(JSONObject()
                .put("timestamp", item.timestamp)
                .put("type", item.type)
                .put("detail", item.detail))
        }
        val file = context.getFileStreamPath(FILE)
        val temp = java.io.File(file.absolutePath + ".tmp")
        temp.writeText(arr.toString(), Charsets.UTF_8)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    fun date(time: Long): String =
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date(time)) + " WIB"
}

internal data class AdminPreferences(
    val displayName: String = "Admin VOKASI",
    val school: String = "SMKN 1 Singosari"
)

internal object LocalAdminPreferences {
    private const val FILE = "admin_panel_preferences"
    fun load(context: Context): AdminPreferences {
        val pref = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return AdminPreferences(
            pref.getString("display_name", "Admin VOKASI") ?: "Admin VOKASI",
            pref.getString("school", "SMKN 1 Singosari") ?: "SMKN 1 Singosari"
        )
    }

    fun save(context: Context, config: AdminPreferences): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("display_name", config.displayName.trim())
            .putString("school", config.school.trim())
            .commit()
}
