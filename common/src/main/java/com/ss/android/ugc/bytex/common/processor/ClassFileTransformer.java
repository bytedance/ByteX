package com.ss.android.ugc.bytex.common.processor;

import com.ss.android.ugc.bytex.common.exception.ByteXException;
import com.ss.android.ugc.bytex.common.exception.GlobalWhiteListManager;
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler;
import com.ss.android.ugc.bytex.common.flow.main.Process;
import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.verify.AsmVerifier;
import com.ss.android.ugc.bytex.common.verify.AsmVerifyClassVisitor;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.common.visitor.SafeClassNode;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileData;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class ClassFileTransformer extends MainProcessFileHandler {
    private boolean needPreVerify;
    private boolean needVerify;
    private TransformContext context;

    @Deprecated
    public ClassFileTransformer(List<MainProcessHandler> handlers, boolean needPreVerify, boolean needVerify) {
        this(null, handlers, needPreVerify, needVerify);
    }

    public ClassFileTransformer(TransformContext context, List<MainProcessHandler> handlers, boolean needPreVerify, boolean needVerify) {
        super(handlers);
        this.context = context;
        this.needPreVerify = needPreVerify;
        this.needVerify = needVerify;
    }

    @Override
    public void handle(FileData fileData) {
        try {
            byte[] raw = fileData.getBytes();
            String relativePath = fileData.getRelativePath();
            int cwFlags = 0;  //compute nothing
            int crFlags = 0;
            for (MainProcessHandler handler : handlers) {
                cwFlags |= handler.flagForClassWriter();
                if ((handler.flagForClassReader(Process.TRANSFORM) & ClassReader.EXPAND_FRAMES) == ClassReader.EXPAND_FRAMES) {
                    crFlags |= ClassReader.EXPAND_FRAMES;
                }
            }
            ClassReader cr = new ClassReader(raw);
            ClassWriter cw = new ClassWriter(cwFlags);
            ClassVisitorChain chain = getClassVisitorChain(relativePath);
            if (needPreVerify) {
                chain.connect(new AsmVerifyClassVisitor());
            }
            if (handlers != null && !handlers.isEmpty()) {
                for (MainProcessHandler handler : handlers) {
                    if (!handler.transform(relativePath, chain)) {
                        fileData.delete();
                        return;
                    }
                }
            }
            ClassNode cn = new SafeClassNode();
            chain.append(cn);
            chain.accept(cr, crFlags);
            for (MainProcessHandler handler : handlers) {
                if (!handler.transform(relativePath, cn)) {
                    fileData.delete();
                    return;
                }
            }
            cn.accept(cw);
            raw = cw.toByteArray();
            if (needVerify) {
                ClassNode verifyNode = new ClassNode();
                new ClassReader(raw).accept(verifyNode, crFlags);
                AsmVerifier.verify(verifyNode);
            }
            fileData.setBytes(raw);
        } catch (ByteXException e) {
            throw e;
        } catch (Exception e) {
            LevelLog.sDefaultLogger.e(String.format("Failed to handle class %s", fileData.getRelativePath()), e);
            if (!GlobalWhiteListManager.INSTANCE.shouldIgnore(fileData.getRelativePath())) {
                if (context != null) {
                    throw new RuntimeException(String.format("Failed to resolve class %s[%s]", fileData.getRelativePath(), Utils.getAllFileCachePath(context, fileData.getRelativePath())), e);
                } else {
                    throw e;
                }
            }
        }
    }
}
