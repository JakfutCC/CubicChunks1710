package com.cardinalstar.cubicchunks.api.worldgen.impl;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.api.registry.IDependencyGraph;
import com.cardinalstar.cubicchunks.api.worldgen.WorldgenRegistry;
import com.cardinalstar.cubicchunks.api.worldgen.decoration.ICubePopulator;
import com.cardinalstar.cubicchunks.api.worldgen.decoration.IWorldDecorator;
import com.cardinalstar.cubicchunks.util.CubePos;
import com.cardinalstar.cubicchunks.util.DependencyGraph;
import com.cardinalstar.cubicchunks.world.cube.Cube;

public class StandardWorldDecorator implements WorldgenRegistry, IWorldDecorator {

    private final DependencyGraph<ICubePopulator> population = new DependencyGraph<>();

    @Override
    public IDependencyGraph<ICubePopulator> population() {
        return population;
    }

    @Override
    public void prepopulate(World world, CubePos pos) {
        for (ICubePopulator populator : population.sorted()) {
            populator.prepopulate(world, pos);
        }
    }

    @Override
    public void populate(World world, Cube cube) {
        for (ICubePopulator populator : population.sorted()) {
            populator.populate(world, cube);
        }
    }
}
