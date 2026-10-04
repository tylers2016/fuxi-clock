package com.fuxizhong.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class YaoItem(
    val position: String,
    val text: String
)

data class BoshuZhouyiItem(
    val order: Int,
    val traditional_name: String,
    val boshu_name: String,
    val symbol: String,
    val guaci: String,
    val yaoci: List<YaoItem> = emptyList()
)

object ZhouyiHelper {
    private var cachedList: List<BoshuZhouyiItem> = emptyList()

    fun getAllGua(context: Context): List<BoshuZhouyiItem> {
        if (cachedList.isNotEmpty()) return cachedList
        return try {
            val jsonString = context.assets.open("boshu_zhouyi_full.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<BoshuZhouyiItem>>() {}.type
            cachedList = Gson().fromJson(jsonString, type)
            cachedList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取指定卦（默认获取第一卦 乾/鍵）
     */
    fun getGuaByOrder(context: Context, order: Int = 1): BoshuZhouyiItem {
        val list = getAllGua(context)
        return list.find { it.order == order } ?: BoshuZhouyiItem(
            order = 1,
            traditional_name = "乾",
            boshu_name = "鍵",
            symbol = "䷀",
            guaci = "鍵：元享，利貞。"
        )
    }
}
