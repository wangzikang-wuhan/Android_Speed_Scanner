package com.wzk.fast_scanner.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzk.fast_scanner.R
import org.opencv.core.Point

/**
 * @author wangzikang
 * @date 2025/11/13 15:28
 */

/**
 * 图像角点选择页面
 * @param bitmap 要处理的图片
 * @param onConfirm 确认回调，返回图片和四个角点（左上、右上、右下、左下）
 * @param onCancel 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedCornerPointScreen(
    bitmap: Bitmap,
    onConfirm: (Bitmap, Array<Point>) -> Unit,
    onCancel: () -> Unit
) {
    // 图片的 ImageBitmap
    val imageBitmap = remember { bitmap.asImageBitmap() }
    
    // Canvas 尺寸
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 图片在 Canvas 中的实际显示尺寸和偏移（考虑缩放和居中）
    var imageScale by remember { mutableStateOf(1f) }
    var imageOffsetX by remember { mutableStateOf(0f) }
    var imageOffsetY by remember { mutableStateOf(0f) }
    var scaledImageWidth by remember { mutableStateOf(0f) }
    var scaledImageHeight by remember { mutableStateOf(0f) }
    
    // 四个角点的位置（相对于 Canvas 坐标系）
    var topLeft by remember { mutableStateOf(Offset.Zero) }
    var topRight by remember { mutableStateOf(Offset.Zero) }
    var bottomLeft by remember { mutableStateOf(Offset.Zero) }
    var bottomRight by remember { mutableStateOf(Offset.Zero) }
    
    // 初始化角点位置（当 Canvas 尺寸改变时）
    fun initializeCorners() {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            // 计算图片缩放比例和位置（保持宽高比，居中显示）
            val canvasAspect = canvasSize.width.toFloat() / canvasSize.height
            val imageAspect = imageBitmap.width.toFloat() / imageBitmap.height
            
            if (imageAspect > canvasAspect) {
                // 图片更宽，以宽度为准
                imageScale = canvasSize.width.toFloat() / imageBitmap.width
                scaledImageWidth = canvasSize.width.toFloat()
                scaledImageHeight = imageBitmap.height * imageScale
                imageOffsetX = 0f
                imageOffsetY = (canvasSize.height - scaledImageHeight) / 2
            } else {
                // 图片更高，以高度为准
                imageScale = canvasSize.height.toFloat() / imageBitmap.height
                scaledImageWidth = imageBitmap.width * imageScale
                scaledImageHeight = canvasSize.height.toFloat()
                imageOffsetX = (canvasSize.width - scaledImageWidth) / 2
                imageOffsetY = 0f
            }
            
            // 设置初始角点位置（在图片区域中间，形成一个矩形）
            val margin = 60.dp.value // 距离边缘的距离
            topLeft = Offset(imageOffsetX + margin, imageOffsetY + margin)
            topRight = Offset(imageOffsetX + scaledImageWidth - margin, imageOffsetY + margin)
            bottomLeft = Offset(imageOffsetX + margin, imageOffsetY + scaledImageHeight - margin)
            bottomRight = Offset(imageOffsetX + scaledImageWidth - margin, imageOffsetY + scaledImageHeight - margin)
        }
    }
    
    // 将 Canvas 坐标转换为图片坐标（用于传递给 OpenCV）
    fun canvasToImageCoordinate(canvasOffset: Offset): Point {
        val imageX = (canvasOffset.x - imageOffsetX) / imageScale
        val imageY = (canvasOffset.y - imageOffsetY) / imageScale
        return Point(imageX.toDouble(), imageY.toDouble())
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.corner_seleced),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "back",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // 底部操作栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.corner_tips),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 确认按钮
                    Button(
                        onClick = {
                            // 转换坐标并回调  顺序是左上右上右下左下
                            val points = arrayOf(
                                canvasToImageCoordinate(topLeft),
                                canvasToImageCoordinate(topRight),
                                canvasToImageCoordinate(bottomRight),
                                canvasToImageCoordinate(bottomLeft)
                            )
                            onConfirm(bitmap, points)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.confrim),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Canvas 用于绘制图片、边框和角点
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        if (canvasSize != size) {
                            canvasSize = size
                            initializeCorners()
                        }
                    }
            ) {
                // 绘制图片
                if (scaledImageWidth > 0 && scaledImageHeight > 0) {
                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(
                            imageOffsetX.toInt(),
                            imageOffsetY.toInt()
                        ),
                        dstSize = androidx.compose.ui.unit.IntSize(
                            scaledImageWidth.toInt(),
                            scaledImageHeight.toInt()
                        )
                    )
                }
                
                // 绘制连接线（形成框）
                val path = Path().apply {
                    moveTo(topLeft.x, topLeft.y)
                    lineTo(topRight.x, topRight.y)
                    lineTo(bottomRight.x, bottomRight.y)
                    lineTo(bottomLeft.x, bottomLeft.y)
                    close()
                }
                
                // 绘制半透明填充
                drawPath(
                    path = path,
                    color = Color(0x3300BCD4),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                
                // 绘制边框线
                drawPath(
                    path = path,
                    color = Color(0xFF00BCD4),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // 绘制四个角点
                val pointRadius = 12.dp.toPx()
                val points = listOf(topLeft, topRight, bottomLeft, bottomRight)
                points.forEach { point ->
                    // 外圈（白色）
                    drawCircle(
                        color = Color.White,
                        radius = pointRadius,
                        center = point
                    )
                    // 内圈（主题色）
                    drawCircle(
                        color = Color(0xFF00BCD4),
                        radius = pointRadius * 0.6f,
                        center = point
                    )
                }
            }
            
            // 可拖动的角点（透明层，用于接收触摸事件）
            val touchRadius = 40.dp.value
            
            // 左上角点
            DraggablePoint(
                offset = topLeft,
                onDrag = { delta -> topLeft += delta },
                touchRadius = touchRadius
            )
            
            // 右上角点
            DraggablePoint(
                offset = topRight,
                onDrag = { delta -> topRight += delta },
                touchRadius = touchRadius
            )
            
            // 左下角点
            DraggablePoint(
                offset = bottomLeft,
                onDrag = { delta -> bottomLeft += delta },
                touchRadius = touchRadius
            )
            
            // 右下角点
            DraggablePoint(
                offset = bottomRight,
                onDrag = { delta -> bottomRight += delta },
                touchRadius = touchRadius
            )
        }
    }
}

/**
 * 可拖动的点
 */
@Composable
private fun DraggablePoint(
    offset: Offset,
    onDrag: (Offset) -> Unit,
    touchRadius: Float
) {
    Box(
        modifier = Modifier
            .size((touchRadius * 2).dp)
            .offset(
                x = (offset.x / androidx.compose.ui.platform.LocalDensity.current.density - touchRadius).dp,
                y = (offset.y / androidx.compose.ui.platform.LocalDensity.current.density - touchRadius).dp
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    )
}
