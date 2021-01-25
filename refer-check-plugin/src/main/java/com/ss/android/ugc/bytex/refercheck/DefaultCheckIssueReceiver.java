package com.ss.android.ugc.bytex.refercheck;

import com.ss.android.ugc.bytex.common.white_list.WhiteList;
import com.ss.android.ugc.bytex.refercheck.visitor.ReferCheckMethodVisitor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultCheckIssueReceiver implements ReferCheckMethodVisitor.CheckIssueReceiver {
    private final List<InaccessibleNode> inaccessableMembers = Collections.synchronizedList(new LinkedList<>());
    private final Map<String, Boolean> checkCache = new ConcurrentHashMap<>();
    @Nonnull
    private final WhiteList whiteList;
    @Nullable
    private final Consumer<InaccessibleNode> consumer;

    public DefaultCheckIssueReceiver(@Nullable WhiteList whiteList, @Nullable Consumer<InaccessibleNode> consumer) {
        this.whiteList = whiteList != null ? whiteList : new WhiteList();
        this.consumer = consumer;
    }

    private boolean shouldCheck(String className, String name) {
        return checkCache.computeIfAbsent(className + "." + name, s -> whiteList.shouldCheck(className, name));
    }

    @Override
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
        if (type == InaccessibleNode.TYPE_CALL_BLOCK_METHOD || (shouldCheck(memberOwner, memberName) && shouldCheck(callClassName, callMethodName))) {
            inaccessableMembers.add(inaccessibleNode);
        } else if (consumer != null) {
            consumer.accept(inaccessibleNode);
        }
    }

    @Override
    public List<InaccessibleNode> getInaccessibleNodes() {
        return Collections.unmodifiableList(inaccessableMembers);
    }
}
