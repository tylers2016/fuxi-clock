package com.fuxizhong.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fuxizhong.app.data.FuxiYaoHelper
import com.fuxizhong.app.data.LunarHelper
import com.fuxizhong.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 伏羲钟二十四节气年周期标准顺序
 */
val SOLAR_TERMS_ORDER = listOf(
    "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
    "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
    "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
    "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"
)

/**
 * 第一模块：「印」—— 七十二候印谱
 * 严格以节气为单位：仅允许翻阅当前节气的 3 候，不允许跨节气翻阅；
 * 跨界阻挡时，提示当令节气、当前斗柄所指卦象，以及上一节气/下一节气结界；
 * 保证印章高清大图满幅展示，尺寸毫无压缩。
 */
@Composable
fun SealScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allPentads = remember { LunarHelper.loadPentads(context) }
    val todayInfo = remember { LunarHelper.getTodayInfo(context) }
    val todayYao = remember(todayInfo) {
        FuxiYaoHelper.getYaoForDate(context, java.util.Date())
    }

    // 当前当令节气名称（如 秋分）
    val currentTerm = remember(todayInfo) {
        todayInfo.currentTerm.ifEmpty { "秋分" }
    }

    // 过滤出属于当前节气的 3 候（初候、二候、三候）
    val termPentads = remember(allPentads, currentTerm) {
        val filtered = allPentads.filter { it.solarTerm == currentTerm }
        if (filtered.isNotEmpty()) filtered else allPentads.take(3)
    }

    // 计算节气循环中的前后邻近节气
    val termIndex = remember(currentTerm) {
        val idx = SOLAR_TERMS_ORDER.indexOf(currentTerm)
        if (idx >= 0) idx else 15 // 默认秋分
    }
    val prevTerm = remember(termIndex) {
        SOLAR_TERMS_ORDER[(termIndex - 1 + SOLAR_TERMS_ORDER.size) % SOLAR_TERMS_ORDER.size]
    }
    val nextTerm = remember(termIndex) {
        SOLAR_TERMS_ORDER[(termIndex + 1) % SOLAR_TERMS_ORDER.size]
    }

    // 当前在当令节气 3 候中的索引 (0..2)
    var currentIndex by remember(termPentads, todayInfo) {
        val initialIndex = termPentads.indexOfFirst { it.id == todayInfo.todayPentad.id }
        mutableStateOf(if (initialIndex >= 0) initialIndex else 0)
    }

    val currentPentad = remember(currentIndex, termPentads) {
        if (termPentads.isNotEmpty() && currentIndex in termPentads.indices) {
            termPentads[currentIndex]
        } else {
            todayInfo.todayPentad
        }
    }

    // 拖拽阻尼位移与时令结界提示
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

    var barrierMessage by remember { mutableStateOf<String?>(null) }
    var barrierJob by remember { mutableStateOf<Job?>(null) }

    fun triggerBarrier(msg: String) {
        barrierJob?.cancel()
        barrierMessage = msg
        barrierJob = coroutineScope.launch {
            delay(2400)
            barrierMessage = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBgColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部标题与当令三候指示
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "七十二候印谱",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = InkDeep,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            // 极简三候流转指示标签（初候 · 二候 · 三候）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                termPentads.forEachIndexed { idx, p ->
                    val isSelected = idx == currentIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CinnabarRed else BorderWarm)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = p.houOrder,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Serif,
                            color = if (isSelected) CinnabarRed else InkLight
                        )
                    }
                }
            }
        }

        // 主体区域：最大化高清印章图片，手势限制在当令 3 候之内
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .pointerInput(termPentads.size, currentIndex) {
                    if (termPentads.isEmpty()) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragOffsetX = 0f
                        },
                        onDragEnd = {
                            val activeGua = FuxiYaoHelper.getSimplifiedGuaName(todayYao.traditional_name).ifEmpty { "既济" }
                            if (dragOffsetX < -50f) {
                                // 试图向左滑（翻向下一候）
                                if (currentIndex < termPentads.size - 1) {
                                    currentIndex++
                                } else {
                                    // 已至三候末尾，阻挡翻阅并提示下一节气！
                                    triggerBarrier("当令【$currentTerm】· 斗柄指${activeGua}卦 ·【$nextTerm】未至不可预涉")
                                }
                            } else if (dragOffsetX > 50f) {
                                // 试图向右滑（翻向上的一候）
                                if (currentIndex > 0) {
                                    currentIndex--
                                } else {
                                    // 已至初候起始，阻挡翻阅并提示上一节气！
                                    triggerBarrier("当令【$currentTerm】· 斗柄指${activeGua}卦 ·【$prevTerm】已过既往不逆")
                                }
                            }
                            dragOffsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // 边缘阻尼感知：处于边界时位移衰减，体现时令阻力
                            val isAtBoundary = (currentIndex == 0 && dragAmount > 0) ||
                                    (currentIndex == termPentads.size - 1 && dragAmount < 0)
                            val factor = if (isAtBoundary) 0.28f else 0.55f
                            dragOffsetX += dragAmount * factor
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 印章大图（支持弹性质感微位移回弹）
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/pentads/${currentPentad.filename}")
                    .crossfade(true)
                    .build(),
                contentDescription = currentPentad.fullTitle,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = animatedOffsetX
                    },
                contentScale = ContentScale.Fit
            )

            // 时令结界浮层阻挡提示（不压缩图尺寸，悬浮于图上方，2秒后自动淡出）
            androidx.compose.animation.AnimatedVisibility(
                visible = barrierMessage != null,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    color = InkDeep.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.2.dp, CinnabarRed.copy(alpha = 0.7f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "时 令 结 界",
                            color = GoldAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 4.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = barrierMessage ?: "",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}
