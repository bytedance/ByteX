package com.ss.android.ugc.bytex.common.processor;


import com.ss.android.ugc.bytex.common.exception.ByteXException;
import com.ss.android.ugc.bytex.common.exception.GlobalWhiteListManager;
import com.ss.android.ugc.bytex.common.flow.main.MainProcessHandler;
import com.ss.android.ugc.bytex.common.graph.GraphBuilder;
import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.visitor.ClassVisitorChain;
import com.ss.android.ugc.bytex.common.visitor.GenerateGraphClassVisitor;
import com.ss.android.ugc.bytex.common.visitor.SafeClassNode;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileData;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

import javax.annotation.Nullable;

import static com.ss.android.ugc.bytex.common.flow.main.Process.TRAVERSE;
import static com.ss.android.ugc.bytex.common.flow.main.Process.TRAVERSE_ANDROID;

/**
 * Created by tlh on 2018/8/29.
 */

public class ClassFileAnalyzer extends MainProcessFileHandler {
    private final boolean fromAndroid;
    private GraphBuilder mGraphBuilder;
    private TransformContext context;

    public ClassFileAnalyzer(TransformContext context, boolean fromAndroid, @Nullable GraphBuilder graphBuilder, List<MainProcessHandler> handlers) {
        super(handlers);
        this.fromAndroid = fromAndroid;
        this.mGraphBuilder = graphBuilder;
        this.context = context;
    }

    @Override
    public void handle(FileData fileData) {
        try {
            byte[] raw = fileData.getBytes();
            String relativePath = fileData.getRelativePath();
            ClassReader cr = new ClassReader(raw);
            List<MainProcessHandler> pluginList = handlers;
            int flag = getFlag(handlers);
            ClassVisitorChain chain = getClassVisitorChain(relativePath);
            pluginList.forEach(plugin -> {
                if (fromAndroid) {
                    plugin.traverseAndroidJar(relativePath, chain);
                } else {
                    plugin.traverse(relativePath, chain);
                }
            });
            if (this.mGraphBuilder != null) {
                //do generate class diagram
                chain.connect(new GenerateGraphClassVisitor(fromAndroid, mGraphBuilder));
            }
            ClassNode cn = new SafeClassNode();
            chain.append(cn);
            chain.accept(cr, flag);
            pluginList.forEach(plugin -> {
                if (fromAndroid) {
                    plugin.traverseAndroidJar(relativePath, cn);
                } else {
                    plugin.traverse(relativePath, cn);
                }
            });
        } catch (ByteXException e) {
            throw new RuntimeException(String.format("Failed to resolve class %s[%s]", fileData.getRelativePath(), Utils.getAllFileCachePath(context, fileData.getRelativePath())), e);
        } catch (Exception e) {
            e.printStackTrace();
            LevelLog.sDefaultLogger.e(String.format("Failed to read class %s", fileData.getRelativePath()), e);
            if (!GlobalWhiteListManager.INSTANCE.shouldIgnore(fileData.getRelativePath())) {
                throw new RuntimeException(String.format("Failed to resolve class %s[%s]", fileData.getRelativePath(), Utils.getAllFileCachePath(context, fileData.getRelativePath())), e);
            }
        }
    }

    private int getFlag(List<MainProcessHandler> handlers) {
        int flag = 0;
        boolean needSkipCode = true;
        boolean needSkipDebug = true;
        boolean needSkipFrame = true;
        for (MainProcessHandler handler : handlers) {
            int flagForClassReader = handler.flagForClassReader(fromAndroid ? TRAVERSE_ANDROID : TRAVERSE);
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
}
