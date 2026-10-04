package com.fuxizhong.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val dateStr: String, // 格式: yyyy-MM-dd
    val content: String,
    val authorName: String = "公众号：庄子江湖",
    val updatedAt: Long = System.currentTimeMillis()
)

object NoteRepository {
    private const val PREFS_NAME = "fuxi_notes_pref"
    private const val KEY_NOTES = "saved_notes_json"
    private const val KEY_DEFAULT_AUTHOR = "custom_author_name"

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateStr(): String = dateFormat.format(Date())

    fun formatDateStr(date: Date): String = dateFormat.format(date)

    fun parseDateStr(dateStr: String): Date {
        return try {
            dateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAuthorName(context: Context): String {
        val saved = getPrefs(context).getString(KEY_DEFAULT_AUTHOR, null)
        return if (saved.isNullOrBlank() || saved == "丌雨書屋" || saved == "公") {
            "公众号：庄子江湖"
        } else {
            saved
        }
    }

    fun saveAuthorName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_DEFAULT_AUTHOR, name).apply()
    }

    fun getAllNotes(context: Context): List<NoteItem> {
        val json = getPrefs(context).getString(KEY_NOTES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NoteItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getNoteByDate(context: Context, dateStr: String): NoteItem? {
        val notes = getAllNotes(context)
        return notes.find { it.dateStr == dateStr }
    }

    fun saveNote(context: Context, dateStr: String, content: String, author: String): NoteItem {
        val notes = getAllNotes(context).toMutableList()
        val existingIndex = notes.indexOfFirst { it.dateStr == dateStr }

        val updatedNote = if (existingIndex >= 0) {
            notes[existingIndex].copy(
                content = content,
                authorName = author,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            NoteItem(
                dateStr = dateStr,
                content = content,
                authorName = author,
                updatedAt = System.currentTimeMillis()
            )
        }

        if (existingIndex >= 0) {
            notes[existingIndex] = updatedNote
        } else {
            notes.add(0, updatedNote)
        }

        val json = gson.toJson(notes)
        getPrefs(context).edit().putString(KEY_NOTES, json).apply()
        return updatedNote
    }

    fun deleteNoteByDate(context: Context, dateStr: String) {
        val notes = getAllNotes(context).toMutableList()
        notes.removeAll { it.dateStr == dateStr }
        val json = gson.toJson(notes)
        getPrefs(context).edit().putString(KEY_NOTES, json).apply()
    }
}
