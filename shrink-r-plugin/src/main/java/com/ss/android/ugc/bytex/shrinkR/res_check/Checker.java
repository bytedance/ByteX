package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.google.common.collect.Streams;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ss.android.ugc.bytex.shrinkR.Context;

import org.apache.commons.io.Charsets;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class Checker {
    private Context context;
    private final AssetsCheckExtension assetsCheckExtension;
    private final ResourceCheckExtension resourceCheckExtension;
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().disableHtmlEscaping().create();
    private AssetsManager assetsManager;
    private ResManager resManager;

    public Checker(Context context) {
        this.context = context;
        this.resourceCheckExtension = context.resCheckExtension;
        this.assetsCheckExtension = context.assetsCheckExtension;
        resManager = new ResManager(context);
        assetsManager = new AssetsManager(context);
    }

    public void prepare() {
        if (resourceCheckExtension.isEnable()) {
            resManager.prepare();
        }
        if (assetsCheckExtension.isEnable()) {
            assetsManager.prepare();
        }
    }

    public boolean saveToLocal(List<Substance> unusedResource) {
        try {
            File resDump = new File(context.buildDir(), "unused_res.json");
            Files.createParentDirs(resDump);
            Writer writer = Files.newWriter(resDump, Charsets.UTF_8);
            gson.toJson(unusedResource, writer);
            writer.close();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUnusedAttr(List<Substance> unusedResource) {
        StringBuilder sb = new StringBuilder();
        for (Substance substance : unusedResource) {
            sb.append(substance.toString());
        }
        return sb.toString();
    }

    public final AssetsManager getAssetsManager() {
        return assetsManager;
    }

    public ResManager getResManager() {
        return resManager;
    }

    public List<Substance> getAllUnReachResource() {
        return Streams.concat(resManager.getAllUnReachResource().stream(), assetsManager.getAllUnReachResource().stream())
                .collect(Collectors.toList());
    }
}
