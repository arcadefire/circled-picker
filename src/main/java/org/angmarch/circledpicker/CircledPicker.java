package org.angmarch.circledpicker;

/**
 * Created by Angelo Marchesin on 15/05/14.
 */

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

@SuppressWarnings("unused")
public class CircledPicker extends View {

    private static final boolean IS_API_10 = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    private static final int VALUE_THRESHOLD = 270;
    private static final int TURN = 360;
    private static final int HALF = 180;
    private static final int TURN_THRESHOLD = 300;
    private static final int DURATION = 200;
    private static final int FULL_OPACITY = 255;
    private static final int ACUTE = 60;
    private static final int THREE_QUARTER = 270;
    private static final float PERCENT = 100f;
    private static final float ALMOST_TURN = 359.99f;
    private static final float RADIUS_MULTIPLIER = .3f;
    private static final float REMAINDER = 0.01f;
    private static final String HOURS = "hours";
    private static final String MINUTES = "minutes";
    private static final String NUMERIC = "numeric";

    private ValueAnimator mAngleAnimator;
    private RectF mArcRect;
    private RectF mArcInnerRect;
    private RectF mShadowRect;
    private RectF mShadowInnerRect;
    private Paint mPaint;
    private Path mPath;
    private Rect mTextBounds;
    private PickerMode mPickerMode;
    private float mCurrentSweep;
    private float mCurrentValue;
    private float mLastAngle;
    private float mDownX;
    private float mMaxValue;
    private float mStep;
    private float mTextSize;
    private boolean mIsFilled;
    private boolean mIsEmpty;
    private int mMidX;
    private int mMidY;
    private int mThickness;
    private int mInnerThickness;
    private int mTouchSlop;
    private int mRadius;
    private int mInnerRadius;
    private int mLineColor;
    private int mSubLineColor;
    private int mTextColor;

    public enum PickerMode {
        HOURS_AND_SECONDS,
        TIME_OF_DAY,
        PERCENT,
        NUMERIC
    }

    public CircledPicker(Context context) {
        super(context);
    }

    public CircledPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircledPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void animateToValue(float value) {
        animateChange((value * TURN) / mMaxValue);
    }

    public static float getAngle(Point target, Point origin) {
        float angle = (float) Math.toDegrees(Math.atan2(target.x - origin.x, target.y - origin.y)) + HALF;
        if (angle < 0) {
            angle += TURN;
        }
        return TURN - angle;
    }

    public int getMultiply(float value) {
        return (int) (value - (value % mStep));
    }

    public int getThickness() {
        return mThickness;
    }

    @Override
    public void onDraw(Canvas canvas) {
        setCirclesBoundingBoxes();
        drawBackground(canvas);
        drawShadow(canvas);
        drawPickerCircle(canvas);
        drawCenteredText(canvas);
    }

