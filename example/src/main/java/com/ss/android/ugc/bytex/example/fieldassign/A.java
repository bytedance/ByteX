package com.ss.android.ugc.bytex.example.fieldassign;

public class A {
    public static final int aa;

    static {
        aa = 0;
    }

    public static boolean sa = false;
    public static char sb = 0;
    public static int sc = 0;
    public static String si = A.class.getSimpleName();
    public static short sd = 0;
    public static float se = 0.0f;
    public static double sf = 0.0;
    public static long sg = 0;
    public static String sh = null;

    public static boolean sa_;
    public static char sb_;
    public static int sc_;
    public static short sd_;
    public static float se_;
    public static double sf_;
    public static long sg_;
    public static String sh_;

    static {
        sa = true;
        sg = 1000;
        sh = A.class.getSimpleName();
    }


    public boolean a = false;
    public byte b = 0;
    public char c = 0;
    public short d = 0;
    public int e = 0;
    public float f = 0;
    public double g = 0.0f;
    public long h = 0;
    public String i = null;

    public static int j = 0;


    private void test() {
        if (a) {
            a = false;
        }
        for (int i = 0; i < 10; i++) {
            a = false;
        }
        switch (d) {
            case 0:
                b = 0;
                break;
            case 1:
                c = 0;
                break;
            case 2:
                d = 0;
                break;
            case 3:
                f = 0;
                break;
            default:
                g = 0;
                break;
        }
        switch (e) {
            case 0:
                b = 0;
                break;
            case 100:
                c = 0;
                break;
            case 2000:
                d = 0;
                break;
            case 300000:
                f = 0;
                break;
            default:
                g = 0;
                break;
        }

        if (a) {
            return;
        }
        e = hashCode();
    }

    static class B extends A {
        static String abc = "avc";

    }


    public static final String sfa = "a";
    public static final String sfb = A.class.getSimpleName();

    public static final int sfc = 10;
    public static final int sfd = A.class.hashCode();

    public boolean a_;
    public char b_;
    public int c_;
    public short d_;
    public float e_;
    public double f_;
    public long g_;
    public String h_;

    public A() {
        a = true;
        g = 1000;
        i = A.class.getSimpleName();
    }

    public A(int c) {
        a = true;
        g = 1000;
        i = A.class.getSimpleName();
    }

}

