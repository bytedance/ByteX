package com.ss.android.ugc.bytex.common.verify;


import com.ss.android.ugc.bytex.common.utils.OpcodesUtils;
import com.ss.android.ugc.bytex.common.utils.Utils;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

/**
 * Created by yangzhiqian on 2019/4/18<br/>
 */
public class AsmVerifier {

    public static void verify(ClassNode classNode) throws AsmVerifyException {
        for (Object methodNode : classNode.methods) {
            verify(classNode.name, (MethodNode) methodNode);
        }
    }

    public static void verify(String owner, MethodNode methodNode) throws AsmVerifyException {
        try {
            new Analyzer(new BasicInterpreter()).analyze(owner, methodNode);
        } catch (AnalyzerException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("verify err:")
                    .append(Utils.replaceSlash2Dot(owner))
                    .append(".")
                    .append(methodNode.name)
                    .append(" ")
                    .append(methodNode.desc)
                    .append("\n");
            int size = methodNode.instructions.size();
            for (int i = 0; i < size; i++) {
                stringBuilder.append(OpcodesUtils.covertToString(methodNode.instructions.get(i))).append("\n");
            }
            throw new AsmVerifyException(stringBuilder.toString(), e);
        }
    }
}
