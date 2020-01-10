package com.ss.android.ugc.bytex.common.graph;


import java.util.concurrent.atomic.AtomicInteger;

public class RefMemberEntity<T extends MemberEntity> extends MemberEntity implements RefEntity {
    protected T origin;
    private AtomicInteger referenceCount = new AtomicInteger(0);

    protected RefMemberEntity(T entity) {
        super(entity.access, entity.className, entity.name, entity.desc);
        this.origin = entity;
    }

    @Override
    public void setAccess(int access) {
        origin.setAccess(access);
    }

    @Override
    public MemberType type() {
        return origin.type();
    }

    @Override
    public int access() {
        return origin.access();
    }

    @Override
    public String className() {
        return origin.className();
    }

    @Override
    public void setClassName(String className) {
        origin.setClassName(className);
    }

    @Override
    public String name() {
        return origin.name();
    }

    @Override
    public String desc() {
        return origin.desc();
    }

    @Override
    public boolean equals(Object o) {
        return origin.equals(o);
    }

    @Override
    public int hashCode() {
        return origin.hashCode();
    }

    @Override
    public String toString() {
        return origin.toString();
    }

    @Override
    public void inc() {
        referenceCount.getAndIncrement();
    }

    @Override
    public void dec() {
        referenceCount.getAndDecrement();
    }

    @Override
    public boolean isFree() {
        return referenceCount.get() <= 0;
    }

    @Override
    public int getCount() {
        return referenceCount.get();
    }

    @Override
    public void setCount(int count) {
        referenceCount.set(count);
    }
}
