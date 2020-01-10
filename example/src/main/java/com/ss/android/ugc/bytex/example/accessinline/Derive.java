package com.ss.android.ugc.bytex.example.accessinline;

import android.util.Log;

public class Derive extends OuterClass {

    public static final String TAG = Derive.class.getSimpleName();
    protected String protectedField = TAG;

    public Derive() {
        Inner inner = new Inner();
        inner.invokeProtectedMethodFromSuper();
        inner.getProtectedFieldFromSuper();
        inner.invokeStaticMethod();
    }

    @Override
    protected String hehe() {
        Log.d(TAG, "hehe() called");
        return TAG;
    }

    class Inner {
        void invokeProtectedMethodFromSuper() {
            assert Derive.super.hehe().equals(OuterClass.TAG);
            assert Derive.this.p().equals(TAG);
            Derive.super.publicMethodInSuper();
        }

        void invokeStaticMethod() {
            protectedStatic();
            publicStatic();
        }

        void getProtectedFieldFromSuper() {
            assert Derive.super.protectedField.equals(OuterClass.TAG);
            Log.d(TAG, "protectedField in super: " + Derive.super.protectedField);
            assert Derive.this.protectedField.equals(TAG);
            Log.d(TAG, "protectedField in subClass: " + Derive.this.protectedField);
        }
    }

}
