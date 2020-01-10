package com.ss.android.ugc.bytex.example.accessinline.other;

import android.util.Log;

public class SuperClassInOtherPackage extends Ancestor0 {
    public static final String TAG = SuperClassInOtherPackage.class.getSimpleName();

    protected String protectedField = TAG;

    protected void protectedMethod() {
        Log.d(TAG, "protectedMethod() called");
    }

    protected static void protectedStatic() {
        Log.d(TAG, "protectedStatic() called");
    }

    public static void publicStatic() {
        Log.d(TAG, "publicStatic() called");
    }

    void packageMethod() {
        Log.d(TAG, "packageMethod() called");
    }

    private void privateMethod() {
        Log.d(TAG, "privateMethod() called");
    }

    class Inner {
        void invoke() {
            privateMethod();
        }
    }
}
