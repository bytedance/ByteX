package com.ss.android.ugc.bytex.common.graph;

interface RefEntity {

    void inc();

    void dec();

    boolean isFree();

    int getCount();

    void setCount(int count);
}
