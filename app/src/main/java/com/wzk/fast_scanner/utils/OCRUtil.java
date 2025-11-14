package com.wzk.fast_scanner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.equationl.paddleocr4android.CpuPowerMode;
import com.equationl.paddleocr4android.OCR;
import com.equationl.paddleocr4android.OcrConfig;
import com.equationl.paddleocr4android.Util.paddle.OcrResultModel;
import com.equationl.paddleocr4android.bean.OcrResult;
import com.equationl.paddleocr4android.callback.OcrInitCallback;
import com.equationl.paddleocr4android.callback.OcrRunCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.wzk.fast_scanner.data.AppSettings;
import com.wzk.fast_scanner.data.SettingsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wangzikang
 * @date 2025/9/15 16:49
 */
public class OCRUtil {

    private static final String TAG = "LocalOCRUtil";

    private static final String MODEL_ASSETS_PATH = "models";

    private static final String CLS_NB_NAME = "cls.nb";

    private static final String DET_NB_NAME = "det.nb";

    private static final String REC_NB_NAME = "rec.nb";

    private static OCR ocr;

    private static AtomicBoolean isInit = new AtomicBoolean(false);

    /**
     * 初始化OCR（只会初始化一次）
     * @param context 应用上下文
     */
    public static synchronized void initModel(Context context) {
        // 如果已经初始化或正在初始化，直接返回
        if (isInit.get()) {
            return;
        }
        try {
            // 创建 OCR 实例
            ocr = new OCR(context.getApplicationContext());
            // 配置
            OcrConfig config = new OcrConfig();
            config.setModelPath(MODEL_ASSETS_PATH);
            config.setClsModelFilename(CLS_NB_NAME);
            config.setDetModelFilename(DET_NB_NAME);
            config.setRecModelFilename(REC_NB_NAME);
            // 是否运行各个模型
            config.setRunDet(true);
            config.setRunCls(true);
            config.setRunRec(true);
            // CPU 功率模式
            config.setCpuPowerMode(CpuPowerMode.LITE_POWER_FULL);
            // 是否绘制文本位置框
            config.setDrwwTextPositionBox(true);
            // 开始初始化
            ocr.initModel(config, new OcrInitCallback() {
                @Override
                public void onSuccess() {
                    isInit.set(true);
                    Log.i(TAG, "OCR初始化成功");
                }
                @Override
                public void onFail(Throwable e) {
                    isInit.set(false);
                    Log.e(TAG, "OCR初始化失败", e);
                }
            });
        } catch (Exception e) {
            isInit.set(false);
            Log.e(TAG, "OCR创建失败", e);
        }
    }

    /**
     * 获取当前ocr识别语言
     * @param context
     * @return 语言
     */
    public static String getOcrLanguage(Context context) {
        AppSettings settings = SettingsManager.INSTANCE.loadSettings(context);
        return settings.getOcrLanguage();
    }

