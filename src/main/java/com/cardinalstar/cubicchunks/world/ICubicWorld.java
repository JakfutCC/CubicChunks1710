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
package com.cardinalstar.cubicchunks.world;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.cardinalstar.cubicchunks.api.ICube;
import com.cardinalstar.cubicchunks.api.util.NotCubicChunksWorldException;
import com.cardinalstar.cubicchunks.util.CubePos;
import com.cardinalstar.cubicchunks.world.api.IMinMaxHeight;
import com.cardinalstar.cubicchunks.world.cube.ICubeProvider;
import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

@ParametersAreNonnullByDefault
public interface ICubicWorld extends IMinMaxHeight {

    /**
     * Returns the {@link ICubeProvider} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     *
     * @return the cube provider
     */
    ICubeProvider getCubeCache();

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions between
     * BlockPos(minBlockX, minBlockY, minBlockZ) and BlockPos(maxBlockX, maxBlockY, maxBlockZ) (including the specified
     * positions). Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     *
     * @param minBlockX minimum block x coordinate
     * @param minBlockY minimum block y coordinate
     * @param minBlockZ minimum block z coordinate
     * @param maxBlockX maximum block x coordinate
     * @param maxBlockY maximum block y coordinate
     * @param maxBlockZ maximum block z coordinate
     * @param test      the test to apply
     * @return false if any invokation of the given predicate returns false, true otherwise
     */
    default boolean testForCubes(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY,
        int maxBlockZ, Predicate<ICube> test) {
        return testForCubes(
            CubePos.fromBlockCoords(minBlockX, minBlockY, minBlockZ),
            CubePos.fromBlockCoords(maxBlockX, maxBlockY, maxBlockZ),
            test);
    }

    /**
     * Returns true iff the given Predicate evaluates to true for given cube and neighbors.
     * Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     *
     * @param start start cube position
     * @param end   end cube position
     * @param test  the test to apply
     * @return false if any invokation of the given predicate returns false, true otherwise
     *
     */
    boolean testForCubes(CubePos start, CubePos end, Predicate<? super ICube> test);

    ICube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

    default ICube getCubeFromCubeCoords(CubePos pos) {
        return getCubeFromCubeCoords(pos.getX(), pos.getY(), pos.getZ());
    }

    ICube getCubeFromBlockCoords(int x, int y, int z);

    ICube getCubeFromBlockCoords(BlockPos pos);

    boolean cubeExists(int x, int y, int z);

    int getMinGenerationHeight();

    int getMaxGenerationHeight();

    enum SurfaceType {
        SOLID,
        BLOCKING_MOVEMENT,
        OPAQUE
    }

}
