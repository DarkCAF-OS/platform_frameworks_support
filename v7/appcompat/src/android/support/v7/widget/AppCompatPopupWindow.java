/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StyleRes;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.PopupWindow;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

class AppCompatPopupWindow extends PopupWindow {

    private static final String TAG = "AppCompatPopupWindow";
    private static final boolean COMPAT_OVERLAP_ANCHOR = Build.VERSION.SDK_INT < 21;

    private boolean mOverlapAnchor;

    public AppCompatPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(11)
    public AppCompatPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.PopupWindow, defStyleAttr, defStyleRes);
        if (a.hasValue(R.styleable.PopupWindow_overlapAnchor)) {
            setSupportOverlapAnchor(a.getBoolean(R.styleable.PopupWindow_overlapAnchor, false));
        }
        // We re-set this for tinting purposes
        setBackgroundDrawable(a.getDrawable(R.styleable.PopupWindow_android_popupBackground));

        final int sdk = Build.VERSION.SDK_INT;
        if (defStyleRes != 0 && sdk < 11) {
            // If we have a defStyleRes, but we're on < API 11, we need to manually set attributes
            // from the style
            // android:popupAnimationStyle was added in API 9
            if (a.hasValue(R.styleable.PopupWindow_android_popupAnimationStyle)) {
                setAnimationStyle(a.getResourceId(
                        R.styleable.PopupWindow_android_popupAnimationStyle, -1));
            }
        }

        a.recycle();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // For devices pre-ICS, we need to wrap the internal OnScrollChangedListener
            // due to NPEs.
            wrapOnScrollChangedListener(this);
        }
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        if (COMPAT_OVERLAP_ANCHOR && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.showAsDropDown(anchor, xoff, yoff);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        if (COMPAT_OVERLAP_ANCHOR && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.showAsDropDown(anchor, xoff, yoff, gravity);
    }

    @Override
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        if (COMPAT_OVERLAP_ANCHOR && mOverlapAnchor) {
            // If we're pre-L, emulate overlapAnchor by modifying the yOff
            yoff -= anchor.getHeight();
        }
        super.update(anchor, xoff, yoff, width, height);
    }

    private static void wrapOnScrollChangedListener(final PopupWindow popup) {
        try {
            final Field fieldAnchor = PopupWindow.class.getDeclaredField("mAnchor");
            fieldAnchor.setAccessible(true);

            final Field fieldListener = PopupWindow.class
                    .getDeclaredField("mOnScrollChangedListener");
            fieldListener.setAccessible(true);

            final OnScrollChangedListener originalListener =
                    (OnScrollChangedListener) fieldListener.get(popup);

            // Now set a new listener, wrapping the original and only proxying the call when
            // we have an anchor view.
            fieldListener.set(popup, new OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    try {
                        WeakReference<View> mAnchor = (WeakReference<View>) fieldAnchor.get(popup);
                        if (mAnchor == null || mAnchor.get() == null) {
                            return;
                        } else {
                            originalListener.onScrollChanged();
                        }
                    } catch (IllegalAccessException e) {
                        // Oh well...
                    }
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "Exception while installing workaround OnScrollChangedListener", e);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(GROUP_ID)
    public void setSupportOverlapAnchor(boolean overlapAnchor) {
        if (COMPAT_OVERLAP_ANCHOR) {
            mOverlapAnchor = overlapAnchor;
        } else {
            PopupWindowCompat.setOverlapAnchor(this, overlapAnchor);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(GROUP_ID)
    public boolean getSupportOverlapAnchor() {
        if (COMPAT_OVERLAP_ANCHOR) {
            return mOverlapAnchor;
        } else {
            return PopupWindowCompat.getOverlapAnchor(this);
        }
    }

}
