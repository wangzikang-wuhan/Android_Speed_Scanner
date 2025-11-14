package com.wzk.fast_scanner.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangzikang
 * @date 2025/9/18 15:56
 */

//图像增强处理
public class ImagePreprocessingUtil {

    private static final String TAG = "ImagePreprocessingUtil";

    /**
     * 选择文档角点并矫正图像
     * @param sourceBitmap 原始图像
     * @param cornerPoints 选择好的角点
     * @return 处理好后
     */
    public Bitmap manualDocumentCorrection(Bitmap sourceBitmap, Point[] cornerPoints) {
        try {
            //将Bitmap转换为Mat，确保正确的颜色通道顺序
            Mat sourceMat = new Mat();
            //把输入的 Bitmap 拷贝成 ARGB_8888 格式，确保每像素32位，便于 OpenCV 操作
            Bitmap bmp32 = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true);
            //类型转换 将Bitmap类型转换成OpenCV的Mat类型
            Utils.bitmapToMat(bmp32, sourceMat);
            // 处理RGBA到BGR的转换 OpenCV中默认使用BGR，而Android是RGBA，这一步把颜色格式转换成OpenCV标准的BGR
            Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_RGBA2BGR);
            // 确保角点不为空且有效 校验角点一定要是四个而且不能为空
            if (cornerPoints == null || cornerPoints.length != 4) {
                Log.e(TAG, "角点无效，无法进行校正");
                return null;
            }
            // 使用用户选择的角点
            Point[] userSelectedCorners = cornerPoints;
            // 调试日志，输出选择的角点坐标
            for (int i = 0; i < userSelectedCorners.length; i++) {
                //角点坐标日志输出
                Log.d(TAG, "角点" + i + ": x=" + userSelectedCorners[i].x + ", y=" + userSelectedCorners[i].y);
            }
            // 计算文档的宽度和高度 用于拉直
            double topWidth = Math.sqrt(Math.pow(userSelectedCorners[1].x - userSelectedCorners[0].x, 2) +
                    Math.pow(userSelectedCorners[1].y - userSelectedCorners[0].y, 2));
            double bottomWidth = Math.sqrt(Math.pow(userSelectedCorners[2].x - userSelectedCorners[3].x, 2) +
                    Math.pow(userSelectedCorners[2].y - userSelectedCorners[3].y, 2));
            //计算上边和下边的长度，取最大值作为"文档宽度" 这里的文档指的是最后变换后文档的图片
            double finalWidth = Math.max(topWidth, bottomWidth);
            //这里也是一样
            double leftHeight = Math.sqrt(Math.pow(userSelectedCorners[3].x - userSelectedCorners[0].x, 2) +
                    Math.pow(userSelectedCorners[3].y - userSelectedCorners[0].y, 2));
            double rightHeight = Math.sqrt(Math.pow(userSelectedCorners[2].x - userSelectedCorners[1].x, 2) +
                    Math.pow(userSelectedCorners[2].y - userSelectedCorners[1].y, 2));
            //计算左右边的长度，取最大值作为"文档高度"
            double finalHeight = Math.max(leftHeight, rightHeight);
            // 检查计算出的宽度和高度是否有效 判断文档的尺寸是否太小了 如果太小可能是选错点了
            if (finalWidth <= 10 || finalHeight <= 10) {
                Log.e(TAG, "计算出的文档尺寸过小，无法进行校正: " + finalWidth + "x" + finalHeight);
                return null;
            }
            // 使用固定的纵横比例，避免奇怪的形变 如果宽高比例不合理（例如太扁或太长），自动调整为 A4 纸比例。
            double aspectRatio = finalWidth / finalHeight;
            if (aspectRatio < 0.5 || aspectRatio > 2.0) {
                Log.w(TAG, "纵横比例异常 (" + aspectRatio + ")，使用标准A4比例");
                aspectRatio = 210.0 / 297.0; // A4纸比例
                finalHeight = finalWidth / aspectRatio;
            }
            // 设置合理的分辨率 为了防止生成太大的图像，占用内存，这里设置了最大宽高（2000px）
            int maxSize = 2000; // 限制最大尺寸
            if (finalWidth > maxSize) {
                finalWidth = maxSize;
                finalHeight = finalWidth / aspectRatio;
            }
            if (finalHeight > maxSize) {
                finalHeight = maxSize;
                finalWidth = finalHeight * aspectRatio;
            }
            // 显示识别后的宽高
            Log.i(TAG, "手动选择的文档尺寸: " + (int)finalWidth + "x" + (int)finalHeight);
            // 创建源点和目标点 srcPoints：用户选择的原图中的四个角点 dstPoints：变换后的矩形目标区域
            MatOfPoint2f srcPoints = new MatOfPoint2f(
                    userSelectedCorners[0], // 左上
                    userSelectedCorners[1], // 右上
                    userSelectedCorners[2], // 右下
                    userSelectedCorners[3]  // 左下
            );
            //获取透视变换矩阵 计算从 srcPoints 到 dstPoints 的透视变换矩阵
            MatOfPoint2f dstPoints = new MatOfPoint2f(
                    new Point(0, 0),                    // 左上
                    new Point(finalWidth - 1, 0),       // 右上
                    new Point(finalWidth - 1, finalHeight - 1), // 右下
                    new Point(0, finalHeight - 1)       // 左下
            );

