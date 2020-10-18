package com.ss.android.ugc.bytex.common;

import org.objectweb.asm.Opcodes;

/**
 * Created by yangzhiqian on 2020/9/21<br/>
 */
public class Constants {
    //此处不用常量是因为后期尝试直接升级，内联则没有这个效果
    public static int ASM_API = Opcodes.ASM6;
}
