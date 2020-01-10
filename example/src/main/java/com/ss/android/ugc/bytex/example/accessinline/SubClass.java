package com.ss.android.ugc.bytex.example.accessinline;

import android.util.Log;

import com.ss.android.ugc.bytex.example.accessinline.other.Ancestor0;
import com.ss.android.ugc.bytex.example.accessinline.other.SuperClassInOtherPackage;

public class SubClass extends SuperClassInOtherPackage {
    public static final String TAG = SubClass.class.getSimpleName();
    private int count = 0;

    private void packageMethod() {
        Log.d(TAG, "packageMethod() called");
    }

    public void privateMethod() {
        Log.d(TAG, "packageMethod() called, this should not override the parent method.");
    }

    public SubClass() {
        Inner inner = new Inner();
        inner.getProtectedFieldFromSuper();
        inner.invokeProtectedMethodFromSuper();
        inner.invokeStaticMethod();
    }

    class Inner {
        void invokeProtectedMethodFromSuper() {
            protectedMethod();
            assert call().equals(Ancestor0.TAG);
        }

        void getProtectedFieldFromSuper() {
            Log.d(TAG, "protectedField = " + protectedField);
        }

        void invokeStaticMethod() {
            protectedStatic();
            publicStatic();
            packageMethod();
            count++;
        }
    }
}
