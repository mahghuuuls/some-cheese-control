package com.mahghuuuls.somecheesecontrol.mixin.client;

import com.mahghuuuls.somecheesecontrol.client.SwingPaths;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The applied-check of the swing Mixins. Their injections are optional ({@code require = 0}),
 * because another mod may have changed the same code, so a path counts as changed only when its
 * Mixin will act:
 * <ul>
 * <li>a wrapped call ({@code @WrapWithCondition}) is inserted after this check runs, so the check
 * looks for the call itself in the target method: if it is still there, the wrap will find it; if
 * another mod replaced it, the wrap finds nothing;</li>
 * <li>a head injection ({@code @Inject}) is already in place, so the check looks for a call to one
 * of this mod's handlers.</li>
 * </ul>
 * It reports to {@link SwingPaths}, which uses no game type: this runs while a game class is being
 * changed.
 */
public final class SwingMixinPlugin implements IMixinConfigPlugin {

    /** Every handler of the swing Mixins starts with this; the Mixin runtime keeps it in the merged name. */
    private static final String HANDLER_MARK = "scc$";

    private static final String ENTITY_PLAYER = "net/minecraft/entity/player/EntityPlayer";
    private static final List<String> SWING = Arrays.asList("swingArm", "func_184609_a");
    private static final List<String> RESET_COOLDOWN = Arrays.asList("resetCooldown", "func_184821_cY");

    private static final String SIMPLE_NETWORK = "net/minecraftforge/fml/common/network/simpleimpl/SimpleNetworkWrapper";

    private static final Map<String, Check> CHECKS = new HashMap<>();

    static {
        CHECKS.put("MinecraftMixin", new Check(SwingPaths.VANILLA_CLICK,
                new Call(Arrays.asList("clickMouse", "func_147116_af"), "net/minecraft/client/entity/EntityPlayerSP", SWING)));
        CHECKS.put("PlayerControllerMPMixin", new Check(SwingPaths.VANILLA_ATTACK_BAR,
                new Call(Arrays.asList("attackEntity", "func_78764_a"), ENTITY_PLAYER, RESET_COOLDOWN)));
        CHECKS.put("EventHandlersClientMixin", new Check(SwingPaths.RLCOMBAT_CLICK,
                new Call(Arrays.asList("onMouseLeftClick"), ENTITY_PLAYER, SWING),
                new Call(Arrays.asList("onMouseRightClick"), ENTITY_PLAYER, SWING),
                new Call(Arrays.asList("onMouseRightClick"), SIMPLE_NETWORK, Arrays.asList("sendToServer"))));
        CHECKS.put("BetterCombatHandMixin", new Check(SwingPaths.RLCOMBAT_ANIMATION));
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        String path = changedPath(mixinClassName, targetClass);
        if (path != null) {
            SwingPaths.markApplied(path);
        }
    }

    /** The swing path the Mixin changed in {@code targetClass}, or null when it changed none. */
    static String changedPath(String mixinClassName, ClassNode targetClass) {
        Check check = CHECKS.get(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
        return check != null && check.passes(targetClass) ? check.path : null;
    }

    /**
     * What must be in the changed class for a path to count as changed: every listed call, or, with
     * no call listed (a head injection), a call to one of this mod's handlers.
     */
    private static final class Check {
        final String path;
        final Call[] calls;

        Check(String path, Call... calls) {
            this.path = path;
            this.calls = calls;
        }

        boolean passes(ClassNode target) {
            if (calls.length == 0) {
                for (MethodNode method : target.methods) {
                    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.contains(HANDLER_MARK)) {
                            return true;
                        }
                    }
                }
                return false;
            }
            for (Call call : calls) {
                if (!call.isIn(target)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A call a wrap needs in a target method. It counts when the call is there, or when another mod's
     * {@code @Redirect} replaced it: a wrap still finds a redirected call, because the injection points
     * of every Mixin are found before any of them is applied. Known limits of the redirect rule: it
     * does not tell which call of that receiver type was redirected, so with the call removed by an
     * overwrite and another call redirected the path counts as changed; and a redirect that takes the
     * receiver as a supertype is not recognized, so that path counts as unchanged.
     */
    private static final class Call {
        final List<String> methods;
        final String owner;
        final List<String> names;

        Call(List<String> methods, String owner, List<String> names) {
            this.methods = methods;
            this.owner = owner;
            this.names = names;
        }

        boolean isIn(ClassNode target) {
            for (MethodNode method : target.methods) {
                if (!methods.contains(method.name)) {
                    continue;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode && matches((MethodInsnNode) insn, target.name)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean matches(MethodInsnNode insn, String targetClass) {
            if (insn.owner.equals(owner) && names.contains(insn.name)) {
                return true;
            }
            // A redirect handler of another mod, merged into the target class, taking the call's receiver first.
            return insn.owner.equals(targetClass) && insn.name.contains("redirect$") && insn.desc.startsWith("(L" + owner + ";");
        }
    }
}
