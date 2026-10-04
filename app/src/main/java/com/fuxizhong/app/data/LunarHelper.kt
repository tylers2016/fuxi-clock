package com.fuxizhong.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.util.Date

data class TodayLunarInfo(
    val gregorianDateStr: String, // 公历 2026年10月1日 星期四
    val ganzhiYear: String,       // 丙午年
    val shengXiao: String,        // 马
    val lunarMonthDay: String,    // 八月廿一
    val ganzhiDay: String,        // 戊辰日
    val currentTerm: String,      // 秋分
    val wuHouName: String,        // 雷始收声
    val todayPentad: PentadItem   // 对应 72 候中的一候
)

object LunarHelper {
    private var cachedPentads: List<PentadItem> = emptyList()

    fun loadPentads(context: Context): List<PentadItem> {
        if (cachedPentads.isNotEmpty()) return cachedPentads
        return try {
            val jsonString = context.assets.open("pentads/pentads_72.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<PentadItem>>() {}.type
            cachedPentads = Gson().fromJson(jsonString, type)
            cachedPentads
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getTodayInfo(context: Context): TodayLunarInfo {
        return getLunarInfo(context, Date())
    }

    fun getLunarInfo(context: Context, date: Date): TodayLunarInfo {
        val pentads = loadPentads(context)
        val solar = Solar.fromDate(date)
        val lunar = Lunar.fromDate(date)

        // 干支与农历信息
        val ganzhiYear = "${lunar.yearInGanZhi ?: ""}年"
        val shengXiao = lunar.yearShengXiao ?: ""
        val lunarMonthDay = "${lunar.monthInChinese ?: ""}月${lunar.dayInChinese ?: ""}"
        val ganzhiDay = "${lunar.dayInGanZhi ?: ""}日"
        val weekStr = try { solar.weekInChinese ?: "" } catch (_: Exception) { "" }
        val gregorianStr = "${solar.year}年${solar.month}月${solar.day}日 星期$weekStr"

        // 获取当前节气与物候（6tail Java 库返回值可能为 null）
        val wuHou: String = lunar.wuHou ?: ""
        // 当前节气
        val jieQiName = when {
            !lunar.jieQi.isNullOrEmpty() -> lunar.jieQi
            lunar.prevJieQi != null && !lunar.prevJieQi.name.isNullOrEmpty() -> lunar.prevJieQi.name
            else -> "立春"
        }

        // 在 72 候列表中寻找匹配项
        val matched = pentads.find { 
            val hName = it.houName.orEmpty()
            wuHou.isNotEmpty() && hName.isNotEmpty() && (hName == wuHou || wuHou.contains(hName) || hName.contains(wuHou))
        } ?: pentads.find { 
            val sTerm = it.solarTerm.orEmpty()
            jieQiName.isNotEmpty() && sTerm.isNotEmpty() && sTerm == jieQiName 
        } ?: pentads.firstOrNull() ?: PentadItem()

        return TodayLunarInfo(
            gregorianDateStr = gregorianStr,
            ganzhiYear = ganzhiYear,
            shengXiao = shengXiao,
            lunarMonthDay = lunarMonthDay,
            ganzhiDay = ganzhiDay,
            currentTerm = jieQiName,
            wuHouName = if (wuHou.isNotEmpty()) wuHou else matched.houName,
            todayPentad = matched
        )
    }
}
