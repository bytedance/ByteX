package com.ss.android.ugc.bytex.common.manifest;


import com.ss.android.ugc.bytex.common.log.LevelLog;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;

public class AndroidManifestXmlReader {
    private final SAXReader reader;
    private String packageName;

    public AndroidManifestXmlReader() {
        this.reader = new SAXReader();
    }

    public void read(String filePath, Visitor visitor) {
        File file = new File(filePath);
        read(file, visitor);
    }

    public void read(File file, Visitor visitor) {
        try {
            if (visitor.visitFile(file)) return;
            Document document = reader.read(file);
            Element root = document.getRootElement();
            readNode(root, visitor);
        } catch (DocumentException e) {
            LevelLog.sDefaultLogger.e(e.getMessage(), e);
        }
    }

    public String getPackageName() {
        return packageName;
    }

    private void readNode(Element node, Visitor visitor) {
        String nodeName = node.getTextTrim().isEmpty() ? node.getName() : node.getTextTrim();
        List<Attribute> attributes = node.attributes();
        if (visitor != null) {
            if (nodeName.equals("manifest")) {
                for (Attribute attribute : attributes) {
                    if (attribute.getName().equals("package")) {
                        packageName = attribute.getValue();
                        visitor.visitPackageName(packageName);
                        break;
                    }
                }
                List<Element> elements = node.elements();
                for (Element e : elements) {
                    readNode(e, visitor);
                }
            } else if (nodeName.equals("application")) {
                for (Attribute attribute : attributes) {
                    if ("name".equals(attribute.getName()) && "android".equals(attribute.getNamespace().getPrefix())) {
                        String className = attribute.getValue();
                        if (className != null && className.startsWith(".") && packageName != null) {
                            className = packageName + className;
                        }
                        if (className != null && !className.startsWith(".")) {
                            visitor.visitApplication(className.replace(".", "/"), attributes);
                        }
                        break;
                    }
                }
                List<Element> elements = node.elements();
                for (Element e : elements) {
                    readNode(e, visitor);
                }
            } else if (nodeName.equals("activity-alias")) {
                for (Attribute attribute : attributes) {
                    if ("targetActivity".equals(attribute.getName()) && "android".equals(attribute.getNamespace().getPrefix())) {
                        String className = attribute.getValue();
                        if (className != null && className.startsWith(".") && packageName != null) {
                            className = packageName + className;
                        }
                        List intentFilters = node.elements("intent-filter");
                        if (className != null && !className.startsWith(".")) {
                            visitor.visitActivity(className.replace(".", "/"), attributes, intentFilters);
                        }
                        break;
                    }
                }
            } else if (nodeName.equals("activity")) {
                for (Attribute attribute : attributes) {
                    if ("name".equals(attribute.getName()) && "android".equals(attribute.getNamespace().getPrefix())) {
                        String className = attribute.getValue();
                        if (className != null && className.startsWith(".") && packageName != null) {
                            className = packageName + className;
                        }
                        List intentFilters = node.elements("intent-filter");
                        if (className != null && !className.startsWith(".")) {
                            visitor.visitActivity(className.replace(".", "/"), attributes, intentFilters);
                        }
                        break;
                    }
                }
            } else if (nodeName.equals("receiver")) {
                for (Attribute attribute : attributes) {
                    if ("name".equals(attribute.getName()) && "android".equals(attribute.getNamespace().getPrefix())) {
                        String className = attribute.getValue();
                        if (className != null && className.startsWith(".") && packageName != null) {
                            className = packageName + className;
                        }
                        if (className != null && !className.startsWith(".")) {
                            visitor.visitReceiver(className.replace(".", "/"));
                        }
                        break;
                    }
                }
            } else if (nodeName.equals("provider")) {
                for (Attribute attribute : attributes) {
                    if ("name".equals(attribute.getName()) && "android".equals(attribute.getNamespace().getPrefix())) {
                        String className = attribute.getValue();
                        if (className != null && className.startsWith(".") && packageName != null) {
                            className = packageName + className;
                        }
                        if (className != null && !className.startsWith(".")) {
                            visitor.visitProvider(className.replace(".", "/"));
                        }
                        break;
                    }
                }
            } else if (nodeName.equals("service")) {
                for (Attribute attribute : attributes) {
                    if ("name".equals(attribute.getName()) && "android".equals(attribute.getNamespace().getPrefix())) {
                        String className = attribute.getValue();
                        if (className != null && className.startsWith(".") && packageName != null) {
                            className = packageName + className;
                        }
                        if (className != null && !className.startsWith(".")) {
                            visitor.visitService(className.replace(".", "/"));
                        }
                        break;
                    }
                }
            }
        }
    }

    public interface Visitor {

        void visitApplication(String className, List<Attribute> attrs);

        void visitActivity(String className, List<Attribute> attrs, List<Element> intentFilters);

        void visitReceiver(String className);

        void visitProvider(String className);

        void visitService(String className);

        default void visitPackageName(String packageName) {
        }

        default boolean visitFile(File file) {
            return false;
        }
    }
}
