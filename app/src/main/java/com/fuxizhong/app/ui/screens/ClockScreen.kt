package com.fuxizhong.app.ui.screens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fuxizhong.app.data.FuxiDayYaoItem
import com.fuxizhong.app.data.FuxiYaoHelper
import com.fuxizhong.app.ui.theme.*
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 伏羲钟二十四节气年周期标准顺序（以冬至 0° 为年周期起点，顺时针各 15°）
 */
val CLOCK_SOLAR_TERMS = listOf(
    "冬至", "小寒", "大寒", "立春", "雨水", "惊蛰",
    "春分", "清明", "谷雨", "立夏", "小满", "芒种",
    "夏至", "小暑", "大暑", "立秋", "处暑", "白露",
    "秋分", "寒露", "霜降", "立冬", "小雪", "大雪"
)

/**
 * 二十四节气精简天象纪实（严格保留纯粹3行：月建星杓、太阳行度）
 */
data class FuxiTermBrief(
    val monthName: String,     // 第2行：月建与招摇指向（如：仲秋之月 · 招摇指酉）
    val solarMotion: String    // 第3行：太阳行度与天象（如：太阳南行抵达赤道上空 · 阴阳平分 · 昼夜等长）
)

val FUXI_CLOCK_LORE = mapOf(
    "立春" to FuxiTermBrief(
        monthName = "孟春之月 · 招摇指寅",
        solarMotion = "太阳北进 · 阳气回暖 · 东风解冻"
    ),
    "雨水" to FuxiTermBrief(
        monthName = "孟春之月 · 招摇指寅",
        solarMotion = "阳气晋天 · 冰消为水 · 春雨菲菲"
    ),
    "惊蛰" to FuxiTermBrief(
        monthName = "仲春之月 · 招摇指卯",
        solarMotion = "阳气升腾 · 春雷震野 · 惊醒蛰虫"
    ),
    "春分" to FuxiTermBrief(
        monthName = "仲春之月 · 招摇指卯",
        solarMotion = "太阳北行抵达赤道上空 · 阴阳平分 · 昼夜等长"
    ),
    "清明" to FuxiTermBrief(
        monthName = "季春之月 · 招摇指辰",
        solarMotion = "阳气充盈 · 万物吐故纳新 · 气清景明"
    ),
    "谷雨" to FuxiTermBrief(
        monthName = "季春之月 · 招摇指辰",
        solarMotion = "四阳渐盛 · 雨生百谷 · 暮春温润"
    ),
    "立夏" to FuxiTermBrief(
        monthName = "孟夏之月 · 招摇指巳",
        solarMotion = "阳气鼎盛 · 万物至此皆长大 · 夏季将始"
    ),
    "小满" to FuxiTermBrief(
        monthName = "孟夏之月 · 招摇指巳",
        solarMotion = "四阳小满 · 巽风吹拂 · 麦类籽粒初满"
    ),
    "芒种" to FuxiTermBrief(
        monthName = "仲夏之月 · 招摇指午",
        solarMotion = "太阳逼近北回归线 · 阳盛至极 · 芒谷可稼"
    ),
    "夏至" to FuxiTermBrief(
        monthName = "仲夏之月 · 招摇指午",
        solarMotion = "太阳抵达北回归线上空 · 圭影最短 · 昼最长夜最短"
    ),
    "小暑" to FuxiTermBrief(
        monthName = "季夏之月 · 招摇指未",
        solarMotion = "一阴初动 · 温风沐野 · 暑气渐大"
    ),
    "大暑" to FuxiTermBrief(
        monthName = "季夏之月 · 招摇指未",
        solarMotion = "酷热达到极致 · 极热熏蒸 · 夏至后三十日极温"
    ),
    "立秋" to FuxiTermBrief(
        monthName = "孟秋之月 · 招摇指申",
        solarMotion = "太阳南行 · 二阴北进 · 凉风至 · 秋季将始"
    ),
    "处暑" to FuxiTermBrief(
        monthName = "孟秋之月 · 招摇指申",
        solarMotion = "阴阳交争 · 阳气涣散 · 暑气伏退"
    ),
    "白露" to FuxiTermBrief(
        monthName = "仲秋之月 · 招摇指酉",
        solarMotion = "水土湿气凝而为露 · 阴气渐重 · 凉意渐深"
    ),
    "秋分" to FuxiTermBrief(
        monthName = "仲秋之月 · 招摇指酉",
        solarMotion = "太阳南行抵达赤道上空 · 阴阳平分 · 昼夜等长"
    ),
    "寒露" to FuxiTermBrief(
        monthName = "季秋之月 · 招摇指戌",
        solarMotion = "太阳继续南藏 · 气寒水冷 · 露气寒凝"
    ),
    "霜降" to FuxiTermBrief(
        monthName = "季秋之月 · 招摇指戌",
        solarMotion = "阴气肃杀 · 天降白霜 · 草木黄落"
    ),
    "立冬" to FuxiTermBrief(
        monthName = "孟冬之月 · 招摇指亥",
        solarMotion = "太阳南沉 · 阴气屯积 · 水始冰 · 冬季将始"
    ),
    "小雪" to FuxiTermBrief(
        monthName = "孟冬之月 · 招摇指亥",
        solarMotion = "阳弱娠地 · 阴强肃杀 · 天降碎雪 · 渐入隆冬"
    ),
    "大雪" to FuxiTermBrief(
        monthName = "仲冬之月 · 招摇指子",
        solarMotion = "太阳抵达极南之前 · 五阴一阳 · 大雪封山"
    ),
    "冬至" to FuxiTermBrief(
        monthName = "仲冬之月 · 招摇指子",
        solarMotion = "太阳南行抵达南回归线上空 · 圭影最长 · 一阳来复"
    ),
    "小寒" to FuxiTermBrief(
        monthName = "季冬之月 · 招摇指丑",
        solarMotion = "太阳折返北归 · 圭影渐敛 · 二阳渐生"
    ),
    "大寒" to FuxiTermBrief(
        monthName = "季冬之月 · 招摇指丑",
        solarMotion = "阴气最盛 · 气候极寒 · 冬至后三十日极寒"
    )
)

