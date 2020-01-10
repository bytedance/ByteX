package com.ss.android.ugc.bytex.example.accessinline.other;


import com.ss.android.ugc.bytex.example.accessinline.OuterClass;

public class SubClassInOtherPackage extends OuterClass {
    public SubClassInOtherPackage() {
        Inner inner = new Inner();
        inner.invokeProtectMethod();
    }

    class Inner {
        void invokeProtectMethod() {
            hehe();
        }
    }
}
