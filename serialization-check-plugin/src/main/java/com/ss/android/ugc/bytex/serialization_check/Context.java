package com.ss.android.ugc.bytex.serialization_check;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.white_list.WhiteList;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

public class Context extends BaseContext<SerializationCheckExtension> {
    private WhiteList whiteList;
    private List<String> errorRecords;

    public Context(Project project, AppExtension android, SerializationCheckExtension extension) {
        super(project, android, extension);
        whiteList = new WhiteList();
        errorRecords = new ArrayList<>();
    }

    public void initWithWhiteList(List<String> whiteList) {
        this.whiteList.initWithWhiteList(whiteList);
    }

    public boolean shouldCheck(String className) {
        return !className.startsWith("[") && whiteList.shouldCheck(className);
    }

    public boolean shouldCheck(String className, String member) {
        if (className.startsWith("[")) return false;
        return whiteList.shouldCheck(className, member);
    }

    public synchronized void recordError(String error) {
        errorRecords.add(error);
    }

    public String outputError() {
        if (errorRecords.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[ByteX]: We found several tips about serialization check, please checkout below, if you have any question about this, feel free to contact @tanlehua.\n");
        sb.append("[ByteX]: 我们检查到代码里面有些地方存在类序列化隐患，请检查这些源码看看这些类是否有必要实现Serializable接口或者让它所引用的类也实现Serializable接口，对此有任何问题可以找@谭乐华\n");
        for (String record : errorRecords) {
            sb.append(record);
        }
        return sb.toString();
    }
}
