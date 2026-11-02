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
package com.cardinalstar.cubicchunks.util;

import static com.cardinalstar.cubicchunks.util.Coords.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;

import com.cardinalstar.cubicchunks.api.XYZAddressable;

/**
 * Position of a cube.
 * <p>
 * Tall Worlds uses a column coordinate system (which is really just a cube coordinate system without the y-coordinate),
 * a cube coordinate system, and two block coordinate systems, a cube-relative system, and a world absolute system.
 * <p>
 * It is important that the systems are kept separate. This class should be used whenever a cube coordinate is passed
 * along, so that it is clear that cube coordinates are being used, and not block coordinates.
 * <p>
 * Additionally, I (Nick) like to use xRel, yRel, and zRel for the relative position of a block inside of a cube. In
 * world space, I (Nick) refer to the coordinates as xAbs, yAbs, and zAbs.
 * <p>
 * This class also contains some helper methods to switch from/to block coordinates.
 */
@ParametersAreNonnullByDefault
public class CubePos implements Cloneable, XYZAddressable {

    protected static final int Y_BITS = 20;
    protected static final int X_BITS = 22;
    protected static final int Z_BITS = 22;

    protected static final int Z_BIT_OFFSET = 0;
    protected static final int X_BIT_OFFSET = Z_BIT_OFFSET + Z_BITS;
    protected static final int Y_BIT_OFFSET = X_BIT_OFFSET + X_BITS;

    private final int cubeX;
    private final int cubeY;
    private final int cubeZ;

    public CubePos(int cubeX, int cubeY, int cubeZ) {
        this.cubeX = cubeX;
        this.cubeY = cubeY;
        this.cubeZ = cubeZ;
    }

    public CubePos(XYZAddressable addressable) {
        this.cubeX = addressable.getX();
        this.cubeY = addressable.getY();
        this.cubeZ = addressable.getZ();
    }

    /**
     * Gets the x position of the cube in the world.
     *
     * @return The x position.
     */
    public int getX() {
        return this.cubeX;
    }

    /**
     * Gets the y position of the cube in the world.
     *
     * @return The y position.
     */
    public int getY() {
        return this.cubeY;
    }

    /**
     * Gets the z position of the cube in the world.
     *
     * @return The z position.
     */
    public int getZ() {
        return this.cubeZ;
    }

    /**
     * Gets the coordinates of the cube as a string.
     *
     * @return The coordinates, formatted as a string.
     */
    @Override
    public String toString() {
        return String.format("CubePos(%d, %d, %d)", cubeX, cubeY, cubeZ);
    }

    /**
     * Compares the CubeCoordinate against the given object.
     *
     * @return True if the cube matches the given object, but false if it doesn't match, or is null, or not a
     *         CubeCoordinate object.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof CubePos) {
            CubePos otherCoords = (CubePos) obj;
            return otherCoords.cubeX == cubeX && otherCoords.cubeY == cubeY && otherCoords.cubeZ == cubeZ;
        }
        return false;
    }

    /**
     * Returns a specification compliant hashCode for this object.
     *
     * @return A 32bit hashCode for this instance of CubePos.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(
            Bits.packSignedToLong(cubeX, Y_BITS, Y_BIT_OFFSET) | Bits.packSignedToLong(cubeY, X_BITS, X_BIT_OFFSET)
                | Bits.packSignedToLong(cubeZ, Z_BITS, Z_BIT_OFFSET));
    }

    public int getMinBlockX() {
        return Coords.cubeToMinBlock(cubeX);
    }

    public int getMinBlockY() {
        return Coords.cubeToMinBlock(cubeY);
    }

    public int getMinBlockZ() {
        return Coords.cubeToMinBlock(cubeZ);
    }

    public int getMaxBlockY() {
        return Coords.cubeToMaxBlock(this.cubeY);
    }

    public ChunkCoordIntPair chunkPos() {
        return new ChunkCoordIntPair(getX(), getZ());
    }

    public static CubePos fromBlockCoords(int blockX, int blockY, int blockZ) {
        return new CubePos(blockToCube(blockX), blockToCube(blockY), blockToCube(blockZ));
    }

    public static CubePos fromEntity(Entity entity) {
        return new CubePos(getCubeXForEntity(entity), getCubeYForEntity(entity), getCubeZForEntity(entity));
    }

    public boolean containsBlock(int x, int y, int z) {
        return this.cubeX == blockToCube(x) && this.cubeY == blockToCube(y) && this.cubeZ == blockToCube(z);
    }

    @Override
    public CubePos clone() {
        try {
            return (CubePos) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