/**
 * 计算给定日期的真实天钟指针角度（0°~360°，顺时针，冬至为0°）
 */
fun calculateDialAngleForDate(date: Date): Float {
    val lunar = Lunar.fromDate(date)
    val solar = Solar.fromDate(date)
    val prevJieQi = lunar.prevJieQi
    val nextJieQi = lunar.nextJieQi

    val termName = if (prevJieQi != null && !prevJieQi.name.isNullOrEmpty()) {
        prevJieQi.name
    } else {
        lunar.jieQi ?: "秋分"
    }

    val termIndex = CLOCK_SOLAR_TERMS.indexOf(termName).let { if (it >= 0) it else 18 }
    val daysPassed = if (prevJieQi != null) {
        solar.subtract(prevJieQi.solar).coerceAtLeast(0)
    } else {
        0
    }

    val totalTermDays = if (prevJieQi != null && nextJieQi != null) {
        nextJieQi.solar.subtract(prevJieQi.solar).coerceIn(14, 16)
    } else {
        15
    }

    val cal = Calendar.getInstance()
    cal.time = date
    val dayFraction = (cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f) / 24f
    val exactDays = daysPassed + dayFraction
    val progress = (exactDays / totalTermDays.toFloat()).coerceIn(0f, 1f)
    return (termIndex * 15f + progress * 15f) % 360f
}

/**
 * 度数反查结构体：实时映射任意度数到具体的【节气第几日】
 */
data class AngleDayMatch(
    val termName: String,
    val dayInTerm: Int,
    val yaoItem: FuxiDayYaoItem? = null
)

fun getMatchForDialAngle(context: Context, angle: Float): AngleDayMatch {
    val norm = ((angle % 360f) + 360f) % 360f
    val termIndex = (norm / 15f).toInt().coerceIn(0, 23)
    val termName = CLOCK_SOLAR_TERMS[termIndex]

    val map = FuxiYaoHelper.loadMap(context)
    val termDays = map[termName] ?: emptyList()
    val totalDays = if (termDays.isNotEmpty()) termDays.size else 15

    val degInTerm = norm - (termIndex * 15f)
    val dayIndex = (degInTerm / 15f * totalDays).toInt().coerceIn(0, totalDays - 1)
    val yaoItem = termDays.getOrNull(dayIndex)

    return AngleDayMatch(
        termName = termName,
        dayInTerm = dayIndex + 1,
        yaoItem = yaoItem
    )
}

