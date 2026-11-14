package com.mahghuuuls.somecheesecontrol.network;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The mod's own network channel. It carries one message: the server's rules, sent to each player at
 * login so the client can refuse its own player's actions the way the server will.
 */
public final class Network {

    private final SimpleNetworkWrapper channel;

    private Network(SimpleNetworkWrapper channel) {
        this.channel = channel;
    }

    /** Creates the channel and starts sending the rules at login. Call once, at pre-initialization, on both sides. */
    public static void register(String channelName) {
        SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(channelName);
        channel.registerMessage(SettingsMessage.Handler.class, SettingsMessage.class, 0, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(new Network(channel));
    }

    /** Fires on the server only. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            channel.sendTo(new SettingsMessage(Settings.get()), (EntityPlayerMP) event.player);
        }
    }
}
