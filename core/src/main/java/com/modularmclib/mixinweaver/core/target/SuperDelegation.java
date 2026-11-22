package com.modularmclib.mixinweaver.core.target;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.ArrayDeque;
import java.util.function.Predicate;

final class SuperDelegation {

    private SuperDelegation() {}

    static boolean onEveryPath(MethodNode method, Predicate<String> isSupertype) {
        if (method.instructions.size() == 0) {
            return false;
        }
        boolean found = false;
        boolean[] visited = new boolean[method.instructions.size()];
        var pending = new ArrayDeque<Integer>();
        pending.add(0);
        while (!pending.isEmpty()) {
            int index = pending.remove();
            if (index == visited.length || visited[index]) {
                continue;
            }
            visited[index] = true;
            var instruction = method.instructions.get(index);
            int opcode = instruction.getOpcode();
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
                return false;
            }
            for (var handler : method.tryCatchBlocks) {
                if (index >= method.instructions.indexOf(handler.start) && index < method.instructions.indexOf(handler.end)) {
                    pending.add(method.instructions.indexOf(handler.handler));
                }
            }
            if (instruction instanceof MethodInsnNode call && opcode == Opcodes.INVOKESPECIAL && call.name.equals(method.name) && call.desc.equals(method.desc) && isSupertype.test(call.owner)) {
                found = true;
                continue;
            }
            switch (instruction) {
                case JumpInsnNode jump -> {
                    pending.add(method.instructions.indexOf(jump.label));
                    if (opcode != Opcodes.GOTO) {
                        pending.add(index + 1);
                    }
                }
                case TableSwitchInsnNode table -> {
                    pending.add(method.instructions.indexOf(table.dflt));
                    table.labels.forEach(label -> pending.add(method.instructions.indexOf(label)));
                }
                case LookupSwitchInsnNode lookup -> {
                    pending.add(method.instructions.indexOf(lookup.dflt));
                    lookup.labels.forEach(label -> pending.add(method.instructions.indexOf(label)));
                }
                default -> pending.add(index + 1);
            }
        }
        return found;
    }
}
