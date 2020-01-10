package com.ss.android.ugc.bytex.common.graph;

import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;


public class InterfaceNode extends Node {

    public List<InterfaceNode> children = Collections.emptyList();
    public List<ClassNode> implementedClasses = Collections.emptyList();

    public InterfaceNode(String className) {
        super(new ClassEntity(className, Opcodes.ACC_INTERFACE), null, Collections.emptyList());
    }
}
