package com.mahghuuuls.somecheesecontrol.network;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The server's rules, sent to a client when it logs in. Which values travel, and in what order, is
 * decided by {@link Settings#writeRules}; this message only carries them.
 */
public final class SettingsMessage implements IMessage {

    private Settings settings;

    /** For the network code, which creates the message before reading it. */
    public SettingsMessage() {
    }

    SettingsMessage(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        settings.writeRules(buf);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        settings = Settings.readRules(buf);
    }

    /**
     * Installs the received rules as the client's copy. It runs on the network thread: the snapshot
     * is complete before it is published, and the item registries it reads no longer change once a
     * client is connected.
     */
    public static final class Handler implements IMessageHandler<SettingsMessage, IMessage> {

        @Override
        public IMessage onMessage(SettingsMessage message, MessageContext ctx) {
            message.settings.resolveRegistryEntries();
            Settings.installClientCopy(message.settings);
            return null;
        }
    }
}
