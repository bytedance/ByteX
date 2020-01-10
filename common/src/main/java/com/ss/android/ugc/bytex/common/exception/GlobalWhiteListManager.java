package com.ss.android.ugc.bytex.common.exception;

import com.ss.android.ugc.bytex.common.configuration.StringProperty;
import com.ss.android.ugc.bytex.common.white_list.WhiteList;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by tanlehua on 2019/4/22.
 */
public enum GlobalWhiteListManager {
    INSTANCE;

    private WhiteList whiteList = new WhiteList();

    public void init(Project project) {
        if (!whiteList.isEmpty()) {
            return;
        }
        parseWhiteList(project, StringProperty.EXCEPTION_IGNORE_LIST.value());
        parseWhiteList(project, StringProperty.GLOBAL_IGNORE_LIST.value());
        project.getGradle().buildFinished(p -> whiteList.clear());
    }

    private void parseWhiteList(Project project, String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(project.getRootDir(), filePath);
            if (file.isFile()) {
                try {
                    List<String> lines = FileUtils.readLines(file);
                    whiteList.initWithWhiteList(lines);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean shouldIgnore(String className) {
        return !whiteList.shouldCheck(className);
    }
}
