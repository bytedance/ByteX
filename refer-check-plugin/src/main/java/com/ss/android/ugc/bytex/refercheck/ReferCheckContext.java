package com.ss.android.ugc.bytex.refercheck;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.white_list.WhiteList;

import org.gradle.api.Project;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class ReferCheckContext extends BaseContext<ReferCheckExtension> {
    private final List<InaccessibleNode> inaccessableMembers = Collections.synchronizedList(new LinkedList<>());
    private WhiteList whiteList;
    private Map<String, Boolean> checkCache = new ConcurrentHashMap<>();

    public ReferCheckContext(Project project, AppExtension android, ReferCheckExtension extension) {
        super(project, android, extension);
    }

    public void prepare() {
        inaccessableMembers.clear();
        whiteList = new WhiteList();
        initWithWhiteList(extension.getWhiteList());
    }

    private void initWithWhiteList(List<String> whiteList) {
        this.whiteList.initWithWhiteList(whiteList);
        this.whiteList.addWhiteListEntry("a", Pair.of(Pattern.compile("android/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("j", Pair.of(Pattern.compile("java/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/apache/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/xml/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/w3c/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/json/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/xmlpull/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("c", Pair.of(Pattern.compile("com/android/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("j", Pair.of(Pattern.compile("javax/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("d", Pair.of(Pattern.compile("dalvik/.+"), Utils.PATTERN_MATCH_ALL));
        this.whiteList.addWhiteListEntry("[", Pair.of(Pattern.compile("(\\[L)+.+"), Utils.PATTERN_MATCH_ALL)); // 对象数组
        this.whiteList.addWhiteListEntry("[", Pair.of(Pattern.compile("\\[+[BCDFISZJ]"), Utils.PATTERN_MATCH_ALL)); // 数组
    }

    private boolean shouldCheck(String className, String name) {
        return checkCache.computeIfAbsent(className + "." + name, s -> whiteList.shouldCheck(className, name));
    }

    public void addNotAccessMember(String callClassName, String callMethodName, String callMethodDesc, int callMethodAccess, @Nullable String sourceFile, int line,
                                   String memberOwner, String memberName, String memberDesc, int memberAccess,
                                   int type) {
        InaccessibleNode inaccessibleNode = new InaccessibleNode(
                callClassName,
                callMethodName,
                callMethodDesc,
                callMethodAccess,
                sourceFile,
                line,
                memberOwner,
                memberName,
                memberDesc,
                memberAccess,
                type
        );
        if (shouldCheck(memberOwner, memberName) && shouldCheck(callClassName, callMethodName)) {
            inaccessableMembers.add(inaccessibleNode);
        } else {
            getLogger().w("Skip InaccessibleNode", inaccessibleNode.toString());
        }
    }

    public List<InaccessibleNode> getInaccessibleMethods() {
        return inaccessableMembers.stream().filter(inaccessibleNode -> Type.getType(inaccessibleNode.memberDesc).getSort() == Type.METHOD).collect(Collectors.toList());
    }

    public List<InaccessibleNode> getInaccessibleFields() {
        return inaccessableMembers.stream().filter(inaccessibleNode -> Type.getType(inaccessibleNode.memberDesc).getSort() != Type.METHOD).collect(Collectors.toList());
    }

    public void releaseContext() {
        super.releaseContext();
        inaccessableMembers.clear();
        checkCache.clear();
        whiteList = null;
    }

}
