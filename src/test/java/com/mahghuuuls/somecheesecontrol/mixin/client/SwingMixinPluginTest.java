package com.mahghuuuls.somecheesecontrol.mixin.client;

import com.mahghuuuls.somecheesecontrol.client.SwingPaths;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The applied-check. A wrapped call is inserted after the check runs, so the check looks for the
 * call in the target method; a head injection is in place already, so it looks for a handler call.
 * The tests ask the check itself ({@code changedPath}), so they share no state; one test covers the
 * report to {@link SwingPaths}.
 */
class SwingMixinPluginTest {

    private static final String MINECRAFT_MIXIN = "com.mahghuuuls.somecheesecontrol.mixin.client.vanilla.MinecraftMixin";
    private static final String CONTROLLER_MIXIN = "com.mahghuuuls.somecheesecontrol.mixin.client.vanilla.PlayerControllerMPMixin";
    private static final String HAND_MIXIN = "com.mahghuuuls.somecheesecontrol.mixin.client.rlcombat.BetterCombatHandMixin";

    @Test
    void theSwingCallStillInTheClickCountsAsChanged() {
        ClassNode target = classWith("func_147116_af", call("net/minecraft/client/entity/EntityPlayerSP", "func_184609_a"));
        assertTrue(SwingPaths.VANILLA_CLICK.equals(SwingMixinPlugin.changedPath(MINECRAFT_MIXIN, target)), "the wrap will find the swing (run-time names)");
    }

    @Test
    void anAttackBarCallRemovedByAnotherModLeavesThePathInactive() {
        ClassNode target = classWith("attackEntity", call("other/mod/Handler", "redirectResetCooldown"));
        assertFalse(SwingPaths.VANILLA_ATTACK_BAR.equals(SwingMixinPlugin.changedPath(CONTROLLER_MIXIN, target)));
    }

    @Test
    void aCallRedirectedByAnotherModStillCounts() {
        ClassNode target = classWith("clickMouse", new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/client/Minecraft",
                "redirect$zzb000$othermod$swing", "(Lnet/minecraft/client/entity/EntityPlayerSP;Lnet/minecraft/util/EnumHand;)V", false));
        target.name = "net/minecraft/client/Minecraft";
        assertTrue(SwingPaths.VANILLA_CLICK.equals(SwingMixinPlugin.changedPath(MINECRAFT_MIXIN, target)), "the wrap still finds a redirected call");
    }

    @Test
    void theRLCombatClickNeedsTheSwingInBothHandlersAndTheOffhandPacket() {
        ClassNode target = classWith("onMouseLeftClick", call("net/minecraft/entity/player/EntityPlayer", "func_184609_a"));
        MethodNode right = new MethodNode(Opcodes.ACC_PUBLIC, "onMouseRightClick", "()V", null, null);
        right.instructions.add(call("net/minecraft/entity/player/EntityPlayer", "func_184609_a"));
        right.instructions.add(new InsnNode(Opcodes.RETURN));
        target.methods.add(right);
        assertFalse(SwingPaths.RLCOMBAT_CLICK.equals(SwingMixinPlugin.changedPath(
                "com.mahghuuuls.somecheesecontrol.mixin.client.rlcombat.EventHandlersClientMixin", target)),
                "without the offhand attack packet the offhand cannot be hidden");
    }

    @Test
    void theCallInAnotherMethodDoesNotCount() {
        ClassNode target = classWith("otherMethod", call("net/minecraft/entity/player/EntityPlayer", "resetCooldown"));
        assertFalse(SwingPaths.VANILLA_ATTACK_BAR.equals(SwingMixinPlugin.changedPath(CONTROLLER_MIXIN, target)));
    }

    @Test
    void aHeadInjectionCountsByItsHandlerCall() {
        ClassNode without = classWith("initiateAnimation", call("bettercombat/mod/client/animation/util/BetterCombatHand", "reset"));
        assertFalse(SwingPaths.RLCOMBAT_ANIMATION.equals(SwingMixinPlugin.changedPath(HAND_MIXIN, without)), "no handler call");
        ClassNode with = classWith("initiateAnimation", call("bettercombat/mod/client/animation/util/BetterCombatHand",
                "handler$zza000$scc$startAnimation"));
        assertTrue(SwingPaths.RLCOMBAT_ANIMATION.equals(SwingMixinPlugin.changedPath(HAND_MIXIN, with)));
    }

    @Test
    void aChangedPathIsReported() {
        ClassNode target = classWith("onMouseLeftClick", call("net/minecraft/entity/player/EntityPlayer", "func_184609_a"));
        MethodNode right = new MethodNode(Opcodes.ACC_PUBLIC, "onMouseRightClick", "()V", null, null);
        right.instructions.add(call("net/minecraft/entity/player/EntityPlayer", "func_184609_a"));
        right.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/fml/common/network/simpleimpl/SimpleNetworkWrapper",
                "sendToServer", "(Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;)V", false));
        right.instructions.add(new InsnNode(Opcodes.RETURN));
        target.methods.add(right);
        new SwingMixinPlugin().postApply("bettercombat.mod.client.handler.EventHandlersClient", target,
                "com.mahghuuuls.somecheesecontrol.mixin.client.rlcombat.EventHandlersClientMixin", null);
        assertTrue(SwingPaths.isApplied(SwingPaths.RLCOMBAT_CLICK), "only this test marks this path");
    }

    private static MethodInsnNode call(String owner, String name) {
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, "()V", false);
    }

    private static ClassNode classWith(String methodName, MethodInsnNode call) {
        ClassNode node = new ClassNode();
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, methodName, "()V", null, null);
        method.instructions.add(call);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
        return node;
    }
}
