package com.ss.android.ugc.bytex.example.consts;

import java.util.Random;

public class ConstTest {
    public static final int intA = 10;
    public static final String stringA = "ConstTeststringA";
    public static final int intRandom = new Random().nextInt();


    public interface InterfaceA {
        public static final int A = 2 * InterfaceB.B;
    } // End of interface

    public interface InterfaceB {
        public static final int B = InterfaceC.C + 1;
    } // End of interface

    public interface InterfaceC extends InterfaceA {
        public static final int C = A + 1;
    } // End of interface


    public static void main(String[] args) {
        System.out.println(intA);
        System.out.println(stringA);

        System.out.println(Consts.intA);
        System.out.println(Consts.stringA);

        System.out.println(Consts.stringA + stringA);

        System.out.println (InterfaceA.A + InterfaceB.B + InterfaceC.C);


    }
}
