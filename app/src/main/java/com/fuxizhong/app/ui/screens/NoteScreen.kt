package com.fuxizhong.app.ui.screens

import android.widget.Toast
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuxizhong.app.data.FuxiDayYaoItem
import com.fuxizhong.app.data.FuxiYaoHelper
import com.fuxizhong.app.data.LunarHelper
import com.fuxizhong.app.data.NoteItem
import com.fuxizhong.app.data.NoteRepository
import com.fuxizhong.app.data.ZhouyiHelper
import com.fuxizhong.app.ui.components.ShareHelper
import com.fuxizhong.app.ui.components.VerticalLetterPaper
import com.fuxizhong.app.ui.theme.*
import com.nlf.calendar.Lunar
import java.text.SimpleDateFormat
import java.util.*

private val PaperBg = Color(0xFFF9F5EC)
private val InkDark = Color(0xFF22201E)
private val InkMid = Color(0xFF45423E)
private val Cinnabar = Color(0xFF9E2A2B)

/**
 * 第三模块：「笺」—— 古典三才信笺笔记本
 * - 左上角点开式日期管理（按年、按月快捷跳转）
 * - 右上角分享按钮
 * - 主体信笺做大铺满屏幕
 * - 左右滑动切换前后邻近日期，向上滑动快捷触发删除
 * - 点击信笺格子直接提笔横向录入，竖排右起呈现
 */
