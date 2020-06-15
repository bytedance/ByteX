package com.ss.android.ugc.bytex.hookproguard;

import com.ss.android.ugc.bytex.common.graph.Graph;
import com.ss.android.ugc.bytex.common.graph.Node;

import java.util.ArrayList;
import java.util.List;

import proguard.KeepClassSpecification;
import proguard.MemberSpecification;
import proguard.util.StringMatcher;
import proguard.util.StringParser;

class KeepClassSpecificationHolder {
    private final KeepClassSpecification instance;
    private StringMatcher classNameMatcher;
    private List<MemberSpecificationHolder> methodSpecifications;

    KeepClassSpecificationHolder(KeepClassSpecification instance, StringParser parser) {
        this.instance = instance;
        if (instance.className != null) {
            this.classNameMatcher = parser.parse(instance.className);
        }
    }

    KeepClassSpecification getInstance() {
        return instance;
    }

    boolean match(String className) {
        return classNameMatcher == null || classNameMatcher.matches(className);
    }

    List<MemberSpecificationHolder> getMethodSpecifications(StringParser parser) {
        if (methodSpecifications == null) {
            parserMethodSpecifications(parser);
        }
        return methodSpecifications;
    }

    void parserMethodSpecifications(StringParser parser) {
        if (methodSpecifications == null) {
            this.methodSpecifications = new ArrayList<>();
            List<MemberSpecification> methodSpecifications = instance.methodSpecifications;
            for (MemberSpecification methodSpecification : methodSpecifications) {
                this.methodSpecifications.add(new MemberSpecificationHolder(methodSpecification, parser));
            }
        }
    }

    Node computeExtendsClassNode(Graph graph) {
        return graph.get(instance.extendsClassName);
    }
}
