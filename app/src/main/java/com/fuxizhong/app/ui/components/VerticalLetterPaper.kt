package com.fuxizhong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fuxizhong.app.data.BoshuZhouyiItem
import com.fuxizhong.app.data.FuxiDayYaoItem
import com.fuxizhong.app.data.FuxiYaoHelper
import com.fuxizhong.app.data.NoteItem
import kotlin.math.abs

// 经典古籍刻本专用配色 (纯粹古纸与徽墨质感，杜绝现代圆角卡片与渐变阴影)
private val AncientPaperBg = Color(0xFFFAF6ED)     // 仿古温润宣纸色
private val AncientInkDeep = Color(0xFF201E1C)     // 浓黑徽墨
private val AncientInkMedium = Color(0xFF423E3A)   // 浓淡适中墨色
private val AncientInkLight = Color(0xFF827D74)    // 淡墨提示色
private val AncientGridLine = Color(0xFFA4BDCF)    // 传统信笺蓝丝栏格线 (细雅蓝栏)
private val AncientFrameOuter = Color(0xFF262422)  // 四周双边：外大边栏墨线
private val AncientFrameInner = Color(0xFF4A4642)  // 四周双边：内细边栏墨线
private val AncientSealRed = Color(0xFF9E2A2B)     // 朱砂红印色

/**
 * 经典古籍刻本/筒子页版式（严格参考 20250820.jpg 构图）：
 * 1. 矩形四周双边框（外粗内细）
 * 2. 左侧书口（版心）：上下两道黑鱼尾，分割为：
 *    - 上部：年份干支生肖（如 丙午马年）
 *    - 中部：固定刻字「伏老庄」
 *    - 下部：自定义书斋号（如 丌雨書屋）
 * 3. 右侧主体：
 *    - 上截：日期 + 帛书周易卦辞 (左) 与 周易卦象 (右)
 *    - 横隔栏线
 *    - 下截：极大化的竖排十行蓝丝栏信笺，竖排右起，点击格子即刻提笔落墨
 * 4. 支持手势：左右滑动切换前后日期，向上滑动触发删除心笺
 */
