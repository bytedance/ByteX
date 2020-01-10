package com.ss.android.ugc.bytex.closeable;


import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.usp.each.saeg.asm.defuse.FlowAnalyzer;

/**
 * ASM bytecode control flow analyzer<br/>
 * We could analyze all flows of the method by calling {@link #analyze()}.<br/>
 * And then,dispatch all routes to {@link FlowTraverse} by calling {@link #dispatch(FlowTraverse)}
 */
public class ControlFlowAnalyzer {
    private final String mClassName;
    private final MethodNode mMethodNode;
    private final RealAnalyzer mRealAnalyzer;
    private FlowController mFlowController;

    public ControlFlowAnalyzer(String className, MethodNode methodNode) {
        this.mClassName = className;
        this.mMethodNode = methodNode;
        this.mRealAnalyzer = new RealAnalyzer();
    }

    public ControlFlowAnalyzer analyze() throws AnalyzerException {
        mRealAnalyzer.analyze(mClassName, mMethodNode);
        return this;
    }

    /**
     * dispatch routes.<br/>
     * {@link #analyze() } must be called before this action
     *
     * @param flowTraverse FlowTraverse
     */
    public void dispatch(FlowTraverse flowTraverse) {
        final List<List<Integer>> routes = new ArrayList<>();
        int[][] successors = mRealAnalyzer.getSuccessors();
        append(successors, routes, new ArrayList<>(), 0);
        for (int i = 0; i < routes.size(); i++) {
            flowTraverse.traverse(i, routes.get(i));
        }
    }

    private void append(final int[][] successors, final List<List<Integer>> routes, final List<Integer> current, final int index) {
        if (index >= successors.length) {
            throw new IllegalStateException();
        }
        if (routes.size() > 500) {
            //too many，skip
            return;
        }
        if (current.contains(index)) {
            //looper
            for (int temp = index; temp < successors.length; temp++) {
                if (successors[temp] != null && successors[temp].length == 2) {
                    if (current.contains(successors[temp][0]) && !current.contains(successors[temp][1])) {
                        append(successors, routes, current, successors[temp][1]);
                    } else if (current.contains(successors[temp][1]) && !current.contains(successors[temp][0])) {
                        append(successors, routes, current, successors[temp][0]);
                    }
                    return;
                }
            }
            return;
        }
        current.add(index);
        int[] successor = successors[index];
        if (successor == null || successor.length <= 0) {
            //finish
            routes.add(current);
            return;
        }
        if (successor.length == 1) {
            //single
            append(successors, routes, current, successor[0]);
        } else {
            if (mFlowController != null) {
                successor = mFlowController.jump(current, index, successor);
            }
            for (int i : successor) {
                append(successors, routes, new ArrayList<>(current), i);
            }
        }
    }

    public void setFlowController(FlowController listener) {
        this.mFlowController = listener;
    }


    private class RealAnalyzer extends FlowAnalyzer<BasicValue> {
        private Map<Integer, Set<Integer>> mExceptionEdges = new HashMap<>();

        RealAnalyzer() {
            super(new BasicInterpreter());
        }

        @Override
        protected void newControlFlowEdge(final int insnIndex, final int successorIndex) {
            super.newControlFlowEdge(insnIndex, successorIndex);
            if (mFlowController != null) {
                mFlowController.onControlFlow(insnIndex, mMethodNode.instructions.get(insnIndex));
            }
        }

        protected boolean newControlFlowExceptionEdge(final int insn,
                                                      final TryCatchBlockNode tcb) {
            boolean result = mFlowController == null ? super.newControlFlowExceptionEdge(insn, tcb) : mFlowController.newControlFlowExceptionEdge(insn, tcb);
            if (result) {
                mExceptionEdges.computeIfAbsent(insn, (it) -> new HashSet<>()).add(mMethodNode.instructions.indexOf(tcb.handler));
            }
            return result;
        }

        public int[][] getSuccessors() {
            final int[][] successors = super.getSuccessors();
            mExceptionEdges.keySet().forEach((it) -> {
                int[] successor = successors[it];
                Set<Integer> integers = mExceptionEdges.get(it);
                if (successor != null) {
                    for (int r : successor) {
                        integers.add(r);
                    }
                }
                successors[it] = successor = new int[integers.size()];
                int i = 0;
                for (Integer integer : integers) {
                    successor[i++] = integer;
                }
            });
            return successors;
        }
    }

    public static class FlowController {
        /**
         * an event invoked while traverse a new instruction
         *
         * @param index   index of the control flow
         * @param insNode instruction
         */
        protected void onControlFlow(int index, AbstractInsnNode insNode) {
        }

        /**
         * Creates a control flow graph edge corresponding to an exception handler.
         *
         * @param ins an instruction index.
         * @param tcb TryCatchBlockNode corresponding to this edge.
         * @return true if this edge must be considered in the data flow analysis
         * performed by this analyzer, or false otherwise. The default value is false
         */
        protected boolean newControlFlowExceptionEdge(final int ins,
                                                      final TryCatchBlockNode tcb) {
            return false;
        }

        /**
         * an event invoked while dispatching routes
         *
         * @param current instructions index of the current route(not full)
         * @param index   current instruction index
         * @param next    default routes of the next step
         * @return real routes of the next step
         */
        protected int[] jump(List<Integer> current, int index, int[] next) {
            return next;
        }
    }

    public interface FlowTraverse {
        /**
         * traverse one single route
         *
         * @param routeIndex route index
         * @param route      all instructions index of the route
         */
        void traverse(int routeIndex, List<Integer> route);
    }
}
