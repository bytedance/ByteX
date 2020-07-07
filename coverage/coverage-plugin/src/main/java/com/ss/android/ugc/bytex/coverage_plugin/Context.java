package com.ss.android.ugc.bytex.coverage_plugin;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.white_list.WhiteList;

import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by jiangzilai on 2019-07-15.
 */
public class Context extends BaseContext<CoverageExtension> {

    private WhiteList whiteList = new WhiteList();

    private static final String moduleName = "coverage";
    private String versionName = "unknown";
    private String basePath = "";
    // mapping备份文件，用于增量，尽量避免id不一致
    private String mappingLatestFilePath = "";
    // 当前mapping文件
    private String mappingFilePath = "";
    // graph文件
    private String graphFilePath = "";
    // proguard mapping
    private Map<String,String> proguardMap;
    private boolean clInitOnly = true;


    public Context(Project project, AppExtension android, CoverageExtension extension) {
        super(project, android, extension);
    }

    @Override
    public void init() {
        super.init();
        buildWhiteList();
    }

    private void buildWhiteList() {
        this.whiteList.initWithWhiteList(extension.getWhiteList());
    }

    public WhiteList getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(WhiteList whiteList) {
        this.whiteList = whiteList;
    }

    public static String getModuleName() {
        return moduleName;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getMappingLatestFilePath() {
        return mappingLatestFilePath;
    }

    public void setMappingLatestFilePath(String mappingBackUpFilePath) {
        this.mappingLatestFilePath = mappingBackUpFilePath;
    }

    public String getMappingFilePath() {
        return mappingFilePath;
    }

    public void setMappingFilePath(String mappingFilePath) {
        this.mappingFilePath = mappingFilePath;
    }

    public String getGraphFilePath() {
        return graphFilePath;
    }

    public void setGraphFilePath(String graphFilePath) {
        this.graphFilePath = graphFilePath;
    }

    public Map<String, String> getProguardMap() {
        return proguardMap;
    }

    public void setProguardMap(Map<String, String> proguardMap) {
        this.proguardMap = proguardMap;
    }

    public boolean isClInitOnly() {
        return clInitOnly;
    }

    public void setClInitOnly(boolean clInitOnly) {
        this.clInitOnly = clInitOnly;
    }

    @Override
    public void releaseContext() {
        super.releaseContext();
        whiteList = new WhiteList();
        versionName = "unknown";
        basePath = "";
        mappingLatestFilePath = "";
        mappingFilePath = "";
        graphFilePath = "";
        proguardMap = null;
        clInitOnly = true;
    }
}