@Composable
fun VerticalLetterPaper(
    note: NoteItem,
    yearGanZhiStr: String,
    yaoItem: FuxiDayYaoItem,
    pentadId: Int = 1,
    pageIndex: Int = 0,
    totalPages: Int = 1,
    onTotalPagesCalculated: (Int) -> Unit = {},
    onEditContentClick: () -> Unit,
    onEditAuthorClick: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onSwipeUpDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. 底层：绘制古籍大边框（四周双边：外粗内细，纯矩形古版刻风格）
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 填充宣纸古底色
            drawRect(color = AncientPaperBg, size = size)

            // 外大边栏 (粗墨线 2.4dp)
            val outerStroke = 2.4.dp.toPx()
            drawRect(
                color = AncientFrameOuter,
                topLeft = Offset(outerStroke / 2, outerStroke / 2),
                size = Size(w - outerStroke, h - outerStroke),
                style = Stroke(width = outerStroke)
            )

            // 内细边栏 (细墨线 0.9dp，距离外边框 3.5dp)
            val inset = 3.5.dp.toPx()
            val innerStroke = 0.9.dp.toPx()
            drawRect(
                color = AncientFrameInner,
                topLeft = Offset(inset + innerStroke / 2, inset + innerStroke / 2),
                size = Size(w - 2 * inset - innerStroke, h - 2 * inset - innerStroke),
                style = Stroke(width = innerStroke)
            )
        }

        // 2. 内部排版：左侧书口栏 (Spine) + 右侧主体 (Main Content)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp) // 位于内细边栏之内
        ) {
            // ================= 【左侧：书口（版心）】 =================
            BookSpineSection(
                yearGanZhiStr = yearGanZhiStr,
                authorName = note.authorName,
                pageIndex = pageIndex,
                totalPages = totalPages,
                onEditAuthorClick = onEditAuthorClick,
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
            )

            // 书口与主体之间的纵向界线 (细竖线)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(AncientFrameInner.copy(alpha = 0.8f))
            )

            // ================= 【右侧：主体版面】 =================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 主体上截：公历日期 + 帛书周易卦辞 (左) 与 周易六爻卦象 (右)
                ZhouyiHeaderSection(
                    dateStr = note.dateStr,
                    yaoItem = yaoItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )

                // 主体上下分割横界栏线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.9.dp)
                        .background(AncientFrameInner.copy(alpha = 0.6f))
                )

                // 主体下截：十行蓝丝栏信笺，竖排右起，字格严格联动
                VerticalGridPaperSection(
                    content = note.content,
                    pentadId = pentadId,
                    pageIndex = pageIndex,
                    onTotalPagesCalculated = onTotalPagesCalculated,
                    onEditClick = onEditContentClick,
                    onPrevDay = onPrevDay,
                    onNextDay = onNextDay,
                    onPrevPage = onPrevPage,
                    onNextPage = onNextPage,
                    onSwipeUpDelete = onSwipeUpDelete,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 左侧书口（版心）：
 * 由上下两道传统古籍黑鱼尾 (▼) 分隔为三截：
 * - 上截：年份干支 (如 丙午马年)
 * - 中截：固定字「伏老庄」
 * - 下截：用户自定义书斋号 (如 丌雨書屋，点击可改)
 */
@Composable
private fun BookSpineSection(
    yearGanZhiStr: String,
    authorName: String,
    pageIndex: Int = 0,
    totalPages: Int = 1,
    onEditAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 上截：年份干支生肖 (如 丙午马年)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            yearGanZhiStr.forEach { ch ->
                Text(
                    text = ch.toString(),
                    color = AncientInkMedium,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp
                )
            }
        }

        // 上黑鱼尾 (古籍正规黑鱼尾：横贯整个版心宽度，两翼贴界线，内凹弧线收于中尖)
        ClassicalFishTail(isDownward = true)

        // 中截：固定庄重大字「伏老庄」与多页时的传统古籍叶次 (如 叶一、叶二)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            "伏老庄".forEach { ch ->
                Text(
                    text = ch.toString(),
                    color = AncientInkDeep,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 16.sp
                )
            }
            if (totalPages > 1) {
                Spacer(modifier = Modifier.height(3.dp))
                val pageLeafStr = when (pageIndex) {
                    0 -> "叶一"
                    1 -> "叶二"
                    2 -> "叶三"
                    3 -> "叶四"
                    4 -> "叶五"
                    else -> "叶${pageIndex + 1}"
                }
                pageLeafStr.forEach { ch ->
                    Text(
                        text = ch.toString(),
                        color = AncientInkMedium,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // 下黑鱼尾 (对鱼尾：横贯整个版心宽度，与上鱼尾呼应相迎)
        ClassicalFishTail(isDownward = false)

        // 下截：用户自定义斋名 / 题款 (点击可修改)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditAuthorClick() }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                authorName.forEach { ch ->
                    Text(
                        text = ch.toString(),
                        color = AncientInkMedium,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 经典古籍刻本黑鱼尾组件：
 * 严格按照古法几何构成：横贯版心的一个扁平矩形（鱼身），接两个轴对称的直角三角形（鱼尾巴鳍）
 */
@Composable
private fun ClassicalFishTail(
    isDownward: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(13.dp)
    ) {
        val w = size.width
        val h = size.height
        val bodyH = h * 0.38f // 扁平矩形（鱼身厚度）
        val path = Path().apply {
            if (isDownward) {
                // 上鱼尾（鱼身在上横贯左右，两个直角三角形鱼尾鳍在下向两翼展开、中间开叉）
                // 1. 顶部扁平矩形横贯左右
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, bodyH)
                // 2. 右侧直角三角形 (直角边在右边界，尖角在最下方)
                lineTo(w, h)
                lineTo(w * 0.58f, bodyH)
                // 3. 鱼尾开叉缺口
                lineTo(w * 0.42f, bodyH)
                // 4. 左侧直角三角形 (直角边在左边界，尖角在最下方)
                lineTo(0f, h)
                lineTo(0f, bodyH)
                close()
            } else {
                // 下鱼尾（对鱼尾，鱼身在下横贯左右，两个直角三角形鱼尾鳍在上向两翼展开、中间开叉）
                moveTo(0f, h)
                lineTo(w, h)
                lineTo(w, h - bodyH)
                lineTo(w, 0f)
                lineTo(w * 0.58f, h - bodyH)
                lineTo(w * 0.42f, h - bodyH)
                lineTo(0f, 0f)
                lineTo(0f, h - bodyH)
                close()
            }
        }
        drawPath(path = path, color = AncientFrameInner)
    }
}

/**
 * 主体上截：日期与帛书周易卦辞 (左) + 周易六爻卦象 (右)
 */
/**
 * 主体上截：日期与帛书周易今日当值爻辞 (左) + 周易六爻卦象与当值朱砂红爻 (右)
 */
@Composable
private fun ZhouyiHeaderSection(
    dateStr: String,
    yaoItem: FuxiDayYaoItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 左侧：公历日期 + 帛书周易卦名 + 当值爻辞 + 卦辞
        Column(modifier = Modifier.weight(1f)) {
            // 第一行：公历日期 + 卦名小印
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    color = AncientInkDeep,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 帛书卦名小刻印
                val sName = FuxiYaoHelper.getSimplifiedGuaName(yaoItem.traditional_name)
                val guaTitle = if (sName == yaoItem.boshu_name || yaoItem.traditional_name == yaoItem.boshu_name) {
                    "帛书周易 · 第${yaoItem.order}卦 · $sName"
                } else {
                    "帛书周易 · 第${yaoItem.order}卦 · $sName【${yaoItem.boshu_name}】"
                }
                Box(
                    modifier = Modifier
                        .background(AncientSealRed.copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = guaTitle,
                        color = AncientSealRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 第二行：红底白字「爻辞」标题 + 帛书爻辞正文（爻题已自含于爻辞中）
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(bottom = 3.dp)
            ) {
                // 红底白字「爻辞」小方签
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .background(AncientSealRed)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "爻辞",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 帛书爻辞正文 (如 初九：官或諭，貞吉...)
                val yaoStatement = yaoItem.yao_text.ifEmpty { "${yaoItem.yao_title}：贞吉。" }
                Text(
                    text = yaoStatement,
                    color = AncientInkDeep,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 第三行：红底白字「卦辞」标题 + 帛书卦辞正文
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // 红底白字「卦辞」小方签
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .background(AncientSealRed)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "卦辞",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 帛书卦辞原经文 (与爻辞并列，字体字号字色完全一致)
                Text(
                    text = yaoItem.guaci,
                    color = AncientInkDeep,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧：周易六爻卦象 (自上而下 6 爻，当值爻为朱砂红) + 卦符
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .width(36.dp)
                    .height(44.dp)
            ) {
                val yaoWidth = size.width
                val yaoHeight = 2.8.dp.toPx()
                val gap = (size.height - 6 * yaoHeight) / 5

                // 绘制六道爻线：i 从 0 至 5（屏幕由上至下）
                // 易经卦象爻位由下至上：初爻=0, 二爻=1, 三爻=2, 四爻=3, 五爻=4, 上爻=5
                // 因此屏幕顶部第 0 道线对应上爻 (index 5)，底部对应初爻 (index 0)
                for (i in 0 until 6) {
                    val lineIndex = 5 - i
                    val y = i * (yaoHeight + gap) + yaoHeight / 2
                    val isYang = yaoItem.lines.getOrElse(lineIndex) { 1 } == 1
                    val isCurrentActive = lineIndex == yaoItem.active_yao_index
                    val strokeColor = if (isCurrentActive) AncientSealRed else AncientInkDeep
                    val currentStrokeW = if (isCurrentActive) yaoHeight * 1.35f else yaoHeight

                    if (isYang) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, y),
                            end = Offset(yaoWidth, y),
                            strokeWidth = currentStrokeW,
                            cap = StrokeCap.Square
                        )
                    } else {
                        val segmentW = yaoWidth * 0.42f
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, y),
                            end = Offset(segmentW, y),
                            strokeWidth = currentStrokeW,
                            cap = StrokeCap.Square
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(yaoWidth - segmentW, y),
                            end = Offset(yaoWidth, y),
                            strokeWidth = currentStrokeW,
                            cap = StrokeCap.Square
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = yaoItem.symbol,
                fontSize = 14.sp,
                color = AncientInkDeep
            )
        }
    }
}

/**
 * 主体下截：极大化十行蓝丝栏信笺纸（自右向左，竖排右起）
 * 点击整个格子区域直接提笔写笺
 * 关键特性：格子高度与单列字数动态联动！随上部空间变化自动计算每列最优字数
 * 恢复印章：使用第一模块中对应72候的真实金石印章透明抠图
 */
@Composable
private fun VerticalGridPaperSection(
    content: String,
    pentadId: Int,
    pageIndex: Int,
    onTotalPagesCalculated: (Int) -> Unit,
    onEditClick: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onSwipeUpDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val columnCount = 10
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onEditClick() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        if (totalDragY < -90f && abs(totalDragY) > abs(totalDragX) * 1.2f) {
                            onSwipeUpDelete()
                        } else if (totalDragX < -60f) {
                            onNextPage()
                        } else if (totalDragX > 60f) {
                            onPrevPage()
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            }
    ) {
        val totalH = maxHeight
        val topPadding = 6.dp
        val bottomPadding = 8.dp
        val usableH = (totalH - topPadding - bottomPadding).coerceAtLeast(100.dp)

        // 格子高度与单列字数的严格联动体系：
        // 目标字格高度设定为 19dp (12sp宋体字在其中纵向居中，上下各有约 3.5dp 呼吸空间)
        val targetCellH = 19.dp
        val colCapacity = (usableH / targetCellH).toInt().coerceIn(12, 45)
        // 严格精确 cellHeight，使得 colCapacity 个格子之和严格等于 usableH，绝无一丝溢出或被截断
        val cellHeight = usableH / colCapacity

        // 印章避让高度：印章高度 54dp + 底部 8dp，留白 ~20dp，总共避让约 80dp
        val sealAvoidCells = kotlin.math.ceil((80.dp / cellHeight).toDouble()).toInt().coerceIn(3, 7)

        val pages = remember(content, colCapacity, sealAvoidCells) {
            ShareHelper.layoutVerticalPages(
                content = content,
                columnCount = columnCount,
                maxCharsPerCol = colCapacity,
                sealAvoidanceColCount = 2,
                sealAvoidanceLines = sealAvoidCells
            )
        }

        val totalPages = pages.size
        LaunchedEffect(totalPages) {
            onTotalPagesCalculated(totalPages)
        }

        val safePageIndex = pageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        val currentCols = pages.getOrElse(safePageIndex) { emptyList() }
        val isLastPage = safePageIndex >= totalPages - 1

        // 绘制十行蓝丝栏垂直格线 (贯通上下)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colW = size.width / columnCount
            for (i in 1 until columnCount) {
                val x = i * colW
                drawLine(
                    color = AncientGridLine,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
        }

        // 如果内容为空，展示古雅淡墨的提示
        if (content.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "【 提 笔 录 心 迹 】\n轻 触 纸 格 · 竖 排 右 起\n左 右 翻 阅 · 溯 流 岁 月\n上 抚 心 笺 · 拂 去 尘 迹",
                    color = AncientInkLight.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 20.sp,
                    letterSpacing = 2.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // 右起纵向排布展示 (自右向左，第0列在最右侧格内)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding),
                horizontalArrangement = Arrangement.End
            ) {
                // 从左至右 10 个格槽 (slot 0 是最左，slot 9 是最右)
                // 竖排右起：文本第0列位于最右侧 (slot 9)
                for (slotIndex in 0 until columnCount) {
                    val textColIndex = columnCount - 1 - slotIndex
                    val colText = currentCols.getOrNull(textColIndex) ?: ""
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            colText.forEach { ch ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cellHeight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ch.toString(),
                                        color = AncientInkDeep,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 若有内容且为末页（或单页），左下角自然加盖真实 72 候金石印章透明抠图 (随候更迭)
            if (isLastPage) {
                val sealFileName = "seal_${String.format("%02d", pentadId.coerceIn(1, 72))}.webp"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/seals/$sealFileName")
                        .crossfade(true)
                        .build(),
                    contentDescription = "七十二候金石印",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 8.dp)
                        .size(54.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
