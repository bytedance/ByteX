package com.ss.android.ugc.bytex.pluginconfig;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.ss.android.ugc.bytex.pluginconfig.anno.PluginConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Created by tanlehua on 2019-05-01.
 */
@AutoService(Processor.class)
public class PluginConfigProcessor extends AbstractProcessor {
    private Map<String, String> plugins = new HashMap<>();

    @Override
    public ImmutableSet<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(PluginConfig.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return processImpl(annotations, roundEnv);
        } catch (Exception e) {
            // We don't allow exceptions of any kind to propagate to the compiler
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            fatalError(writer.toString());
            return true;
        }
    }

    private boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateConfigFiles();
        } else {
            processAnnotations(annotations, roundEnv);
        }

        return true;
    }


    private void processAnnotations(Set<? extends TypeElement> annotations,
                                    RoundEnvironment roundEnv) {

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(PluginConfig.class);

        for (Element e : elements) {
            TypeElement providerImplementer = (TypeElement) e;
            AnnotationMirror annotationMirror = MoreElements.getAnnotationMirror(e, PluginConfig.class).get();
            String pluginId = getValueFieldOfClasses(annotationMirror);
            if (pluginId == null) {
                error("No service interfaces provided for element!", e, annotationMirror);
                continue;
            }
            plugins.put(pluginId, getBinaryName(providerImplementer));
        }
    }

    /**
     * Returns the binary name of a reference type. For example,
     * {@code com.google.Foo$Bar}, instead of {@code com.google.Foo.Bar}.
     */
    private String getBinaryName(TypeElement element) {
        return getBinaryNameImpl(element, element.getSimpleName().toString());
    }

    private String getBinaryNameImpl(TypeElement element, String className) {
        Element enclosingElement = element.getEnclosingElement();

        if (enclosingElement instanceof PackageElement) {
            PackageElement pkg = (PackageElement) enclosingElement;
            if (pkg.isUnnamed()) {
                return className;
            }
            return pkg.getQualifiedName() + "." + className;
        }

        TypeElement typeElement = (TypeElement) enclosingElement;
        return getBinaryNameImpl(typeElement, typeElement.getSimpleName() + "$" + className);
    }


    /**
     * Returns the contents of a "value" field in a given {@code annotationMirror}.
     */
    private String getValueFieldOfClasses(AnnotationMirror annotationMirror) {
        return trim(
                AnnotationMirrors.getAnnotationValue(annotationMirror, "value")
                        .toString()
        );
    }

    private String trim(String source) {
        StringBuilder sb = new StringBuilder();
        if (source.charAt(0) == '\"')
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (i == 0 && ch == '\"') {
                    continue;
                }
                if (i == source.length() - 1 && ch == '\"') {
                    continue;
                }
                sb.append(ch);
            }
        return sb.toString();
    }

    private void generateConfigFiles() {
        Filer filer = processingEnv.getFiler();

        for (Map.Entry<String, String> entry : plugins.entrySet()) {
            String pluginId = entry.getKey();
            String implement = entry.getValue();
            String resourceFile = PropertiesFile.getPath(pluginId);
            try {
                FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                        resourceFile);
                OutputStream out = fileObject.openOutputStream();
                PropertiesFile.write(implement, out);
                out.close();
            } catch (IOException e) {
                fatalError("Unable to create " + resourceFile + ", " + e);
                return;
            }
        }
    }

    private void error(String msg, Element element, AnnotationMirror annotation) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element, annotation);
    }

    private void fatalError(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg);
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }
}
