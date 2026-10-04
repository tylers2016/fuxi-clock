package com.fuxizhong.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.util.Date

/**
 * 伏羲钟六十四卦及当值爻模型：
 * 完整包含帛书周易卦名、卦辞、今日当值爻题（如初九、六二等）、帛书爻辞正文及六爻阴阳配置
 */
data class FuxiDayYaoItem(
    val day: Int,
    val order: Int,
    val traditional_name: String,
    val boshu_name: String,
    val symbol: String,
    val guaci: String,
    val active_yao_index: Int, // 0..5 (0 = 初爻在最底部, 5 = 上爻在最顶部)
    val yao_title: String,     // 初九、六二、尚九等
    val yao_text: String,      // 帛书爻辞正文
    val lines: List<Int>       // 6个元素：0为阴爻(断线)，1为阳爻(实线)，自下而上(初爻至上爻)
) {
    fun toBoshuItem(): BoshuZhouyiItem = BoshuZhouyiItem(
        order = order,
        traditional_name = traditional_name,
        boshu_name = boshu_name,
        symbol = symbol,
        guaci = guaci
    )
}

object FuxiYaoHelper {
    private var cachedMap: Map<String, List<FuxiDayYaoItem>> = emptyMap()

    fun loadMap(context: Context): Map<String, List<FuxiDayYaoItem>> {
        if (cachedMap.isNotEmpty()) return cachedMap
        return try {
            val jsonString = context.assets.open("fuxi_terms_yao_map.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, List<FuxiDayYaoItem>>>() {}.type
            cachedMap = Gson().fromJson(jsonString, type)
            cachedMap
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * 根据公历日期，计算当前节气及节气内第几日，
     * 并动态匹配《伏羲钟二十四节气图》中对应当值的周易卦象与当值爻辞
     */
    fun getYaoForDate(context: Context, date: Date): FuxiDayYaoItem {
        val map = loadMap(context)
        val lunar = Lunar.fromDate(date)
        val solar = Solar.fromDate(date)

        // 1. 获取当前所处的二十四节气
        val prevJieQi = lunar.prevJieQi
        val termName = if (prevJieQi != null && !prevJieQi.name.isNullOrEmpty()) {
            prevJieQi.name
        } else {
            lunar.jieQi ?: "秋分"
        }

        // 2. 计算自该节气起始日经过的天数 (1-based: 第一天为 1)
        val daysPassed = if (prevJieQi != null) {
            (solar.subtract(prevJieQi.solar) + 1).coerceAtLeast(1)
        } else {
            1
        }

        // 3. 在 24 节气列表中查找该节气的卦爻日程表
        val termDays = map[termName] ?: map["秋分"] ?: emptyList()
        if (termDays.isEmpty()) {
            return fallbackItem()
        }

        // 4. 匹配第 daysPassed 天当值的卦与爻
        return termDays.find { it.day == daysPassed }
            ?: termDays.getOrNull((daysPassed - 1).coerceIn(0, termDays.size - 1))
            ?: termDays.first()
    }

    private fun fallbackItem(): FuxiDayYaoItem {
        return FuxiDayYaoItem(
            day = 1,
            order = 1,
            traditional_name = "乾",
            boshu_name = "鍵",
            symbol = "䷀",
            guaci = "鍵：元享，利貞。",
            active_yao_index = 0,
            yao_title = "初九",
            yao_text = "初九：浸龍勿用。",
            lines = listOf(1, 1, 1, 1, 1, 1)
        )
    }

    fun getSimplifiedGuaName(name: String): String {
        return when (name) {
            "旣濟", "既濟" -> "既济"
            "未濟" -> "未济"
            "師" -> "师"
            "臨" -> "临"
            "觀" -> "观"
            "復" -> "复"
            "晉" -> "晋"
            "豐" -> "丰"
            "漸" -> "渐"
            "隨" -> "随"
            "損" -> "损"
            "頤" -> "颐"
            "節" -> "节"
            "歸妹" -> "归妹"
            "大過" -> "大过"
            "小過" -> "小过"
            "渙" -> "涣"
            "恆" -> "恒"
            "嗛" -> "谦"
            "訟" -> "讼"
            "無妄" -> "无妄"
            "大壯" -> "大壮"
            else -> name
        }
    }
}
