package com.cardinalstar.cubicchunks.world.cube.blockview;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;

/**
 * A view into a world-like structure. This may be a World, a Chunk, an EBS, or anything else. The coordinate space is
 * implementation and context dependent.
 */
public interface IMutableBlockView extends IBlockView {

    void setBlock(int x, int y, int z, @Nonnull Block block);

    void setBlockMetadata(int x, int y, int z, int meta);

    void setBlock(int x, int y, int z, @Nonnull Block block, int meta);
}
