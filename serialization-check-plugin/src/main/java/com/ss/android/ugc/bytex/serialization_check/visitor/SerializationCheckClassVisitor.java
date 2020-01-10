package com.ss.android.ugc.bytex.serialization_check.visitor;

import com.ss.android.ugc.bytex.common.graph.ClassNode;
import com.ss.android.ugc.bytex.common.graph.Node;
import com.ss.android.ugc.bytex.common.utils.TypeUtil;
import com.ss.android.ugc.bytex.common.visitor.BaseClassVisitor;
import com.ss.android.ugc.bytex.serialization_check.Context;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerializationCheckClassVisitor extends BaseClassVisitor {
    private final Context context;
    private boolean isSerializable;
    private boolean shouldCheck;
    private static final String INTERFACE_SERIALIZABLE = Serializable.class.getName().replace(".", "/");
    private static final String CLASS_LIST = List.class.getName().replace(".", "/");
    private String className;

    public SerializationCheckClassVisitor(Context context) {
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        isSerializable = implementsSerializable(interfaces);
        shouldCheck = context.shouldCheck(name);
        this.className = name;
    }

    private boolean implementsSerializable(String[] interfaces) {
        if (interfaces != null && interfaces.length > 0) {
            for (String itf : interfaces) {
                if (INTERFACE_SERIALIZABLE.equals(itf)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean implementsSerializable(List<String> interfaces) {
        if (interfaces != null && interfaces.size() > 0) {
            for (String itf : interfaces) {
                if (INTERFACE_SERIALIZABLE.equals(itf)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean implementsSerializable(Node node) {
        return node.interfaces.stream().anyMatch(interfaceNode -> interfaceNode.entity.name.equals(INTERFACE_SERIALIZABLE));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        if (shouldCheck && isSerializable && !TypeUtil.isTransient(access) &&
                !TypeUtil.isStatic(access) && !TypeUtil.isPrimitive(descriptor)) {
            return new SerializationCheckFieldVisitor(fv, context, className, name, descriptor);
        }
        return fv;
    }

    private class SerializationCheckFieldVisitor extends FieldVisitor {
        private AnnotationNode suppressLintAnnotation;
        private Context context;
        private String className;
        private String name;
        private String descriptor;

        SerializationCheckFieldVisitor(FieldVisitor fieldVisitor, Context context, String className, String name, String descriptor) {
            super(Opcodes.ASM5, fieldVisitor);
            this.context = context;
            this.className = className;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
            if ("android/annotation/SuppressLint".equals(TypeUtil.desc2Name(descriptor))) {
                AnnotationVisitor exAnnotationVisitor = av;
                av = suppressLintAnnotation = new AnnotationNode(Opcodes.ASM5, descriptor) {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (suppressLintAnnotation != null && exAnnotationVisitor != null) {
                            suppressLintAnnotation.accept(exAnnotationVisitor);
                        }
                    }
                };
            }
            return av;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (isSuppressByLint() || descriptor.startsWith("[")) return;
            String type = TypeUtil.desc2Name(descriptor);
            if (CLASS_LIST.equals(type)) return;
            if (type.startsWith("[") || type.startsWith("java") || !context.shouldCheck(className, name)) {
                return;
            }
            Node node = context.getClassGraph().get(type);
            if (!(node instanceof ClassNode)) {
//                context.recordError(String.format("In class %s , field %s %s is not Serializable, " +
//                                "please checkout and consider to make interface %s implements java.io.Serializable. \n",
//                        TypeUtil.getCanonicalName(className), TypeUtil.getCanonicalName(type), name, TypeUtil.getCanonicalName(node.entity.name)));
                return;
            }
            AtomicBoolean foundSerializable = new AtomicBoolean(false);
            context.getClassGraph().backtrackToParent((ClassNode) node, classNode -> {
                if (implementsSerializable(classNode.entity.interfaces)) {
                    foundSerializable.set(true);
                    return true;
                }
                return false;
            });
            if (!foundSerializable.get()) {
                if ("this$0".equals(name)) {
                    context.recordError(String.format("The class %s is non-static inner class, and should not implement java.io.Serializable, or you can make %s to be static class. \n",
                            TypeUtil.getCanonicalName(className), TypeUtil.getCanonicalName(className)));
                } else {
                    context.recordError(String.format("In class %s , field %s %s should implement interface java.io.Serializable. \n",
                            TypeUtil.getCanonicalName(className), TypeUtil.getCanonicalName(type), name));
                }
            }
        }

        private boolean isSuppressByLint() {
            if (suppressLintAnnotation != null) {
                List<Object> values = suppressLintAnnotation.values;
                if (values != null && values.size() == 2) {
                    Object key = values.get(0);
                    Object value = values.get(1);
                    if ("value".equals(key) && value instanceof List) {
                        return "SerializableImplementsRule".equals(((List) value).get(0));
                    }
                }
            }
            return false;
        }
    }

}
