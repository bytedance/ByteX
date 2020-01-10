package com.ss.android.ugc.bytex.coverage_plugin.visitors;

import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.coverage_plugin.Context;
import com.ss.android.ugc.bytex.coverage_plugin.util.MappingIdGen;

import org.objectweb.asm.MethodVisitor;

public class CoverageClassVisitor extends BaseClassVisitor {

    private String className;

    private Context context;
    private MappingIdGen mappingIdGen;

    public CoverageClassVisitor(Context context, MappingIdGen mappingIdGen) {
        this.context = context;
        this.mappingIdGen = mappingIdGen;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    private int index = 0;

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        boolean visit = false;
        // 灰度发现很多其他的anr会报到这个插件，因此减少插桩数量，只在静态初始化方法插桩，一个类只统计上报一次
        // only insert <clinit> to improve performance
        if (context.isClInitOnly() && name.equals("<clinit>")) {
            visit = true;
        } else if (!context.isClInitOnly() && (name.equals("<init>") || name.equals("<clinit>"))) {
            visit = true;
        }
        if (visit) {
            String rawClassName = context.getProguardMap().get(className);
            // 没有找到mapping对应值时，按混淆的类名插桩
            // just use the raw className if there is no mapping
            if (rawClassName == null) {
                rawClassName = className;
            }
            final int id = mappingIdGen.genMappingId(rawClassName, name, String.valueOf(index));
            index++;
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new CoverageMethodVisitor(access, name, descriptor, signature, exceptions, context, id, className, mv);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
