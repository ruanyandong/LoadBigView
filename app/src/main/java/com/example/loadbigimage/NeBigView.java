package com.example.loadbigimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;
import java.io.IOException;
import java.io.InputStream;

public class NeBigView extends View implements GestureDetector.OnGestureListener,View.OnTouchListener{

    private final Rect mRect;
    private final BitmapFactory.Options mOptions;
    private final GestureDetector mGestureDetector;
    private final Scroller mScroller;
    private int mImageWidth;
    private int mImageHeight;
    private BitmapRegionDecoder mDecoder;
    private int mViewWidth;
    private int mViewHeight;
    private float mScale;
    private Bitmap mBitmap;

    public NeBigView(Context context) {
        this(context,null);
    }

    public NeBigView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public NeBigView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 第一步：设置一些BigView所需要的成员变量
        mRect = new Rect();
        // 需要做内存复用
        mOptions = new BitmapFactory.Options();
        // 手势识别
        mGestureDetector = new GestureDetector(context,this);
        // 滚动类
        mScroller = new Scroller(context);

        setOnTouchListener(this);
    }

    // 第2步：设置图片,得到图片信息，并且，由于我们加载图片的时候，只加载一部分
    public void setImage(InputStream is){
        // 获取图片宽和高，又不能将整个图片加载进内存
        mOptions.inJustDecodeBounds = true;
        // 不把图片加载进内存得到宽和高
        BitmapFactory.decodeStream(is,null,mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;
        Log.d("ruanyandong", "setImage: mImageWidth = "+mImageWidth);
        Log.d("ruanyandong", "setImage: mImageHeight = "+mImageHeight);
        // 开启复用
        mOptions.inMutable = true;
        // 设置格式为RGB565
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        mOptions.inJustDecodeBounds = false;

        // 创建一个区域解码器
        try {
            mDecoder = BitmapRegionDecoder.newInstance(is,false);// false表示不共享解码器
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();
    }

    // 第三步：开始测量，测量我们图片到底要缩放成什么样子
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();
        Log.d("ruanyandong", "onMeasure: mViewWidth = "+mViewWidth);
        Log.d("ruanyandong", "onMeasure: mViewHeight = "+mViewHeight);
        // 确定加载图片的区域
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = mImageWidth;
        // 得到图片的宽度，又知道View的宽度，计算缩放因子
        mScale = mViewWidth/(float)mImageWidth;
        mRect.bottom = (int) (mViewHeight/mScale);

        Log.d("ruanyandong", "onMeasure: mScale = "+mScale);
        Log.d("ruanyandong", "onMeasure: mRect.bottom = "+mRect.bottom);
    }

    // 第四步：画出具体内容
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 解码器没拿到，表示没有设置过要显示的图片
        if (mDecoder == null){
            return;
        }
        // 复用内存 1:复用的bitmap必须跟即将解码的bitmap尺寸一样
        mOptions.inBitmap = mBitmap;
        // 指定解码的区域
        mBitmap = mDecoder.decodeRegion(mRect,mOptions);
        // 得到一个矩阵进行缩放，相当于得到view的大小
        Matrix matrix = new Matrix();
        matrix.setScale(mScale,mScale);
        canvas.drawBitmap(mBitmap,matrix,null);
    }

    // 第5步，处理点击事件
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 直接将事件交给手势事件处理
        return mGestureDetector.onTouchEvent(event);
    }

    // 第六步：手按下去
    @Override
    public boolean onDown(MotionEvent e) {
        // 如果移动没有停止，就强行停止
        if (!mScroller.isFinished()){
            mScroller.forceFinished(true);
        }
        // 继续接收后续事件
        return true;
    }

    // 第七步，处理滑动事件
    // e1：开始事件，手指按下去，开始获取坐标
    // e2：获取当前事件坐标
    // x y 轴移动的距离
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // 上下移动的时候，mRect需要改变显示区域
        mRect.offset(0,(int)distanceY);
        // 移动时，处理到达顶部和底部的情况
        if (mRect.bottom > mImageHeight){
            Log.d("ruanyandong", "onScroll:bottom ====mRect.bottom = "+mRect.bottom);
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight -(int)(mViewHeight/mScale);
            Log.d("ruanyandong", "onScroll:bottom ====mRect.top = "+mRect.top);
        }
        if (mRect.top < 0){
            Log.d("ruanyandong", "onScroll:top ====mRect.top = "+mRect.top);
            mRect.top = 0;
            mRect.bottom = (int)(mViewHeight/mScale);
        }
        invalidate();
        return false;
    }

    // 第8步，处理惯性问题
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mScroller.fling(0,mRect.top,0,(int)-velocityY,0,0,0,mImageHeight-(int)(mViewHeight/mScale));
        return false;
    }

    // 第九步，处理计算结果
    @Override
    public void computeScroll() {
        if (mScroller.isFinished()){
            return;
        }
        // 返回为true的时候，滑动还没有结果
        if (mScroller.computeScrollOffset()){
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + (int)(mViewHeight/mScale);
            invalidate();
        }
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e) {

    }

}
