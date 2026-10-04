package com.fuxizhong.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuxizhong.app.R
import com.fuxizhong.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 纯粹开屏动画：
 * 1. 纯透明青铜双龙交尾太极图标（同步加载零延迟）
 * 2. 说明文字严格按照指定内容分行呈现：
 *    伏羲钟来源
 *    《张远山作品集》第十六卷
 *    《伏羲之道：解密华夏文化总基因》第六章
 * 绝无任何多余英文或额外大字
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 柔和微动的动画效果
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val gentleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentleScale"
    )

    LaunchedEffect(Unit) {
        // 展示 2.5 秒后自动平滑进入主页面
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // 右上角：跳过按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSplashFinished() }
                .background(InkMedium.copy(alpha = 0.08f))
                .border(0.6.dp, InkMedium.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "跳过",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = InkMedium
            )
        }

        // 核心内容区：严格只展示纯透明图标 + 说明文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            // 纯透明青铜交尾双龙太极图标
            Box(
                modifier = Modifier
                    .size(165.dp)
                    .scale(gentleScale),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fuxi_splash_icon),
                    contentDescription = "伏羲双龙交尾太极图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            // 说明文字：分行排版
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 第一行：伏羲钟来源
                Text(
                    text = "伏羲钟来源",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = CinnabarRed,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 第二行：《张远山作品集》第十六卷
                Text(
                    text = "《张远山作品集》第十六卷",
                    fontSize = 16.5.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDeep,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 第三行：《伏羲之道：解密华夏文化总基因》第六章
                Text(
                    text = "《伏羲之道：解密华夏文化总基因》第六章",
                    fontSize = 14.5.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    color = InkMedium,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