@Composable
fun NoteScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 当前选中的公历日期
    var currentDate by remember { mutableStateOf(Date()) }
    val currentDateStr = remember(currentDate) { NoteRepository.formatDateStr(currentDate) }

    // 动态根据当前选中的公历日期，计算匹配《伏羲钟》当值卦象及当值爻
    val currentYaoItem = remember(currentDate) {
        FuxiYaoHelper.getYaoForDate(context, currentDate)
    }

    // 读取书斋名与当前日期的笔记
    var authorName by remember { mutableStateOf(NoteRepository.getAuthorName(context)) }
    var currentNote by remember {
        mutableStateOf(
            NoteRepository.getNoteByDate(context, currentDateStr) ?: NoteItem(
                dateStr = currentDateStr,
                content = "",
                authorName = authorName
            )
        )
    }

    // 弹窗状态
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAuthorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSharePreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val currentPentadId = remember(currentDate) {
        try {
            val info = LunarHelper.getLunarInfo(context, currentDate)
            info.todayPentad.id
        } catch (e: Exception) {
            1
        }
    }

    // 计算当年的干支生肖 (如: 丙午马年)
    val lunar = remember(currentDate) { Lunar.fromDate(currentDate) }
    val yearGanZhiStr = "${lunar.yearInGanZhi.orEmpty()}${lunar.yearShengXiao.orEmpty()}年"

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPagesForNote by remember { mutableIntStateOf(1) }

    // 当日期切换时，自动同步读取笔记并重置页码
    LaunchedEffect(currentDateStr) {
        currentNote = NoteRepository.getNoteByDate(context, currentDateStr) ?: NoteItem(
            dateStr = currentDateStr,
            content = "",
            authorName = authorName
        )
        currentPageIndex = 0
    }

    val displayDateFmt = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBgColor)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ================= 1. 极简顶栏：左上角日历管理抽象图标 · 右上角分享抽象图标 =================
        // 上下预留舒适舒展的古雅行距，左右两端对齐呼应
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左上角：抽象日历图标（轻触唤出年月历法管理弹窗）
            IconButton(
                onClick = { showDatePickerDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "历法与日期管理",
                    tint = InkMid,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 右上角：抽象分享图标 (生成高清古籍画卷并弹窗预览与分享)
            IconButton(
                onClick = {
                    val bmp = ShareHelper.generateLetterBitmap(
                        context = context,
                        note = currentNote,
                        yearGanZhiStr = yearGanZhiStr,
                        yaoItem = currentYaoItem,
                        pentadId = currentPentadId,
                        pageIndex = currentPageIndex
                    )
                    previewBitmap = bmp
                    showSharePreviewDialog = true
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = InkMid,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ================= 2. 核心：仿真古籍信笺主体 (书口在左，支持多页与日期翻折动效) =================
        var isForwardTransition by remember { mutableStateOf(true) }

        AnimatedContent(
            targetState = Pair(currentDate, currentPageIndex),
            transitionSpec = {
                if (isForwardTransition) {
                    // 模拟从右向左翻折（书口在左侧固定，右侧卷动翻折）
                    (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350)))
                        .togetherWith(slideOutHorizontally(tween(250)) { -it / 3 } + fadeOut(tween(250)))
                } else {
                    // 模拟向右回翻
                    (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)))
                        .togetherWith(slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)))
                }
            },
            label = "AncientBookPageTurn",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp)
        ) { (targetDate, targetPageIndex) ->
            val pageNote = remember(targetDate, authorName, currentNote) {
                val dStr = NoteRepository.formatDateStr(targetDate)
                NoteRepository.getNoteByDate(context, dStr) ?: NoteItem(
                    dateStr = dStr,
                    content = "",
                    authorName = authorName
                )
            }
            val pageLunar = remember(targetDate) { Lunar.fromDate(targetDate) }
            val pageGanZhi = "${pageLunar.yearInGanZhi.orEmpty()}${pageLunar.yearShengXiao.orEmpty()}年"

            val pentadId = remember(targetDate) {
                try {
                    val info = LunarHelper.getLunarInfo(context, targetDate)
                    info.todayPentad.id
                } catch (e: Exception) {
                    1
                }
            }

            val pageYaoItem = remember(targetDate) {
                FuxiYaoHelper.getYaoForDate(context, targetDate)
            }

            VerticalLetterPaper(
                note = pageNote,
                yearGanZhiStr = pageGanZhi,
                yaoItem = pageYaoItem,
                pentadId = pentadId,
                pageIndex = targetPageIndex,
                totalPages = totalPagesForNote,
                onTotalPagesCalculated = { total ->
                    totalPagesForNote = total
                    if (currentPageIndex >= total) {
                        currentPageIndex = (total - 1).coerceAtLeast(0)
                    }
                },
                onEditContentClick = { showEditDialog = true },
                onEditAuthorClick = { showAuthorDialog = true },
                onPrevPage = {
                    if (currentPageIndex > 0) {
                        isForwardTransition = false
                        currentPageIndex--
                    } else {
                        isForwardTransition = false
                        val cal = Calendar.getInstance().apply {
                            time = currentDate
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        currentDate = cal.time
                    }
                },
                onNextPage = {
                    if (currentPageIndex < totalPagesForNote - 1) {
                        isForwardTransition = true
                        currentPageIndex++
                    } else {
                        isForwardTransition = true
                        val cal = Calendar.getInstance().apply {
                            time = currentDate
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        currentDate = cal.time
                    }
                },
                onPrevDay = {
                    isForwardTransition = false
                    val cal = Calendar.getInstance().apply {
                        time = currentDate
                        add(Calendar.DAY_OF_YEAR, -1)
                    }
                    currentDate = cal.time
                },
                onNextDay = {
                    isForwardTransition = true
                    val cal = Calendar.getInstance().apply {
                        time = currentDate
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    currentDate = cal.time
                },
                onSwipeUpDelete = {
                    if (pageNote.content.isNotBlank()) {
                        showDeleteConfirmDialog = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // ================= 3. 点开式日期管理弹窗 (按年、按月、按日跳转) =================
    if (showDatePickerDialog) {
        DatePickerManageDialog(
            currentDate = currentDate,
            onSelectDate = {
                currentDate = it
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // ================= 4. 编辑信笺弹窗 (横向输入 · 纵向落墨) =================
    if (showEditDialog) {
        var tempContent by remember { mutableStateOf(currentNote.content) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color(0xFFFBF8F1),
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Cinnabar),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("笺", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "提笔书心迹 · 横向录入",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkDark
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "支持随意换行，录入后自动转换为右起十行蓝丝栏竖格：",
                        fontSize = 11.sp,
                        color = InkMid,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempContent,
                        onValueChange = { if (it.length <= 800) tempContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        placeholder = {
                            Text(
                                "在此提笔，记下今日修身感悟、读书心得或随性心迹...",
                                fontSize = 12.sp,
                                color = InkMid.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cinnabar,
                            unfocusedBorderColor = BorderWarm
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${tempContent.length} / 800 字 (超出一页自动分折成页，轻触左右翻阅，印章处优雅避让)",
                        fontSize = 10.sp,
                        color = InkMid,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val saved = NoteRepository.saveNote(
                            context = context,
                            dateStr = currentDateStr,
                            content = tempContent,
                            author = authorName
                        )
                        currentNote = saved
                        currentPageIndex = 0
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cinnabar)
                ) {
                    Text("落墨成笺", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("收起", color = InkMid)
                }
            }
        )
    }

    // ================= 5. 修改书斋名弹窗 =================
    if (showAuthorDialog) {
        var tempAuthor by remember { mutableStateOf(authorName) }
        AlertDialog(
            onDismissRequest = { showAuthorDialog = false },
            containerColor = Color(0xFFFBF8F1),
            shape = RoundedCornerShape(12.dp),
            title = {
                Text("题定书斋号 / 款识", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            },
            text = {
                OutlinedTextField(
                    value = tempAuthor,
                    onValueChange = { if (it.length <= 15) tempAuthor = it },
                    singleLine = true,
                    label = { Text("书口下截题款 (如: 公众号：庄子江湖)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cinnabar,
                        unfocusedBorderColor = BorderWarm
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authorName = tempAuthor.ifBlank { "公众号：庄子江湖" }
                        NoteRepository.saveAuthorName(context, authorName)
                        currentNote = currentNote.copy(authorName = authorName)
                        showAuthorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cinnabar)
                ) {
                    Text("定款", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthorDialog = false }) {
                    Text("取消", color = InkMid)
                }
            }
        )
    }

    // ================= 6. 向上滑动 / 快捷删除确认弹窗 =================
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFFFBF8F1),
            shape = RoundedCornerShape(12.dp),
            title = {
                Text("除却心笺", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            },
            text = {
                Text("是否确认抹去 ${currentDateStr} 的手札墨迹？此操作无法撤销。", fontSize = 13.sp, color = InkMid)
            },
            confirmButton = {
                Button(
                    onClick = {
                        NoteRepository.deleteNoteByDate(context, currentDateStr)
                        currentNote = currentNote.copy(content = "")
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "已抹去该日心笺", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cinnabar)
                ) {
                    Text("确认抹去", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("留存", color = InkMid)
                }
            }
        )
    }

    // ================= 7. 古籍手札画卷生成预览与分享/保存弹窗 =================
    if (showSharePreviewDialog && previewBitmap != null) {
        val bmp = previewBitmap!!
        AlertDialog(
            onDismissRequest = { showSharePreviewDialog = false },
            containerColor = Color(0xFFFBF8F1),
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Cinnabar),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("印", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "古籍手札画卷",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = InkDark
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "已生成高清古典宣纸画卷，已加盖七十二候金石印：",
                        fontSize = 12.sp,
                        color = InkMid,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BorderWarm, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "画卷预览",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val success = ShareHelper.saveBitmapToGallery(context, bmp, currentDateStr)
                            if (success) {
                                Toast.makeText(context, "已成功保存画卷至系统相册！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "保存失败，请检查存储权限", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("保存相册", color = InkDark)
                    }
                    Button(
                        onClick = {
                            ShareHelper.shareBitmap(context, bmp, currentDateStr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cinnabar)
                    ) {
                        Text("调起分享", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSharePreviewDialog = false }) {
                    Text("关闭", color = InkMid)
                }
            }
        )
    }
}

/**
 * 点开式日期管理弹窗：
 * 支持按年快捷切换（< 2026年 >）、按月快捷切换（< 10月 >）、点选任意日期或一键返回今日
 */
@Composable
private fun DatePickerManageDialog(
    currentDate: Date,
    onSelectDate: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = Calendar.getInstance().apply { time = currentDate }
    var year by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0-based

    // 计算当月天数与首日星期几
    val daysInMonth = remember(year, month) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(year, month) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // Sunday is 1, Monday is 2 ...
        (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 转为周一为0，周日为6
    }

    val selectedDay = remember(currentDate, year, month) {
        val c = Calendar.getInstance().apply { time = currentDate }
        if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
            c.get(Calendar.DAY_OF_MONTH)
        } else -1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFAF7EE),
        shape = RoundedCornerShape(12.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 年份与月份切换栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 年份切换
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { year -= 1 }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ChevronLeft, "上一年", tint = InkMid)
                        }
                        Text(
                            text = "${year}年",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkDark
                        )
                        IconButton(onClick = { year += 1 }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ChevronRight, "下一年", tint = InkMid)
                        }
                    }

                    // 月份切换
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (month == 0) {
                                    year -= 1
                                    month = 11
                                } else {
                                    month -= 1
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, "上一月", tint = InkMid)
                        }
                        Text(
                            text = "${month + 1}月",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = InkDark
                        )
                        IconButton(
                            onClick = {
                                if (month == 11) {
                                    year += 1
                                    month = 0
                                } else {
                                    month += 1
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, "下一月", tint = InkMid)
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 星期表头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                        Text(
                            text = w,
                            color = InkMid.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 日期网格
                val totalSlots = firstDayOfWeek + daysInMonth
                val rows = (totalSlots + 6) / 7

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0 until 7) {
                            val slotIndex = r * 7 + c
                            val dayNum = slotIndex - firstDayOfWeek + 1
                            if (dayNum in 1..daysInMonth) {
                                val isSelected = dayNum == selectedDay
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Cinnabar else Color.Transparent)
                                        .clickable {
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayNum)
                                            }
                                            onSelectDate(newCal.time)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = if (isSelected) Color.White else InkDark,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelectDate(Date()) }
            ) {
                Text("返回今日", color = Cinnabar, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = InkMid)
            }
        }
    )
}
