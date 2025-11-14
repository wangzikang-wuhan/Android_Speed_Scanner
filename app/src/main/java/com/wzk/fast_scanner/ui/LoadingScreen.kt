package com.wzk.fast_scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.wzk.fast_scanner.R
import com.wzk.fast_scanner.ui.theme.Fast_scannerTheme
import kotlinx.coroutines.delay


/**
 * @author wangzikang
 * @date 2025/11/13 17:03
 */

/**
 * 加载页面
 * 显示 Lottie 动画
 * @param onCancel 取消加载的回调，如果为null则不显示取消按钮
 */
@Composable
fun LoadingScreen(onCancel: (() -> Unit)? = null) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.recognizing))
    
    // 跟踪加载时间
    var elapsedSeconds by remember { mutableLongStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(400.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 优化后的文字
            Text(
                text = stringResource(R.string.loading_tips),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
            
            // 显示已等待时间
            if (elapsedSeconds > 5) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.waiting_text,elapsedSeconds),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            
            // 如果提供了取消回调且等待时间超过10秒，显示取消按钮
            if (onCancel != null && elapsedSeconds > 10) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(text = "Cancel Loading")
                }
            }
        }

    }
}

@Preview(showBackground = true, name = "Loading Screen")
@Composable
fun test(){
    Fast_scannerTheme(darkTheme = false, dynamicColor = false){
        LoadingScreen()
    }
}


