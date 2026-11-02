package com.cardinalstar.cubicchunks.api;

import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.cardinalstar.cubicchunks.world.ICubicWorld;

@SuppressWarnings("unused")
public final class CCAPI {

    private CCAPI() {}

    /// Gets a loaded cube. Does not load or generate the cube. Will return null if the cube is not loaded.
    @Nullable
    public static ICube getLoadedCube(World world, int cubeX, int cubeY, int cubeZ) {
        return ((ICubicWorld) world).getCubeCache()
            .getLoadedCube(cubeX, cubeY, cubeZ);
    }
}
