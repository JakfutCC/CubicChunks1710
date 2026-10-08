/*
 * This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 * Copyright (c) 2015-2021 OpenCubicChunks
 * Copyright (c) 2015-2021 contributors
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cardinalstar.cubicchunks.network;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.CubicChunks;
import com.cardinalstar.cubicchunks.client.CubeProviderClient;
import com.cardinalstar.cubicchunks.network.PacketEncoderCube.PacketCube;
import com.cardinalstar.cubicchunks.util.Coords;
import com.cardinalstar.cubicchunks.util.CubePos;
import com.cardinalstar.cubicchunks.util.CubeStatusVisualizer;
import com.cardinalstar.cubicchunks.util.CubeStatusVisualizer.CubeStatus;
import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.github.bsideup.jabel.Desugar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@ParametersAreNonnullByDefault
public class PacketEncoderCube extends CCPacketEncoder<PacketCube> {

    @Desugar
    public record PacketCube(CubePos cubePos, byte[] data) implements CCPacket {

        @Override
        public byte getPacketID() {
            return CCPacketEntry.Cube.id;
        }
    }

    public PacketEncoderCube() {}

    public static PacketCube createPacket(Cube cube) {
        CubeStatusVisualizer.put(cube.getCoords(), CubeStatus.Synced);

        ByteBuf cubeData = Unpooled.buffer();

        WorldEncoder.encodeCube(new CCPacketBuffer(cubeData), cube);

        byte[] data = new byte[cubeData.writerIndex()];

        cubeData.readBytes(data);

        return new PacketCube(cube.getCoords(), data);
    }

    @Override
    public byte getPacketID() {
        return CCPacketEntry.Cube.id;
    }

    @Override
    public void writePacket(CCPacketBuffer buffer, PacketCube packet) {
        buffer.writeCubePos(packet.cubePos);

        buffer.writeByteArray(packet.data);
    }

    @Override
    public PacketCube readPacket(CCPacketBuffer buf) {
        CubePos pos = buf.readCubePos();

        byte[] data = buf.readByteArray();

        return new PacketCube(pos, data);
    }

    @Override
    public void process(World world, PacketCube packet) {
        CubeProviderClient cubeCache = (CubeProviderClient) world.getChunkProvider();

        CubePos pos = packet.cubePos;

        Cube cube = cubeCache.loadCube(pos); // new cube
        if (cube == null) {
            CubicChunks.LOGGER.error("Out of order cube received! No column for cube at {} exists!", pos);
            return;
        }

        cube.setClientCube();

        WorldEncoder.decodeCube(new CCPacketBuffer(Unpooled.wrappedBuffer(packet.data)), cube, world);

        if (!cube.isCubeLoaded()) {
            cube.onCubeLoad();
        } else {
            relightCubeNeighbors(world, cube);
        }
        cube.markForRenderUpdate();
    }

    private static void relightCubeNeighbors(World world, Cube cube) {
        int x = Coords.cubeToMinBlock(cube.getX());
        int y = Coords.cubeToMinBlock(cube.getY());
        int z = Coords.cubeToMinBlock(cube.getZ());

        // The snapshot already contains this cube's light. Neighbors without block
        // changes receive no packet, so propagate from the outside of each face.
        for (int a = 0; a < Cube.SIZE; a++) {
            for (int b = 0; b < Cube.SIZE; b++) {
                world.func_147451_t(x - 1, y + a, z + b);
                world.func_147451_t(x + Cube.SIZE, y + a, z + b);
                world.func_147451_t(x + a, y - 1, z + b);
                world.func_147451_t(x + a, y + Cube.SIZE, z + b);
                world.func_147451_t(x + a, y + b, z - 1);
                world.func_147451_t(x + a, y + b, z + Cube.SIZE);
            }
        }
    }
}
