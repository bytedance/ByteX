package com.ss.android.ugc.bytex.common.utils;

import com.android.utils.Pair;
import com.ss.android.ugc.bytex.transformer.TransformContext;
import com.ss.android.ugc.bytex.transformer.cache.FileCache;
import com.ss.android.ugc.bytex.transformer.location.Location;
import com.ss.android.ugc.bytex.transformer.location.SearchScope;

import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static final String MATCH_ALL = ".+";
    public static final Pattern PATTERN_MATCH_ALL = Pattern.compile(MATCH_ALL);
    //    public static final Pattern PATTERN_MATCH_CLASS_NAME = Pattern.compile("(?<package>(.+/)?)(?<class>[^/.]+)");
//    public static final Pattern PATTERN_MATCH_CLASS_RELATIVE_PATH = Pattern.compile("(?<package>(.+/)?)(?<class>[^/.]+).class");
    private static final Pattern rClassSimpleNamePattern = Pattern.compile("R(\\$.+)?");
    private static final Pattern r2ClassSimpleNamePattern = Pattern.compile("R2?(\\$.+)?");

    @Deprecated
    public static boolean isReleaseBuild(Project project) {
        List<String> taskNames = project.getGradle().getStartParameter().getTaskNames();
        for (int index = 0; index < taskNames.size(); ++index) {
            String taskName = taskNames.get(index);
            if (taskName.toLowerCase().contains("release")) {
                return true;
            }
        }
        return false;
    }

    public static <T> List<T> asArrayList(final T[] array) {
        if (array == null) {
            return new ArrayList<T>();
        }
        ArrayList<T> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }

    public static String convertToPatternString(String input) {
        // ?	Zero or one character
        // *	Zero or more of character
        // +	One or more of character
        Map<Character, String> map = new HashMap<>(4);
        map.put('.', "\\.");
        map.put('?', ".?");
        map.put('*', ".*");
        map.put('+', ".+");
        StringBuilder sb = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            Character ch = input.charAt(i);
            String replacement = map.get(ch);
            sb.append(replacement == null ? ch : replacement);
        }
        return sb.toString();
    }

    public static String resolveDollarChar(String s) {
        // 内部类的类名定义用的是$做分隔符
        s = s.replaceAll("\\$", "\\\\\\$");
        return s;
    }

    public static Pair<String, String> resolveClassName(String className) {
        int packageEnd = className.lastIndexOf("/");
        if (packageEnd <= 0) {
            return Pair.of("", className);
        }
        String packageName = className.substring(0, packageEnd);
        className = className.substring(packageEnd + 1);
        return Pair.of(packageName, className);
    }

    public static Pair<String, String> resolveClassPath(String relativePath) {
        int classNameEnd = relativePath.lastIndexOf(".class");
        if (classNameEnd < 0) {
            return resolveClassName(relativePath);
        } else if (classNameEnd == 0) {
            return Pair.of("", "");
        } else {
            return resolveClassName(relativePath.substring(0, classNameEnd));
        }
    }

    public static String getClassName(String relativePath) {
        int classNameEnd = relativePath.lastIndexOf(".class");
        return classNameEnd > 0 ? relativePath.substring(0, classNameEnd) : relativePath;
    }

    /**
     * 获取包名
     *
     * @param className 类名，A/B/C
     * @return 包名  A/B
     */
    public static String getPackage(String className) {
        int packageEnd = className.lastIndexOf("/");
        return packageEnd > 0 ? className.substring(0, packageEnd) : "";
    }

    public static <T> List<T> newList(T elem) {
        List<T> list = new ArrayList<>();
        list.add(elem);
        return list;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static boolean isRFile(String relativePath) {
        int end = relativePath.lastIndexOf(".class");
        return end > 0 && isRClass(relativePath.substring(0, end));
    }

    public static boolean isR2File(String relativePath) {
        int end = relativePath.lastIndexOf(".class");
        return end > 0 && isR2Class(relativePath.substring(0, end));
    }

    public static boolean isRClass(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf("/");
        return rClassSimpleNamePattern.matcher(name.substring(classNameStart + 1)).matches();
    }

    public static boolean isR2Class(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf("/");
        return r2ClassSimpleNamePattern.matcher(name.substring(classNameStart + 1)).matches();
    }

    public static boolean isRClassName(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf(".");
        return rClassSimpleNamePattern.matcher(name.substring(classNameStart + 1)).matches();
    }

    public static boolean isRStyleableClass(String name) {
        if (name == null || name.isEmpty()) return false;
        int classNameStart = name.lastIndexOf("/");
        return "R$styleable".equals(name.substring(classNameStart + 1));
    }

    public static String getInnerRClass(String className) {
        if (className == null || className.isEmpty()) return "";
        int innerClassStart = className.lastIndexOf("$");
        if (innerClassStart == -1) return "";
        return className.substring(innerClassStart + 1);
    }

    public static String replaceDot2Slash(String str) {
        return str.replace('.', '/');
    }

    public static String replaceSlash2Dot(String str) {
        return str.replace('/', '.');
    }

    public static String getAllFileCachePath(TransformContext context, String relativePath) {
        StringBuilder result = new StringBuilder();
        result.append("\ntransform input:");
        Stream<Location.FileLocation> inputLocations = context.getLocator().findLocation(relativePath, SearchScope.INPUT);
        if (inputLocations != null) {
            List<Location.FileLocation> locations = inputLocations.collect(Collectors.toList());
            if (locations.isEmpty()) {
                result.append("not found");
            } else {
                for (Location.FileLocation location : locations) {
                    result.append("\n\t").append(location.getLocation());
                }
            }
        } else {
            result.append("not found");
        }

        result.append("\nproject input:");
        Stream<Location.FileLocation> projectLocations = context.getLocator().findLocation(relativePath, SearchScope.ORIGIN);
        if (projectLocations != null) {
            List<Location.FileLocation> locations = projectLocations.collect(Collectors.toList());
            if (locations.isEmpty()) {
                result.append("not found");
            } else {
                for (Location.FileLocation location : locations) {
                    result.append("\n\t").append(location.getLocation());
                }
            }
        } else {
            result.append("not found");
        }

        result.append("\naar input:");
        Stream<Location.FileLocation> aarLocations = context.getLocator().findLocation(relativePath, SearchScope.ORIGIN_AAR);
        if (aarLocations != null) {
            List<Location.FileLocation> locations = aarLocations.collect(Collectors.toList());
            if (locations.isEmpty()) {
                result.append("not found");
            } else {
                for (Location.FileLocation location : locations) {
                    result.append("\n\t").append(location.getLocation());
                }
            }
        } else {
            result.append("not found");
        }
        return result.toString();
    }

    public static boolean inSamePackage(String classA, String classB) {
        return Objects.equals(getPackage(classA), getPackage(classB));
    }

    public static boolean isClassInit(String name) {
        return "<clinit>".equals(name);
    }
}
