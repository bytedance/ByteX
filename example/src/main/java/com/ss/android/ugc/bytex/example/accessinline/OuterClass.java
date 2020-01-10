package com.ss.android.ugc.bytex.example.accessinline;

import android.util.Log;

import com.ss.android.ugc.bytex.example.accessinline.other.Ancestor0;

public class OuterClass extends Ancestor0 {
    public static final String TAG = OuterClass.class.getSimpleName();
    private int a = 10;
    private double d = 10;
    public String s = "abc";
    protected String protectedField = TAG;

    private String haha() {
        return "haha";
    }

    private void enen() {
    }

    protected String hehe() {
        Log.d(TAG, "hehe() called. ");
        return TAG;
    }

    public void publicMethodInSuper() {
        Log.d(TAG, "publicMethodInSuper() called");
    }

    public void run() {
        Inner inner = new Inner();
        inner.ha();
        inner.en();
        inner.he();
        inner.getA();
        inner.getD();
        inner.getStr();
        inner.callOutside();
    }

    protected String p() {
        Log.d(TAG, "p was called.");
        return TAG;
    }

    protected static void protectedStatic() {
        Log.d(TAG, "protectedStatic() called");
    }

    public static void publicStatic() {
        Log.d(TAG, "publicStatic() called");
    }

    @Override
    protected String call() {
        return TAG;
    }

    class Inner {
        final String TAG = Inner.class.getSimpleName();

        private void ha() {
            haha();
        }

        private void en() {
            enen();
        }

        private int getA() {
            return a;
        }

        private double getD() {
            return d;
        }

        private String getStr() {
            return s;
        }

        private void he() {
            Log.d(TAG, "he() called");
            hehe();
        }

        private void callOutside() {
            assert call().equals(TAG);
        }
    }
}