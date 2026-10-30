package com.cardinalstar.cubicchunks.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.cardinalstar.cubicchunks.CubicChunks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Makes sure client and server use the same network protocol. Peers with a different protocol use a different channel
 * ({@link NetworkChannel#PROTOCOL_VERSION} is part of its name), so they never read each other's packets; each side
 * sends a hello and gives up with a clear message if none arrives in time.
 */
public final class ProtocolCheck {

    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

    private static final String MISMATCH = "This server uses CubicChunks network protocol "
        + NetworkChannel.PROTOCOL_VERSION
        + ", which your CubicChunks does not. Install the same CubicChunks version as the server.";

    /** Players whose client sent a matching hello; written on network threads. */
    private static final Set<UUID> confirmed = ConcurrentHashMap.newKeySet();
    /** Players sent a hello and when, server thread only. */
    private static final Map<UUID, Long> awaiting = new HashMap<>();

    private ProtocolCheck() {}

    /** A player entered a cubic world: once per connection, send the protocol and wait for the client's. */
    public static void onCubicJoin(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        if (confirmed.contains(id) || awaiting.containsKey(id)) return;

        awaiting.put(id, System.nanoTime());
        PacketEncoderProtocolHello.createPacket(NetworkChannel.PROTOCOL_VERSION)
            .sendToPlayer(player);
    }

    static void clientHello(EntityPlayerMP player, int version) {
        if (version == NetworkChannel.PROTOCOL_VERSION) confirmed.add(player.getUniqueID());
    }

    /** Disconnects players whose client did not confirm the protocol in time. Server thread. */
    public static void serverTick(Iterable<EntityPlayerMP> players) {
        if (awaiting.isEmpty()) return;

        long now = System.nanoTime();
        for (EntityPlayerMP player : players) {
            UUID id = player.getUniqueID();
            Long since = awaiting.get(id);
            if (since == null) continue;

            if (confirmed.contains(id)) {
                awaiting.remove(id);
            } else if (now - since > TIMEOUT_NANOS) {
                awaiting.remove(id);
                CubicChunks.LOGGER.warn(
                    "{} did not confirm CubicChunks network protocol {}; disconnecting",
                    player.getCommandSenderName(),
                    NetworkChannel.PROTOCOL_VERSION);
                player.playerNetServerHandler.kickPlayerFromServer(MISMATCH);
            }
        }
    }

    public static void onLogout(EntityPlayerMP player) {
        confirmed.remove(player.getUniqueID());
        awaiting.remove(player.getUniqueID());
    }

    // --- client

    /** The connection whose server sent a matching hello. Client thread, like packet handling. */
    @SideOnly(Side.CLIENT)
    private static NetHandlerPlayClient confirmedConnection, connection;
    private static long joinedAt;

    static void serverHello(int version) {
        if (version != NetworkChannel.PROTOCOL_VERSION) return;

        confirmClient();
        NetworkChannel.CHANNEL.sendToServer(PacketEncoderProtocolHello.createPacket(NetworkChannel.PROTOCOL_VERSION));
    }

    @SideOnly(Side.CLIENT)
    private static void confirmClient() {
        confirmedConnection = Minecraft.getMinecraft()
            .getNetHandler();
    }

    /** Leaves a server that did not send a matching protocol in time after joining. Client thread. */
    @SideOnly(Side.CLIENT)
    public static void clientTick(Minecraft mc) {
        NetHandlerPlayClient handler = mc.getNetHandler();
        if (mc.theWorld == null || handler == null || handler == confirmedConnection) return;

        if (handler != connection) {
            connection = handler;
            joinedAt = System.nanoTime();
            return;
        }

        // an integrated server is this same mod (and pauses with the game)
        if (mc.isIntegratedServerRunning() || System.nanoTime() - joinedAt <= TIMEOUT_NANOS) return;

        confirmedConnection = handler;
        CubicChunks.LOGGER.warn(
            "The server did not send CubicChunks network protocol {}; disconnecting",
            NetworkChannel.PROTOCOL_VERSION);
        handler.getNetworkManager()
            .closeChannel(
                new ChatComponentText(
                    "The server's CubicChunks uses a different network protocol than yours ("
                        + NetworkChannel.PROTOCOL_VERSION
                        + "). Install the same CubicChunks version as the server."));
    }
}
