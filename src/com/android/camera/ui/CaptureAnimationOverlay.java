/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.android.camera.util.Gusterpolator;
import com.android.camera2.R;

/**
 * This class handles all the animations at capture time. Post capture animations
 * will be handled in a separate place.
 */
public class CaptureAnimationOverlay extends View {
    private final static String TAG = "CaptureAnimationOverlay";

    private final static int FLASH_CIRCLE_SHRINK_DURATION_MS = 200;
    private final static int FLASH_CIRCLE_SLIDE_DURATION_MS = 400;
    private final static int FLASH_CIRCLE_SLIDE_START_DELAY_MS = 0;
    private final static int FLASH_ALPHA_BEFORE_SHRINK = 180;
    private final static int FLASH_ALPHA_AFTER_SHRINK = 50;
    private final static int FLASH_COLOR = Color.WHITE;

    private int mWidth;
    private int mHeight;
    private int mFlashCircleCenterX;
    private int mFlashCircleCenterY;
    private int mFlashCircleRadius = 0;
    private AnimatorSet mFlashAnimation;
    private final Paint mPaint = new Paint();
    private final int mFlashCircleSizeAfterShrink;

    public CaptureAnimationOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(FLASH_COLOR);
        mFlashCircleSizeAfterShrink = getResources()
                .getDimensionPixelSize(R.dimen.flash_circle_size_after_shrink);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top;
    }

    /**
     * Start flash animation with the circle of the flash being at position (x, y).
     *
     * @param x position on x axis
     * @param y position on y axis
     */
    public void startFlashAnimation(int x, int y) {
        if (mFlashAnimation != null && mFlashAnimation.isRunning()) {
            mFlashAnimation.cancel();
        }

        mFlashCircleCenterX = x;
        mFlashCircleCenterY = y;

        // Flash circle shrink.
        final ValueAnimator alphaAnimator = ValueAnimator.ofInt(FLASH_ALPHA_BEFORE_SHRINK,
                FLASH_ALPHA_AFTER_SHRINK);
        alphaAnimator.setDuration(FLASH_CIRCLE_SHRINK_DURATION_MS);

        int startingRadius = (int) Math.sqrt(mWidth * mWidth + mHeight * mHeight) / 2;
        final ValueAnimator radiusAnimator = ValueAnimator.ofInt(startingRadius,
                mFlashCircleSizeAfterShrink);
        radiusAnimator.setDuration(FLASH_CIRCLE_SHRINK_DURATION_MS);
        radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFlashCircleRadius = (Integer) radiusAnimator.getAnimatedValue();
                int alpha = (Integer) alphaAnimator.getAnimatedValue();
                mPaint.setAlpha(alpha);
                invalidate();
            }
        });

        // Flash circle slide to the right edge of the screen.
        int endPositionX = mWidth + mFlashCircleSizeAfterShrink;
        final ValueAnimator slideAnimatorX = ValueAnimator.ofInt(mFlashCircleCenterX, endPositionX);
        slideAnimatorX.setDuration(FLASH_CIRCLE_SLIDE_DURATION_MS);
        slideAnimatorX.setStartDelay(FLASH_CIRCLE_SLIDE_START_DELAY_MS);

        final ValueAnimator slideAnimatorY = ValueAnimator.ofInt(y, mHeight / 2);
        slideAnimatorY.setDuration(FLASH_CIRCLE_SLIDE_DURATION_MS);
        slideAnimatorY.setStartDelay(FLASH_CIRCLE_SLIDE_START_DELAY_MS);
        slideAnimatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFlashCircleCenterX = (Integer) slideAnimatorX.getAnimatedValue();
                mFlashCircleCenterY = (Integer) slideAnimatorY.getAnimatedValue();
                invalidate();
            }
        });

        mFlashAnimation = new AnimatorSet();
        mFlashAnimation.play(alphaAnimator)
                .with(radiusAnimator)
                .with(slideAnimatorX)
                .with(slideAnimatorY);
        mFlashAnimation.setInterpolator(Gusterpolator.INSTANCE);
        mFlashAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFlashAnimation = null;
                setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mFlashAnimation.start();
    }

    /**
     * Start flash animation in the center of the screen.
     */
    public void startFlashAnimation() {
        int x = mWidth / 2;
        int y = mHeight / 2;
        startFlashAnimation(x, y);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mFlashAnimation != null && mFlashAnimation.isRunning()) {
            canvas.drawCircle(mFlashCircleCenterX, mFlashCircleCenterY, mFlashCircleRadius, mPaint);
        }
    }
}