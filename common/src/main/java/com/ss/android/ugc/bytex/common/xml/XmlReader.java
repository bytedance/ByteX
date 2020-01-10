package com.ss.android.ugc.bytex.common.xml;


import com.ss.android.ugc.bytex.common.log.LevelLog;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class XmlReader {
    private final SAXReader reader;

    public XmlReader() {
        this.reader = new SAXReader();
    }

    public void read(String filePath, Visitor visitor) {
        File file = new File(filePath);
        read(file, Collections.singletonList(visitor));
    }

    public void read(File file, Visitor visitor) {
        read(file, Collections.singletonList(visitor));
    }

    public void read(String filePath, List<Visitor> visitors) {
        File file = new File(filePath);
        read(file, visitors);
    }

    public void read(File file, List<Visitor> visitors) {
        try {
            Document document = reader.read(file);
            Element root = document.getRootElement();
            readNode(root, visitors);
        } catch (DocumentException e) {
            LevelLog.sDefaultLogger.e(e.getMessage(), e);
        }
    }

    private void readNode(Element node, List<Visitor> visitors) {
        if (visitors == null || visitors.isEmpty()) {
            return;
        }
        String nodeName = node.getTextTrim().isEmpty() ? node.getName() : node.getTextTrim();
        List<Attribute> attributes = node.attributes();
        List<Visitor> visitElementVisitors = new LinkedList<>();
        for (Visitor visitor : visitors) {
            if (visitor.visitNode(node.getQualifiedName(), nodeName, attributes)) {
                visitElementVisitors.add(visitor);
            }
        }
        if (visitElementVisitors.isEmpty()) {
            return;
        }
        List<Element> elements = node.elements();
        for (Element e : elements) {
            readNode(e, visitElementVisitors);
        }
    }

    public interface Visitor {
        /**
         * @param name
         * @param attrs
         * @return should visit child
         */
        boolean visitNode(String qName, String name, List<Attribute> attrs);
    }
}
