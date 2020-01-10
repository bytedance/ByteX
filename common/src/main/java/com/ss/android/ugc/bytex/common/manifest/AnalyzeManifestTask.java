package com.ss.android.ugc.bytex.common.manifest;

import com.ss.android.ugc.bytex.common.BaseContext;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * if you want to get the file AndroidManifest.xml, please try
 * TransformContext.getArtifact(Artifact.MERGED_MANIFESTS) instead.
 */
@Deprecated
public class AnalyzeManifestTask implements Action<Task> {
    private final String variantName;
    private final BaseContext context;
    public final AndroidManifestXmlReader xmlReader = new AndroidManifestXmlReader();
    private AndroidManifestXmlReader.Visitor visitor;

    public AnalyzeManifestTask(String variantName, BaseContext context, @NotNull AndroidManifestXmlReader.Visitor visitor) {
        this.variantName = variantName;
        this.context = context;
        this.visitor = visitor;
    }

    @Override
    public void execute(Task task) {
        File manifest = findFullManifest(task.getOutputs().getFiles());
        context.getLogger().i(String.format("Found merged AndroidManifest.xml [%s].", manifest.getAbsolutePath()));
        xmlReader.read(manifest, visitor);
    }

    private File findFullManifest(FileCollection fileCollection) {
        File[] files = findFilesByName(fileCollection, "AndroidManifest.xml");
        if (files == null) {
            throw new IllegalStateException("full AndroidManifest.xml not found");
        }
        if (files.length > 1) {
            context.getLogger().e("multi AndroidManifest.xml found");
        }
        return files[0];
    }


    public static File[] findFilesByName(final FileCollection fileCollection, final String name) {
        if (fileCollection == null || name == null) {
            return null;
        }

        List<File> files = new ArrayList<>();
        for (File file : fileCollection) {
            findFilesByName(file, name, files);
        }
        return files.isEmpty() ? null : files.toArray(new File[0]);
    }

    private static void findFilesByName(final File file, final String name, final List<File> founds) {
        if (file.isFile()) {
            if (name.equals(file.getName())) {
                founds.add(file);
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                Arrays.stream(files).forEach(f -> findFilesByName(f, name, founds));
            }
        }
    }
}
