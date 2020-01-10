package com.ss.android.ugc.bytex.closeable;

import com.ss.android.ugc.bytex.common.BaseExtension;

import java.util.ArrayList;
import java.util.List;

public class CloseableCheckExtension extends BaseExtension {
    public static final String SEPARATOR = "#";
    /**
     * The classes in the whiteList will skip check
     * Format:ClassName#MethodName# method signature
     * Support pattern matching
     * such as "java*"
     */
    private List<String> whiteList = new ArrayList<>();
    /**
     * Specify which closeable classes need to be checked. (include all sub classes)
     * for example:
     * "java/io/InputStream",
     * "java/io/OutputStream",
     * "java/io/PrintStream",
     * "java/io/Writer",
     * "java/io/Reader",
     * "java/io/RandomAccessFile",
     * "java/nio/file/FileSystem",
     * "android/database/Cursor",
     * "java/util/zip/ZipFile",
     * "android/database/sqlite/SQLiteClosable",
     * "okhttp3/Response",
     * "android/media/MediaDataSource",
     * "java/net/MediaDataSource",
     * "android/net/LocalSocket",
     * "okio/Sink",
     * "okio/Source",
     * "okio/UnsafeCursor",
     * "java/nio/channels/Selector",
     * "android/arch/persistence/db/SupportSQLiteProgram"
     */
    private List<String> closeableList = new ArrayList<>();
    /**
     * Specify which closeable classes need skip checked.('close()' action is useless)
     * "java/io/StringReader",
     * "java/io/StringWriter",
     * "java/io/ByteArrayOutputStream",
     * "java/io/ByteArrayInputStream",
     */
    private List<String> excludeCloseableList = new ArrayList<>();
    /**
     * Only the classes in the onlyCheckList will be checked
     * Format:ClassName#MethodName# method signature
     * Support pattern matching
     * For Example:
     * "com/ss*",
     * "com/bytedance*"
     */
    private List<String> onlyCheckList = new ArrayList<>();
    /**
     * Whether to ignore unclosed objects passed as parameters to closeable objects
     */
    private boolean ignoreWhenMethodParam = false;
    /**
     * Whether to ignore unclosed objects passed as return value
     */
    private boolean ignoreAsReturn = true;
    /**
     * Whether to ignore unclosed objects while closeable objects are fields
     */
    private boolean ignoreField = true;
    /**
     * Whether to ignore the calling method may throw an exception and not close
     */
    private boolean ignoreMethodThrowException = true;
    /**
     * Whether to strictly judge the close state of the Closeable object when all code is abnormal
     */
    private boolean strictMode = true;

    @Override
    public String getName() {
        return "closeable_checker";
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    public List<String> getCloseableList() {
        return closeableList;
    }

    public void setCloseableList(List<String> closeableList) {
        this.closeableList = closeableList;
    }

    public boolean isIgnoreWhenMethodParam() {
        return ignoreWhenMethodParam;
    }

    public void setIgnoreWhenMethodParam(boolean ignoreWhenMethodParam) {
        this.ignoreWhenMethodParam = ignoreWhenMethodParam;
    }

    public boolean isIgnoreAsReturn() {
        return ignoreAsReturn;
    }

    public void setIgnoreAsReturn(boolean ignoreAsReturn) {
        this.ignoreAsReturn = ignoreAsReturn;
    }

    public boolean isIgnoreField() {
        return ignoreField;
    }

    public void setIgnoreField(boolean ignoreField) {
        this.ignoreField = ignoreField;
    }

    public List<String> getExcludeCloseableList() {
        return excludeCloseableList;
    }

    public void setExcludeCloseableList(List<String> excludeCloseableList) {
        this.excludeCloseableList = excludeCloseableList;
    }

    public List<String> getOnlyCheckList() {
        return onlyCheckList;
    }

    public void setOnlyCheckList(List<String> onlyCheckList) {
        this.onlyCheckList = onlyCheckList;
    }

    public boolean isIgnoreMethodThrowException() {
        return ignoreMethodThrowException;
    }

    public void setIgnoreMethodThrowException(boolean ignoreMethodThrowException) {
        this.ignoreMethodThrowException = ignoreMethodThrowException;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public String getSeparator() {
        return SEPARATOR;
    }
}
