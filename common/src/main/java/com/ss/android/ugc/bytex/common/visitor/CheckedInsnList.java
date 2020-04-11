package com.ss.android.ugc.bytex.common.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/**
 * Created by yangzhiqian on 2020-04-03<br/>
 */
public class CheckedInsnList extends InsnList {

    @Override
    public void add(AbstractInsnNode insn) {
        if (insn.getNext() != null || insn.getPrevious() != null) {
            throw new IllegalStateException();
        }
        super.add(insn);
    }

    @Override
    public void set(AbstractInsnNode location, AbstractInsnNode insn) {
        if (!contains(location) || insn.getNext() != null || insn.getPrevious() != null) {
            throw new IllegalStateException();
        }
        super.set(location, insn);
    }

    @Override
    public void remove(AbstractInsnNode insn) {
        if (!contains(insn)) {
            throw new IllegalStateException();
        }
        super.remove(insn);
    }

    @Override
    public void insert(AbstractInsnNode insn) {
        if (insn.getNext() != null || insn.getPrevious() != null) {
            throw new IllegalStateException();
        }
        super.insert(insn);
    }

    @Override
    public void insert(AbstractInsnNode location, InsnList insns) {
        if (!contains(location)) {
            throw new IllegalStateException();
        }
        super.insert(location, insns);
    }

    @Override
    public void insertBefore(AbstractInsnNode location, InsnList insns) {
        if (!contains(location)) {
            throw new IllegalStateException();
        }
        super.insertBefore(location, insns);
    }

    @Override
    public void insertBefore(AbstractInsnNode location, AbstractInsnNode insn) {
        if (!contains(location) || insn.getNext() != null || insn.getPrevious() != null) {
            throw new IllegalStateException();
        }
        super.insertBefore(location, insn);
    }

    @Override
    public void insert(AbstractInsnNode location, AbstractInsnNode insn) {
        if (!contains(location) || insn.getNext() != null || insn.getPrevious() != null) {
            throw new IllegalStateException();
        }
        super.insert(location, insn);
    }

    @Override
    public int size() {
        int size = 0;
        AbstractInsnNode node = getFirst();
        while (node != null) {
            size++;
            node = node.getNext();
        }
        if (super.size() != size) {
            throw new IllegalStateException();
        }
        return size;
    }

    @Override
    public void accept(MethodVisitor mv) {
        size();
        super.accept(mv);
    }
}
