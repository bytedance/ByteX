package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.ss.android.ugc.bytex.common.xml.XmlReader;
import com.ss.android.ugc.bytex.shrinkR.Context;

import org.dom4j.Attribute;

import java.io.File;
import java.util.List;

public class RecordResReachXmlVisitor implements XmlReader.Visitor {
    private final File file;
    private final String resType;
    private final String resName;
    private final ResManager resManager;

    RecordResReachXmlVisitor(Context context, File file, String resType, String resName) {
        this.file = file;
        this.resType = resType;
        this.resName = resName;
        this.resManager = context.getChecker().getResManager();
    }

    @Override
    public boolean visitNode(String qName, String name, List<Attribute> attrs) {
        if (name.startsWith("@")) {
            String[] split = name.split("/");
            if (split.length == 2) {
                String resType = split[0].substring(1);
                String resName = split[1];
                resManager.reachResource(this.resType, this.resName, resType, resName);
            }
        }
        for (Attribute attr : attrs) {
            String value = attr.getValue().trim();
            if (attr.getName().equals("name")) {
                resManager.defineResource(qName, value, file.getAbsolutePath());
            }
            if (value.startsWith("@")) {
                String[] split = value.split("/");
                if (split.length == 2) {
                    String resType = split[0].substring(1);
                    String resName = split[1];
                    resManager.reachResource(this.resType, this.resName, resType, resName);
                }
            }
        }
        return true;
    }
}
