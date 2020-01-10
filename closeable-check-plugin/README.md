**[English](README.md)** | [简体中文](README-zh.md)
## Feature
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The Plugin will check the closeable objects(classes which implement the Closeable interface such as FileInputStream) for possible unclosed (close method not called) code block during project compilation

## Introduction
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The plugin scans all instructions of each method in the project, determines and marks each instruction as opening a closeable object, new closeable object, closing a closeable object, may throw an exception , exception handling, the closeable object nonnull routes, and returns a closeable object. Analyze all possible execution routes according to the jump instruction in the method, and find out that there are cases where the closeable object is not closed (including closeable object has not been closed while method exiting with  instruction throwing a uncatched exception).And finally,report the issues the plugin found.

## Quick Start
* add build classpath

  >classpath "com.bytedance.android.byteX:closeable-check-plugin:${plugin_version}"
* apply and configure the plugin in your build.gradle(application)

    ```groovy
    apply plugin: 'bytex.closeable_checker'
    closeable_checker {
            enable true//enable state
            enableInDebug true//whether enable in debug build
            ignoreWhenMethodParam = true//whether tread ParameterType as an close operation。
            ignoreAsReturn = true//whether tread ReturnType as an close operation。true is Recommended
            ignoreField = true//whether ignore when closeable object is  field while analyzing。true is Recommended
            ignoreMethodThrowException = true//whether ignore all exception object is  field while analyzing。true is Recommended
            strictMode = true//strict mode:whether to check all exception handlers 。true is Recommended。
            logLevel "DEBUG"
            onlyCheckList=[
                    //classpaths。 check all if empty 
                    "com/ss/*"     
            ]
            whiteList = [
                    //classpaths those will be skip to check
                    "android*",
                    "kotlin*",
                    "java*"
            ]
            closeableList = [
                    //closeable object(Automatically include its subclasses)
                    "java/io/InputStream",
                    "java/io/OutputStream",
                    "java/io/PrintStream",
                    "java/io/Writer",
                    "java/io/Reader",
                    "java/io/RandomAccessFile",
                    "java/nio/file/FileSystem",
                    "android/database/Cursor",
                    "java/util/zip/ZipFile",
                    "android/database/sqlite/SQLiteClosable",
                    "okhttp3/Response",
                    "android/media/MediaDataSource",
                    "java/net/MediaDataSource",
                    "android/net/LocalSocket",
                    "okio/Sink",
                    "okio/Source",
                    "okio/UnsafeCursor",
                    "java/nio/channels/Selector",
                    "android/arch/persistence/db/SupportSQLiteProgram"
            ]
            excludeCloseableList = [
                    //closeable object which will be ignored(Without its subclasses)
                    "java/io/StringReader",
                    "java/io/StringWriter",
                    "java/io/ByteArrayOutputStream",
                    "java/io/ByteArrayInputStream",
            ]
    }
    ```
    
## Analysis Result<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The check result will be printed into the corresponding `app/build/ByteX/ByteX_report_ByteX.html` by default.The result may look like the picture below:<br/>
![Check Result](img/检查结果.png)
## Deficiencies
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Normally，closeable objects are used much complicated in our projects，such as passing a closeable object to anther method and it is much diffcult to  analysis，so we choose to analyze only the instructions in one method and make some general assumptions.So the result is not completely accurate。 As tested in douyin project，the accuracy of the check result can be about 40%，You can add whitelists if there are inaccurate inspection results .

## Analysis Process
- 1、Use ASM Node library read and record all instructions of the method passed by ByteX
- 2、In order to speed up the analysis, first of all , find out whether the method's instructions contains any operations about the closeable object. If not, skip the analysis directly.。
- 3、scan and mark instructions
     * ReturnCloseableType：a closeable object is returned by the instruction
     * NewType:a closeable object is created by the instruction
     * NullType: an instruction to determine whether a closeable object is null
     * NotNullType:an instruction to determine whether a closeable object is not null
     * ParameterType:a closeable object is passed as parameter to a method call instruction
     * ExceptionType:An instruction that may throw an exception
     * ReturnType:a closeable object is returned by the method return instruction
- 4、Use the FlowAnalyzer(Asm Analysis library) to read back all instructions and analyze all execution routes。
- 5、Analyze all instructions executed by each execution route: An OpenCloseableType or NewType instruction will be record as an opening operation, if the instruction has been recorded as opening before, we believe that there is no closure to the closeable object here(no closed in a loop body); do for NullType and NotNullType The adaptive discard judgment, because close is not required for the closeable object which is null; CloseType is marked as the closing operation, ParameterType and ReturnType are marked as closed operations(special handling) as appropriate. The opening operations we recorded before will be removed as we find a corresponding closing operation. Any opening operation(no removed) before An ExceptionType with no exception handling will be marked as ' Not Closed With Exception'.
- 6、At the end, all opening operations which were not removed will be marked 'Not Closed'.
- 7、Print result.