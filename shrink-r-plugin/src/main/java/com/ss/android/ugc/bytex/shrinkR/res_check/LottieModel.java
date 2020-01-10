package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LottieModel {
    public List<Asset> assets;

    public static class Asset {
        @SerializedName("u")
        public String type;
        @SerializedName("p")
        public String name;
    }
}
