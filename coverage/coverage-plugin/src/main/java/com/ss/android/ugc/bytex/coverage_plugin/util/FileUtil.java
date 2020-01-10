package com.ss.android.ugc.bytex.coverage_plugin.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;

/**
 * Created by jiangzilai on 2019-07-22.
 */
public class FileUtil {

    public static void saveFile(String content, String path) throws IOException {
        File file = new File(path);
        checkFile(file);
        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(content);
            bufferedWriter.flush();
        }
    }

    public static void appendContent(String content, String path) throws IOException {
        File file = new File(path);
        checkFile(file);
        try (FileWriter fileWriter = new FileWriter(file, true)) {
            fileWriter.write(content);
            fileWriter.flush();
        }
    }

    public static void readTxtByLine(String filePath, Function<String, Boolean> function) throws IOException {
        File file = new File(filePath);
        checkFile(file);
        try (FileReader fileReader = new FileReader(file);
             BufferedReader br = new BufferedReader(fileReader)) {
            String lineContent;
            while ((lineContent = br.readLine()) != null) {
                function.apply(lineContent);
            }
        }
    }

    public static void checkFile(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
