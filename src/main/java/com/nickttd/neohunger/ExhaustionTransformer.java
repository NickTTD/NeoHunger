package com.nickttd.neohunger;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/*
 * ExhaustionTransformer.java
 *
 * This class is an ASM coremod transformer for Minecraft 1.7.10.
 *
 * Purpose:
 *   - Overrides the exhaustion values for various player actions (jumping, sprinting, attacking, mining, etc.)
 *     in net.minecraft.entity.player.EntityPlayer to match the behavior of modern Minecraft.
 *
 * Implementation:
 *   - Implements IClassTransformer to intercept the loading of EntityPlayer.
 *   - Uses ASM to visit and modify the bytecode of specific methods.
 *   - For each relevant method (jump, addMovementStat, attack, etc.), it intercepts calls to addExhaustion(float)
 *     and replaces the exhaustion value with a custom one, or injects logic to calculate it dynamically.
 *
 * Design:
 *   - This code uses the Visitor Pattern, as implemented by ASM's ClassVisitor and MethodVisitor/AdviceAdapter classes.
 *   - The transformer visits each method in EntityPlayer and, for those of interest, wraps them with a custom
 *     MethodVisitor (PatchAddExhaustionAdapter) that intercepts and rewrites bytecode instructions as needed.
 *
 * Author: NickTTD
 */

public class ExhaustionTransformer implements IClassTransformer {
    // ASM hook: patch EntityPlayer for custom exhaustion, leave others unchanged
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.entity.player.EntityPlayer")) {
            return patchEntityPlayer(basicClass);
        }
        return basicClass;
    }

    private byte[] patchEntityPlayer(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassVisitor(Opcodes.ASM4, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                // Patch jump, onLivingUpdate, attackTargetEntityWithCurrentItem, onBlockDestroyed, damageEntity, addMovementStat
                if (name.equals("jump") && desc.equals("()V")) {
                    return new PatchAddExhaustionAdapter(mv, access, name, desc);
                }
                if (name.equals("onLivingUpdate") && desc.equals("()V")) {
                    return new PatchAddExhaustionAdapter(mv, access, name, desc);
                }
                if (name.equals("attackTargetEntityWithCurrentItem") && desc.equals("(Lnet/minecraft/entity/Entity;)V")) {
                    return new PatchAddExhaustionAdapter(mv, access, name, desc);
                }
                if (name.equals("onBlockDestroyed") && desc.equals("(Lnet/minecraft/world/World;IIIILnet/minecraft/block/Block;)V")) {
                    return new PatchAddExhaustionAdapter(mv, access, name, desc);
                }
                if (name.equals("damageEntity") && desc.equals("(Lnet/minecraft/util/DamageSource;F)V")) {
                    return new PatchAddExhaustionAdapter(mv, access, name, desc);
                }
                if (name.equals("addMovementStat") && desc.equals("(DDD)V")) {
                    return new PatchAddExhaustionAdapter(mv, access, name, desc);
                }
                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    // Adapter to patch addExhaustion(float) calls to use custom values
    private static class PatchAddExhaustionAdapter extends AdviceAdapter {
        private final String methodName;
        protected PatchAddExhaustionAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM4, mv, access, name, desc);
            this.methodName = name;
        }
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            // Intercept calls to addExhaustion(float) in EntityPlayer
            if (owner.equals("net/minecraft/entity/player/EntityPlayer") && name.equals("addExhaustion") && desc.equals("(F)V")) {
                // Remove the original float argument from the stack
                super.visitInsn(Opcodes.POP);
                if (methodName.equals("onBlockDestroyed")) {
                    // Block breaking: always use 0.005F exhaustion
                    super.visitLdcInsn(0.005F);
                } else if (methodName.equals("jump")) {
                    // --- JUMP EXHAUSTION LOGIC ---
                    // if (this.isSprinting()) exhaustion = 0.2F; else exhaustion = 0.05F;
                    // Load 'this' onto the stack
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    // Call isSprinting() on 'this'
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/entity/player/EntityPlayer", "isSprinting", "()Z", false);
                    // If not sprinting, jump to notSprinting label
                    Label notSprinting = new Label();
                    super.visitJumpInsn(Opcodes.IFEQ, notSprinting);
                    // If sprinting: push 0.2F (sprint-jump exhaustion)
                    super.visitLdcInsn(0.2F);
                    // Jump to end label to skip the else case
                    Label end = new Label();
                    super.visitJumpInsn(Opcodes.GOTO, end);
                    // Not sprinting: push 0.05F (normal jump exhaustion)
                    super.visitLabel(notSprinting);
                    super.visitLdcInsn(0.05F);
                    // End label: stack now has the correct exhaustion value
                    super.visitLabel(end);
                } else if (methodName.equals("attackTargetEntityWithCurrentItem") || methodName.equals("damageEntity")) {
                    // Attacking or taking damage: always use 0.1F exhaustion
                    super.visitLdcInsn(0.1F);
                } else if (methodName.equals("addMovementStat")) {
                    // --- SPRINTING MOVEMENT EXHAUSTION LOGIC ---
                    //The goal is to track small movements (less than a block, say cm)
                    //And accumulate them until they add up to a full block (meter).
                    // Calculate exhaustion: 0.1F * metersMoved, where metersMoved = f * 0.01F
                    // f = (float)Math.round(Math.sqrt(x*x + z*z) * 100.0F)
                    // Load x (double, local var 1) and square it
                    super.visitVarInsn(Opcodes.DLOAD, 1); // x
                    super.visitVarInsn(Opcodes.DLOAD, 1); // x
                    super.visitInsn(Opcodes.DMUL); // x*x
                    // Load z (double, local var 5) and square it
                    super.visitVarInsn(Opcodes.DLOAD, 5); // z
                    super.visitVarInsn(Opcodes.DLOAD, 5); // z
                    super.visitInsn(Opcodes.DMUL); // z*z
                    // Add x*x + z*z
                    super.visitInsn(Opcodes.DADD);
                    // sqrt(x*x + z*z)
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false);
                    // Multiply by 100.0 to convert meters to centimeters
                    super.visitLdcInsn(100.0);
                    super.visitInsn(Opcodes.DMUL);
                    // Round to nearest integer (centimeters moved)
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "round", "(D)J", false);
                    // Convert long to float
                    super.visitInsn(Opcodes.L2F);
                    // Duplicate the float value (for comparison and calculation)
                    Label skip = new Label();
                    super.visitInsn(Opcodes.DUP);
                    // Compare to 0 (if f <= 0, skip calculation)
                    super.visitInsn(Opcodes.FCONST_0);
                    super.visitInsn(Opcodes.FCMPG);
                    super.visitJumpInsn(Opcodes.IFLE, skip);
                    // If f > 0: exhaustion = 0.1F * f * 0.01F
                    super.visitLdcInsn(0.1F);
                    super.visitInsn(Opcodes.FMUL);
                    super.visitLdcInsn(0.01F);
                    super.visitInsn(Opcodes.FMUL);
                    // skip label: stack now has the correct exhaustion value (or 0 if no movement)
                    super.visitLabel(skip);
                //} else {
                    //super.visitLdcInsn(0.0F);
                }
            }
            // Call the original method with the (possibly replaced) exhaustion value
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
