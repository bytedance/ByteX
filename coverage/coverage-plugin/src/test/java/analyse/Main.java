package analyse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import analyse.bean.Clazz;
import analyse.bean.Hit;
import analyse.bean.Node;
import analyse.util.FileUtil;
import analyse.util.LogUtil;

/**
 * Created by jiangzilai on 2019-12-09.
 * analyze the data from server. find unused object, hotCode, deduplicate HitCount
 */
public class Main {

    private static Map<String, Node> nodeMap;
    private static Map<String, Clazz> id2ClazzMap;

    private static Map<String, Integer> className2HitCountMap;
    private static Map<String, Boolean> className2ClinitMap;

    private static Gson gson = new Gson();
    private static final String OBJECT = "java/lang/Object";
    private static final String RESULT = "-r";
    private static final String LOG_PATH = "-l";
    private static final String MAPPING = "-m";
    private static final String GRAPH = "-g";

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args == null || args.length < 8) {
            System.out.println("args error");
            System.exit(0);
        }

        long ts = System.currentTimeMillis();

        String resultDir = "";
        String logPath = "";
        String mappingPath = "";
        String graphPath = "";

        for (int i = 0; i < args.length; i = i + 2) {
            switch (args[i]) {
                case RESULT:
                    resultDir = args[i + 1];
                    break;
                case LOG_PATH:
                    logPath = args[i + 1];
                    break;
                case MAPPING:
                    mappingPath = args[i + 1];
                    break;
                case GRAPH:
                    graphPath = args[i + 1];
                    break;
            }
        }

        if (!resultDir.endsWith("/")) {
            resultDir = resultDir + "/";
        }

//         File unusedClassFile = new File(resultDir + "unusedClass.txt");
        File rawHitCountFile = new File(resultDir + "rawHitCount.txt");
        File hitCountFile = new File(resultDir + "hitCount.txt");
