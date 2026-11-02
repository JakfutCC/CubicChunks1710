package com.cardinalstar.cubicchunks.api.worldgen;

import com.cardinalstar.cubicchunks.api.registry.IDependencyGraph;
import com.cardinalstar.cubicchunks.api.worldgen.decoration.ICubePopulator;

public interface WorldgenRegistry {

    /// Populators finalize cubes and put the 'window dressing' on the world - ores, trees, etc.
    IDependencyGraph<ICubePopulator> population();
}