            // 获取透视变换矩阵 计算从 srcPoints 到 dstPoints 的透视变换矩阵
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);

            // 检查变换矩阵是否有效
            if (perspectiveTransform.empty()) {
                Log.e(TAG, "透视变换矩阵计算失败");
                return null;
            }
            // 应用透视变换
            Mat warpedMat = new Mat((int)finalHeight, (int)finalWidth, CvType.CV_8UC3);
            //用变换矩阵将文档拉直
            Imgproc.warpPerspective(sourceMat, warpedMat, perspectiveTransform, warpedMat.size());
            // 检查变换结果是否有效
            if (warpedMat.empty() || warpedMat.cols() == 0 || warpedMat.rows() == 0) {
                Log.e(TAG, "透视变换失败，结果为空");
                return null;
            }
            // 应用增强处理 - 直接在透视变换后应用图像增强
            Mat enhancedMat = enhanceDocumentImage(warpedMat);
            // 转换回Bitmap
            Bitmap resultBitmap = Bitmap.createBitmap((int)finalWidth, (int)finalHeight, Bitmap.Config.ARGB_8888);
            // 转回RGBA格式 - 使用增强后的Mat
            Imgproc.cvtColor(enhancedMat, enhancedMat, Imgproc.COLOR_BGR2RGBA);
            Utils.matToBitmap(enhancedMat, resultBitmap);
            // 释放Mat资源
            sourceMat.release();
            warpedMat.release();
            enhancedMat.release();
            perspectiveTransform.release();
            // 记录增强完成的日志
            Log.i(TAG, "文档校正和增强完成");
            //返回矫正并增强后的bitmap
            return resultBitmap;
        } catch (Exception e) {
            Log.e(TAG, "手动文档校正时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 图像文字增强
     * @param inputMat 输入图像
     * @return 处理后的图像
     */
    public Mat enhanceDocumentImage(Mat inputMat) {
        try {
            // 1. 创建输出矩阵和保存原始输入
            Mat outputMat = new Mat();
            Mat originalMat = inputMat.clone();

            // 2. 转换到灰度图像进行处理
            Mat grayMat = new Mat();
            if (inputMat.channels() == 3) {
                Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY);
            } else {
                inputMat.copyTo(grayMat);
            }

            // 3. 简单的对比度和亮度调整 - 轻微增强以保持原始细节
            Mat enhancedGray = new Mat();
            Core.convertScaleAbs(grayMat, enhancedGray, 1.2, 0);

            // 4. 应用适度的高斯模糊来减少噪点，但保留文字边缘
            Mat blurredMat = new Mat();
            Imgproc.GaussianBlur(enhancedGray, blurredMat, new Size(3, 3), 0);

            // 5. 应用自适应阈值，但使用更大的块大小和更小的C值以保留更多细节
            Mat binaryMat = new Mat();
            Imgproc.adaptiveThreshold(blurredMat, binaryMat, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 25, 5);

            // 6. 使用Otsu阈值作为备选二值化方法
            Mat otsuMat = new Mat();
            double otsuThresh = Imgproc.threshold(blurredMat, otsuMat, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            // 使用略低的阈值以保留更多文字细节
            Imgproc.threshold(blurredMat, otsuMat, otsuThresh - 10, 255, Imgproc.THRESH_BINARY);

            // 7. 创建二值图像的混合版本 - 保留更多细节
            Mat combinedBinaryMat = new Mat();
            Core.bitwise_and(binaryMat, otsuMat, combinedBinaryMat);

            // 8. 转换到LAB色彩空间以分别处理亮度和颜色
            if (inputMat.channels() == 3) {
                Mat labMat = new Mat();
                Imgproc.cvtColor(originalMat, labMat, Imgproc.COLOR_BGR2Lab);

                // 9. 分离LAB通道
                List<Mat> labChannels = new ArrayList<>();
                Core.split(labMat, labChannels);

                // 10. 对L通道应用CLAHE，但使用较小的clipLimit以避免过度增强
                Mat enhancedL = new Mat();
                CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
                clahe.apply(labChannels.get(0), enhancedL);
                labChannels.set(0, enhancedL);

                // 11. 适度增强L通道的对比度，避免过度处理
                Core.convertScaleAbs(labChannels.get(0), labChannels.get(0), 1.3, 5);

                // 12. 重新合并LAB通道
                Core.merge(labChannels, labMat);

                // 13. 转回BGR色彩空间
                Imgproc.cvtColor(labMat, outputMat, Imgproc.COLOR_Lab2BGR);

                // 释放通道资源
                for (Mat m : labChannels) {
                    if (m != null && !m.empty()) m.release();
                }
                labMat.release();
            } else {
                // 如果是单通道图像，直接使用增强的灰度图
                enhancedGray.copyTo(outputMat);
            }

            // 14. 轻微锐化以增强边缘，但不过度
            Mat sharpened = new Mat();
            Mat sharpenKernel = new Mat(3, 3, CvType.CV_32F);
            float[] kernelData = new float[] {
                    0, -0.5f, 0,
                    -0.5f, 3.0f, -0.5f,
                    0, -0.5f, 0
            };
            sharpenKernel.put(0, 0, kernelData);
            Imgproc.filter2D(outputMat, sharpened, -1, sharpenKernel);

            // 15. 应用双边滤波以保留边缘的同时减少噪声
            Mat filtered = new Mat();
            Imgproc.bilateralFilter(sharpened, filtered, 5, 50, 50);

            // 16. 将二值图与增强后的图像混合，但使用较低的二值图权重
            if (outputMat.channels() == 3) {
                Mat binaryBGR = new Mat();
                Imgproc.cvtColor(combinedBinaryMat, binaryBGR, Imgproc.COLOR_GRAY2BGR);

                // 使用较低的二值图权重，保留更多原始图像细节
                Mat blended = new Mat();
                Core.addWeighted(filtered, 0.7, binaryBGR, 0.3, 0, blended);
                blended.copyTo(outputMat);

                binaryBGR.release();
                blended.release();
            } else {
                filtered.copyTo(outputMat);
            }

            // 17. 释放资源
            originalMat.release();
            grayMat.release();
            enhancedGray.release();
            blurredMat.release();
            binaryMat.release();
            otsuMat.release();
            combinedBinaryMat.release();
            sharpened.release();
            sharpenKernel.release();
            filtered.release();
            return outputMat;
        } catch (Exception e) {
            Log.e(TAG, "图像增强出错: " + e.getMessage());
            e.printStackTrace();
            // 如果增强失败，返回原始图像
            return inputMat.clone();
        }
    }

}
