package com.ss.android.ugc.bytex.common.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OpcodesUtils {
    private static Map<Integer, String> OPCODE_MAP;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "NOP");
        map.put(1, "ACONST_NULL");
        map.put(2, "ICONST_M1");
        map.put(3, "ICONST_0");
        map.put(4, "ICONST_1");
        map.put(5, "ICONST_2");
        map.put(6, "ICONST_3");
        map.put(7, "ICONST_4");
        map.put(8, "ICONST_5");
        map.put(9, "LCONST_0");
        map.put(10, "LCONST_1");
        map.put(11, "FCONST_0");
        map.put(12, "FCONST_1");
        map.put(13, "FCONST_2");
        map.put(14, "DCONST_0");
        map.put(15, "DCONST_1");
        map.put(16, "BIPUSH");
        map.put(17, "SIPUSH");
        map.put(18, "LDC");
        map.put(19, "LDC_W");
        map.put(20, "LDC2_W");
        map.put(21, "ILOAD");
        map.put(22, "LLOAD");
        map.put(23, "FLOAD");
        map.put(24, "DLOAD");
        map.put(25, "ALOAD");
        map.put(26, "ILOAD_0");
        map.put(27, "ILOAD_1");
        map.put(28, "ILOAD_2");
        map.put(29, "ILOAD_3");
        map.put(30, "LLOAD_0");
        map.put(31, "LLOAD_1");
        map.put(32, "LLOAD_2");
        map.put(33, "LLOAD_3");
        map.put(34, "FLOAD_0");
        map.put(35, "FLOAD_1");
        map.put(36, "FLOAD_2");
        map.put(37, "FLOAD_3");
        map.put(38, "DLOAD_0");
        map.put(39, "DLOAD_1");
        map.put(40, "DLOAD_2");
        map.put(41, "DLOAD_3");
        map.put(42, "ALOAD_0");
        map.put(43, "ALOAD_1");
        map.put(44, "ALOAD_2");
        map.put(45, "ALOAD_3");
        map.put(46, "IALOAD");
        map.put(47, "LALOAD");
        map.put(48, "FALOAD");
        map.put(49, "DALOAD");
        map.put(50, "AALOAD");
        map.put(51, "BALOAD");
        map.put(52, "CALOAD");
        map.put(53, "SALOAD");
        map.put(54, "ISTORE");
        map.put(55, "LSTORE");
        map.put(56, "FSTORE");
        map.put(57, "DSTORE");
        map.put(58, "ASTORE");
        map.put(59, "ISTORE_0");
        map.put(60, "ISTORE_1");
        map.put(61, "ISTORE_2");
        map.put(62, "ISTORE_3");
        map.put(63, "LSTORE_0");
        map.put(64, "LSTORE_1");
        map.put(65, "LSTORE_2");
        map.put(66, "LSTORE_3");
        map.put(67, "FSTORE_0");
        map.put(68, "FSTORE_1");
        map.put(69, "FSTORE_2");
        map.put(70, "FSTORE_3");
        map.put(71, "DSTORE_0");
        map.put(72, "DSTORE_1");
        map.put(73, "DSTORE_2");
        map.put(74, "DSTORE_3");
        map.put(75, "ASTORE_0");
        map.put(76, "ASTORE_1");
        map.put(77, "ASTORE_2");
        map.put(78, "ASTORE_3");
        map.put(79, "IASTORE");
        map.put(80, "LASTORE");
        map.put(81, "FASTORE");
        map.put(82, "DASTORE");
        map.put(83, "AASTORE");
        map.put(84, "BASTORE");
        map.put(85, "CASTORE");
        map.put(86, "SASTORE");
        map.put(87, "POP");
        map.put(88, "POP2");
        map.put(89, "DUP");
        map.put(90, "DUP_X1");
        map.put(91, "DUP_X2");
        map.put(92, "DUP2");
        map.put(93, "DUP2_X1");
        map.put(94, "DUP2_X2");
        map.put(95, "SWAP");
        map.put(96, "IADD");
        map.put(97, "LADD");
        map.put(98, "FADD");
        map.put(99, "DADD");
        map.put(100, "ISUB");
        map.put(101, "LSUB");
        map.put(102, "FSUB");
        map.put(103, "DSUB");
        map.put(104, "IMUL");
        map.put(105, "LMUL");
        map.put(106, "FMUL");
        map.put(107, "DMUL");
        map.put(108, "IDIV");
        map.put(109, "LDIV");
        map.put(110, "FDIV");
        map.put(111, "DDIV");
        map.put(112, "IREM");
        map.put(113, "LREM");
        map.put(114, "FREM");
        map.put(115, "DREM");
        map.put(116, "INEG");
        map.put(117, "LNEG");
        map.put(118, "FNEG");
        map.put(119, "DNEG");
        map.put(120, "ISHL");
        map.put(121, "LSHL");
        map.put(122, "ISHR");
        map.put(123, "LSHR");
        map.put(124, "IUSHR");
        map.put(125, "LUSHR");
        map.put(126, "IAND");
        map.put(127, "LAND");
        map.put(128, "IOR");
        map.put(129, "LOR");
        map.put(130, "IXOR");
        map.put(131, "LXOR");
        map.put(132, "IINC");
        map.put(133, "I2L");
        map.put(134, "I2F");
        map.put(135, "I2D");
        map.put(136, "L2I");
        map.put(137, "L2F");
        map.put(138, "L2D");
        map.put(139, "F2I");
        map.put(140, "F2L");
        map.put(141, "F2D");
        map.put(142, "D2I");
        map.put(143, "D2L");
        map.put(144, "D2F");
        map.put(145, "I2B");
        map.put(146, "I2C");
        map.put(147, "I2S");
        map.put(148, "LCMP");
        map.put(149, "FCMPL");
        map.put(150, "FCMPG");
        map.put(151, "DCMPL");
        map.put(152, "DCMPG");
        map.put(153, "IFEQ");
        map.put(154, "IFNE");
        map.put(155, "IFLT");
        map.put(156, "IFGE");
        map.put(157, "IFGT");
        map.put(158, "IFLE");
        map.put(159, "IF_ICMPEQ");
        map.put(160, "IF_ICMPNE");
        map.put(161, "IF_ICMPLT");
        map.put(162, "IF_ICMPGE");
        map.put(163, "IF_ICMPGT");
        map.put(164, "IF_ICMPLE");
        map.put(165, "IF_ACMPEQ");
        map.put(166, "IF_ACMPNE");
        map.put(167, "GOTO");
        map.put(168, "JSR");
        map.put(169, "RET");
        map.put(170, "TABLESWITCH");
        map.put(171, "LOOKUPSWITCH");
        map.put(172, "IRETURN");
        map.put(173, "LRETURN");
        map.put(174, "FRETURN");
        map.put(175, "DRETURN");
        map.put(176, "ARETURN");
        map.put(177, "RETURN");
        map.put(178, "GETSTATIC");
        map.put(179, "PUTSTATIC");
        map.put(180, "GETFIELD");
        map.put(181, "PUTFIELD");
        map.put(182, "INVOKEVIRTUAL");
        map.put(183, "INVOKESPECIAL");
        map.put(184, "INVOKESTATIC");
        map.put(185, "INVOKEINTERFACE");
        map.put(186, "INVOKEDYNAMIC");
        map.put(187, "NEW");
        map.put(188, "NEWARRAY");
        map.put(189, "ANEWARRAY");
        map.put(190, "ARRAYLENGTH");
        map.put(191, "ATHROW");
        map.put(192, "CHECKCAST");
        map.put(193, "INSTANCEOF");
        map.put(194, "MONITORENTER");
        map.put(195, "MONITOREXIT");
        map.put(196, "WIDE");
        map.put(197, "MULTIANEWARRAY");
        map.put(198, "IFNULL");
        map.put(199, "IFNONNULL");
        map.put(200, "GOTO_W");
        map.put(201, "JSR_W");
        OPCODE_MAP = Collections.unmodifiableMap(map);
    }

    public static String getOpcodeString(int code) {
        String opcodeString = OPCODE_MAP.get(code);
        if (opcodeString == null || "".equals(opcodeString.trim())) {
            return String.valueOf(code);
        }
        return opcodeString;
    }

    public static String covertToString(AbstractInsnNode node) {
        if (node == null) {
            return null;
        }
        final String DIVIDER = " ";
        StringBuilder stringBuilder = new StringBuilder();
        if (node instanceof MethodInsnNode) {
            MethodInsnNode realInsnNode = (MethodInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.owner).
                    append(DIVIDER).append(realInsnNode.name).
                    append(DIVIDER).append(realInsnNode.desc).
                    append(DIVIDER).append(realInsnNode.itf);
        } else if (node instanceof FieldInsnNode) {
            FieldInsnNode realInsnNode = (FieldInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.owner).
                    append(DIVIDER).append(realInsnNode.name).
                    append(DIVIDER).append(realInsnNode.desc);
        } else if (node instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode realInsnNode = (TableSwitchInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.min).
                    append(DIVIDER).append(realInsnNode.max).
                    append(DIVIDER).append(covertToString(realInsnNode.dflt)).
                    append(DIVIDER).append("{");
            for (Object label : realInsnNode.labels) {
                stringBuilder.append(covertToString((AbstractInsnNode) label)).append(";");
            }
            stringBuilder.append("}");
        } else if (node instanceof LineNumberNode) {
            LineNumberNode realInsnNode = (LineNumberNode) node;
            stringBuilder.
                    append("LINENUMBER").
                    append(DIVIDER).append(realInsnNode.line).
                    append(DIVIDER).append(covertToString(realInsnNode.start));
        } else if (node instanceof IincInsnNode) {
            IincInsnNode realInsnNode = (IincInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.var).
                    append(DIVIDER).append(realInsnNode.incr);
        } else if (node instanceof IntInsnNode) {
            IntInsnNode realInsnNode = (IntInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(realInsnNode.getOpcode())).
                    append(DIVIDER).append(realInsnNode.operand);
        } else if (node instanceof LabelNode) {
            LabelNode realInsnNode = (LabelNode) node;
            String position;
            try {
                position = String.valueOf(realInsnNode.getLabel().getOffset());
            } catch (Exception ignore) {
                position = Integer.toHexString(realInsnNode.hashCode());
            }
            stringBuilder.
                    append("L").append(position);
        } else if (node instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode realInsnNode = (MultiANewArrayInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.desc).
                    append(DIVIDER).append(realInsnNode.dims);
        } else if (node instanceof LdcInsnNode) {
            LdcInsnNode realInsnNode = (LdcInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.cst);
        } else if (node instanceof TypeInsnNode) {
            TypeInsnNode realInsnNode = (TypeInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(realInsnNode.getOpcode())).
                    append(DIVIDER).append(realInsnNode.desc);
        } else if (node instanceof VarInsnNode) {
            VarInsnNode realInsnNode = (VarInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(realInsnNode.getOpcode())).
                    append(DIVIDER).append(realInsnNode.var);
        } else if (node instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode realInsnNode = (InvokeDynamicInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(node.getOpcode())).
                    append(DIVIDER).append(realInsnNode.name).
                    append(DIVIDER).append(realInsnNode.desc).
                    append(DIVIDER).append(realInsnNode.bsm.getOwner()).append(".").append(realInsnNode.bsm.getName())
                    .append("(");
            for (Object bsmArg : realInsnNode.bsmArgs) {
                stringBuilder.append(bsmArg).append(",");
            }
            stringBuilder.append(")");
        } else if (node instanceof FrameNode) {
            FrameNode realInsnNode = (FrameNode) node;
            switch (realInsnNode.type) {
                case Opcodes.F_NEW:
                case Opcodes.F_FULL:
                    stringBuilder.append(realInsnNode.type == Opcodes.F_NEW ? "F_NEW" : "F_FULL")
                            .append(DIVIDER).append(realInsnNode.local)
                            .append(DIVIDER).append(realInsnNode.stack);
                    break;
                case Opcodes.F_APPEND:
                    stringBuilder.append("F_APPEND")
                            .append(DIVIDER).append(realInsnNode.local);
                    break;
                case Opcodes.F_CHOP:
                    stringBuilder.append("F_CHOP")
                            .append(DIVIDER).append(realInsnNode.local);
                    break;
                case Opcodes.F_SAME:
                    stringBuilder.append("F_SAME");
                    break;
                case Opcodes.F_SAME1:
                    stringBuilder.append("F_SAME1")
                            .append(DIVIDER).append(realInsnNode.stack);
                    break;
            }
        } else if (node instanceof JumpInsnNode) {
            JumpInsnNode realInsnNode = (JumpInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(realInsnNode.getOpcode())).
                    append(DIVIDER).append(covertToString(realInsnNode.label));
        } else if (node instanceof InsnNode) {
            InsnNode realInsnNode = (InsnNode) node;
            stringBuilder.
                    append(getOpcodeString(realInsnNode.getOpcode()));
        } else if (node instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode realInsnNode = (LookupSwitchInsnNode) node;
            stringBuilder.
                    append(getOpcodeString(realInsnNode.getOpcode())).
                    append(DIVIDER).append(covertToString(realInsnNode.dflt)).
                    append(DIVIDER).append(realInsnNode.keys).
                    append(DIVIDER).append("[");

            for (Object label : realInsnNode.labels) {
                stringBuilder.append(covertToString((AbstractInsnNode) label)).append(";");
            }
            stringBuilder.append("]");
        } else {
            stringBuilder.append("unknow insnNode ").append(getOpcodeString(node.getOpcode()));
        }
        return stringBuilder.toString();
    }
}
