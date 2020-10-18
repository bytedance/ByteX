**[English](ByteX-Developer-API-en.md)** | [简体中文](ByteX-Developer-API-zh.md)
## Quick Start

### Setup
- Clone the project code(<https://github.com/bytedance/ByteX>) to your computer and checkout a new branch for developing.
- ByteX project is composed by each module,it means that one plugin corresponds to one module.So. just create a new java library module.

<img src="AS_new_module.png" width="60%" height="60%" align=center><Br/>

- And then,configare in the module's build.gradle 

```groovy
//Use common dependencies and publish configuration
apply from: rootProject.file('gradle/plugin.gradle')
//extra dependencies 
dependencies {
    compile project(':TransformEngine')
    implementation "br.usp.each.saeg:asm-defuse:0.0.5"
}
```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The plugin module is ready now!!!

### Custom Plugin

- Crate a new plugin based on ByteX, you need to create at least two classes

1. One is Extension:It is much more like java bean which is used to configure the plugin
2. Another is Plugin,It must implement the Plugin <Project> interface. Of course you can implement this Plugin in a simpler way:inherit from the abstract class AbsMainProcessPlugin or CommonPlugin directly. A simple example is shown as follow:

```java
//@PluginConfig("bytex.sourcefile")
public class SourceFileKillerPlugin extends CommonPlugin<SourceFileExtension, SourceFileContext> {
    @Override
    protected SourceFileContext getContext(Project project, AppExtension android, SourceFileExtension extension) {
        return new SourceFileContext(project, android, extension);
    }

    @Override
    public boolean transform(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain) {
        //We are going to  modify the bytecodes, so we need to register a ClassVisitor
        chain.connect(new SourceFileClassVisitor(extension));
        return super.transform(relativePath, chain);
    }

    @Nonnull
    @Override
    public TransformConfiguration transformConfiguration() {
        return new TransformConfiguration() {
            @Override
            public boolean isIncremental() {
                //The plugin is incremental by default.It should return false if incremental is not supported by the plugin
                return true;
            }
        };
    }
}
```

- After creating the plugin, you need to make gradle recognize our plugin. We have two configurate methods shown as follow：

1. Use Annotation(recommanded)

 ```
@PluginConfig("bytex.sourcefile")
public class SourceFileKillerPlugin extends CommonPlugin<SourceFileExtension, SourceFileContext> {...}
```

2.  write properties file <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;We need to create a properties file in the resource directory. The file name of the properties file corresponds to the id of the plugin. The id of  the plugin is decided by yourself but it must be unique in the project. As is shown in the figure below, the properties file name is bytex.sourcefile.properties,  you can configure it like this:

<img src="plugin_module_directory_tree.png" width="60%" height="60%" align=center><Br/>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;You need to configure the full class name (package name + class name) of our Plugin class in the properties file, for example:

```properties
implementation-class=com.ss.android.ugc.bytex.sourcefilekiller.SourceFileKillerPlugin
```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Then you need to configure this plugin to use this plugin in your app project:

```groovy
apply plugin: 'bytex.sourcefile'
```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Now, our new plugin has begun to take shape, to make our plugin process class files, you need to define the corresponding ClassVisitor or directly operate the ClassNode.

### Publish Plugin

- publish to local

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Execute the publish script in the root directory of the project.

```
./publish.sh
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Or double-click uploadArchives to publish the plugin to the gralde_plugins directory of the local project.

<img src="uploadArchives.png" width="50%" height="50%" align=center><Br/>

<img src="gralde_plugins.png" width="50%" height="50%" align=center><Br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;You need publish the pulgin again if you want to take effect the changed code.

- publish to maven

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If the plugin has been  developed and passed the test, we need to publish the plugin to the maven to integrate it into the actual project.
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;First,create a new local.properties configuration file in the root directory of ByteX and add the following configuration:

```
UPLOAD_MAVEN_URL=xxx
UPLOAD_MAVEN_URL_SNAPSHOT=xxx
USERNAME=xxx
PASSWORD=xxx
USERNAME_SNAPSHOT=xxx
PASSWORD_SNAPSHOT=xxx
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Then, upgrade the upload_version of ext.gradle.

![alter_upload_version](alter_upload_version.png)

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Similarly, execute the script or double-click uploadArchives to publish the plugin to online maven.

```
./publish.sh -m
```

- publish to snapshot

 version=$current_version-${user.name}--SNAPSHOT
 
```
./publish.sh -m -t
```

### Debug Plugins

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Create a new 'run configuration' in AndroidStudio.

<img src="AS_edit_configuration.png" width="50%" height="50%" align=center><Br/>

<img src="AS_remote_debug.png" width="60%" height="60%" align=center><Br/>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;After publishing the plugin locally and connecting it to the app project, append parameters at the end of the build-command before executing the build-command.For example:

```
./gradlew clean :example:assembleDouyinCnRelease -Dorg.gradle.debug=true --no-daemon
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Then switch to the Configuration that you created just now, and click the debug button.

<img src="AS_debug_button.png" width="50%" height="50%" align=center><Br/>


### Demo

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;SourceFileKiller is a custom plugin with less code and can be used as a demo. It does very simple things：delete SourceFile and line number attributes in bytecodes

<img src="plugin_module_directory_tree.png" width="60%" height="60%" align=center><Br/>

### For External Project
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If you need to develop a plugin based on ByteX in external project , you need to configure dependencies like below:

```groovy
compile gradleApi()
compileOnly "com.android.tools.build:gradle:$gradle_version"
compile "com.bytedance.android.byteX:common:${bytex_version}"
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If you want to register the plugin with annotations, you can introduce the following dependencies (optional):

```groovy
compile "com.bytedance.android.byteX:PluginConfigProcessor:${bytex_version}"
kapt "com.bytedance.android.byteX:PluginConfigProcessor:${bytex_version}"
```

## Primary API

### Modify Class With ASM

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ByteX is based on ASM, we can read and write class files by registering ClassVisitor or operating ClassNode directly when processing class files. (If you need to receive the bytecode of a file as input, you can refer to the Advanced API below).

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;By default, the Transform formed by ByteX has at least one regular process (MainProcess) for Class files which includes the following steps:

1. traverse callback：Iterate through all the build products (class files and jar class) in the project once, do analysis only and without modifying the input files;
2. traverseAndroidJar callback：Iterate through all the class files in android.jar (The version of android.jar is determined by the target api in the project). It is designed for building a complete class diagram.
3. transform callback：Iterate through all the build products in the project again, process the class file and output it (It may be directly written to the file as transform outputs or be used as input for the next process).

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;So, one process will traverse through all the classes in the project twice. We call the transform  process as one TransformFlow. Developers can design their own  TransformFlow( it can contain multiple traverses, or only contain transform classes but no traverse, etc.), please refer to the Advanced API.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Let's back to the SourceFileKiller Plugin we talked before, the plugin inheriteds from CommonPlugin,if we need to process the class file during the transform phase (the third step), the Plugin class needs to override one of the following two methods:

```java
/**
 * transform all the classes in the project 
 *
 * @param relativePath relativePath of the class
 * @param chain        object for ClassVisitor registration
 * @return if return true, this class will be outputed ；if return false, this class will be deleted.
 */
@Override
public boolean transform(String relativePath, extension chain) {
    chain.connect(new SourceFileClassVisitor(context));
    return true;
}
/**
 * transform all the classes in the project 
 *
 * @param relativePath relativePath of the class
 * @param node         classNode which contains all class infos.
 * @return  if return true, this class will be outputed ；if return false, this class will be deleted.

 */
@Override
public boolean transform(String relativePath, ClassNode node) {
    // do something with ClassNode
    return true;
}
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;We can see that the only difference between these two overloaded methods is their input parameters.The former uses ASM's ClassVisitor, and the latter uses ASM's Tree API, which can directly handle ClassNode.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Similarly, if we need to analyze the class file during the traverse phase, the Plugin class can override the following methods:

```java
    /**
     * traverse all the classes in the project 
     *
     * @param relativePath relativePath of the class
     * @param chain        object for ClassVisitor registration
     */
    void traverse(@Nonnull String relativePath, @Nonnull ClassVisitorChain chain);

    /**
     * traverse all the classes in the project
     *
     * @param relativePath relativePath of the class
     * @param node        classNode which contains all class infos.
     */
    void traverse(@Nonnull String relativePath, @Nonnull ClassNode node);
```

### Log

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;We recommend developers record all the modifications the plugin did while transforming the classes into logs. We can find out what has been modified by our plugins and find bugs through these logs. Each plugin has its own logger and log file which provided by ByteX. Developers can record all changes by ByteX logger as easily as common Logger
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;When you need to record logs, you could obtain the Logger object from the context and call the corresponding log method.

![Context.getLogger](Context.getLogger.png)

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The logs will be recorded in the file whose path will locate at the `app/build/ByteX/$ {variantName}/${extension_name}/${logFile}`. If the logFile is not configured in gradle, the file name will use `$(extension_name)_log.txt` by default.<br/> 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;At the same time, a visual html log file will be generated as transform ends. The data of this html page comes from plugins of the transform, developers don`t need to care about it, it is generated automatically. The file locates at app/build/ByteX/ByteX_report_{transformName}.html.

<img src="log_dir.png" width="40%" height="40%" align=center><Br/>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Tips: If the plugin has needs to generate extra files, we recommend developers use `context.buildDir()` to get a directory to place the files, This directory locates at the `app/build/ByteX/$ {extension_name}/` 



## Advanced API

### TransformFlow

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;In order to provide more flexibility for ByteX-based plugins, we introduce the concept of TransformFlow.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The process of processing all the build products (usually class files) is defined as a TransformFlow. A plugin can run in an independent TransformFlow , or you can take a ride of the global MainTransformFlow（Traverse, traverseAndroidJar and transform form a MainTransformFlow）.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;You need to override provideTransformFlow which is a method belongs to IPlugin if you want use a customized TransformFlow for the plugin.

```java
// Hitchhiking to the global MainTransformFlow, the way of most plugins use
@Override
protected TransformFlow provideTransformFlow(@Nonnull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
    return mainFlow.appendHandler(this);
}
// create a MainTransformFlow which is independent of the global MainTransformFlow
@Override
protected TransformFlow provideTransformFlow(@Nonnull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
    return new MainTransformFlow(transformer, new BaseContext(project, android, extension));
}
// create a customized TransformFlow which is independent of the global MainTransformFlow
@Override
protected TransformFlow provideTransformFlow(@Nonnull MainTransformFlow mainFlow, @Nonnull TransformContext transformContext) {
    return new AbsTransformFlow(transformer, new BaseContext(project, android, extension)) {
        @Override
        protected AbsTransformFlow beforeTransform(Transformer transformer) {
            return this;
        }
    
        @Override
        protected AbsTransformFlow afterTransform(Transformer transformer) {
            return this;
        }
    
        @Override
        public void run() throws IOException, InterruptedException {
            // do something in flow.
        }
    };
}
```

![TransformFlow](TransformFlow.png)

### Class Diagram

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Normally，there should be a Class Diagram in each TransformFlow which constains all class relationship between classes of project、classes of jar and classes of Android.jar，it depends on your implementation of TransformFlow.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;When your plugin run in MainTransformFlow (the default is this TransformFlow), the plugin will generate the class diagram of this TransformFlow automatically after traverse (including traverseArtifactOnly and traverseAndroidJarOnly), and the diagram will be placed in the corresponding context object. Class graph object can be obtained by call `context.getClassGraph()`.

```java
public class BaseContext<E extends BaseExtension> {
    protected final Project project;
    protected final AppExtension android;
    public final E extension;
    private ILogger logger;
    private Graph classGraph;//class diagram 
    ...
    public Graph getClassGraph() {
        return classGraph;
    }
}
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Notes:

- In cases when TransformFlow does not complete the traverse (to be precise, beforeTransform of CommonPlugin), the class diagram does not exist, and the object that obtains the class diagram will be null.<br/>
- Every plugin that inherits from CommonPlugin must call the corresponding super method if it overrides the beforeTransform method, otherwise the class diagram object will not be passed to the Context object of the current plugin.<br/>
- The two TransformFlow class diagrams are isolated. Generally, each TransformFlow will modify the classes, the class diagrams generated by the two TransformFlows are generally different.

### File Locator
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ByteX has basic reading and output capabilities for INPUTS.If you need to obtain more information such as transform inputs, project inputs, and input aars, you can use the capabilities provided by the Engine layer to obtain the corresponding file inputs.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;You can get all the inputs of the transform by following way:

```
context.getTransformContext().allFiles()
```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;You can get all the merged resources of the project by following way:

```
context.getTransformContext().getArtifact(Artifact.MERGED_RES)
```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;You can get the location of the file by scope by following way:

```
context.getTransformContext().getLocator().findLocation("${className}.class",SearchScope.ORIGIN)
```


### MainProcessHandler

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MainProcessHandler is bound to the MainTransformFlow processor, and each class will be processed in each step by calling the corresponding method of MainProcessHandler for processing. Generally, our plugins have already implemented this interface, and developers can override the corresponding methods to get the corresponding callbacks.

- There are series of methods like init、 traverse, and transform all process class files through ASM in the MainProcessHandler. In order to provide greater flexibility, you can register your own FileProcessor by overriding the `List<FileProcessor> process (Process process)` method .

- There a method named flagForClassReader in the MainProcessHandler which could customize the flag passed in when ClassReader calls the accept method to read the class file.The default value is `ClassReader.SKIP_DEBUG`

### FileProcessor

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If developers don`t want to use the upper-layer interface encapsulated by ASM to process class files, ByteX also provides a low-level API-FileProcessor.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FileProcessor is similar to the interceptor design of OkHttp. Each class file will be processed through a series of FileProcessors. The advantages of using this interface is that it is more flexible! As an interceptor, you can make the subsequent FileProcessor finish processing before processing, or you can even process it without passing it to the following FileProcessor.

```java
public interface FileProcessor {
    Output process(Chain chain) throws IOException;
    interface Chain {
        Input input();
        Output proceed(Input input) throws IOException;
    }
}

public class CustomFileProcessor implements FileProcessor {
    @Override
    public Output process(Chain chain) throws IOException {
        Input input = chain.input();
        FileData fileData = input.getFileData();
       	// do something with fileData
        return chain.proceed(input);
    }
}
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;To register a custom FileProcessor, we also provide a more convenient way:Register the FileProcessor with the annotation @Processor on the custom Plugin.

```java
@Processor(implement = CustomFileProcessor.class)
@Processor(implement = CustomFileProcessor.class, process = Process.TRAVERSE)
public class CustomPlugin extends CommonPlugin<Extension, Context> {...}
```



### FileHandler

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FileHandler is an interface that is further encapsulated by FileProcessor.The input parameter is a FileData, and the FileData contains the bytecode of the file.

```java
public interface FileHandler {
    void handle(FileData fileData);
}
public class CustomFileHandler implements FileHandler {
    @Override
    public void handle(FileData fileData) {
       	// do something with fileData
    }
}
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;To register a custom FileHandler, just like FileProcessor, we also provide a more convenient way:Register the FileHandler with the annotation @Handler on the custom Plugin.

```java
@Handler(implement = CustomFileHandler.class)
@Handler(implement = CustomFileHandler.class, process = Process.TRAVERSE)
public class FlavorCodeOptPlugin extends CommonPlugin<Extension, Context> {...}
```

### TransformConfiguration

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The methods in this interface correspond to the methods in the Transform interface in the Transform API.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Each plugin can customize some configurations by overriding the corresponding interface method belongs to transformConfiguration.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;For example:

```java
@Override
public TransformConfiguration transformConfiguration() {
    return new TransformConfiguration() {
        @Override
        public Set<QualifiedContent.ContentType> getInputTypes() {
            return TransformManager.CONTENT_JARS;
        }
    };
}
```

### Transform Hooker

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Relying on Android's transform api registerTransform, you can only register your transform before to proguard, dex, and other built-in transforms. If you want the plugin doing something after proguard, you need some reflection hooks. The IPlugin interface of ByteX provides a hookTransformName method .Just need to override this method and return the name of the transform you need to hook, then your plugin can execute before this transform.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;For example, if you want your plugin to be executed after proguard (before dex), you can do this:

```java
public class DoAfterProguardPlugin extends CommonPlugin<Extension, Context> {
    @Override
    public String hookTransformName() {
        return "dexBuilder";
    }
}
```
### Add Extra Files

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If you need to output some additional files during the transform, you can override the beforeTransform method and call the addFile method provided by TransformEngine.

```
    @Override
    public void beforeTransform(@Nonnull @NotNull TransformEngine engine) {
        super.beforeTransform(engine);
        engine.addFile("affinity",new FileData("addFile test".getBytes(),"com/ss/android/ugc/bytex/test.txt"));
    }
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The first parameter of addFile, affinity, can be set at will. If the affinity of the two addFile calls is the same, then the two files will be in the same output directory.


### Incremental Plugin

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Plugin base on bytex configuared incremental by default.During incremental build, the ByteX framework will call the plugin‘s traverseIncremental method (two overloaded methods.Note: this method is called before the beforeTraverse), and will pass all files except the NotChanged.It will be decompressed if it is a Jar file and pass each entry as a single file (abstract into a FileData) in. The two methods are defined below:

```java
    /**
     * traverse all incremental file which status is ADD,REMOVE or CHANGED
     * file will be uncompressed which is jar input.
     * only valid while during incremental build
     *
     * @param fileData incremental file
     * @param chain  	If it is a class, the corresponding ClassVisitorChain will be passed to add a custom ClassVisitor, or null if there is not a class
     */
    default void traverseIncremental(@Nonnull FileData fileData, @Nullable ClassVisitorChain chain) {
    }

    /**
     * traverse all incremental file which status is ADD,REMOVE or CHANGED
     * file will be uncompressed which is jar input.
     * only valid while during incremental build
     *
     * @param fileData Incremental file, and the file must be a class file.
     * @param node     Tree Node
     */
    default void traverseIncremental(@Nonnull FileData fileData, @Nonnull ClassNode node) {
    }
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If your plugin supports incremental mode, but finds that incremental processing cannot be continued in the incremental mode, you can request full compilation from bytex in the beforeTraverse method or before:

```java
	context.getTransformContext().requestNotIncremental();
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;If your plugin can't provide incremental compilation,please configuare as follows:

```java
public class SourceFileKillerPlugin extends CommonPlugin<SourceFileExtension, SourceFileContext> {
    @Nonnull
    @Override
    public TransformConfiguration transformConfiguration() {
        return new TransformConfiguration() {
            @Override
            public boolean isIncremental() {
                //The plugin is incremental by default.It should return false if incremental is not supported by the plugin
                return true;
            }
        };
    }
    ...
}
```

## Perceive The Lifecycle Of ByteX
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ByteX adds the corresponding hook code at the beginning and end of the key lifecycle events  for developers monitoring to do some operations when ByteX executes at certain timings.You can set your custom listeners with `ByteXBuildListenerManager`,for example:<br/>
```java
ByteXBuildListenerManager.INSTANCE.registerByteXBuildListener(yourByteXBuildListener)
ByteXBuildListenerManager.INSTANCE.registerMainProcessHandlerListener(yourMainProcessHandlerListener)
```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;More details could be found in `ByteXBuildListener` and `MainProcessHandlerListener`

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;By default, ByteX has a built-in default listener to record lifecycle events . Results will be recorded in the two jsons located at app/build/ByteX/build/ after compilation is completed.


## Configuration Properties In ByteX
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Users can config each plugin with it`s Extension in DSL.In addition, there are some properties witch are used by ByteX engine and could be configured in gradle.properties.Here is the list:<br/>

- bytex.globalIgnoreClassList:A relative file path relative to the project root directory,witch contains a list of classes(supporting pattern matching). If an exception occurs when processing the classes in the lists, ByteX will process it internally instead of throwing it to terminate the compilation
- bytex.enableDuplicateClassCheck:Whether to check for class duplication，boolean,true by default
- bytex.enableHtmlLog:Whether to generate html report，boolean,true by default
- bytex.enableRAMCache:Whether to enable memory cache to store ByteX`s cache, this configuration is used to optimize incremental build, because loading files may take time, boolean, true by default. If it is non-incremental (CI), it is recommended to configure to false
- bytex.enableRAMNodesCache:Whether to cache the Nodes in the Graph to the memory, it takes effect when enableRAMCache is true，boolean,true by default
- bytex.enableRAMClassesCache:Whether to cache the ClassEntity in Graph to memory, it takes effect when enableRAMCache is true，boolean,false by default
- bytex.asyncSaveCache：Whether to save the cache asynchronously (non-incremental plugins doing nothing), boolean , true by default.
- bytex.verifyProguardConfigurationChanged:Whether to verify whether the keep rules obtained by the plugin is the same as the keep rules when Proguard is executed. boolean , true by default.
- bytex.checkIncrementalInDebug:Whether to check non-incremental plugins running with its enableInDebug is true，boolean , false by default.
- bytex.enableSeparateProcessingNotIncremental:Whether to executing non-incremental plugins in single TransformTask automatically. If there is a plugin which is incremental, all ByteX plugins will run in non-incremental, which will greatly reduce the speed of incremental build. After the switch is turned on, the plugins that support increment will be executed together. Incremental plugins will run independently in a transform. oolean ,false by default.
- bytex.${extension.getName()}.alone:Whether to run the plugin independently，boolean , false by default.
- bytex.useFixedTimestamp:Whether to use the fixed timestamp (0) of the entity in the output jar, this has a great benefit for incremental compilation because the entity is same and the timestamp is unchanged,and tasks after bytex could hit the cache (such as DexBuilder).boolean , false by default.

## Development Considerations
### Branch Management
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;For new features, in principle, it can only be pulled from the develop branch.When it needs to be merged into the master branch, it needs to be merged into the develop branch first, and unified into the master branch later.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;For simple bugfixes, you can pull the modification from the master branch directly and submit a mr to the master branch. After mr is merged, you need to submit another mr from master-> develop branch to sync the changes.
### Development Specifications
  - Name Of Module<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Every word must be lowercase, use "-" (underscore) to separate multiple words. Please end the module name with "-plugin" If it is a plugin module. If the feature contains both a plugin module and a runtime library ("java Or library "), please put all modules related into the one sub-directory in the ByteX root directory to avoid a feature scattered in the byteX root directory.
  - Package<br/>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Package name must start with "com.ss.android.ugc.bytex", and dd a subpackage name named after the feature to distinguish other modules  <br/>
  - Comments<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;It is recommended to write comments for complex or core code.It is recommended to use English for comments.
  - Documentation<br/>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;It is recommended to write the documentation after the plugin is stable.The document contains two copies in Chinese and English, both placed under the module of the corresponding plugin.