#!/usr/bin/env bash

##############################################################################
##
##  Upload gradle_plugins to TT Nexus (https://wiki.bytedance.net/pages/viewpage.action?pageId=173661094)
##  http://git.bytedance.com:8081/#browse/browse/components:ugc_android    （search key : com.bytedance.android.byteX）
##  run ./publish.sh -m to upload to maven.
##  run ./publish.sh -m -t to upload to maven(snapshot -t=-test).
##  run ./publish.sh to upload to gradle_plugins directory.
##  run ./publish.sh -b to upload to bintray jcenter.
##############################################################################
upload2Maven=false
useSnapshot=false
upload2JCenter=false


while getopts "mtb" opt
do
  case ${opt} in
    m)
      upload2Maven=true
      ;;
    b)
      upload2Maven=true
      upload2JCenter=true
      ;;
    t)
      useSnapshot=true
      ;;
  esac
done
echo ${upload2Maven}
echo ${upload2JCenter}
echo ${useSnapshot}
echo "upload2Maven=$upload2Maven" > upload.properties
echo "useSnapshotMaven=$useSnapshot" >> upload.properties
echo "excludeApp=true" >> upload.properties


#./gradlew clean

if [[ "$upload2JCenter" == "true" ]]; then
    ./gradlew bintrayUpload --stacktrace

else
    ./gradlew uploadArchives
fi

rm -rf ./upload.properties
