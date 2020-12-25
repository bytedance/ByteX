# Change Log
### Version 0.2.6
- fix bug in access-inline with coroutines[Issue](https://github.com/bytedance/ByteX/issues/31)
- add quick-refer-check task:processQuickReferCheckWith${variant.capitalize}

### Version 0.2.5
- fix get applying variant scope(Compatible with AGP)

### Version 0.2.4
- fix APK with empty code on the higher version of AGP due to getScope
- fix blocking compilation while calling getArtifact
  > Note :This is an experimental bug fix.Need to enable the
  > switch(bytex.forbidUseLenientMutationDuringGetArtifact) in
  > gradle.properties manually
- fix a badcase in access-inline-plugin

### Version 0.2.3
- use ASM6
- remove usages of internal interfaces
- configurable output with fixed timestamp

### Version 0.2.2
- fix bugs:default transform configuration、NPE
- optimize daemon cache with --no-daemon

### Version 0.2.1
- optimize incremental build:Cache graph nodes with daemon

### Version 0.1.9
- optimize refer checker:More detailed error information
- add [ByteXBuildListener](wiki/ByteX-Developer-API-en.md#perceive-the-lifecycle-of-bytex)

### Version 0.1.8
- improve compatibility
- optimize hook proguard
- add checkIncrementalInDebug
- fix npe cased by refer-checker

### Version 0.1.7
- Optimize incremental build and fix bugs
- Fix memory leak in plugins
- Support multi variants build

### Version 0.1.6
- Upgrade AGP 3.5.3
- Fix memory leak in HookProguard
- Run refer-check-plugin in single flow in order to check any issues producted by bytex'plugin
- Unified threadpool and run pipleline lifecycles in parallel

### Version 0.1.5
- Fix bugs with getter-setter-inline plugins

### Version 0.1.4
- Fixed a bug caused by incorrectly processing removed input file.
- Optimize graph cache reading and writing efficiency and  size
- More efficient and convenient way for creating a [incremental](wiki/ByteX-Developer-API-en.md#incremental-plugin) plugin 

### Version 0.1.3
- shrink styleable(shrink 500KB+ on douyin)
- RFileKnife(fix R.java code to large)
- ButterKnifeCheck(detect exceptions caused by cross-module use of ButterKnife)
- optimize graph cache and html log
- fix bug(fd leak)

### Version 0.1.2
- fix memory leak
- optional html log、optional graph cache for incremental build

### Version 0.1.1

- Removed some code in GradleToolKit and add `booster-android-gradle-api` as dependencies.

### Version 0.1.0

Initial release