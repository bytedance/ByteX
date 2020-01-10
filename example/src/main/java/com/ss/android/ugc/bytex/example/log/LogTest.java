package com.ss.android.ugc.bytex.example.log;

import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

public class LogTest {
    public int xxxtest(int key, int value) throws RemoteException {
        LogTest logTest = new LogTest();
        int a = 0;
        if (key == 22) {
            if (value == 0) {
                Log.i("xxx", "wgadsfadfh");
            } else if (value == 2) {
                Log.i("dfsdf", "wfadfadfh");
            } else if (value == 1) {
                Process.killProcess(Process.myPid());
                System.exit(0);
                String jcrash = null;
                Log.i("fadf", "fadsafdfads");
                if (((String) jcrash).endsWith("java")) {
                    throw new RemoteException("sdfadsgadsfa");
                }

                Log.i("dfadf", "ofadfadg");
            }
        }
        return a;
    }


    public void test(boolean con) {
        Log.d(con ? "con" : "no con", "con");
    }


    public static String TAG = "test";


    private String abc = "abc";

    public void test() {
        Log.d(TAG, TAG == null ? "null" : TAG);
        LogTest logTest = new LogTest();
        LogTest[] logTest1 = new LogTest[10];
        byte[] bytes = new byte[10];
        byte x = (byte) bytes.length;
        LogTest[][] logTest2 = new LogTest[100][100];
        abc = "temp";
        TAG = "temptag";
        Log.d(abc, logTest2[1].length + "");


        Log.d("log", "log");
        Log.d(TAG, "log");
        Log.d("log", String.valueOf(1));
        Log.d("log", "log" + 1 + "c" + 2 + "d" + toString());
        Log.d("log", getClass().getSimpleName());
        Log.d("log", this.toString());
        Log.d(getClass().getSimpleName(), "log");
        Log.d(getClass().getSimpleName(), getClass().getSimpleName());
        getLong();
        long a = getLong();


        test(1);
//        Integer x = 1;
        test(x);
//        test(null);


        CharSequence charSequence = "";
        String abc = (String) charSequence;

    }

    private void test(int x) {

    }

    private void test(Integer x) {

    }

    public void test(int[] x) {
    }

    //public void test(int... x){}
    public void test(Integer[] x) {
    }

    public void test(Void v) {
//        void a;
    }


    public long getLong() {
        return System.currentTimeMillis();
    }

    public static long getLong(LogTest logTest) {
        return 10;
    }
}
