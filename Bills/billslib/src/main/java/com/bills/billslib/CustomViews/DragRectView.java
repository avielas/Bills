package com.bills.billslib.CustomViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DragRectView extends View {

    private Paint mCircePaint;
    private Paint mRectPaint;

    public Point TopLeft;
    public Point TopRight;
    public Point ButtomLeft;
    public Point ButtomRight;

    private Point mSelectedPoint;

    private boolean mFirstCall = true;

    private int mStartX = 0;
    private int mStartY = 0;
    private int mEndX = 0;
    private int mEndY = 0;
    private boolean mDrawRect = false;
    private TextPaint mTextPaint = null;

    private OnUpCallback mCallback = null;

    public interface OnUpCallback {
        void onRectFinished(Rect rect);
    }

    public DragRectView(final Context context) {
        super(context);

        init();
    }

    public DragRectView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragRectView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Sets callback for up
     *
     * @param callback {@link OnUpCallback}
     */
    public void setOnUpCallback(OnUpCallback callback) {
        mCallback = callback;
    }

    /**
     * Inits internal data
     */
    private void init() {
        mRectPaint = new Paint();
        mRectPaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(5); // TODO: should take from resources

        mCircePaint = new Paint();
        mCircePaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mCircePaint.setStrokeWidth(10);
        mCircePaint.setStyle(Paint.Style.FILL);
        mCircePaint.setAntiAlias(true);

        mTextPaint = new TextPaint();
        mTextPaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mTextPaint.setTextSize(20);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {

        final int x = (int) event.getX();
        final int y = (int) event.getY();
        // TODO: be aware of multi-touches
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                mSelectedPoint = GetSelectedPoint((int) event.getX(), (int) event.getY());
                if(mSelectedPoint == null){
                    break;
                }
                mDrawRect = false;

                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                if(mSelectedPoint == null){
                    break;
                }

                if ((!mDrawRect || Math.abs(x - mSelectedPoint.x) > 5 || Math.abs(y - mSelectedPoint.y) > 5)) {
                    mSelectedPoint.x = x;
                    mSelectedPoint.y = y;
                    invalidate();
                }

                mDrawRect = true;
                break;

            case MotionEvent.ACTION_UP:
                if(mSelectedPoint == null){
                    break;
                }

                if (mCallback != null) {
                    mCallback.onRectFinished(new Rect(Math.min(mStartX, mEndX), Math.min(mStartY, mEndY),
                            Math.max(mEndX, mStartX), Math.max(mEndY, mStartX)));
                }
                if(IsValidMove(x,y)){
                    mSelectedPoint.x = x;
                    mSelectedPoint.y = y;
                }
                invalidate();
                break;

            default:
                break;
        }

        return true;
    }

    private boolean IsValidMove(int x, int y) {
        //check if new position too close to one of other points
        if(mSelectedPoint != TopLeft && GetDistance(x, y, TopLeft.x, TopLeft.y) < 50){
            return false;
        }
        if(mSelectedPoint != TopRight && GetDistance(x, y, TopRight.x, TopRight.y) < 50){
            return false;
        }
        if(mSelectedPoint != ButtomRight && GetDistance(x, y, ButtomRight.x, ButtomRight.y) < 50){
            return false;
        }
        if(mSelectedPoint != ButtomLeft && GetDistance(x, y, ButtomLeft.x, ButtomLeft.y) < 50){
            return false;
        }

//        check if new angles will be higher than 180deg
        if(ValidateP3AboveLine(TopLeft, ButtomRight, TopRight) ||
                !ValidateP3AboveLine(TopLeft, ButtomRight, ButtomLeft) ||
                ValidateP3AboveLine(TopRight, ButtomLeft, TopLeft) ||
                !ValidateP3AboveLine(TopRight, ButtomLeft, ButtomRight)){
            return false;
        }

        return true;
    }

    private double GetDistance(int x1, int y1, int x2, int y2){
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1- y2, 2));
    }

    private boolean ValidateP3AboveLine(final Point p1, final Point p2, final Point p3){
        double m = (1.0 * p1.y - p2.y)/(1.0 * p1.x - p2.x);
        double b = p1.y - p1.x * m;

        if(p3.y > m * p3.x + b + 50){
            return true;
        }
        return false;
    }

    private Point GetSelectedPoint(int x, int y) {
        int radius = 30;
        if(Math.abs(TopLeft.x - x) < radius && Math.abs(TopLeft.y - y) < radius){
            return TopLeft;
        }
        if(Math.abs(TopRight.x - x) < radius && Math.abs(TopRight.y - y) < radius){
            return TopRight;
        }
        if(Math.abs(ButtomLeft.x - x) < radius && Math.abs(ButtomLeft.y - y) < radius){
            return ButtomLeft;
        }
        if(Math.abs(ButtomRight.x - x) < radius && Math.abs(ButtomRight.y - y) < radius){
            return ButtomRight;
        }
        return null;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if(mFirstCall){
            if(TopLeft == null || TopRight == null ||
                    ButtomRight == null || ButtomLeft == null) {
                TopLeft = new Point(canvas.getWidth() / 3, canvas.getHeight() / 3);
                TopRight = new Point(2 * canvas.getWidth() / 3, canvas.getHeight() / 3);
                ButtomLeft = new Point(canvas.getWidth() / 3, 2 * canvas.getHeight() / 3);
                ButtomRight = new Point(2 * canvas.getWidth() / 3, 2 * canvas.getHeight() / 3);
            }

            canvas.drawLine(TopLeft.x, TopLeft.y, TopRight.x, TopRight.y, mRectPaint);
            canvas.drawLine(TopRight.x, TopRight.y, ButtomRight.x, ButtomRight.y, mRectPaint);
            canvas.drawLine(ButtomRight.x, ButtomRight.y, ButtomLeft.x, ButtomLeft.y, mRectPaint);
            canvas.drawLine(ButtomLeft.x, ButtomLeft.y, TopLeft.x, TopLeft.y, mRectPaint);

            canvas.drawCircle(TopLeft.x, TopLeft.y, 20, mCircePaint);
            canvas.drawCircle(TopRight.x, TopRight.y, 20, mCircePaint);
            canvas.drawCircle(ButtomRight.x, ButtomRight.y, 20, mCircePaint);
            canvas.drawCircle(ButtomLeft.x, ButtomLeft.y, 20, mCircePaint);

            mFirstCall = false;

        }
        else if (mDrawRect) {
            canvas.drawLine(TopLeft.x, TopLeft.y, TopRight.x, TopRight.y, mRectPaint);
            canvas.drawLine(TopRight.x, TopRight.y, ButtomRight.x, ButtomRight.y, mRectPaint);
            canvas.drawLine(ButtomRight.x, ButtomRight.y, ButtomLeft.x, ButtomLeft.y, mRectPaint);
            canvas.drawLine(ButtomLeft.x, ButtomLeft.y, TopLeft.x, TopLeft.y, mRectPaint);

            canvas.drawCircle(TopLeft.x, TopLeft.y, 20, mCircePaint);
            canvas.drawCircle(TopRight.x, TopRight.y, 20, mCircePaint);
            canvas.drawCircle(ButtomRight.x, ButtomRight.y, 20, mCircePaint);
            canvas.drawCircle(ButtomLeft.x, ButtomLeft.y, 20, mCircePaint);
        }
    }

    @Override
    public void setBackground(Drawable background) {
        final int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            super.setBackgroundDrawable(background);
        }
        else {
            mFirstCall = true;
            super.setBackground(background);
        }
        invalidate();
    }
}