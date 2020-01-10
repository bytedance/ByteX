package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ss.android.ugc.bytex.shrinkR.Context;

import java.util.List;

class LottieJsonHandler {
    private static final Gson gson = new Gson();
    private final AssetsManager assetsManager;

    LottieJsonHandler(Context context) {
        this.assetsManager = context.getChecker().getAssetsManager();
    }

    void process(final String json) {
        LottieModel lottieModel;
        try {
            lottieModel = gson.fromJson(json, LottieModel.class);
        } catch (JsonSyntaxException ignored) {
            return;
        }
        if (lottieModel != null) {
            List<LottieModel.Asset> assets = lottieModel.assets;
            if (assets != null) {
                for (int i = 0; i < assets.size(); i++) {
                    LottieModel.Asset asset = assets.get(i);
                    String type = asset.type;
                    if (type != null && type.endsWith("/")) {
                        type = type.substring(0, type.lastIndexOf("/"));
                    }
                    assetsManager.reachAsset(type, asset.name);
                }
            }
        }
    }
}