    private void updateCirle(float angle) {
        mCurrentSweep = angle;

        if (mCurrentSweep - mLastAngle < -VALUE_THRESHOLD && !mIsFilled && !mIsEmpty) {
            mIsFilled = true;
        } else if (mCurrentSweep - mLastAngle > VALUE_THRESHOLD && !mIsEmpty && !mIsFilled) {
            mIsEmpty = true;
        }

        if (mCurrentSweep < TURN && mCurrentSweep > TURN_THRESHOLD && mIsFilled) {
            mIsFilled = false;
        } else if (mCurrentSweep < ACUTE && mCurrentSweep > 0 && mIsEmpty) {
            mIsEmpty = false;
        }

        if (mIsFilled) {
            mCurrentSweep = ALMOST_TURN;
        } else if (mIsEmpty) {
            mCurrentSweep = 0;
        } else {
            mLastAngle = mCurrentSweep;
        }

        mCurrentValue = getMultiply(((mCurrentSweep + REMAINDER) * mMaxValue) / TURN);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float angle = getAngle(new Point((int) event.getX(), (int) event.getY()),
                new Point(mMidX, mMidY));
        if (isClickable()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(mDownX - event.getX()) > mTouchSlop &&
                            (mAngleAnimator != null && !mAngleAnimator.isRunning())) {
                        if (!IS_API_10) {
                            updateCirle(angle);
                            postInvalidate();
                        }
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    mDownX = event.getX();
                    mIsFilled = mIsEmpty = false;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (Math.abs(mDownX - event.getX()) < mTouchSlop) {
                        if (!IS_API_10) {
                            animateChange(angle);
                            mIsFilled = mIsEmpty = false;
                        }
                    }
                    break;
            }
        }
        return true;
    }

    public void setThickness(int thickness) {
        mThickness = thickness;
        postInvalidate();
    }

    public void setValue(float value) {
        mCurrentValue = value;
        mCurrentSweep = ((value * TURN) / mMaxValue);
    }

    private static String addAZeroIfNeeds(int value) {
        return value > 9 ? value + "" : "0" + value;
    }

    private void animateChange(float finalAngle) {
        if (!IS_API_10) {
            if (mAngleAnimator.isRunning()) {
                mAngleAnimator.end();
            }
            mAngleAnimator.setFloatValues(mLastAngle, finalAngle);
            mAngleAnimator.setDuration(DURATION);
            mAngleAnimator.setInterpolator(new DecelerateInterpolator());
            mAngleAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentSweep = (Float) animation.getAnimatedValue();
                    mCurrentValue = getMultiply((mCurrentSweep * mMaxValue) / TURN);
                    postInvalidate();
                }
            });
            mAngleAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    mLastAngle = mCurrentSweep;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mLastAngle = mCurrentSweep;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }
            });
            mAngleAnimator.start();
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setAlpha(FULL_OPACITY);
    }

    private void drawCenteredText(Canvas canvas) {
        String centerLabel = "";

        mPaint.reset();
        mPaint.setAlpha(FULL_OPACITY);
        mPaint.setShader(null);
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);

        switch (mPickerMode) {
            case TIME_OF_DAY:
                centerLabel = getHoursString() + ':' + getMinutesString();
                break;
            case HOURS_AND_SECONDS:
                centerLabel = getHoursString() + "h " + getMinutesString() + 'm';
                break;
            case PERCENT:
                centerLabel = getPercentString();
                break;
            case NUMERIC:
                centerLabel = String.valueOf((int) mCurrentValue);
                break;
        }

        mPaint.getTextBounds(centerLabel, 0, centerLabel.length(), mTextBounds);

        float halfWidth = (float) mTextBounds.width() / 2;
        float textVerticalOffset = mMidY + ((float) mTextBounds.height() / 2);
        canvas.drawText(centerLabel, mMidX - halfWidth, textVerticalOffset, mPaint);
    }

    private String getPercentString() {
        int percent = (int) ((mCurrentValue / mMaxValue) * PERCENT);
        return percent + "%";
    }

    private void drawPickerCircle(Canvas canvas) {
        mPath.reset();
        mPath.arcTo(mArcRect, THREE_QUARTER, mCurrentSweep);
        mPath.arcTo(mArcInnerRect, THREE_QUARTER + mCurrentSweep, -mCurrentSweep);
        mPath.close();
        mPaint.setColor(mLineColor);
        canvas.drawPath(mPath, mPaint);
    }

    private void drawShadow(Canvas canvas) {
        mPath.arcTo(mShadowRect, 0, ALMOST_TURN);
        mPath.arcTo(mShadowInnerRect, ALMOST_TURN, -ALMOST_TURN);
        mPath.close();
        mPaint.setColor(mSubLineColor);
        canvas.drawPath(mPath, mPaint);
    }

    private String getMinutesString() {
        int hour = (int) (mCurrentValue / ACUTE);
        int minutes = (int) (mCurrentValue - (hour * ACUTE));
        return addAZeroIfNeeds(minutes);
    }

    private String getHoursString() {
        int hour = (int) (mCurrentValue / ACUTE);
        return addAZeroIfNeeds(hour);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircledPicker);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        Resources resources = getResources();
        String pickerMode;

        mLineColor = typedArray.getColor(R.styleable.CircledPicker_lineColor, resources.getColor(R.color.defaultLineColor));
        mSubLineColor = typedArray.getColor(R.styleable.CircledPicker_subLineColor, resources.getColor(R.color
                .defaultSubLineColor));
        mTextColor = typedArray.getColor(R.styleable.CircledPicker_textColor, resources.getColor(R.color.defaultLineColor));
        mStep = typedArray.getInteger(R.styleable.CircledPicker_step, 1);
        mMaxValue = typedArray.getInteger(R.styleable.CircledPicker_maxValue, 100);
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.CircledPicker_textSize, 0);
        mThickness = typedArray.getDimensionPixelSize(R.styleable.CircledPicker_outerThickness,
                (int) MeasureUtils.convertDpToPixel(context, 5));
        mInnerThickness = mThickness - typedArray.getDimensionPixelSize(R.styleable.CircledPicker_innerThickness,
                (int) MeasureUtils.convertDpToPixel(context, 1));
        pickerMode = typedArray.getString(R.styleable.CircledPicker_pickerMode);

        if (pickerMode != null) {
            mPickerMode = pickerMode.equalsIgnoreCase(HOURS) ? PickerMode.HOURS_AND_SECONDS :
                    pickerMode.equalsIgnoreCase(MINUTES) ? PickerMode.TIME_OF_DAY :
                            pickerMode.equalsIgnoreCase(NUMERIC) ? PickerMode.NUMERIC :
                                    PickerMode.PERCENT;
        } else {
            mPickerMode = PickerMode.NUMERIC;
        }

        mLastAngle = mCurrentSweep;
        mShadowRect = new RectF();
        mShadowInnerRect = new RectF();
        mTextBounds = new Rect();
        mArcRect = new RectF();
        mArcInnerRect = new RectF();
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mAngleAnimator = ValueAnimator.ofFloat(0, 0);
        mPaint = new Paint();
        mPath = new Path();

        typedArray.recycle();

        setClickable(true);
    }

    private void setCirclesBoundingBoxes() {
        mPath.reset();
        // Set the shadow circle's bounding box
        mShadowRect.left = mMidX - mRadius + mInnerThickness;
        mShadowRect.top = mMidY - mRadius + mInnerThickness;
        mShadowRect.right = mMidX + mRadius - mInnerThickness;
        mShadowRect.bottom = mMidY + mRadius - mInnerThickness;
        // Set the picker circle's bounding box
        mShadowInnerRect.left = mMidX - mInnerRadius - mInnerThickness;
        mShadowInnerRect.top = mMidY - mInnerRadius - mInnerThickness;
        mShadowInnerRect.right = mMidX + mInnerRadius + mInnerThickness;
        mShadowInnerRect.bottom = mMidY + mInnerRadius + mInnerThickness;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMidX = MeasureSpec.getSize(widthMeasureSpec) / 2;
        mMidY = MeasureSpec.getSize(heightMeasureSpec) / 2;

        if (mMidX < mMidY) {
            mRadius = mMidX;
        } else {
            mRadius = mMidY;
        }

        if (mTextSize == 0) {
            mTextSize = mRadius * RADIUS_MULTIPLIER;
        }
        mInnerRadius = mRadius - mThickness;
        mArcRect.left = mMidX - mRadius;
        mArcRect.top = mMidY - mRadius;
        mArcRect.right = mMidX + mRadius;
        mArcRect.bottom = mMidY + mRadius;
        mArcInnerRect.left = mMidX - mInnerRadius;
        mArcInnerRect.top = mMidY - mInnerRadius;
        mArcInnerRect.right = mMidX + mInnerRadius;
        mArcInnerRect.bottom = mMidY + mInnerRadius;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
