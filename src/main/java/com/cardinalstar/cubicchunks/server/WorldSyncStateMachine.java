package com.cardinalstar.cubicchunks.server;

import java.util.Iterator;
import java.util.function.IntBinaryOperator;

import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

import org.joml.Vector2ic;

import com.cardinalstar.cubicchunks.CubicChunks;
import com.cardinalstar.cubicchunks.api.XYZAddressable;
import com.cardinalstar.cubicchunks.api.event.CubeEvent;
import com.cardinalstar.cubicchunks.network.PacketEncoderColumn;
import com.cardinalstar.cubicchunks.network.PacketEncoderCube;
import com.cardinalstar.cubicchunks.network.PacketEncoderCubeBlockChange;
import com.cardinalstar.cubicchunks.network.PacketEncoderCubeBlockChange.PacketCubeBlockChange;
import com.cardinalstar.cubicchunks.network.PacketEncoderHeightMapUpdate;
import com.cardinalstar.cubicchunks.network.PacketEncoderUnloadColumn;
import com.cardinalstar.cubicchunks.network.PacketEncoderUnloadCube;
import com.cardinalstar.cubicchunks.server.CubicPlayerManager.WatchingPlayer;
import com.cardinalstar.cubicchunks.server.chunkio.CubeInitLevel;
import com.cardinalstar.cubicchunks.util.AddressTools;
import com.cardinalstar.cubicchunks.util.BooleanArray2D;
import com.cardinalstar.cubicchunks.util.CubePos;
import com.cardinalstar.cubicchunks.util.HashMap2D;
import com.cardinalstar.cubicchunks.util.HashMap3D;
import com.cardinalstar.cubicchunks.util.HashSet3D;
import com.cardinalstar.cubicchunks.world.core.IColumnInternal;
import com.cardinalstar.cubicchunks.world.cube.Cube;

