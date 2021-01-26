package com.ss.android.ugc.bytex.refercheck;

import com.android.build.gradle.AppExtension;
import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.utils.MethodMatcher;
import com.ss.android.ugc.bytex.common.utils.Utils;
import com.ss.android.ugc.bytex.common.white_list.WhiteList;
import com.ss.android.ugc.bytex.refercheck.visitor.ReferCheckMethodVisitor;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class ReferCheckContext extends BaseContext<ReferCheckExtension> implements ReferCheckMethodVisitor.CheckIssueReceiver {
    private ReferCheckMethodVisitor.CheckIssueReceiver checkIssueReceiver;
    private final List<MethodMatcher> blockMethodCallMatcher = new ArrayList<>();

    public ReferCheckContext(Project project, AppExtension android, ReferCheckExtension extension) {
        super(project, android, extension);
    }

    public void prepare() {
        WhiteList whiteList = new WhiteList();
        whiteList.initWithWhiteList(extension.getWhiteList());
        appendCommonWhiteList(whiteList);
        for (String s : extension.getCallBlockList()) {
            blockMethodCallMatcher.add(new MethodMatcher(s));
        }
        checkIssueReceiver = new DefaultCheckIssueReceiver(whiteList, new Consumer<InaccessibleNode>() {
            @Override
            public void accept(InaccessibleNode inaccessibleNode) {
                getLogger().w("Skip InaccessibleNode", inaccessibleNode.toString());
            }
        });
    }

    public static void appendCommonWhiteList(WhiteList whiteList) {
        whiteList.addWhiteListEntry("a", Pair.of(Pattern.compile("android/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("j", Pair.of(Pattern.compile("java/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/apache/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/xml/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/w3c/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/json/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("o", Pair.of(Pattern.compile("org/xmlpull/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("c", Pair.of(Pattern.compile("com/android/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("j", Pair.of(Pattern.compile("javax/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("d", Pair.of(Pattern.compile("dalvik/.+"), Utils.PATTERN_MATCH_ALL));
        whiteList.addWhiteListEntry("[", Pair.of(Pattern.compile("(\\[L)+.+"), Utils.PATTERN_MATCH_ALL)); // 对象数组
        whiteList.addWhiteListEntry("[", Pair.of(Pattern.compile("\\[+[BCDFISZJ]"), Utils.PATTERN_MATCH_ALL)); // 数组
    }

    @Override
    public void addNotAccessMember(String callClassName, String callMethodName, String callMethodDesc, int callMethodAccess, @Nullable String sourceFile, int line,
                                   String memberOwner, String memberName, String memberDesc, int memberAccess,
                                   int type) {
        checkIssueReceiver.addNotAccessMember(
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
    }

    @Override
    public List<InaccessibleNode> getInaccessibleNodes() {
        return checkIssueReceiver.getInaccessibleNodes();
    }

    public List<MethodMatcher> getBlockMethodCallMatcher() {
        return blockMethodCallMatcher;
    }

    public void releaseContext() {
        super.releaseContext();
        checkIssueReceiver = null;
        blockMethodCallMatcher.clear();
    }

}
