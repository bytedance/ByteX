package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.android.utils.Pair;
import com.google.common.io.Files;
import com.ss.android.ugc.bytex.gradletoolkit.Artifact;
import com.ss.android.ugc.bytex.shrinkR.Context;
import com.ss.android.ugc.bytex.transformer.io.Files_;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AssetsManager {
    private final AssetsCheckExtension extension;
    private Map<String, Map<String, Asset>> assets;
    private Map<String, Asset> resolvedAssets;
    private Context context;

    public AssetsManager(Context context) {
        this.context = context;
        this.assets = new HashMap<>();
        extension = context.assetsCheckExtension;
    }


    private void resolve(Collection<File> assetsDirs) {
        List<File> lottieJsonFiles = new ArrayList<>();
        for (File assetsDir : assetsDirs) {
            if (!assetsDir.isDirectory()) {
                continue;
            }
            URI base = assetsDir.toURI();
            for (File f : Files_.fileTreeTraverser().preOrderTraversal(assetsDir)) {
                if (f.isFile() && !f.getName().equalsIgnoreCase(".DS_Store")) {
                    Pair<String, String> assetsTypeAndName = getAssetsTypeAndNameFromPath(f, base.relativize(f.toURI()).toString());
                    Asset assets = new Asset(assetsTypeAndName.getFirst(), assetsTypeAndName.getSecond());
                    assets.define(f.getPath());
                    Map<String, Asset> map = this.assets.computeIfAbsent(assetsTypeAndName.getFirst(), e -> new HashMap<>());
                    map.put(assetsTypeAndName.getSecond(), assets);
                    if (f.getName().endsWith(".json")) {
                        lottieJsonFiles.add(f);
                    }
                }
            }
        }
        LottieJsonHandler handler = new LottieJsonHandler(context);
        try {
            for (File jsonFile : lottieJsonFiles) {
                handler.process(Files.asCharSource(jsonFile, Charset.forName("utf-8")).read());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // resolve all assets files.
        this.resolvedAssets = new HashMap<>();
        for (Map<String, Asset> map : assets.values()) {
            for (Asset asset : map.values()) {
                this.resolvedAssets.put(asset.getName(), asset);
                if (!asset.getPath().equals("")) {
                    this.resolvedAssets.put(asset.getPath() + "/" + asset.getName(), asset);
                }
            }
        }
    }


    private static Pair<String, String> getAssetsTypeAndNameFromPath(File file, String relativePath) {
        int typeNameEndIdx = relativePath.lastIndexOf("/");
        String type = typeNameEndIdx < 0 ? "" : relativePath.substring(0, typeNameEndIdx);
        String name = file.getName();
        return Pair.of(type, name);
    }


    public void reachAsset(String type, String name) {
        Map<String, Asset> assetNameMap = assets.get(type);
        if (assetNameMap != null) {
            Asset asset = assetNameMap.get(name);
            if (asset != null) {
                asset.refer();
            }
        } else {
            for (Map<String, Asset> assetMap : assets.values()) {
                for (Asset asset : assetMap.values()) {
                    if (asset.getName().equals(name))
                        asset.refer();
                }
            }
        }
    }

    public void tryReachAsset(String name) {
        if (!extension.isEnable()) return;
        Asset asset = resolvedAssets.get(name);
        if (asset != null) {
            asset.refer();
        }
    }

    List<Asset> getAllUnReachResource() {
        if (!context.extension.isEnable() || !extension.isEnable()) {
            return Collections.emptyList();
        }
        return resolvedAssets.values().stream()
                .filter(asset -> !asset.canReach())
                .filter(asset -> shouldCheck(asset.getPath() + "/" + asset.getName()))
                .collect(Collectors.toList());
    }

    boolean shouldCheck(String path) {
        List<String> keepBySuffixs = context.assetsCheckExtension.getKeepBySuffix();
        if (keepBySuffixs != null && !keepBySuffixs.isEmpty()) {
            for (String suffix : keepBySuffixs) {
                if (path.endsWith(suffix)) return false;
            }
        }

        List<String> keepAssets = context.assetsCheckExtension.getKeepAssets();
        if (keepAssets != null && !keepAssets.isEmpty()) {
            for (String keepAsset : keepAssets) {
                if (path.contains(keepAsset)) return false;
            }
        }
        return true;
    }

    public void prepare() {
        resolve(context.getTransformContext().getArtifact(Artifact.RAW_ASSET_SETS));
    }
}