import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class WorldSyncStateMachine {

    private final CubeProviderServer provider;

    private final WatchingPlayer player;

    private static class ColumnData {

        public int syncedCubeCount;

        /** The column's top block heights as the client last received them, by local address. */
        public final int[] sentHeights = new int[Cube.SIZE * Cube.SIZE];
    }

    private final HashMap2D<ColumnData> syncedColumns = new HashMap2D<>();
    private final HashSet3D syncedCubes = new HashSet3D();

    private final HashSet3D dirtyCubes = new HashSet3D();
    private final HashMap3D<ShortOpenHashSet> dirtyBlocks = new HashMap3D<>();
    private final HashMap2D<BooleanArray2D> dirtyHeightCols = new HashMap2D<>();

    public WorldSyncStateMachine(CubeProviderServer provider, WatchingPlayer player) {
        this.provider = provider;
        this.player = player;
    }

    public void flush() {
        Iterator<XYZAddressable> cubeIter = dirtyCubes.fastIterator();

        while (cubeIter.hasNext()) {
            var pos = cubeIter.next();

            Cube cube = provider.getLoadedCube(pos.getX(), pos.getY(), pos.getZ());

            boolean isWatched = syncedCubes.contains(pos);

            boolean watchable = cube != null && cube.isInitializedToLevel(CubeInitLevel.Lit)
                && player.isWatchingCube(pos.getX(), pos.getY(), pos.getZ());

            if (isWatched && !watchable) {
                CubePos cubePos = new CubePos(pos);

                PacketEncoderUnloadCube.createPacket(cubePos)
                    .sendToPlayer(player.player);

                MinecraftForge.EVENT_BUS.post(new CubeEvent.UnWatch(provider.worldObj, cubePos, player.player));

                syncedCubes.remove(pos);

                ColumnData columnData = syncedColumns.get(pos.getX(), pos.getZ());

                columnData.syncedCubeCount--;

                if (columnData.syncedCubeCount <= 0) {
                    syncedColumns.remove(pos.getX(), pos.getZ());

                    PacketEncoderUnloadColumn.createPacket(pos.getX(), pos.getZ())
                        .sendToPlayer(player.player);

                    MinecraftForge.EVENT_BUS.post(
                        new ChunkWatchEvent.UnWatch(new ChunkCoordIntPair(pos.getX(), pos.getZ()), player.player));
                }
            } else if (!isWatched && watchable) {
                ColumnData columnData = syncedColumns.get(pos.getX(), pos.getZ());

                if (columnData == null) {
                    columnData = new ColumnData();
                    syncedColumns.put(pos.getX(), pos.getZ(), columnData);

                    PacketEncoderColumn.createPacket(cube.getColumn())
                        .sendToPlayer(player.player);
                    readHeights(cube.getColumn(), columnData.sentHeights);

                    MinecraftForge.EVENT_BUS
                        .post(new ChunkWatchEvent.Watch(new ChunkCoordIntPair(pos.getX(), pos.getZ()), player.player));
                }

                columnData.syncedCubeCount++;
                syncedCubes.add(pos);

                sendCubeData(cube, columnData);

                dirtyBlocks.remove(pos.getX(), pos.getY(), pos.getZ());

                MinecraftForge.EVENT_BUS.post(new CubeEvent.Watch(provider.worldObj, cube, player.player));
            }
        }

        for (var e : dirtyBlocks.fastEntryIterable()) {
            if (!syncedCubes.contains(e.getX(), e.getY(), e.getZ())) {
                CubicChunks.LOGGER.trace(
                    "Tried to sync {} block updates to a cube at {} which was not synced",
                    e.getValue()
                        .size(),
                    new CubePos(e.getX(), e.getY(), e.getZ()));

                continue;
            }

            if (player.isWatchingCube(e.getX(), e.getY(), e.getZ())) {
                Cube cube = provider.getLoadedCube(e.getX(), e.getY(), e.getZ());

                if (cube != null) {
                    PacketCubeBlockChange packet = PacketEncoderCubeBlockChange.createPacket(cube, e.getValue());
                    packet.sendToPlayer(player.player);

                    // the packet carries the heights of the block columns it changes
                    int[] sentHeights = syncedColumns.get(e.getX(), e.getZ()).sentHeights;
                    for (int heightValue : packet.heightValues()) {
                        sentHeights[heightValue & 0xFF] = heightValue >> 8;
                    }

                    ShortIterator blocks = e.getValue()
                        .iterator();

                    while (blocks.hasNext()) {
                        short pos = blocks.nextShort();

                        int rx = AddressTools.getLocalX(pos);
                        int ry = AddressTools.getLocalY(pos);
                        int rz = AddressTools.getLocalZ(pos);

                        TileEntity te = cube.getTileEntityUnsafe(rx, (e.getY() << 4) + ry, rz);

                        if (te != null) {
                            Packet desc = te.getDescriptionPacket();

                            if (desc != null) {
                                player.player.playerNetServerHandler.sendPacket(desc);
                            }
                        }
                    }
                }
            }
        }

        for (var e : dirtyHeightCols.fastEntryIterable()) {
            ColumnData columnData = syncedColumns.get(e.getX(), e.getZ());

            if (columnData == null) {
                CubicChunks.LOGGER.trace(
                    "Tried to sync {} height map updates to a column at {} which was not synced",
                    e.getValue()
                        .cardinality(),
                    new ChunkCoordIntPair(e.getX(), e.getZ()));

                continue;
            }

            if (player.isWatchingColumn(e.getX(), e.getZ())) {
                Chunk column = provider.getLoadedColumn(e.getX(), e.getZ());

                if (column != null) {
                    BooleanArray2D changed = changedHeights(
                        e.getValue(),
                        columnData.sentHeights,
                        ((IColumnInternal) column)::getTopYWithStaging);

                    if (!changed.isEmpty()) {
                        PacketEncoderHeightMapUpdate.createPacket(changed, column)
                            .sendToPlayer(player.player);
                    }
                }
            }
        }

        dirtyCubes.clear();
        dirtyBlocks.clear();
        dirtyHeightCols.clear();
    }

    public void onCubeMarkedDirty(int x, int y, int z) {
        dirtyCubes.add(x, y, z);
    }

    public void onColumnHeightMarkedDirty(int x, int z) {
        dirtyHeightCols.computeIfAbsent(x >> 4, z >> 4, (x1, z1) -> new BooleanArray2D(16, 16))
            .set2d(x & 0xF, z & 0xF);
    }

    public void onColumnHeightsChanged(int columnX, int columnZ) {
        dirtyHeightCols.computeIfAbsent(columnX, columnZ, (x1, z1) -> new BooleanArray2D(16, 16))
            .set(0, 256, true);
    }

    public void onBlockMarkedDirty(int x, int y, int z) {
        dirtyBlocks.computeIfAbsent(x >> 4, y >> 4, z >> 4, (x1, y1, z1) -> new ShortOpenHashSet())
            .add((short) AddressTools.getLocalAddress(x, y, z));
    }

    public void resendCube(Cube cube) {
        CubePos pos = cube.getCoords();

        if (syncedCubes.contains(pos) && player.isWatchingCube(pos.getX(), pos.getY(), pos.getZ())) {
            sendCubeData(cube, syncedColumns.get(pos.getX(), pos.getZ()));
        }
    }

    private void sendCubeData(Cube cube, ColumnData columnData) {
        PacketEncoderCube.createPacket(cube)
            .sendToPlayer(player.player);

        // non-empty cubes carry their column's heights (WorldEncoder.encodeCube)
        if (!cube.isEmpty()) {
            readHeights(cube.getColumn(), columnData.sentHeights);
        }

        cube.cubeTileEntityMap.forEach((tePos, te) -> {
            Packet desc = te.getDescriptionPacket();

            if (desc != null) {
                player.player.playerNetServerHandler.sendPacket(desc);
            }
        });
    }

    private static void readHeights(Chunk column, int[] heights) {
        for (int i = 0; i < heights.length; i++) {
            heights[i] = ((IColumnInternal) column)
                .getTopYWithStaging(AddressTools.getLocalX(i), AddressTools.getLocalZ(i));
        }
    }

    /**
     * Returns the marked block columns whose top height differs from the height the client has, and records their
     * new heights as sent. Block changes below the top and surfaces the client already knows send nothing.
     */
    static BooleanArray2D changedHeights(BooleanArray2D marked, int[] sentHeights, IntBinaryOperator topY) {
        BooleanArray2D changed = new BooleanArray2D(16, 16);

        for (Vector2ic v : marked) {
            int index = AddressTools.getLocalAddress(v.x(), v.y());
            int height = topY.applyAsInt(v.x(), v.y());

            if (sentHeights[index] != height) {
                sentHeights[index] = height;
                changed.set2d(v.x(), v.y());
            }
        }

        return changed;
    }
}