@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember { Date() }

    // 今日真实历法与指针基准角度（精确到日内时辰）
    val baseAngle = remember(today) {
        calculateDialAngleForDate(today)
    }

    // 状态：周天演历旋转中
    var isPlaying by remember { mutableStateOf(false) }
    var isUserInteracting by remember { mutableStateOf(false) }
    val rotationAnimatable = remember { Animatable(baseAngle) }

    // 状态：缩放与平移定位
    var isZoomed by remember { mutableStateOf(false) }
    var userScale by remember { mutableFloatStateOf(1f) }
    var userOffsetX by remember { mutableFloatStateOf(0f) }
    var userOffsetY by remember { mutableFloatStateOf(0f) }
    var dialBoxSize by remember { mutableStateOf(IntSize.Zero) }

    // 周天旋转协程控制
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // 点击开始旋转：平稳顺时针周流一岁 (每圈16秒，日日如轮转)
            while (isActive) {
                rotationAnimatable.animateTo(
                    targetValue = rotationAnimatable.value + 360f,
                    animationSpec = tween(durationMillis = 16000, easing = LinearEasing)
                )
            }
        } else {
            // 点击停止：优雅回弹归位至今日真实日辰角度
            val currentVal = rotationAnimatable.value
            val targetVal = (Math.round((currentVal - baseAngle) / 360f) * 360f + baseAngle).toFloat()
            rotationAnimatable.animateTo(
                targetValue = targetVal,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // 当前即时指针度数与反查出的日辰
    val currentAngle = rotationAnimatable.value
    val currentMatch = remember(currentAngle) {
        getMatchForDialAngle(context, currentAngle)
    }

    // 局部自动定位靶向计算
    val targetScale = if (isZoomed) 2.35f else 1f
    val (targetOffsetX, targetOffsetY) = remember(isZoomed, currentAngle, dialBoxSize) {
        if (!isZoomed || dialBoxSize.width == 0) {
            Pair(0f, 0f)
        } else {
            val rad = Math.toRadians((currentAngle - 90.0))
            val radius = dialBoxSize.width * 0.32f
            val targetX = (cos(rad) * radius).toFloat()
            val targetY = (sin(rad) * radius).toFloat()
            Pair(-targetX * (2.35f - 1f), -targetY * (2.35f - 1f))
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isZoomed) targetScale else userScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "clockScale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isZoomed) targetOffsetX else userOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "clockOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isZoomed) targetOffsetY else userOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "clockOffsetY"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBgColor)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部控制导航栏：与第三模块「笺」完全一致的极简顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左上角：局部精准定位/全览切换键
            IconButton(
                onClick = {
                    isZoomed = !isZoomed
                    if (!isZoomed) {
                        userScale = 1f
                        userOffsetX = 0f
                        userOffsetY = 0f
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isZoomed) Icons.Outlined.ZoomOut else Icons.Outlined.ZoomIn,
                    contentDescription = if (isZoomed) "全盘概览" else "局部定位",
                    tint = if (isZoomed) CinnabarRed else InkMedium,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 右上角：旋转播放与归位键
            val isAwayFromToday = isPlaying || isUserInteracting || Math.abs((currentAngle % 360f) - (baseAngle % 360f)) > 1.5f
            IconButton(
                onClick = {
                    if (isPlaying) {
                        isPlaying = false
                    } else if (isUserInteracting || Math.abs((currentAngle % 360f) - (baseAngle % 360f)) > 1.5f) {
                        isUserInteracting = false
                        coroutineScope.launch {
                            val currentVal = rotationAnimatable.value
                            val targetVal = (Math.round((currentVal - baseAngle) / 360f) * 360f + baseAngle).toFloat()
                            rotationAnimatable.animateTo(
                                targetValue = targetVal,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    } else {
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isAwayFromToday) Icons.Outlined.Replay else Icons.Outlined.PlayArrow,
                    contentDescription = if (isAwayFromToday) "归位今日" else "岁序演历",
                    tint = if (isAwayFromToday) CinnabarRed else InkMedium,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 主钟盘显示区域卡片：占据上方全部剩余空间，把钟表做长做大占满屏幕
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = InkDeep.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(16.dp))
                .background(PaperWhite)
                .border(1.dp, BorderWarm, RoundedCornerShape(16.dp))
                .clipToBounds()
                .onSizeChanged { dialBoxSize = it }
                .pointerInput(isZoomed, userScale) {
                    if (isZoomed || userScale > 1.05f) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            isZoomed = false
                            val newScale = (userScale * zoom).coerceIn(1f, 3.8f)
                            val maxPanX = (dialBoxSize.width * (newScale - 1f)) / 2f
                            val maxPanY = (dialBoxSize.height * (newScale - 1f)) / 2f
                            userScale = newScale
                            userOffsetX = (userOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                            userOffsetY = (userOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = {
                                isPlaying = false
                                isUserInteracting = true
                            },
                            onDrag = { change, _ ->
                                val centerPxX = dialBoxSize.width / 2f
                                val centerPxY = dialBoxSize.height / 2f
                                val touchX = change.position.x
                                val touchY = change.position.y
                                val rad = atan2((touchY - centerPxY).toDouble(), (touchX - centerPxX).toDouble())
                                val deg = (Math.toDegrees(rad) + 90.0 + 360.0) % 360.0
                                coroutineScope.launch {
                                    rotationAnimatable.snapTo(deg.toFloat())
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val dialSize = if (maxWidth < maxHeight) maxWidth * 0.96f else maxHeight * 0.96f
            Box(
                modifier = Modifier
                    .size(dialSize)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        translationX = animatedOffsetX
                        translationY = animatedOffsetY
                    },
                contentAlignment = Alignment.Center
            ) {
                // 底层：1080x1080 纯净表盘
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/clock/fuxi_dial_clean.png")
                        .crossfade(false)
                        .build(),
                    contentDescription = "伏羲钟表盘",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 表层：以天权为圆心的北斗七星指针（阴阳鱼内旋转）
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/clock/beidou_pointer_clean.png")
                        .crossfade(false)
                        .build(),
                    contentDescription = "北斗七星指针",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = currentAngle
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 底部时令天道信息面板：很短很精简，纯粹3行，无卦辞爻辞
        ClockInfoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 6.dp),
            isPlaying = isPlaying,
            isUserInteracting = isUserInteracting,
            match = currentMatch,
            currentAngle = currentAngle,
            baseAngle = baseAngle
        )
    }
}

/**
 * 底部信息面板：纯粹精简的 3 行结构
 * 1. 今日立针 · 节气第几日 · 圆周度数
 * 2. 仲秋之月 · 招摇指酉 · 斗柄指否
 * 3. 太阳南行抵达赤道上空 · 阴阳平分 · 昼夜等长
 */
@Composable
private fun ClockInfoCard(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isUserInteracting: Boolean,
    match: AngleDayMatch,
    currentAngle: Float,
    baseAngle: Float
) {
    val lore = FUXI_CLOCK_LORE[match.termName] ?: FuxiTermBrief(
        monthName = "仲秋之月 · 招摇指酉",
        solarMotion = "太阳南行抵达赤道上空 · 阴阳平分 · 昼夜等长"
    )
    val activeGua = FuxiYaoHelper.getSimplifiedGuaName(match.yaoItem?.traditional_name ?: "既济")

    val isLiveToday = !isPlaying && !isUserInteracting && Math.abs((currentAngle % 360f) - (baseAngle % 360f)) <= 1.0f

    Surface(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(14.dp), spotColor = InkDeep.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(14.dp),
        color = PaperWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderWarm)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // 1. 第一行：今日立针 这个圆周的度数
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isLiveToday) CinnabarRed else GoldAccent)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = when {
                            isPlaying -> "岁序周流中"
                            isUserInteracting -> "拨盘探赜中"
                            else -> "今日立针"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = if (isLiveToday) CinnabarRed else GoldAccent
                    )
                }

                val normAngle = ((currentAngle % 360f) + 360f) % 360f
                Text(
                    text = "${match.termName}第${match.dayInTerm}日 · ${String.format("%.1f", normAngle)}°",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = InkMedium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 第二行：仲秋之月 · 招摇指酉 · 斗柄指既济卦
            Text(
                text = "${lore.monthName} · 斗柄指${activeGua}卦",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = InkDeep,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 第三行：太阳南行抵达赤道上空（太阳行度与天象）
            Text(
                text = lore.solarMotion,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                color = CinnabarDark,
                lineHeight = 18.sp
            )
        }
    }
}
