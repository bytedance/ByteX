package com.ss.android.ugc.bytex.common.white_list;

import com.android.utils.Pair;
import com.ss.android.ugc.bytex.common.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.ss.android.ugc.bytex.common.utils.Utils.convertToPatternString;
import static com.ss.android.ugc.bytex.common.utils.Utils.resolveDollarChar;

public class WhiteList {
    private final Map<String, List<Pair<Pattern, Pattern>>> excludeList = new HashMap<>(); // 白名单，这些类的成员不检查


    public void initWithWhiteList(List<String> whiteList) {
        if (!excludeList.isEmpty()) {
            excludeList.clear();
        }
        if (whiteList != null) {
            whiteList.forEach(s -> {
                String[] split = s.split("#");
                String key = s.substring(0, 1);
                if (key.equals("*") || key.equals(".") || key.equals("?") || key.equals("+")) {
                    key = "";
                }
                if (split.length == 1) {
                    addWhiteListEntry(key, Pair.of(Pattern.compile(convertToPatternString(resolveDollarChar(s))), Utils.PATTERN_MATCH_ALL));
                } else if (split.length == 2) {
                    addWhiteListEntry(key, Pair.of(Pattern.compile(convertToPatternString(resolveDollarChar(split[0]))),
                            Pattern.compile(convertToPatternString(resolveDollarChar(split[1])))));
                }
            });
        }

//        excludeList.forEach(clz -> Log.i(TAG, "Exclude checking class: " + clz));

//        if (!methodCache.isEmpty()) {
//            methodCache.clear();
//        }
    }


    public void addWhiteListEntry(String prefix, Pair<Pattern, Pattern> entry) {
        excludeList.computeIfAbsent(prefix, k -> new ArrayList<>()).add(entry);
    }


    public boolean shouldCheck(String className) {
        return shouldCheck(className, ".*");
    }

    public boolean shouldCheck(String className, String member) {
        boolean matched = false;
        if (className.isEmpty()) {
            return true;
        }
        List<Pair<Pattern, Pattern>> whiteList = getWhiteList(className);
        if (whiteList == null || whiteList.isEmpty()) {
            return true;
        }
        for (Pair<Pattern, Pattern> pair : whiteList) {
            Pattern classPat = pair.getFirst();
            Pattern methodPat = pair.getSecond();
            if (classPat.matcher(className).matches() && methodPat.matcher(member).matches()) {
                matched = true;
                break;
            }
        }
        return !matched;
    }

    private List<Pair<Pattern, Pattern>> getWhiteList(String className) {
        List<Pair<Pattern, Pattern>> whiteList = excludeList.get(className.substring(0, 1));
        if (whiteList == null || whiteList.isEmpty()) {
            return excludeList.get("");
        }
        return whiteList;
    }

    public boolean isEmpty() {
        return excludeList.isEmpty();
    }

    public void clear() {
        excludeList.clear();
    }
}
