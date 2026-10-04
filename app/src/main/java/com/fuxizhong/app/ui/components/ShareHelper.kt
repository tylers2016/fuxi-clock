package com.fuxizhong.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.fuxizhong.app.data.BoshuZhouyiItem
import com.fuxizhong.app.data.FuxiDayYaoItem
import com.fuxizhong.app.data.NoteItem
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    /**
     * 生成高清古籍信笺画卷 Bitmap
     */
    fun generateLetterBitmap(
        context: Context,
        note: NoteItem,
        yearGanZhiStr: String,
        yaoItem: FuxiDayYaoItem,
        pentadId: Int = 1,
        pageIndex: Int = 0
    ): Bitmap {
        val width = 1200
        val height = 1800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 仿古宣纸底色
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FAF6ED")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. 古籍外框：四周双边 (外粗墨框、内细墨框)
        val outerStroke = 7f
        val outerBorderPaint = Paint().apply {
            color = Color.parseColor("#262422")
            style = Paint.Style.STROKE
            strokeWidth = outerStroke
            isAntiAlias = true
        }
        canvas.drawRect(outerStroke / 2, outerStroke / 2, width - outerStroke / 2, height - outerStroke / 2, outerBorderPaint)

        val innerInset = 16f
        val innerBorderPaint = Paint().apply {
            color = Color.parseColor("#4A4642")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        canvas.drawRect(innerInset, innerInset, width - innerInset, height - innerInset, innerBorderPaint)

        // 3. 左侧书口栏 (Spine)
        val spineLeft = innerInset
        val spineWidth = 110f
        val spineRight = spineLeft + spineWidth

        // 书口与主体之间的垂直界线
        val spineDividerPaint = Paint().apply {
            color = Color.parseColor("#4A4642")
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        canvas.drawLine(spineRight, innerInset, spineRight, height - innerInset, spineDividerPaint)

        // 鱼尾绘制
        val fishTailPaint = Paint().apply {
            color = Color.parseColor("#3A3632")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val ftWidth = 44f
        val ftHeight = 24f
        val spineCenterX = spineLeft + spineWidth / 2f

        // 上鱼尾
        val y1 = height * 0.30f
        val path1 = Path().apply {
            moveTo(spineCenterX - ftWidth / 2f, y1)
            lineTo(spineCenterX + ftWidth / 2f, y1)
            lineTo(spineCenterX, y1 + ftHeight)
            close()
        }
        canvas.drawPath(path1, fishTailPaint)

        // 下鱼尾
        val y2 = height * 0.65f
        val path2 = Path().apply {
            moveTo(spineCenterX - ftWidth / 2f, y2)
            lineTo(spineCenterX + ftWidth / 2f, y2)
            lineTo(spineCenterX, y2 + ftHeight)
            close()
        }
        canvas.drawPath(path2, fishTailPaint)

        // 书口文字
        val spineTextPaint = Paint().apply {
            color = Color.parseColor("#3A3632")
            textSize = 34f
            isAntiAlias = true
            typeface = Typeface.SERIF
            textAlign = Paint.Align.CENTER
        }
        // 上部: 年份干支
        drawVerticalText(canvas, spineTextPaint, yearGanZhiStr, spineCenterX, innerInset + 60f, 44f)

        // 中部: 固定大字 "伏老庄"
        val fuLaoZhuangPaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            textSize = 42f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawVerticalText(canvas, fuLaoZhuangPaint, "伏老庄", spineCenterX, height * 0.44f, 54f)

        // 下部: 用户自定义斋名
        val authorPaint = Paint().apply {
            color = Color.parseColor("#3A3632")
            textSize = 30f
            isAntiAlias = true
            typeface = Typeface.SERIF
            textAlign = Paint.Align.CENTER
        }
        drawVerticalText(canvas, authorPaint, note.authorName, spineCenterX, height * 0.76f, 42f)

        // 4. 主体上部: 日期 + 帛书周易卦名 + 今日当值爻辞 + 卦辞 (左) 与 周易六爻卦象 (右)
        val contentLeft = spineRight + 36f
        val contentRight = width - innerInset - 36f

        // 公历日期
        val datePaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            textSize = 34f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(note.dateStr, contentLeft, innerInset + 65f, datePaint)

        // 帛书卦名
        val guaciTitlePaint = Paint().apply {
            color = Color.parseColor("#9E2A2B") // 朱砂红
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("【帛書周易】第${yaoItem.order}卦 · ${yaoItem.traditional_name}【${yaoItem.boshu_name}】", contentLeft, innerInset + 115f, guaciTitlePaint)

        // 今日爻辞与卦辞标牌画笔
        val badgePaint = Paint().apply {
            color = Color.parseColor("#9E2A2B") // 朱砂红
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        // 1. 红底白字「爻辞」小方标牌 + 帛书爻辞正文
        val yaociBadgeStr = " 爻辞 "
        val yaociBadgeW = badgeTextPaint.measureText(yaociBadgeStr) + 6f
        val yaociTop = innerInset + 138f
        canvas.drawRect(contentLeft, yaociTop, contentLeft + yaociBadgeW, yaociTop + 30f, badgePaint)
        canvas.drawText(yaociBadgeStr, contentLeft + 3f, yaociTop + 22f, badgeTextPaint)

        val yaociBodyPaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            textSize = 25f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val yaociTextStartX = contentLeft + yaociBadgeW + 10f
        val maxYaociW = contentRight - yaociTextStartX - 130f
        val yaociText = yaoItem.yao_text.ifEmpty { "${yaoItem.yao_title}：贞吉。" }
        val yaociBottomY = drawWrappedText(canvas, yaociBodyPaint, yaociText, yaociTextStartX, yaociTop + 22f, maxYaociW, 34f)

        // 2. 红底白字「卦辞」小方标牌 + 帛书卦辞原经文
        val guaciBadgeStr = " 卦辞 "
        val guaciBadgeW = badgeTextPaint.measureText(guaciBadgeStr) + 6f
        val guaciTop = (yaociBottomY + 22f).coerceAtLeast(innerInset + 185f)
        canvas.drawRect(contentLeft, guaciTop, contentLeft + guaciBadgeW, guaciTop + 30f, badgePaint)
        canvas.drawText(guaciBadgeStr, contentLeft + 3f, guaciTop + 22f, badgeTextPaint)

        val guaciBodyPaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            textSize = 25f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val guaciTextStartX = contentLeft + guaciBadgeW + 10f
        val maxGuaciW = contentRight - guaciTextStartX - 130f
        val guaciBottomY = drawWrappedText(canvas, guaciBodyPaint, yaoItem.guaci, guaciTextStartX, guaciTop + 22f, maxGuaciW, 34f)

        // 右侧卦象 (六爻，当值爻为朱砂红) + 卦符
        val guaCenterX = contentRight - 60f
        val guaCenterY = innerInset + 130f
        drawHexagramSymbol(canvas, yaoItem.lines, yaoItem.active_yao_index, guaCenterX, guaCenterY, yaoItem.symbol)

        // 动态高度自适应：根据文字实际落墨位置计算主体上下分割横隔栏线
        val dynamicHeaderBottom = (guaciBottomY + 28f).coerceIn(height * 0.22f, height * 0.35f)
        val headerBottom = dynamicHeaderBottom

        // 上下分割横隔栏线
        val splitPaint = Paint().apply {
            color = Color.parseColor("#4A4642")
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(spineRight, headerBottom, width - innerInset, headerBottom, splitPaint)

        // 5. 主体下部: 极大化十行蓝丝栏信笺
        val columnCount = 10
        val paperTop = headerBottom
        val paperBottom = height - innerInset
        val paperWidth = (width - innerInset) - spineRight
        val colWidth = paperWidth / columnCount

        val gridLinePaint = Paint().apply {
            color = Color.parseColor("#A4BDCF") // 蓝丝栏格线
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            isAntiAlias = true
        }

        for (i in 1 until columnCount) {
            val x = spineRight + i * colWidth
            canvas.drawLine(x, paperTop, x, paperBottom, gridLinePaint)
        }

        // 6. 纵向排版墨迹 (自右向左，自上而下，竖排右起)
        val inkPaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            textSize = 36f
            isAntiAlias = true
            typeface = Typeface.SERIF
            textAlign = Paint.Align.CENTER
        }

        val availableH = paperBottom - paperTop - 70f
        val charStepY = 46f
        val fullColChars = (availableH / charStepY).toInt()
        val pages = layoutVerticalPages(
            content = note.content,
            columnCount = columnCount,
            maxCharsPerCol = fullColChars,
            sealAvoidanceColCount = 2,
            sealAvoidanceLines = 4
        )
        val cols = pages.getOrElse(pageIndex) { emptyList() }

        // 竖排右起：第 0 列在最右侧格内
        for (colIndex in cols.indices) {
            val textInCol = cols[colIndex]
            val colCenterX = (width - innerInset) - (colIndex + 0.5f) * colWidth
            var charY = paperTop + 55f
            for (ch in textInCol) {
                canvas.drawText(ch.toString(), colCenterX, charY, inkPaint)
                charY += charStepY
            }
        }

        // 7. 若有内容且为末页（或单页），左下角自然加盖真实 72 候金石印章
        val isLastPage = pageIndex >= pages.size - 1
        if (note.content.isNotBlank() && isLastPage) {
            try {
                val sealFileName = "seals/seal_${String.format("%02d", pentadId.coerceIn(1, 72))}.webp"
                val stream = context.assets.open(sealFileName)
                val sealBitmap = BitmapFactory.decodeStream(stream)
                stream.close()
                if (sealBitmap != null) {
                    val sealW = 160f
                    val sealH = 160f * (sealBitmap.height.toFloat() / sealBitmap.width.toFloat())
                    val sealLeft = spineRight + 20f
                    val sealTop = paperBottom - sealH - 24f
                    val destRect = RectF(sealLeft, sealTop, sealLeft + sealW, sealTop + sealH)
                    canvas.drawBitmap(sealBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return bitmap
    }

    /**
     * 调起系统分享画卷
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, dateStr: String) {
        try {
            val cachePath = File(context.cacheDir, "shared_letters")
            if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "fuxi_letter_${dateStr}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "分享手札心笺").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "调起分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 保存画卷至系统相册 (Pictures/FuxiZhong)
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, dateStr: String): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "fuxi_letter_${dateStr}_${System.currentTimeMillis()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FuxiZhong")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawVerticalText(canvas: Canvas, paint: Paint, text: String, x: Float, startY: Float, lineHeight: Float) {
        var currentY = startY
        for (ch in text) {
            canvas.drawText(ch.toString(), x, currentY, paint)
            currentY += lineHeight
        }
    }

    private fun drawWrappedText(canvas: Canvas, paint: Paint, text: String, x: Float, startY: Float, maxWidth: Float, lineHeight: Float): Float {
        var currentY = startY
        var currentLine = StringBuilder()

        for (ch in text) {
            val testLine = currentLine.toString() + ch
            val measure = paint.measureText(testLine)
            if (measure > maxWidth) {
                canvas.drawText(currentLine.toString(), x, currentY, paint)
                currentY += lineHeight
                currentLine = StringBuilder(ch.toString())
            } else {
                currentLine.append(ch)
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), x, currentY, paint)
        }
        return currentY
    }

    private fun drawHexagramSymbol(
        canvas: Canvas,
        lines: List<Int>,
        activeIndex: Int,
        cx: Float,
        cy: Float,
        symbol: String
    ) {
        val strokeW = 7.5f
        val activeStrokeW = 9.5f
        val yaoW = 85f
        val gap = 14f
        val totalH = 6 * strokeW + 5 * gap
        val startY = cy - totalH / 2f
        val left = cx - yaoW / 2f
        val right = cx + yaoW / 2f

        val normalPaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            strokeWidth = strokeW
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
            isAntiAlias = true
        }
        val activePaint = Paint().apply {
            color = Color.parseColor("#9E2A2B")
            strokeWidth = activeStrokeW
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
            isAntiAlias = true
        }

        // i 从 0 到 5 (屏幕自上而下)
        // 爻位自下而上：初爻=0, 上爻=5
        for (i in 0 until 6) {
            val lineIndex = 5 - i
            val y = startY + i * (strokeW + gap)
            val isYang = lines.getOrElse(lineIndex) { 1 } == 1
            val isActive = lineIndex == activeIndex
            val paint = if (isActive) activePaint else normalPaint

            if (isYang) {
                canvas.drawLine(left, y, right, y, paint)
            } else {
                val segW = yaoW * 0.42f
                canvas.drawLine(left, y, left + segW, y, paint)
                canvas.drawLine(right - segW, y, right, y, paint)
            }
        }

        // 卦符
        val symPaint = Paint().apply {
            color = Color.parseColor("#1F1D1B")
            textSize = 30f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(symbol, cx, startY + totalH + 34f, symPaint)
    }

    /**
     * 将长文本切分为多页（每页 columnCount 列）的纵向排版列表。
     * 返回 List<List<String>>，外层为页（Page 0, Page 1...），内层为该页的列（Column 0 至 Column 9，自右向左）。
     */
    fun layoutVerticalPages(
        content: String,
        columnCount: Int = 10,
        maxCharsPerCol: Int,
        sealAvoidanceColCount: Int = 2,
        sealAvoidanceLines: Int = 4
    ): List<List<String>> {
        if (content.isBlank()) return listOf(emptyList())

        val allColumns = mutableListOf<String>()
        val paragraphs = content.split("\n")

        for (para in paragraphs) {
            var remaining = para
            if (remaining.isEmpty()) {
                allColumns.add("")
                continue
            }
            while (remaining.isNotEmpty()) {
                val colInPage = allColumns.size % columnCount
                // 仅在每一页的最后两列（靠近左下角印章位置）在底部避让印章
                val isSealColumn = colInPage >= (columnCount - sealAvoidanceColCount)
                val colCapacity = if (isSealColumn) {
                    (maxCharsPerCol - sealAvoidanceLines).coerceAtLeast(5)
                } else {
                    maxCharsPerCol
                }

                val takeLen = remaining.length.coerceAtMost(colCapacity)
                allColumns.add(remaining.substring(0, takeLen))
                remaining = remaining.substring(takeLen)
            }
        }

        val pages = mutableListOf<List<String>>()
        var startIndex = 0
        while (startIndex < allColumns.size) {
            val endIndex = (startIndex + columnCount).coerceAtMost(allColumns.size)
            pages.add(allColumns.subList(startIndex, endIndex))
            startIndex = endIndex
        }

        return if (pages.isEmpty()) listOf(emptyList()) else pages
    }

    /**
     * 纵向排版核心算法（取第一页）：
     * - 右侧 8 列（非印章列）：占满纸格至最底部 (maxCharsPerCol)
     * - 左侧 2 列（覆盖印章列）：从顶部正常落墨，但在印章上方收笔避让 (maxCharsPerCol - sealAvoidanceLines)
     */
    fun layoutVerticalColumns(
        content: String,
        columnCount: Int,
        maxCharsPerCol: Int,
        sealAvoidanceColCount: Int = 2,
        sealAvoidanceLines: Int = 4
    ): List<String> {
        val pages = layoutVerticalPages(content, columnCount, maxCharsPerCol, sealAvoidanceColCount, sealAvoidanceLines)
        return pages.firstOrNull() ?: emptyList()
    }
}
