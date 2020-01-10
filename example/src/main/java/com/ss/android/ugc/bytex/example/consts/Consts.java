package com.ss.android.ugc.bytex.example.consts;

import android.support.annotation.Keep;

import com.google.gson.annotations.SerializedName;

public class Consts {
    private static final long serialVersionUID = 67305479L;

    public static final boolean intA1 = true;
    public static final byte intA2 = 10;
    public static final short intA3 = 10;
    public static final char intA4 = 10;

    @Keep
    public static final char intA5 = 12;
    @SerializedName("dfsfa")
    public static final char intA6 = 13;


    public static final int intA = 10;
    public static final int intB = intA + 1;
    public static final int intC = 0;
//    public static final int intD = KtConsts.class.hashCode();
//    public static final int intE = intA + KtConsts.class.hashCode();
    public static final int intF;

    static {
        intF = 0;
    }


    public static final String stringA = "ConstsstringA";
    public static final String stringB = stringA + "---";
    public static final String stringC = null;
//    public static final String stringD = KtConsts.class.getName();
//    public static final String stringE = stringA + KtConsts.class.getName();


    public static void main(String[] args) {
        try {
            Consts.class.getField("intA");
            Consts consts = new Consts();
            consts.getClass().getField("intA");
            Class.forName("com.ss.android.ugc.bytex.consts.Consts").getField("intA");

            Consts.class.getFields();
            consts.getClass().getFields();
            Class.forName("com.ss.android.ugc.bytex.consts.Consts").getFields();

            Consts.class.getDeclaredField("intA");
            consts.getClass().getDeclaredField("intA");
            Class.forName("com.ss.android.ugc.bytex.consts.Consts").getDeclaredField("intA");

            Consts.class.getDeclaredFields();
            consts.getClass().getDeclaredFields();
            Class.forName("com.ss.android.ugc.bytex.consts.Consts").getDeclaredFields();

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Integer a() {
        try {
            return (Integer)Class.forName("android.os.Build$VERSION").getField("SDK_INT").get((Object)null);
        } catch (Exception var2) {
            System.err.println("Failed to retrieve value from android.os.Build$VERSION.SDK_INT due to the following exception.");
            var2.printStackTrace(System.err);
            return null;
        }
    }
}
