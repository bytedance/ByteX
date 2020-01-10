package com.ss.android.ugc.bytex.example.getter_setter;

import android.support.annotation.Keep;

public class OldBean extends Bean {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    private String abc;

    public String getAbc() {
        return abc;
    }

    @Keep
    public void setAbc(String abc) {
        this.abc = abc;
    }

    public String getNameAndAge() {
        return getName() + getAge();
    }
}
