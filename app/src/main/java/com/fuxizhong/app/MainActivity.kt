package com.fuxizhong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fuxizhong.app.ui.components.FuxiBottomBar
import com.fuxizhong.app.ui.components.FuxiTab
import com.fuxizhong.app.ui.screens.ClockScreen
import com.fuxizhong.app.ui.screens.NoteScreen
import com.fuxizhong.app.ui.screens.SealScreen
import com.fuxizhong.app.ui.screens.SplashScreen
import com.fuxizhong.app.ui.theme.FuxiZhongTheme
import com.fuxizhong.app.ui.theme.AppBgColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuxiZhongTheme {
                // 开屏动画展示状态
                var isSplashFinished by remember { mutableStateOf(false) }
                var currentTab by remember { mutableStateOf(FuxiTab.SEAL) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // 主界面 (在开屏结束时呈现)
                    if (isSplashFinished) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = AppBgColor,
                            bottomBar = {
                                FuxiBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { currentTab = it }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentTab) {
                                    FuxiTab.SEAL -> SealScreen()
                                    FuxiTab.NOTE -> NoteScreen()
                                    FuxiTab.CLOCK -> ClockScreen()
                                }
                            }
                        }
                    }

                    // 开屏动画页面 (带平滑淡出)
                    AnimatedVisibility(
                        visible = !isSplashFinished,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(600))
                    ) {
                        SplashScreen(
                            onSplashFinished = { isSplashFinished = true }
                        )
                    }
                }
            }
        }
    }
}
