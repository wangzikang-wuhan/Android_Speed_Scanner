package com.wzk.fast_scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.wzk.fast_scanner.data.DatabaseProvider
import com.wzk.fast_scanner.data.LanguageManage
import com.wzk.fast_scanner.data.MainViewModel
import com.wzk.fast_scanner.data.OCRRecordViewModel
import com.wzk.fast_scanner.data.OCRRecordViewModelFactory
import com.wzk.fast_scanner.data.ScanType
import com.wzk.fast_scanner.data.SettingsManager
import com.wzk.fast_scanner.ui.LoadingScreen
import com.wzk.fast_scanner.ui.MainScreen
import com.wzk.fast_scanner.ui.ResultScreen
import com.wzk.fast_scanner.ui.SelectedCornerPointScreen
import com.wzk.fast_scanner.ui.SettingsScreen
import com.wzk.fast_scanner.ui.theme.Fast_scannerTheme
import com.wzk.fast_scanner.utils.ImagePreprocessingUtil
import com.wzk.fast_scanner.utils.OCRUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import java.io.File


class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val viewModel: MainViewModel by viewModels()
    
    // OCR记录ViewModel，用于保存扫描结果
    private lateinit var ocrRecordViewModel: OCRRecordViewModel
    
    // 图像预处理工具
    private val imagePreprocessingUtil = ImagePreprocessingUtil()
    
    // 相机拍照的临时文件 URI
    private var cameraImageUri: Uri? = null
    
    // 拍照后的图片
    private var capturedBitmap: Bitmap? = null

    override fun attachBaseContext(newBase: Context) {
        val settings = SettingsManager.loadSettings(newBase)
        val localizedContext = LanguageManage.getLocalizedContext(newBase, settings.language)
        super.attachBaseContext(newBase)
    }
    
    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            // 权限已授予，打开相机
            openCamera()
        } else {
            Toast.makeText(this, getString(R.string.need_camera_permissions), Toast.LENGTH_SHORT).show()
        }
    }
    
    // 相机拍照启动器
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            try {
                // 从 URI 加载图片
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, cameraImageUri)
                // 修正图片方向（根据 EXIF 信息）
                val correctedBitmap = fixImageOrientation(bitmap, cameraImageUri!!)
                capturedBitmap = correctedBitmap
                // 触发显示角点选择页面
                showCornerSelectionScreen.value = true
            } catch (e: Exception) {
                Log.e(TAG, "加载拍照图片失败: ${e.message}")
                Toast.makeText(this, getString(R.string.load_image_err), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 相册选择启动器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        Log.d(TAG, "相册选择回调触发，uri: $uri")
        if (uri != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // 显示加载页面
                    isLoading.value = true
                    loadingStartTime = System.currentTimeMillis()
                    Log.d(TAG, "开始处理相册图片")
                    
                    // 在后台线程加载图片
                    val bitmap = withContext(Dispatchers.IO) {
                        val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        // 修正图片方向
                        fixImageOrientation(originalBitmap, uri)
                    }
                    
                    if (bitmap != null) {
                        // 在后台线程进行OCR识别
                        val ocrResults = withContext(Dispatchers.IO) {
                            OCRUtil.commonOCR(this@MainActivity, bitmap)
                        }
                        
                        // 将识别结果合并为字符串
                        val resultText = if (ocrResults.isEmpty()) {
                            "No text is recognized!"
                        } else {
                            ocrResults.joinToString("\n")
                        }
                        
                        Log.d(TAG, "相册图片OCR识别结果: $resultText")
                        
                        // 保存图片到文件
                        val imagePath = withContext(Dispatchers.IO) {
                            saveBitmapToFile(bitmap, "gallery_ocr")
                        }
                        
                        // 关闭加载页面，更新结果状态，触发显示结果页面
                        isLoading.value = false
                        loadingStartTime = 0
                        isFromHistory.value = false
                        ocrResultBitmap.value = bitmap
                        ocrResultText.value = resultText
                        ocrResultImagePath.value = imagePath
                        ocrResultScanType.value = ScanType.OCR
                        showOcrResult.value = true
                        Log.d(TAG, "相册图片处理完成")
                    } else {
                        isLoading.value = false
                        loadingStartTime = 0
                        Toast.makeText(this@MainActivity, getString(R.string.load_image_err), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isLoading.value = false
                    loadingStartTime = 0
                    Log.e(TAG, "处理相册图片失败: ${e.message}", e)
                    Toast.makeText(this@MainActivity, getString(R.string.handle_err)+": ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d(TAG, "用户取消选择图片或URI为空")
            // 确保重置加载状态（防止之前有未完成的加载）
            if (isLoading.value) {
                isLoading.value = false
                loadingStartTime = 0
            }
        }
    }
    
    // 文档扫描启动器
    private val documentScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            
            scanningResult?.pages?.let { pages ->
                if (pages.isNotEmpty()) {
                    // 获取第一页的图片URI
                    val imageUri = pages[0].imageUri
                    
                    // 在协程中处理文档扫描结果
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // 显示加载页面
                            isLoading.value = true
                            loadingStartTime = System.currentTimeMillis()
                            Log.d(TAG, "开始处理文档扫描结果")
                            
                            // 在后台线程加载图片
                            val bitmap = withContext(Dispatchers.IO) {
                                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                            }
                            
                            if (bitmap != null) {
                                // 在后台线程进行OCR识别
                                val ocrResults = withContext(Dispatchers.IO) {
                                    OCRUtil.commonOCR(this@MainActivity, bitmap)
                                }
                                
                                // 将识别结果合并为字符串
                                val resultText = if (ocrResults.isEmpty()) {
                                    "No text is recognized!"
                                } else {
                                    ocrResults.joinToString("\n")
                                }
                                Log.d(TAG, "文档扫描OCR识别结果: $resultText")
                                
                                // 保存图片到文件
                                val imagePath = saveBitmapToFile(bitmap, "doc_scan")

                                // 关闭加载页面，更新结果状态，触发显示结果页面
                                isLoading.value = false
                                loadingStartTime = 0
                                isFromHistory.value = false
                                ocrResultBitmap.value = bitmap
                                ocrResultText.value = resultText
                                ocrResultImagePath.value = imagePath
                                ocrResultScanType.value = ScanType.DOCUMENT_SCAN
                                showOcrResult.value = true
                                Log.d(TAG, "文档扫描处理完成")
                            } else {
                                isLoading.value = false
                                loadingStartTime = 0
                                Toast.makeText(this@MainActivity, getString(R.string.load_document_err), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            isLoading.value = false
                            loadingStartTime = 0
                            Log.e(TAG, "处理文档扫描结果失败: ${e.message}", e)
                            Toast.makeText(this@MainActivity, getString(R.string.handle_err)+": ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.not_document), Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, getString(R.string.res_empty), Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "文档扫描取消或失败")
        }
    }
    
    // 角点选择页面显示状态
    private val showCornerSelectionScreen = mutableStateOf(false)
    
    // OCR 结果状态
    private val ocrResultBitmap = mutableStateOf<Bitmap?>(null)
    private val ocrResultText = mutableStateOf("")
    private val ocrResultImagePath = mutableStateOf<String?>(null)
    private val ocrResultScanType = mutableStateOf(ScanType.OCR)
    private val showOcrResult = mutableStateOf(false)
    private val isFromHistory = mutableStateOf(false)
    
    // 加载状态
    private val isLoading = mutableStateOf(false)
    
    // 加载开始时间，用于超时检测
    private var loadingStartTime: Long = 0
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity OnResume")
        
        // 检查是否有卡住的加载状态（超过30秒）
        if (isLoading.value && loadingStartTime > 0) {
            val elapsedTime = System.currentTimeMillis() - loadingStartTime
            if (elapsedTime > 30000) { // 30秒超时
                Log.w(TAG, "检测到加载超时，重置状态")
                isLoading.value = false
                loadingStartTime = 0
                Toast.makeText(this, getString(R.string.handle_err), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //保存应用设置的语言
        val settings = SettingsManager.loadSettings(this)
        LanguageManage.setLanguage(this,settings.language)

        // 初始化 ViewModel
        viewModel.init(this)
        
        // 初始化 OCRRecordViewModel
        val dao = DatabaseProvider.getRecordDao(this)
        ocrRecordViewModel = ViewModelProvider(
            this, 
            OCRRecordViewModelFactory(dao)
        )[OCRRecordViewModel::class.java]

        // 初始化 OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "成功加载 OpenCV")
        } else {
            Log.e(TAG, "OpenCV加载失败")
        }
        
        // 初始化 OCR
        OCRUtil.initModel(this)
        Log.d(TAG, "开始初始化 OCR")

        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            
            Fast_scannerTheme(darkTheme = settings.isDarkMode, dynamicColor = false) {
                MainAppScreen(
                    viewModel = viewModel,
                    settings = settings,
                    ocrRecordViewModel = ocrRecordViewModel,
                    onOpenCamera = { openCamera() },
                    showCornerSelection = showCornerSelectionScreen.value,
                    capturedBitmap = capturedBitmap,
                    showOcrResult = showOcrResult.value,
                    ocrResultBitmap = ocrResultBitmap.value,
                    ocrResultText = ocrResultText.value,
                    ocrResultImagePath = ocrResultImagePath.value,
                    ocrResultScanType = ocrResultScanType.value,
                    isFromHistory = isFromHistory.value,
                    isLoading = isLoading.value,
                    onCornerSelectionCancel = {
                        showCornerSelectionScreen.value = false
                        capturedBitmap = null
                    },
                    onOcrResultBack = {
                        showOcrResult.value = false
                        ocrResultBitmap.value = null
                        ocrResultText.value = ""
                        ocrResultImagePath.value = null
                        ocrResultScanType.value = ScanType.OCR
                    },
                    onCancelLoading = {
                        isLoading.value = false
                        loadingStartTime = 0
                        Toast.makeText(this@MainActivity, "已取消加载", Toast.LENGTH_SHORT).show()
                    },
                    onRecordClick = { record ->
                        // 点击历史记录，打开结果页面
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // 加载图片（如果有的话）
                                val bitmap = if (record.imagePath.isNotEmpty()) {
                                    withContext(Dispatchers.IO) {
                                        val file = File(record.imagePath)
                                        if (file.exists()) {
                                            BitmapFactory.decodeFile(record.imagePath)
                                        } else {
                                            null
                                        }
                                    }
                                } else {
                                    null
                                }
                                
                                // 转换 type 字符串为 ScanType
                                val scanType = when (record.type) {
                                    "ocr" -> ScanType.OCR
                                    "scan code" -> ScanType.SCAN_CODE
                                    "document" -> ScanType.DOCUMENT_SCAN
                                    else -> ScanType.OCR
                                }
                                
                                // 设置结果状态，显示结果页面（来自历史记录）
                                isFromHistory.value = true
                                ocrResultBitmap.value = bitmap
                                ocrResultText.value = record.content
                                ocrResultImagePath.value = if (record.imagePath.isNotEmpty()) record.imagePath else null
                                ocrResultScanType.value = scanType
                                showOcrResult.value = true
                                
                                Log.d(TAG, "打开历史记录: ${record.type} - ${record.content.take(20)}")
                            } catch (e: Exception) {
                                Log.e(TAG, "加载历史记录失败: ${e.message}", e)
                            }
                        }
                    },
                    onCornerSelectionConfirm = { bitmap, points ->
                        // 在协程中进行文档矫正和OCR识别
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // 关闭角点选择页面，显示加载页面
                                showCornerSelectionScreen.value = false
                                isLoading.value = true
                                loadingStartTime = System.currentTimeMillis()
                                Log.d(TAG, "开始处理拍照图片")
                                
                                // 在后台线程进行文档矫正
                                val correctedBitmap = withContext(Dispatchers.IO) {
                                    imagePreprocessingUtil.manualDocumentCorrection(bitmap, points)
                                }
                                
                                if (correctedBitmap != null) {
                                    // 在后台线程进行 OCR 识别
                                    val ocrResults = withContext(Dispatchers.IO) {
                                        OCRUtil.commonOCR(this@MainActivity, correctedBitmap)
                                    }
                                    
                                    // 将识别结果合并为字符串
                                    val resultText = if (ocrResults.isEmpty()) {
                                        "No text is recognized!"
                                    } else {
                                        ocrResults.joinToString("\n")
                                    }

                                    Log.d(TAG, "OCR 识别结果: $resultText")
                                    
                                    // 保存图片到文件
                                    val imagePath = withContext(Dispatchers.IO) {
                                        saveBitmapToFile(correctedBitmap, "camera_ocr")
                                    }

                                //关闭加载页面，更新结果状态，触发显示结果页面
                                isLoading.value = false
                                loadingStartTime = 0
                                isFromHistory.value = false
                                ocrResultBitmap.value = correctedBitmap
                                ocrResultText.value = resultText
                                ocrResultImagePath.value = imagePath
                                ocrResultScanType.value = ScanType.OCR
                                showOcrResult.value = true
                                Log.d(TAG, "拍照图片处理完成")
                                } else {
                                    isLoading.value = false
                                    loadingStartTime = 0
                                    Toast.makeText(this@MainActivity, getString(R.string.document_correction_err), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isLoading.value = false
                                loadingStartTime = 0
                                Log.e(TAG, "处理失败: ${e.message}", e)
                                Toast.makeText(this@MainActivity, getString(R.string.handle_err)+": ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onQRScan = {
                        //启动扫码器
                        qrCodeScan()
                    },
                    onDocumentScan = {
                        //启动文档扫描
                        documentScan()
                    },
                    onOpenPicture = {
                        //相册
                        openGallery()
                    },
                    onLanguageChangeAndRestart = {
                        //语言切换
                        newLanguage ->
                        LanguageManage.setLanguage(this@MainActivity,newLanguage)
                        recreate()
                    }
                )
            }
        }
    }

    /**
     * 文档扫描
     */
    private fun documentScan(){
        try {
            //文档扫描选项
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                //限制页数
                .setPageLimit(1)
                .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                .setScannerMode(SCANNER_MODE_FULL)
                .build()
            val scanner = GmsDocumentScanning.getClient(options)
            
            // 启动扫描器
            scanner.getStartScanIntent(this)
                .addOnSuccessListener { intentSender ->
                    // 创建 IntentSenderRequest
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    // 启动文档扫描
                    documentScannerLauncher.launch(request)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "启动文档扫描器失败: ${e.message}", e)
                    Toast.makeText(this, getString(R.string.start_doc_scanner_err), Toast.LENGTH_SHORT).show()
                }
        }catch (e: Exception){
            Log.e(TAG,"文档扫描器启动报错："+e.message)
            Toast.makeText(this, getString(R.string.doc_scanner_err)+": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * 二维码扫描
     */
    private fun qrCodeScan(){
        try {
            //创建GmsBarcodeScannerOptions扫描器选项
            var scannerOptions = GmsBarcodeScannerOptions
                .Builder()
                //设置扫描的类型
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E
                )
                //开启自动缩放
                .enableAutoZoom()
                .build()
            //GmsBarcodeScanner实例
            val scanner = GmsBarcodeScanning.getClient(this,scannerOptions)
            //启动
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    if (barcode == null){
                        Log.w(TAG, "扫码结果为空");
                        Toast.makeText(this@MainActivity,getString(R.string.code_scan_res_empty), Toast.LENGTH_SHORT).show();
                    }else{
                        var res = barcode.rawValue
                        Log.d(TAG, "扫码结果: $res")
                        if (!res.isNullOrEmpty()){
                            //如果是连接那么直接用浏览器打开
                            if (res.startsWith("http://") || res.startsWith("https://")){
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res))
                                startActivity(intent)
                                // 链接也保存到数据库
                                ocrRecordViewModel.addRecord(
                                    type = ScanType.SCAN_CODE,
                                    content = res,
                                    imagePath = null
                                )
                                Log.d(TAG, "链接已保存到数据库")
                            } else {
                                //不是链接 跳转到结果页面
                                isFromHistory.value = false
                                ocrResultBitmap.value = null
                                ocrResultText.value = res
                                ocrResultImagePath.value = null
                                ocrResultScanType.value = ScanType.SCAN_CODE
                                showOcrResult.value = true
                                Log.d(TAG, "扫码结果跳转到结果页面")
                            }
                        }
                    }
                }
                .addOnCanceledListener {
                    Toast.makeText(this@MainActivity, getString(R.string.cancel_code_scan), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG,"异常："+e.message)
                    Toast.makeText(this@MainActivity, getString(R.string.code_scan_err), Toast.LENGTH_SHORT).show()
                }
        }catch (e: Exception){
            Log.e(TAG,"扫码器启动报错："+e.message)
        }
    }

    /**
     * 打开相机拍照
     */
    private fun openCamera() {
        // 检查相机权限
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                //权限已授予，直接打开相机
                launchCamera()
            }
            else -> {
                //请求权限
                val permissions = mutableListOf(Manifest.permission.CAMERA)
                
                //Android13以下需要存储权限
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                requestPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }
    
    /**
     * 保存Bitmap 到应用私有存储
     * @param bitmap 要保存的图片
     * @param prefix 文件名前缀
     * @return 保存的文件路径，失败返回 null
     */
    private fun saveBitmapToFile(bitmap: Bitmap, prefix: String = "ocr"): String? {
        return try {
            // 创建保存目录
            val imagesDir = File(getExternalFilesDir(null), "ocr_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            // 创建文件
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val imageFile = File(imagesDir, fileName)
            
            // 保存 Bitmap
            imageFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            Log.d(TAG, "图片已保存: ${imageFile.absolutePath}")
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存图片失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 修正图片方向
     */
    private fun fixImageOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        try {
            // 读取 EXIF 信息
            val inputStream = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()
            
            // 获取方向信息
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            
            // 根据方向计算旋转角度
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            
            // 如果需要旋转
            if (rotation != 0f) {
                Log.d(TAG, "图片需要旋转 $rotation 度")
                val matrix = Matrix()
                matrix.postRotate(rotation)
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 
                    0, 
                    0, 
                    bitmap.width, 
                    bitmap.height, 
                    matrix, 
                    true
                )
                // 回收原始 bitmap
                if (bitmap != rotatedBitmap) {
                    bitmap.recycle()
                }
                return rotatedBitmap
            }
            
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "修正图片方向失败: ${e.message}")
            return bitmap
        }
    }
    
    /**
     * 打开相册
     */
    private fun openGallery() {
        try {
            pickImageLauncher.launch("image/*")
            Log.d(TAG, "打开相册")
        } catch (e: Exception) {
            Log.e(TAG, "打开相册失败: ${e.message}")
            Toast.makeText(this, getString(R.string.open_photo_album_err), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 启动相机
     */
    private fun launchCamera() {
        try {
            // 创建临时文件
            val photoFile = File(
                getExternalFilesDir(null),
                "camera_${System.currentTimeMillis()}.jpg"
            )
            // 创建URI
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            cameraImageUri = uri
            // 启动相机
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e(TAG, "打开相机失败: ${e.message}")
            Toast.makeText(this, getString(R.string.open_camera_err), Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * 底部导航项数据类
 */
data class NavigationItem(
    val title: String,
    val icon: Int,
    val route: String
)

/**
 * 主应用界面
 */
@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    settings: com.wzk.fast_scanner.data.AppSettings,
    ocrRecordViewModel: OCRRecordViewModel,
    onOpenCamera: () -> Unit,
    showCornerSelection: Boolean,
    capturedBitmap: Bitmap?,
    showOcrResult: Boolean,
    ocrResultBitmap: Bitmap?,
    ocrResultText: String,
    ocrResultImagePath: String?,
    ocrResultScanType: ScanType,
    isFromHistory: Boolean,
    isLoading: Boolean,
    onCornerSelectionCancel: () -> Unit,
    onOcrResultBack: () -> Unit,
    onRecordClick: (com.wzk.fast_scanner.data.RecordEntity) -> Unit,
    onCornerSelectionConfirm: (Bitmap, Array<org.opencv.core.Point>) -> Unit,
    onQRScan: () -> Unit,
    onDocumentScan: () -> Unit,
    onOpenPicture: () -> Unit,
    onCancelLoading: () -> Unit,
    onLanguageChangeAndRestart: (String) -> Unit
) {
    var selectedItemIndex by remember { mutableIntStateOf(0) }
    
    // 导航状态：是否显示结果页面
    var showResultScreen by remember { mutableStateOf(false) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultText by remember { mutableStateOf("") }
    var resultImagePath by remember { mutableStateOf<String?>(null) }
    var resultScanType by remember { mutableStateOf(ScanType.OCR) }
    
    val items = listOf(
        NavigationItem(
            title = stringResource(R.string.home),
            icon = R.raw.home,
            route = "home"
        ),
        NavigationItem(
            title = stringResource(R.string.settings),
            icon = R.raw.settings,
            route = "settings"
        )
    )
    
    // 显示结果页面的回调函数
    val onShowResult: (Bitmap?, String, String?, ScanType) -> Unit = { bitmap, text, imagePath, scanType ->
        resultBitmap = bitmap
        resultText = text
        resultImagePath = imagePath
        resultScanType = scanType
        showResultScreen = true
    }

    // 根据导航状态显示不同的页面
    if (isLoading) {
        // 显示加载页面，提供取消加载的回调
        LoadingScreen(onCancel = onCancelLoading)
    } else if (showCornerSelection && capturedBitmap != null) {
        // 显示角点选择页面
        SelectedCornerPointScreen(
            bitmap = capturedBitmap,
            onConfirm = onCornerSelectionConfirm,
            onCancel = onCornerSelectionCancel
        )
    } else if (showOcrResult) {
        // 显示OCR结果页面
        ResultScreen(
            bitmap = ocrResultBitmap,
            recognizedText = ocrResultText,
            scanType = ocrResultScanType,
            imagePath = ocrResultImagePath,
            viewModel = if (isFromHistory) null else ocrRecordViewModel, // 来自历史记录时不保存
            onBackClick = onOcrResultBack
        )
    } else if (showResultScreen) {
        // 显示结果页面
        ResultScreen(
            bitmap = resultBitmap,
            recognizedText = resultText,
            scanType = resultScanType,
            imagePath = resultImagePath,
            viewModel = ocrRecordViewModel,
            onBackClick = {
                showResultScreen = false
                selectedItemIndex = 0 // 返回首页
            }
        )
    } else {
        //显示主界面
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedItemIndex == index,
                            onClick = { selectedItemIndex = index },
                            icon = {
                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(item.icon))
                                LottieAnimation(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(45.dp)
                                )

                            },
                            label = { Text(text = item.title) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Unspecified,
                                unselectedIconColor = Color.Unspecified,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier.padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedItemIndex) {
                    0 -> MainScreen(
                        ocrRecordViewModel = ocrRecordViewModel,
                        onCameraClick = {
                            // 打开相机拍照
                            Log.d("MainActivity", "点击相机识别")
                            onOpenCamera()
                        },
                        onGalleryClick = {
                            // 打开相册
                            Log.d("MainActivity", "点击相册上传")
                            onOpenPicture()
                        },
                        onDocumentScanClick = {
                            Log.d("MainActivity", "点击文档扫描")
                            onDocumentScan()
                        },
                        onQRScanClick = {
                            Log.d("MainActivity", "点击扫码器")
                            onQRScan()
                        },
                        onToggleDarkMode = { darkMode ->
                            viewModel.updateSettings(settings.copy(isDarkMode = darkMode))
                            Log.d("MainActivity", "暗黑模式: $darkMode")
                        },
                        isDarkMode = settings.isDarkMode,
                        onRecordClick = onRecordClick
                    )
                    1 -> SettingsScreen(
                        currentLanguage = settings.language,
                        onLanguageChange = { newLang ->
                            viewModel.updateSettings(settings.copy(language = newLang))
                            onLanguageChangeAndRestart(newLang)
                            Log.d("MainActivity", "语言更改为: $newLang")
                        },
                        currentOCRLanguage = settings.ocrLanguage,
                        onOCRLanguageChange = { newLanguage ->
                            viewModel.updateSettings(settings.copy(ocrLanguage = newLanguage))
                            Log.d("MainActivity", "OCR 识别语言更改为: $newLanguage")
                        },
                        onClearCache = {
                            Log.d("MainActivity", "清除缓存")
                        },
                        onClearHistory = {
                            // 清除历史记录
                            ocrRecordViewModel.clearHistory()
                            Log.d("MainActivity", "清除历史记录")
                        }
                    )
                }
            }
        }
    }
}
