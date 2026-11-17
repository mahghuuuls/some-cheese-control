package com.mahghuuuls.somecheesecontrol.diagnostic;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.DiagnosticLog;
import com.mahghuuuls.somecheesecontrol.core.PlayerSituation;
import com.mahghuuuls.somecheesecontrol.feature.boatescape.BoatEscape;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingTracker;
import com.mahghuuuls.somecheesecontrol.feature.vehiclefall.VehicleFallTracker;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import com.mahghuuuls.somecheesecontrol.integration.originalmods.OriginalMods;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * {@code /somecheesecontrol} (alias {@code /scc}) for operators (permission level 2): {@code status}
 * and {@code inspect <player>}. Both only read what the owners of the state expose and change
 * nothing.
 */
public final class SccCommand extends CommandBase {

    /** How far the operator's look reaches for the mob to inspect, in blocks. */
    private static final double LOOK_REACH = 16.0;

    @Override
    public String getName() {
        return "somecheesecontrol";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("scc");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/somecheesecontrol status | /somecheesecontrol inspect <player>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            send(sender, status());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            EntityPlayerMP target = getPlayer(server, sender, args[1]);
            Entity looking = sender.getCommandSenderEntity();
            send(sender, inspect(target, looking instanceof EntityPlayerMP ? (EntityPlayerMP) looking : null));
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "inspect");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    private static List<String> status() {
        Settings settings = Settings.get();
        List<String> integrations = new ArrayList<>();
        String shieldBash = StatusReport.shieldBash(false, false, Collections.<String>emptyList());
        for (IntegrationGate.Record record : IntegrationGate.records()) {
            integrations.add(StatusReport.integration(record.name(), stateText(record.state()), record.problems()));
            if (record.modId().equals("spartanshields")) {
                shieldBash = StatusReport.shieldBash(record.state() != IntegrationGate.State.ABSENT,
                        record.state() == IntegrationGate.State.ACTIVE && record.problems().isEmpty(), record.problems());
            }
        }
        return StatusReport.lines(settings.featureSwitches(), settings.valueLines(), integrations, shieldBash,
                OriginalMods.installed(), settings.problems().errors(), settings.problems().warnings());
    }

    private static String stateText(IntegrationGate.State state) {
        switch (state) {
            case ABSENT:
                return "not installed";
            case DETECTED:
                return "installed";
            case ACTIVE:
                return "installed, integration active";
            default:
                return "installed, integration failed and disabled";
        }
    }

    private static List<String> inspect(EntityPlayerMP target, @Nullable EntityPlayerMP operator) {
        BlockPos climbing = PlayerSituation.climbingBlock(target);
        String climbingBlock = climbing != null ? position(climbing) + " " + blockName(target, climbing) : null;
        List<String> lines = new ArrayList<>(InspectReport.playerLines(target.getName(), ClimbingTracker.isRestricted(target),
                ClimbingTracker.releaseTicksLeft(target), climbingBlock, PlayerSituation.isAirborne(target), VehicleFallTracker.pendingFall(target)));
        Entity mob = operator != null ? lookedAtMob(operator) : null;
        String mobLine = InspectReport.mobLine(operator != null, mob != null ? DiagnosticLog.entityName(mob) + " #" + mob.getEntityId() : null,
                mob != null ? BoatEscape.reboardTicksLeft(mob) : 0);
        if (mobLine != null) {
            lines.add(mobLine);
        }
        return lines;
    }

    /**
     * The nearest living non-player entity the operator looks at within reach, not behind a block,
     * alive (health above 0) and hittable; null when none.
     */
    @Nullable
    private static Entity lookedAtMob(EntityPlayerMP operator) {
        Vec3d eye = operator.getPositionEyes(1.0F);
        Vec3d look = operator.getLook(1.0F);
        Vec3d end = eye.add(look.scale(LOOK_REACH));
        RayTraceResult block = operator.world.rayTraceBlocks(eye, end, false, true, false);
        if (block != null && block.hitVec != null) {
            end = block.hitVec;
        }
        double reach = eye.distanceTo(end);
        AxisAlignedBB area = operator.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach).grow(1.0);
        Entity nearest = null;
        double nearestDistance = reach;
        for (Entity entity : operator.world.getEntitiesWithinAABBExcludingEntity(operator, area)) {
            if (!(entity instanceof EntityLivingBase) || entity instanceof EntityPlayer || !entity.isEntityAlive() || !entity.canBeCollidedWith()) {
                continue;
            }
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
            if (box.contains(eye)) {
                return entity;
            }
            RayTraceResult hit = box.calculateIntercept(eye, end);
            if (hit != null && eye.distanceTo(hit.hitVec) < nearestDistance) {
                nearest = entity;
                nearestDistance = eye.distanceTo(hit.hitVec);
            }
        }
        return nearest;
    }

    private static String position(BlockPos pos) {
        return String.format(Locale.ROOT, "%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String blockName(EntityPlayer player, BlockPos pos) {
        ResourceLocation name = player.world.getBlockState(pos).getBlock().getRegistryName();
        return name != null ? name.toString() : "unknown";
    }

    private static void send(ICommandSender sender, List<String> lines) {
        for (String line : lines) {
            sender.sendMessage(new TextComponentString(line));
        }
    }
}
