package com.ss.android.ugc.bytex.common.visitor;

import com.ss.android.ugc.bytex.common.graph.ClassEntity;
import com.ss.android.ugc.bytex.common.graph.FieldEntity;
import com.ss.android.ugc.bytex.common.graph.GraphBuilder;
import com.ss.android.ugc.bytex.common.graph.MethodEntity;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nonnull;

public class GenerateGraphClassVisitor extends BaseClassVisitor {

    private ClassEntity entity;
    private boolean fromAndroidSDK;
    private GraphBuilder mGraphBuilder;

    public GenerateGraphClassVisitor(boolean fromAndroidSDK, @Nonnull GraphBuilder graphBuilder) {
        this.fromAndroidSDK = fromAndroidSDK;
        this.mGraphBuilder = graphBuilder;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        entity = new ClassEntity(access, name, superName, interfaces == null ? Collections.emptyList() : Arrays.asList(interfaces));
        entity.fromAndroid = fromAndroidSDK;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        entity.fields.add(new FieldEntity(access, entity.name, name, desc, signature));
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        entity.methods.add(new MethodEntity(access, entity.name, name, desc, exceptions));
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        mGraphBuilder.add(entity);
    }
}
