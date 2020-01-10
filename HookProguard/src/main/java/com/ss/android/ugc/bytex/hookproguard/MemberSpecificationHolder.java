package com.ss.android.ugc.bytex.hookproguard;

import proguard.MemberSpecification;
import proguard.util.ClassNameParser;
import proguard.util.StringMatcher;
import proguard.util.StringParser;

class MemberSpecificationHolder {
    private final MemberSpecification instance;
    private StringMatcher methodNameMatcher;
    private StringMatcher descMatcher;

    MemberSpecificationHolder(MemberSpecification instance, StringParser parser) {
        this.instance = instance;
        if (instance.name != null) {
            this.methodNameMatcher = parser.parse(instance.name);
        }
        if (instance.descriptor != null) {
            this.descMatcher = new ClassNameParser().parse(instance.descriptor);
        }
    }

    MemberSpecification getInstance() {
        return instance;
    }

    boolean match(String name, String desc) {
        return (methodNameMatcher == null || methodNameMatcher.matches(name))
                && (descMatcher == null || descMatcher.matches(desc));
    }

}
