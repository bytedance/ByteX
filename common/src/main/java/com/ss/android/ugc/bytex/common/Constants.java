package com.ss.android.ugc.bytex.common;

import com.ss.android.ugc.bytex.common.configuration.StringProperty;

import org.objectweb.asm.Opcodes;

/**
 * Created by yangzhiqian on 2020/9/21<br/>
 */
public class Constants {
    //此处不用常量是因为后期尝试直接升级，内联则没有这个效果
    public static int ASM_API;

    static {
        switch (StringProperty.ASM_API.value()) {
            case "ASM4":
                ASM_API = Opcodes.ASM4;
                break;
            case "ASM5":
                ASM_API = Opcodes.ASM5;
                break;
            case "ASM6":
                ASM_API = Opcodes.ASM6;
                break;
            case "ASM7":
                ASM_API = Opcodes.ASM7;
                break;
            case "ASM8":
                ASM_API = Opcodes.ASM8;
                break;
            case "ASM9":
                ASM_API = Opcodes.ASM9;
                break;
            default: {
                throw new IllegalArgumentException(StringProperty.ASM_API.value() + " is not supported!");
            }
        }
    }
}
