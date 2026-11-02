package com.cardinalstar.cubicchunks.world.worldgen;

import static com.cardinalstar.cubicchunks.api.worldgen.BuiltinWorldDecorators.CUBIC_VANILLA;

import com.cardinalstar.cubicchunks.util.Mods;
import com.cardinalstar.cubicchunks.world.worldgen.compat.DeepslateCubePopulator;

import cpw.mods.fml.common.Optional;

public class WorldGenerators {

    public static void init() {
        if (Mods.EtFuturumRequiem.isModLoaded()) {
            initEFRPopulation();
        }
    }

    @Optional.Method(modid = Mods.ModIDs.ET_FUTURUM_REQUIEM)
    private static void initEFRPopulation() {
        CUBIC_VANILLA.population()
            .addObject("low-deepslate", new DeepslateCubePopulator() /* , "requires:biomes" */ );
    }

}
