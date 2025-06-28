package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义视图，用于在相机预览上方绘制人脸检测结果，包括人脸框、轮廓、表情以及新增的人物框。
 *
 * 在当前 TP-Link 摄像头集成方案中，FaceOverlayView 不会接收到实时的视频帧数据进行人脸识别，
 * 因此它主要作为一个叠加层存在，并根据手动更新的 `faces` 列表进行绘制。
 */
public class FaceOverlayView extends View {
    // 绘图工具：
    private Paint facePaint;       // 绘制人脸边界框的画笔 (在当前onDraw中，这个画笔未直接用于绘制人脸Rect)
    private Paint contourPaint;    // 绘制人脸轮廓点的画笔
    private Paint textPaint;       // 绘制文字信息的画笔
    private Paint emojiBgPaint;    // 绘制信息面板背景的画笔

    // ML Kit人脸数据：
    private List<Face> faces;      // 存储所有检测到的人脸对象

    // 视图和图像尺寸信息：
    private int previewWidth;      // 相机预览视图（PreviewView 或 FrameLayout）的宽度
    private int previewHeight;     // 相机预览视图（PreviewView 或 FrameLayout）的高度
    private float scaleX;          // X轴缩放比例，将ML Kit原始坐标转换为视图坐标
    private float scaleY;          // Y轴缩放比例
    private int imageWidth;        // 原始相机图像的宽度 (从ImageProxy获取，此集成中通常不会收到)
    private int imageHeight;       // 原始相机图像的高度 (从ImageProxy获取，此集成中通常不会收到)
    private int rotation;          // 原始相机图像的旋转角度 (从ImageProxy获取，此集成中通常不会收到)

    // 表情和信息面板相关：
    private String currentMood = "😐"; // 当前检测到的表情（用表情符号表示）

    // 人物框相关属性 (来自你之前提供的 FaceOverlayView 代码)
    private Paint personBoxPaint;      // 绘制人物框的画笔
    private Paint personTextPaint;     // 绘制人物框标签的画笔
    private static final float PERSON_BOX_SCALE = 2.5f; // 人物框高度是人脸框的2.5倍
    private static final float VERTICAL_OFFSET = 50f;   // 人物框相对于人脸垂直方向的额外偏移量 (向屏幕底部偏移)

