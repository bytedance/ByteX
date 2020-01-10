package com.ss.android.ugc.bytex.example.accessinline;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

public class TextViewForTest extends TextView {
    public static final String TAG = TextViewForTest.class.getSimpleName();

    public TextViewForTest(Context context) {
        super(context);
        Inner inner = new Inner();
        inner.getProtectedFieldFromSuper();
        inner.invokeProtectedMethodFromSuper();
    }

    class Inner {
        void invokeProtectedMethodFromSuper() {
            // This case should skip.
            assert !getDefaultEditable();
            Log.d(TAG, "getDefaultEditable = " + getDefaultEditable());
        }

        void getProtectedFieldFromSuper() {
            assert VIEW_LOG_TAG.equals("View");
            Log.d(TAG, "VIEW_LOG_TAG: " + VIEW_LOG_TAG);
        }
    }
}
