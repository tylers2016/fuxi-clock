package com.fuxizhong.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuxizhong.app.ui.theme.*

/**
 * 底部三大模块：第1模块为「印」，第2模块为「钟」，第3模块为「笺」
 * 仅以单独一个书法/古典宋体汉字呈现图标
 */
enum class FuxiTab(val label: String) {
    SEAL("印"),
    CLOCK("钟"),
    NOTE("笺")
}

@Composable
fun FuxiBottomBar(
    currentTab: FuxiTab,
    onTabSelected: (FuxiTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppBgColor)
            .navigationBarsPadding()
    ) {
        // 顶部一道极淡的古籍隔栏线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(AppBorderColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FuxiTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) CinnabarRed else InkMedium.copy(alpha = 0.6f),
                    animationSpec = tween(durationMillis = 200),
                    label = "tabTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tab.label,
                            color = animatedColor,
                            fontSize = if (isSelected) 22.sp else 20.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // 选中时底部一枚朱砂小墨点/印痕标记
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 4.dp else 0.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) CinnabarRed else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