    /**
     * 构造函数，用于通过XML布局文件实例化自定义View。
     * @param context 当前应用上下文
     * @param attrs 从XML中获取的属性集
     */
    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(); // 调用初始化方法
    }

    /**
     * 初始化画笔和数据结构。
     */
    private void init() {
        // 初始化人脸边界框画笔 (在当前的 onDraw 逻辑中未使用此画笔直接绘制人脸Rect)
        facePaint = new Paint();
        facePaint.setColor(Color.WHITE); // 白色
        facePaint.setStyle(Paint.Style.STROKE); // 空心
        facePaint.setStrokeWidth(2.0f); // 描边宽度

        // 初始化人脸轮廓画笔
        contourPaint = new Paint();
        contourPaint.setColor(Color.YELLOW); // 默认黄色 (在drawFaceContours中会根据轮廓类型更改颜色)
        contourPaint.setStyle(Paint.Style.FILL); // 填充 (用于绘制轮廓点)
        contourPaint.setStrokeWidth(2.0f); // 描边宽度 (对于线条绘制，会创建新的画笔实例)

        // 初始化文本画笔
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE); // 白色
        textPaint.setTextSize(40f); // 字体大小
        textPaint.setAntiAlias(true); // 抗锯齿

        // 初始化信息面板背景画笔
        emojiBgPaint = new Paint();
        emojiBgPaint.setColor(Color.argb(100, 0, 0, 0)); // 半透明黑色
        emojiBgPaint.setStyle(Paint.Style.FILL); // 填充

        // 初始化人物框画笔
        personBoxPaint = new Paint();
        personBoxPaint.setColor(Color.parseColor("#4CAF50")); // 绿色
        personBoxPaint.setStyle(Paint.Style.STROKE); // 空心
        personBoxPaint.setStrokeWidth(6.0f); // 描边宽度
        personBoxPaint.setAntiAlias(true); // 抗锯齿

        // 初始化人物框标签画笔
        personTextPaint = new Paint();
        personTextPaint.setColor(Color.parseColor("#4CAF50")); // 绿色
        personTextPaint.setTextSize(45f); // 字体大小
        personTextPaint.setAntiAlias(true); // 抗锯齿
        personTextPaint.setFakeBoldText(true); // 粗体

        // 初始化人脸列表
        faces = new ArrayList<>();
    }

    /**
     * 设置原始相机图像的尺寸和旋转信息。
     * 这些信息理论上用于将ML Kit检测结果从原始图像坐标映射到视图坐标。
     * 在当前 TP-Link 摄像头集成中，这些值通常不会被实时更新，除非你有其他图像来源。
     * @param width 原始图像宽度
     * @param height 原始图像高度
     * @param rotation 原始图像旋转角度（0, 90, 180, 270）
     */
    public void setImageSourceInfo(int width, int height, int rotation) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.rotation = rotation;
        calculateScaleFactor(); // 图像源信息变化后，重新计算缩放因子
    }

    /**
     * 设置相机预览视图（PreviewView 或 FrameLayout）的尺寸。
     * 这是 View 在屏幕上占据的实际大小。
     * @param width 预览视图宽度
     * @param height 预览视图高度
     */
    public void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
        calculateScaleFactor(); // 预览尺寸变化后，重新计算缩放因子
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 如果此 FaceOverlayView 本身的尺寸发生变化，也需要重新计算缩放因子
        setPreviewSize(w, h); // 直接使用视图自身的宽高更新预览尺寸
    }

    /**
     * 根据原始图像（如果有）和预览视图的尺寸，计算用于坐标转换的缩放因子。
     *
     * 注意：这里采用的缩放因子计算方式 (`previewWidth * 0.4f / imageWidth`)
     * 可能与 TP-Link SDK 视频渲染的实际缩放和居中方式不完全匹配。
     * 如果 ML Kit 实际能够获取到帧数据，绘制结果可能会有偏差。
     */
    private void calculateScaleFactor() {
        if (imageWidth > 0 && imageHeight > 0 && previewWidth > 0 && previewHeight > 0) {
            // 这个 targetWidth 0.4f 的比例是沿用你之前 FaceOverlayView 代码中的逻辑。
            // 它意味着 ML Kit 的图像（如果有的话）会被缩放到预览宽度的 40%
            float targetWidth = previewWidth * 0.4f;
            scaleX = targetWidth / imageWidth;
            scaleY = scaleX; // 保持X和Y的缩放比例一致，以避免图像拉伸
        }
    }

    /**
     * 更新检测到的人脸列表，并触发视图重绘。
     * @param faces 新的人脸列表。传入 `Collections.emptyList()` 可清空绘制。
     */
    public void updateFaces(List<Face> faces) {
        this.faces = faces;
        invalidate(); // 请求重绘视图
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 如果没有人脸数据或图像尺寸信息不完整，则不进行绘制 ML Kit 相关图形
        if (faces == null || faces.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            // 此时 FaceOverlayView 仍然是可见的，如果它有背景，背景会绘制。
            // 但如果 FaceOverlayView 本身是透明的，就什么都不显示。
            // 你也可以在这里只绘制半透明背景，或者根据需要调整。
            canvas.drawColor(Color.argb(80, 0, 0, 0)); // 绘制半透明背景层
            return;
        }

        // 绘制一个半透明背景层，覆盖整个视图
        canvas.drawColor(Color.argb(80, 0, 0, 0));

        // 从所有检测到的人脸中选择面积最大的人脸，假设为主检测目标。
        Face targetFace = Collections.max(faces, (f1, f2) -> Float.compare(
                f1.getBoundingBox().width() * f1.getBoundingBox().height(),
                f2.getBoundingBox().width() * f2.getBoundingBox().height()
        ));

        // 获取选定人脸的边界框 (原始ML Kit坐标)
        Rect faceRect = targetFace.getBoundingBox();

        // 计算人物框尺寸（基于人脸高度）
        float personHeight = faceRect.height() * PERSON_BOX_SCALE;
        // 计算人物框相对于人脸顶部边界的垂直偏移，使其位于人脸下方 (并加上固定 VERTICAL_OFFSET)
        float verticalOffsetForPersonBoxTop = personHeight - faceRect.height();

        // 动态扩展人物框的宽度和高度，使人物框在水平方向上比人脸宽一些
        float expandWidth = faceRect.width() * 0.2f;  // 扩展人脸宽度的20%
        // float expandHeight = faceRect.height() * 0.2f; // 该变量在之前的代码中计算了但实际未使用

        // 定义人物框的浮点矩形，基于人脸框和计算出的偏移量
        RectF personRect = new RectF(
                faceRect.left - expandWidth,                                  // 左边界：人脸左侧偏移 expandWidth
                faceRect.top - verticalOffsetForPersonBoxTop + VERTICAL_OFFSET, // 顶部边界：人脸顶部向上偏移后，再向下偏移 VERTICAL_OFFSET
                faceRect.right + expandWidth,                                 // 右边界：人脸右侧偏移 expandWidth
                faceRect.bottom + VERTICAL_OFFSET                             // 底部边界：人脸底部向下偏移 VERTICAL_OFFSET
        );

        // 将人物框的原始坐标转换为屏幕预览视图的坐标
        personRect.left = personRect.left * scaleX;
        personRect.top = personRect.top * scaleY;
        personRect.right = personRect.right * scaleX;
        personRect.bottom = personRect.bottom * scaleY;

        // 处理前置摄像头镜像（ML Kit的结果对于前置摄像头通常是镜像的）
        boolean isFrontCamera = true; // 此处硬编码为true，表示总是假定使用前置摄像头
        if (isFrontCamera) {
            float tempLeft = personRect.left;
            personRect.left = previewWidth - personRect.right;
            personRect.right = previewWidth - tempLeft;
        }

        // 处理相机旋转 (此处逻辑基于旧有FaceOverlayView，假设了一种特定相机-预览的适配关系)
        // 这个转换逻辑假设了特定的预览配置和图像处理流程。
        // 如果 CameraX 的配置或 View 的 scaleType 发生变化，这里的逻辑可能需要调整。
        if (rotation == 90 || rotation == 270) {
            float tempLeft = personRect.left;
            float tempTop = personRect.top;
            float tempRight = personRect.right;
            float tempBottom = personRect.bottom;

            personRect.left = tempTop; // 新 left = 旧 top
            personRect.top = previewHeight - tempRight; // 新 top = 预览高 - 旧 right (实现翻转)
            personRect.right = tempBottom; // 新 right = 旧 bottom
            personRect.bottom = previewHeight - tempLeft; // 新 bottom = 预览高 - 旧 left (实现翻转)
        }


        // 绘制圆角矩形的人物框
        canvas.drawRoundRect(personRect, 25, 25, personBoxPaint);

        // 绘制人物标签 (例如 "Person")
        String label = "Person";
        float textWidth = personTextPaint.measureText(label); // 测量文本宽度
        canvas.drawText(
                label,
                personRect.centerX() - textWidth / 2, // 文本中心与框中心对齐
                personRect.top - 30, // 绘制在人物框上方，偏移30像素
                personTextPaint
        );

        // 原有表情分析逻辑
        String mood = analyzeFacialExpression(targetFace);
        currentMood = mood;

        // 绘制人脸轮廓。
        // 注意：这里的 offsetX 和 offsetY 被硬编码为 50,50，这意味着轮廓会绘制到屏幕的固定左上角位置，
        // 而不是跟随视频中实际人脸的位置。这在演示或特定调试场景下可能有用，但并非通用实时对齐方案。
        drawFaceContours(canvas, targetFace, 50, 50);

        // 绘制信息面板。
        // 注意：这个信息面板的绘制位置同样是固定的，依赖于 imageWidth/imageHeight 乘缩放因子后，
        // 加上硬编码的偏移量。
        drawInfoPanel(canvas, targetFace, 50, 50);

        postInvalidate(); // 请求下一次重绘，实现连续动画或更新效果
    }

    /**
     * 分析人脸表情并返回一个代表情绪的字符串（表情符号）。
     * @param face 检测到的人脸对象
     * @return 对应的表情符号
     */
    private String analyzeFacialExpression(Face face) {
        String mood = "😐"; // 默认表情为中性

        // 判断微笑概率
        if (face.getSmilingProbability() != null) {
            float smileProb = face.getSmilingProbability();
            if (smileProb > 0.8) mood = "😄"; // 很高兴
            else if (smileProb > 0.3) mood = "🙂"; // 有点高兴
        }

        // 判断眼睛睁开概率 (如果双眼都闭着，则表示眨眼或闭眼)
        if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
            float leftEye = face.getLeftEyeOpenProbability();
            float rightEye = face.getRightEyeOpenProbability();
            if (leftEye < 0.3 && rightEye < 0.3) {
                mood = "😉"; // 眨眼或闭眼
            }
        }
        return mood;
    }

    /**
     * 绘制人脸的所有轮廓点和连接线。
     * @param canvas 用于绘制的画布
     * @param face 人脸对象，包含轮廓数据
     * @param offsetX X轴的固定偏移量 (请注意，这会使轮廓绘制到固定位置)
     * @param offsetY Y轴的固定偏移量 (请注意，这会使轮廓绘制到固定位置)
     */
    private void drawFaceContours(Canvas canvas, Face face, float offsetX, float offsetY) {
        for (FaceContour contour : face.getAllContours()) {
            // 根据轮廓类型设置不同的颜色
            switch (contour.getFaceContourType()) {
                case FaceContour.FACE:
                    contourPaint.setColor(Color.WHITE);
                    break;
                case FaceContour.LEFT_EYE:
                case FaceContour.RIGHT_EYE:
                    contourPaint.setColor(Color.CYAN);
                    break;
                case FaceContour.LEFT_EYEBROW_TOP:
                case FaceContour.RIGHT_EYEBROW_TOP:
                case FaceContour.LEFT_EYEBROW_BOTTOM:
                case FaceContour.RIGHT_EYEBROW_BOTTOM:
                    contourPaint.setColor(Color.BLUE);
                    break;
                case FaceContour.NOSE_BRIDGE:
                case FaceContour.NOSE_BOTTOM:
                    contourPaint.setColor(Color.RED);
                    break;
                case FaceContour.UPPER_LIP_TOP:
                case FaceContour.UPPER_LIP_BOTTOM:
                case FaceContour.LOWER_LIP_TOP:
                case FaceContour.LOWER_LIP_BOTTOM:
                    contourPaint.setColor(Color.MAGENTA);
                    break;
                default:
                    contourPaint.setColor(Color.YELLOW);
            }

            // 绘制轮廓上的每一个点
            for (PointF point : contour.getPoints()) {
                // 将ML Kit返回的点坐标缩放并加上固定偏移量
                float x = point.x * scaleX + offsetX;
                float y = point.y * scaleY + offsetY;
                canvas.drawCircle(x, y, 3f, contourPaint); // 绘制一个小圆点

                // 如果轮廓有多个点，则连接它们形成线条
                if (contour.getPoints().size() > 1) {
                    Paint linePaint = new Paint(contourPaint); // 创建一个拷贝的画笔
                    linePaint.setStyle(Paint.Style.STROKE); // 设置为描边
                    linePaint.setStrokeWidth(2f); // 设置描边宽度

                    List<PointF> points = contour.getPoints();
                    for (int i = 0; i < points.size() - 1; i++) {
                        PointF p1 = points.get(i);
                        PointF p2 = points.get(i + 1);
                        // 转换每个点的坐标
                        float x1 = p1.x * scaleX + offsetX;
                        float y1 = p1.y * scaleY + offsetY;
                        float x2 = p2.x * scaleX + offsetX;
                        float y2 = p2.y * scaleY + offsetY;
                        canvas.drawLine(x1, y1, x2, y2, linePaint); // 绘制点之间的线段
                    }
                }
            }
        }
    }

    /**
     * 在屏幕上绘制一个信息面板，显示当前的表情和一些概率值。
     * @param canvas 用于绘制的画布
     * @param face 人脸对象，用于获取概率值
     * @param offsetX X轴的固定偏移量 (请注意，这会使面板绘制到固定位置)
     * @param offsetY Y轴的固定偏移量，计算位置时会用到图像高度 (请注意，这会使面板绘制到固定位置)
     */
    private void drawInfoPanel(Canvas canvas, Face face, float offsetX, float offsetY) {
        // 计算信息面板的背景矩形位置
        float bgLeft = offsetX;
        float bgTop = offsetY + imageHeight * scaleY + 20; // 放置在“图像”下方，偏移20像素
        float bgRight = bgLeft + 300; // 固定宽度300
        float bgBottom = bgTop + 150; // 固定高度150
        // 绘制圆角矩形背景
        canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, 20, 20, emojiBgPaint);

        // 绘制当前表情符号
        canvas.drawText(currentMood, bgLeft + 20, bgTop + 50, textPaint);
        // 缩小字体大小，用于显示详细概率
        textPaint.setTextSize(30f);

        // 绘制微笑概率
        if (face.getSmilingProbability() != null) {
            String smileText = String.format("微笑指数: %.0f%%", face.getSmilingProbability() * 100);
            canvas.drawText(smileText, bgLeft + 20, bgTop + 100, textPaint);
        }

        // 绘制眼睛睁开的平均概率
        if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
            String eyeText = String.format("眼睛睁开: %.0f%%",
                    (face.getLeftEyeOpenProbability() + face.getRightEyeOpenProbability()) * 50); // 左右眼概率取平均值
            canvas.drawText(eyeText, bgLeft + 20, bgTop + 140, textPaint);
        }
    }
}