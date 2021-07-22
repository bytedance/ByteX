package com.ss.android.ugc.bytex.common.processor;


import com.android.build.api.transform.Status;
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty;
import com.ss.android.ugc.bytex.common.exception.ByteXException;
import com.ss.android.ugc.bytex.common.exception.GlobalWhiteListManager;
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler;
import com.ss.android.ugc.bytex.common.flow.main.Process;
import com.ss.android.ugc.bytex.common.graph.GraphBuilder;
import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.common.visitor.GenerateGraphClassVisitor;
import com.ss.android.ugc.bytex.common.visitor.SafeClassNode;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileData;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

import javax.annotation.Nullable;

import static com.ss.android.ugc.bytex.common.flow.main.Process.TRAVERSE;
import static com.ss.android.ugc.bytex.common.flow.main.Process.TRAVERSE_ANDROID;

/**
 * Created by tlh on 2018/8/29.
 */

public class ClassFileAnalyzer extends MainProcessFileHandler {
    private final Process process;
    private final GraphBuilder mGraphBuilder;
    private final TransformContext context;

    @Deprecated
    public ClassFileAnalyzer(TransformContext context, boolean fromAndroid, @Nullable GraphBuilder graphBuilder, List<MainProcessHandler> handlers) {
        this(context, fromAndroid ? TRAVERSE_ANDROID : TRAVERSE, graphBuilder, handlers);
    }

    public ClassFileAnalyzer(TransformContext context,
                             Process process,
                             @Nullable GraphBuilder graphBuilder,
                             List<MainProcessHandler> handlers) {
        super(handlers);
        this.process = process;
        this.mGraphBuilder = graphBuilder;
        this.context = context;
    }

    @Override
    public void handle(FileData fileData) {
        try {
            List<MainProcessHandler> pluginList = handlers;
            if (fileData.getStatus() == Status.REMOVED) {
                if (process != Process.TRAVERSE_INCREMENTAL) {
                    throw new IllegalStateException("REMOVED State is only valid in TRAVERSE_INCREMENTAL process");
                }
                for (MainProcessHandler handler : pluginList) {
                    handler.traverseIncremental(fileData, (ClassVisitorChain) null);
                    handler.traverseIncremental(fileData, (ClassNode) null);
                }
                return;
            }
            byte[] raw = fileData.getBytes();
            String relativePath = fileData.getRelativePath();
            ClassReader cr = new ClassReader(raw);
            int flag = getFlag(handlers);
            ClassNode checkCn = null;
            if (BooleanProperty.CHECK_TRAVERSE_MODIFY.value() && (context.isReleaseBuild() || BooleanProperty.CHECK_DEBUG_TRAVERSE_MODIFY.value())) {
                //需要检验是否有插件会改指令，我们先accept一下，用一个ClassNode接收一下，后面再check
                checkCn = new SafeClassNode();
                cr.accept(checkCn, flag);
            }
            ClassVisitorChain chain = getClassVisitorChain(relativePath);
            if (this.mGraphBuilder != null) {
                //do generate class diagram
                chain.connect(new GenerateGraphClassVisitor(process == TRAVERSE_ANDROID, mGraphBuilder));
            }
            pluginList.forEach(plugin -> {
                switch (process) {
                    case TRAVERSE_INCREMENTAL:
                        plugin.traverseIncremental(fileData, chain);
                        break;
                    case TRAVERSE:
                        plugin.traverse(relativePath, chain);
                        break;
                    case TRAVERSE_ANDROID:
                        plugin.traverseAndroidJar(relativePath, chain);
                        break;
                    default:
                        throw new RuntimeException("Unsupported Process");
                }
            });
            ClassNode cn = new SafeClassNode();
            chain.append(cn);
            if (checkCn != null) {
                //之前解析过，这次不解析，直接用之前的ClassNode来传递数据
                chain.accept(checkCn);
                if (checkClassModified(cn, checkCn)) {
                    throw new IllegalStateException("Found plugin modified the class during the traverse process");
                }
            } else {
                chain.accept(cr, flag);
            }
            pluginList.forEach(plugin -> {
                switch (process) {
                    case TRAVERSE_INCREMENTAL:
                        plugin.traverseIncremental(fileData, cn);
                        break;
                    case TRAVERSE:
                        plugin.traverse(relativePath, cn);
                        break;
                    case TRAVERSE_ANDROID:
                        plugin.traverseAndroidJar(relativePath, cn);
                        break;
                    default:
                        throw new RuntimeException("Unsupported Process");
                }
            });
            if (checkCn != null && checkClassModified(cn, checkCn)) {
                throw new IllegalStateException("Found plugin modified the class during the traverse process");
            }
        } catch (ByteXException e) {
            throw new RuntimeException(String.format("%s\n\tFailed to resolve class %s[%s]", e.getMessage(), fileData.getRelativePath(), Utils.getAllFileCachePath(context, fileData.getRelativePath())), e);
        } catch (Exception e) {
            e.printStackTrace();
            LevelLog.sDefaultLogger.e(String.format("Failed to read class %s", fileData.getRelativePath()), e);
            if (!GlobalWhiteListManager.INSTANCE.shouldIgnore(fileData.getRelativePath())) {
                throw new RuntimeException(String.format("%s\n\tFailed to resolve class %s[%s]", e.getMessage(), fileData.getRelativePath(), Utils.getAllFileCachePath(context, fileData.getRelativePath())), e);
            }
        }
    }

    private int getFlag(List<MainProcessHandler> handlers) {
        int flag = 0;
        boolean needSkipCode = true;
        boolean needSkipDebug = true;
        boolean needSkipFrame = true;
        for (MainProcessHandler handler : handlers) {
            int flagForClassReader = handler.flagForClassReader(process);
            flag |= flagForClassReader;
            needSkipCode = needSkipCode && (flagForClassReader & ClassReader.SKIP_CODE) != 0;
            needSkipDebug = needSkipDebug && (flagForClassReader & ClassReader.SKIP_DEBUG) != 0;
            needSkipFrame = needSkipFrame && (flagForClassReader & ClassReader.SKIP_FRAMES) != 0;

        }
        if (!needSkipCode) {
            flag = flag & ~ClassReader.SKIP_CODE;
        }
        if (!needSkipDebug) {
            flag = flag & ~ClassReader.SKIP_DEBUG;
        }
        if (!needSkipFrame) {
            flag = flag & ~ClassReader.SKIP_FRAMES;
        }
        if ((flag & ClassReader.EXPAND_FRAMES) != 0) {
            flag = flag & ~ClassReader.SKIP_CODE;
            flag = flag & ~ClassReader.SKIP_FRAMES;
        }
        return flag;
    }


    private static boolean checkClassModified(ClassNode n1, ClassNode n2) {
        ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        n1.accept(cw1);
        byte[] b1 = cw1.toByteArray();

        ClassWriter cw2 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        n2.accept(cw2);
        byte[] b2 = cw2.toByteArray();

        if (b1.length != b2.length) {
            return true;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return true;
            }
        }
        return false;
    }
}