//         File rawDataFile = new File(resultDir + "rawData.txt");

        prepareResultFile(rawHitCountFile, hitCountFile);

        nodeMap = gson.fromJson(FileUtil.readFile(graphPath), new TypeToken<Map<String, Node>>() {
        }.getType());

        id2ClazzMap = new HashMap<>(80000);
        className2HitCountMap = new HashMap<>(60000);
        className2ClinitMap = new HashMap<>(10000);

        // 从文件读写数据
        readMappingMap(mappingPath);
        readLogMap(logPath);

        // 生成从上向下的树，同时补全节点
        Node objectNode = prepareObjectNode();

        LogUtil.log("-----------------");
        LogUtil.log("nodeMap size:" + nodeMap.size());
        LogUtil.log("id2ClazzMap.size():" + id2ClazzMap.size());
        LogUtil.log("className2HitCountMap.size():" + className2HitCountMap.size());
        LogUtil.log("className2ClinitMap.size():" + className2ClinitMap.size());
        LogUtil.log("-----------------");

        CountDownLatch countDownLatch = new CountDownLatch(2);

        new Thread(() -> {
            // 生成原始hit数据文件
            try {
                buildRawHitCountFile(rawHitCountFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            } finally {
                countDownLatch.countDown();
            }
        }).start();

        // 生成未使用类文件
//         buildUnusedFile(unusedClassFile.getPath());

        new Thread(() -> {
            // 清理重复计数
            computeHit(objectNode);

            // 生成清理后的hit数据文件
            try {
                buildCoverageFile(hitCountFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        }).start();

        countDownLatch.await();
        long t = System.currentTimeMillis() - ts;
        System.out.println("fine! use time :" + t + " ms");
    }

    // 按命中次数排序，从大到小
    private static void buildRawHitCountFile(String filePath) throws IOException {
        List<Hit> hitList = new ArrayList<>(className2HitCountMap.size());
        for (Map.Entry<String, Integer> stringIntegerEntry : className2HitCountMap.entrySet()) {
            String className = stringIntegerEntry.getKey();
            int hitCount = stringIntegerEntry.getValue();
            if (hitCount == 0) {
                // 如果调用过构造方法，那么设置为1
                if (className2ClinitMap.get(className) != null && className2ClinitMap.get(className)) {
                    hitCount = 1;
                }
            }
            hitList.add(new Hit(className, hitCount));
        }
        buildHitFile(hitList, filePath);
    }

    private static void buildCoverageFile(String filePath) throws IOException {
        List<Hit> hitList = new ArrayList<>(className2HitCountMap.size());
        for (Map.Entry<String, Integer> stringIntegerEntry : className2HitCountMap.entrySet()) {
            String className = stringIntegerEntry.getKey();
            int hitCount = stringIntegerEntry.getValue();
            Boolean isClinit = className2ClinitMap.get(className);
            if (hitCount == 0 && isClinit != null && isClinit) {
                hitCount = 1;
            }
            hitList.add(new Hit(className, hitCount));
        }
        buildHitFile(hitList, filePath);
    }

    private static void buildHitFile(List<Hit> hitList, String filePath) throws IOException {
        hitList.sort((hit, t1) -> {
            if (hit.getHitCount() == t1.getHitCount()) {
                return hit.getClassName().compareTo(t1.getClassName());
            } else {
                return t1.getHitCount() - hit.getHitCount();
            }
        });
        FileWriter fileWriter;
        BufferedWriter bufferedWriter;
        fileWriter = new FileWriter(filePath);
        bufferedWriter = new BufferedWriter(fileWriter);
        for (Hit hit : hitList) {
            bufferedWriter.write(hit.getHitCount() + " " + hit.getClassName() + "\n");
        }
        bufferedWriter.flush();
        bufferedWriter.close();
        fileWriter.close();
    }

    // 先序遍历计算重复次数，递归
    private static void computeHit(Node self) {
        final int rowHitCount = self.getHitCount();

        if (self.getChildrenNodes() != null) {
            // 非叶子节点，继续递归
            for (Node childrenNode : self.getChildrenNodes()) {
                computeHit(childrenNode);
            }
        }
//         if (self.getParent().equals(OBJECT) || self.getHitCount() == 0) {
        if (self.getHitCount() == 0) {
            return;
        }

        Node parentNode = nodeMap.get(self.getParent());
        if (parentNode == null) {
            // object
            return;
        }
        final int newHitCount = self.getHitCount();
        final int parentHitCount = parentNode.getHitCount();
        if (className2HitCountMap.get(parentNode.getClassName()) != null) {
            className2HitCountMap.put(parentNode.getClassName(), parentHitCount - newHitCount);
        }
        parentNode.setHitCount(parentHitCount - newHitCount);
//         LogUtil.log("parentHitCount: " + parentNode.getClassName() + " " + parentHitCount + " -> " + (parentHitCount - newHitCount));

        if (rowHitCount != parentNode.getHitCount()) {
            LogUtil.log("changed: " + (rowHitCount - parentNode.getHitCount()) + " " + self.getClassName() + " " + rowHitCount + " -> " + parentNode.getHitCount());
        }
    }

    private static void buildUnusedFile(String filePath) throws IOException {
        List<String> unusedList = new ArrayList<>(30000);
        for (Map.Entry<String, Integer> entry : className2HitCountMap.entrySet()) {
            String className = entry.getKey();
            if (nodeMap.get(className) == null) {
//                 LogUtil.log("nodeMap can't find " + className);
//                 LogUtil.log("hits : " + entry.getValue());
                continue;
            }
            Node node = nodeMap.get(className);
            if (node.getHitCount() == 0) {
                // 没有调用clinit
                Boolean hasInit = className2ClinitMap.get(className);
                if (hasInit != null && hasInit) {
                    // 调用过clinit，跳过
                    // 这里只调用了静态方法的类
                } else {
                    unusedList.add(className);
                }
            }
        }

        unusedList.sort(String::compareTo);

        FileWriter fileWriter = new FileWriter(filePath);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        for (String s : unusedList) {
            bufferedWriter.write(s + "\n");
        }
        bufferedWriter.flush();
        bufferedWriter.close();
        fileWriter.close();
    }

    private static Node prepareObjectNode() {
        Node objectNode = new Node(OBJECT, new ArrayList<>(nodeMap.size() >> 1), "");
        objectNode.setChildrenNodes(new ArrayList<>(nodeMap.size() >> 1));

        nodeMap.values().forEach(n -> {
            Node parent = nodeMap.get(n.getParent());
            if (parent != null && !parent.getClassName().equals(OBJECT)) {
                if (parent.getChildrenNodes() == null) {
                    parent.setChildrenNodes(new ArrayList<>());
                }
                parent.getChildrenNodes().add(n);
            } else {
                objectNode.getChildren().add(n.getClassName());
                objectNode.getChildrenNodes().add(n);
//                 n.setParent(OBJECT);
            }
        });

        completeTree(objectNode);

        return objectNode;
    }

    private static void completeTree(Node node) {
        if (node.getChildrenNodes() == null || node.getChildrenNodes().size() == 0) {
            return;
        }
        for (Node childrenNode : node.getChildrenNodes()) {
            completeTree(childrenNode);
//             Integer hit = className2HitCountMap.get(childrenNode.getClassName());
//             if (hit == null) {
//                 childrenNode.setHitCount(0);
//                 className2HitCountMap.put(childrenNode.getClassName(), 0);
// //                 LogUtil.log(childrenNode.getClassName() + " is auto completed");
//             } else {
//                 totalSize += className2HitCountMap.get(childrenNode.getClassName());
//             }
        }
        int hit = node.getHitCount();
        int totalSize = 0;
        for (Node childrenNode : node.getChildrenNodes()) {
            totalSize += childrenNode.getHitCount();
        }
        if (hit < totalSize) {
            hit = totalSize;
        }
        node.setHitCount(hit);
        if (className2HitCountMap.get(node.getClassName()) != null) {
            className2HitCountMap.put(node.getClassName(), hit);
        }
//         int hitResult;
//         if (hit == null) {
//             node.setHitCount(totalSize);
// //             if (hit != 0) {
// //                 LogUtil.log("node is auto complete : " + node.getClassName() + " hit:" + hit);
// //             }
//         } else {
//             if (hit < totalSize) {
//                 // 子类在白名单，但在类图转换时转换了进来。孙子不在白名单，被执行，子类没有增加，被修复后增加，导致自身比所有子类之和要小。
//                 // 因此这种情况直接修复就好。
// //                 LogUtil.log(node.getClassName() + " hitCount less than children nodes count");
//                 className2HitCountMap.put(node.getClassName(), totalSize);
//                 node.setHitCount(totalSize);
// //                 LogUtil.log("node is auto complete : " + node.getClassName() + " hit:" + totalSize);
//             }
//             if (hit >= totalSize){
// 
//             }
//         }
//         node.setHitCount(totalSize);
    }

    private static void readLogMap(String userLogPath) throws IOException {
        FileReader fileReader = new FileReader(userLogPath);
        BufferedReader br = new BufferedReader(fileReader);
        String lineContent;
        List<Clazz> mappingList = new ArrayList<>(1000);
        while ((lineContent = br.readLine()) != null) {
            String[] s = lineContent.split(" ");
            String id = s[0];
            try {
                int hitCount = Integer.parseInt(s[1]);
                Clazz clazz = id2ClazzMap.get(id);
                if (clazz != null) {
                    if (nodeMap.get(clazz.getClassName()) == null) {
                        // interface
                        LogUtil.log("exclude interface " + clazz.getClassName());
                        continue;
                    }
                    mappingList.add(clazz);
                    if (clazz.getMethodName().equals(Clazz.CLINIT)) {
                        className2ClinitMap.put(clazz.getClassName(), true);
                        className2HitCountMap.putIfAbsent(clazz.getClassName(), 0);
                    } else {
                        // 如果是构造方法
                        className2ClinitMap.putIfAbsent(clazz.getClassName(), false);
                        Integer hit = className2HitCountMap.get(clazz.getClassName());
                        if (hit == null) {
                            className2HitCountMap.put(clazz.getClassName(), hitCount);
                            nodeMap.get(clazz.getClassName()).setHitCount(hitCount);
                        } else {
                            className2HitCountMap.put(clazz.getClassName(), hit + hitCount);
                            nodeMap.get(clazz.getClassName()).setHitCount(hit + hitCount);
                        }
                    }
                } else {
                    LogUtil.log("没有找到 mappingId:" + id);
                }
            } catch (Exception e) {
                LogUtil.log("error:" + id);
            }
        }

        br.close();
        fileReader.close();

//         FileWriter fileWriter = new FileWriter(rawDataFile);
//         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//         for (Clazz clazz : mappingList) {
//             if (nodeMap.get(clazz.getClassName()) == null) {
//                 LogUtil.log("nodeMap doesn't contain " + clazz.getClassName());
//             } else {
//                 Node n = nodeMap.get(clazz.getClassName());
//                 int hitCount = n.getHitCount();
//                 if (hitCount == 0) {
//                     if (className2ClinitMap.get(n.getClassName())) {
//                         hitCount = 1;
//                     }
//                 }
//                 bufferedWriter.write(clazz.getClassName() + " " + hitCount + " " + clazz.getMethodName() + "\n");
//             }
//         }
//         bufferedWriter.flush();
//         bufferedWriter.close();
//         fileWriter.close();
    }

    private static void readMappingMap(String coverageMappingPath) throws IOException {
        FileReader fileReader = new FileReader(coverageMappingPath);
        BufferedReader br = new BufferedReader(fileReader);
        String lineContent;
        while ((lineContent = br.readLine()) != null) {
            String[] s = lineContent.split(" ");
            String id = s[0];
            String className = s[1];
            String methodName = s[2];
            String desc = s[3];
            Clazz clazz = new Clazz(id, className, methodName, desc);
            className2HitCountMap.put(clazz.getClassName(), 0);
            id2ClazzMap.put(id, clazz);
        }
        br.close();
        fileReader.close();
    }

    private static void prepareResultFile(File... file) throws IOException {
        for (File f : file) {
            if (f.exists()) {
                f.delete();
            } else {
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                f.createNewFile();
            }
        }
    }
}