    /**
     * 通用ocr识别
     * @param bitmap 需要识别的图像
     * @return 处理好的文字
     */
    public static List<String> commonOCR(Context context,Bitmap bitmap){
        try{
            if (bitmap == null){
                Log.e(TAG,"输入图像为空");
                return Collections.emptyList();
            }
            String ocrLanguage = getOcrLanguage(context);
            if (ocrLanguage.isBlank()){
                //默认为英语
                ocrLanguage = "local_en_zh";
            }
            //
            switch (ocrLanguage){
                case "local_en_zh":
                    return chineseEnglishOCR(bitmap);
                default:
                    return mlKitMultilingualOCR(bitmap,ocrLanguage);
            }
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return Collections.emptyList();
    }


    /**
     * ML KIT 语言识别器
     * @param languageCode 语言代码
     * @return 识别器
     */
    private static TextRecognizer getTextRecognizerForLanguage(String languageCode) {
        try {
            switch (languageCode) {
                case "ja":
                    return TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
                case "ko":
                    return TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
                case "zh":
                    return TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
                default:
                    return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            }
        } catch (Exception e) {
            Log.e(TAG, "创建" + languageCode + "TextRecognizer时出现异常: " + e.getMessage(), e);
            return null;
        }
    }


    /**
     * MLKIT多语言OCR
     * @param bitmap 输入图像
     * @param languageCode 语言代码
     * @return 返回的结果
     */
    private static List<String> mlKitMultilingualOCR(Bitmap bitmap, String languageCode){
        List<String> resultTexts = new ArrayList<>();
        try{
            if (bitmap == null) {
                return resultTexts;
            }
            //图像转换成InputImage对象 后面是图像旋转的角点 这里可以根据图像角点自动旋转图像
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            //语言识别器
            TextRecognizer recognizer = getTextRecognizerForLanguage(languageCode);
            if (recognizer == null) {
                Log.e(TAG, "无法为语言 " + languageCode + " 创建TextRecognizer，返回空结果");
                return Collections.emptyList();
            }

            CountDownLatch latch = new CountDownLatch(1);

            //识别
            recognizer
                    .process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            try{
                                ArrayList<TextBlock> blocks = new ArrayList<>();
                                for (Text.TextBlock textBlock : text.getTextBlocks()) {
                                    String content = textBlock.getText();
                                    Rect box = textBlock.getBoundingBox();
                                    TextBlock tb = new TextBlock(
                                            content,
                                            box.top,
                                            box.bottom,
                                            box.left,
                                            box.right
                                    );
                                    Log.d(TAG,"TextBlock："+tb.toString());
                                    blocks.add(tb);
                                }
                                //文字分类
                                List<String> res = textClassify(blocks);
                                resultTexts.addAll(res);
                            }finally {
                                latch.countDown();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, languageCode + "OCR识别失败: " + e.getMessage(), e);
                            latch.countDown();
                        }
                    });
            //等待结果
            latch.await();
        }catch (Exception e){
            Log.e(TAG,"出现异常:"+e.getMessage());
        }
        return resultTexts;
    }


    //解析出的文本块及其坐标
    private static class TextBlock {

        //识别到的文本内容
        String text;
        //文本框最上边的y坐标
        int minY;
        //文本框最下边的y坐标
        int maxY;
        //文本框最左边的x坐标
        int minX;

        //文本框最右边的x坐标
        int maxX;

        //文本框最中间的y坐标
        int centerY;

        public TextBlock(String text, int minY, int maxY, int minX, int maxX) {
            this.text = text;
            this.minY = minY;
            this.maxY = maxY;
            this.minX = minX;
            this.maxX = maxX;
            this.centerY = (minY + maxY) / 2;
        }

        @Override
        public String toString() {
            return "TextBlock{" +
                    "text='" + text + '\'' +
                    ", minY=" + minY +
                    ", maxY=" + maxY +
                    ", minX=" + minX +
                    ", maxX=" + maxX +
                    ", centerY=" + centerY +
                    '}';
        }
    }

    /**
     * 本地中英文识别
     * @param bitmap 输入图像
     * @return 返回的结果
     */
    private static List<String> chineseEnglishOCR(Bitmap bitmap) throws InterruptedException {
        if (bitmap == null) {
            return Collections.emptyList();
        }

        if (!isInit.get() || ocr == null){
            Log.d(TAG,"模型还未初始化,等待初始化完成....");
            Thread.sleep(3000);
        }
        Log.d(TAG,"开始识别");
        List<String> resultTexts = new ArrayList<>();
        try{
            CountDownLatch latch = new CountDownLatch(1);
            //异步
            ocr.run(bitmap, new OcrRunCallback() {
                @Override
                public void onSuccess(@NonNull OcrResult ocrResult) {
                    Log.d(TAG,"耗时："+ocrResult.getInferenceTime());
                    try{
                        ArrayList<OcrResultModel> outputRawResult = ocrResult.getOutputRawResult();
                        List<TextBlock> textBlocks = new ArrayList<TextBlock>();
                        for (OcrResultModel res : outputRawResult) {
                            //文字所在的区域角点 顺序是左上 右上 右下 左下
                            List<Point> points = res.getPoints();
                            //文字本身
                            String subText = res.getLabel();
                            Log.d(TAG,subText+"  "+points.toString());
                            if (points.isEmpty() || points.size() < 4 || subText.isBlank()){
                                continue;
                            }

                            //构造文本块对象
                            TextBlock tb = new TextBlock(
                                    subText,
                                    points.get(0).y,
                                    points.get(3).y,
                                    points.get(0).x,
                                    points.get(1).x
                            );
                            Log.d(TAG,tb.toString());
                            textBlocks.add(tb);
                        }

                        // 按照中心Y坐标排序文本块
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            textBlocks.sort((a, b) -> Integer.compare(a.centerY, b.centerY));
                        }

                        //文本分类
                        List<String> res = textClassify(textBlocks);
                        Log.d(TAG,"分类后的结果:"+res.toString());
                        resultTexts.addAll(res);
                    }finally {
                        latch.countDown();
                    }
                }
                @Override
                public void onFail(@NonNull Throwable throwable) {
                    Log.e(TAG, "识别失败！报错："+throwable.getMessage());
                    latch.countDown();
                }
            });
            latch.await();
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return resultTexts;
    }

    /**
     * 文本块分类和自动填充空格 同一行放到一起 还原原本的格式
     * @param textBlocks 文本块集合
     * @return 解析好的数据
     */
    private static List<String> textClassify(List<TextBlock> textBlocks){
        List<String> res = new ArrayList<>();
        try{
            // 对位于同一行的文本进行水平排序并合并
            List<List<TextBlock>> textLines = new ArrayList<>();
            List<TextBlock> currentLine = new ArrayList<>();

            // 行间距阈值 根据情况调整 同一行的阈值
            final int LINE_HEIGHT_THRESHOLD = 30;

            // 空格阈值 同一行才加空格 换行了就不用管
            final int LINE_SPACE_THRESHOLD = 10;

            // 将文本块分组到不同行
            if (!textBlocks.isEmpty()) {
                currentLine.add(textBlocks.get(0));
                //从第二个开始遍历 第一个已经在集合中了
                for (int i = 1; i < textBlocks.size(); i++) {
                    //当前块
                    TextBlock current = textBlocks.get(i);
                    //上一个已经在集合中的
                    TextBlock previous = textBlocks.get(i - 1);
                    // 如果当前文本块与前一个在垂直方向上足够接近 中心Y轴上的误差在阈值范围内就分到同一行
                    if (Math.abs(current.centerY - previous.centerY) <= LINE_HEIGHT_THRESHOLD) {
                        currentLine.add(current);
                    } else {
                        // 当前文本块与前一个不在同一行 开始新行 因为前面已经根据中心点排序了 顺序已经是从上到下从左到右 只要不是就说明换行了
                        textLines.add(new ArrayList<>(currentLine));
                        currentLine.clear();
                        currentLine.add(current);
                    }
                }
                // 添加最后一行
                if (!currentLine.isEmpty()) {
                    textLines.add(currentLine);
                }
            }
            // 清空结果列表，准备添加排序后的文本
            res.clear();
            // 正序遍历文本行列表，确保文本从上到下排列与原始图片一致
            for (int i = 0; i < textLines.size(); i++) {
                List<TextBlock> line = textLines.get(i);
                // 按照X坐标排序 从左到右排序
                line.sort((a, b) -> Integer.compare(a.minX, b.minX));
                // 合并同一行的文本
                StringBuilder lineText = new StringBuilder();
                for (int j = 0;  j < line.size(); j++){
                    TextBlock tb = line.get(j);
                    // 第一块直接加
                    if (j == 0) {
                        lineText.append(tb.text);
                        continue;
                    }
                    // 计算与前一个块的水平间距
                    TextBlock prev = line.get(j - 1);
                    int rawSpace = tb.minX - prev.maxX;
                    // 重叠时加1个空格
                    if (rawSpace <= 0) {
                        lineText.append(" ");
                        lineText.append(tb.text);
                        continue;
                    }
                    //有间距根据间距计算数量
                    int spaceCount = Math.max(1, (int) Math.ceil(rawSpace / (double) LINE_SPACE_THRESHOLD));
                    //空格
                    lineText.append(" ".repeat(spaceCount));
                    lineText.append(tb.text);
                }
                // 添加行文本到结果列表
                res.add(lineText.toString());
            }
            //返回结果
            return res;
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return res;
    }


}
