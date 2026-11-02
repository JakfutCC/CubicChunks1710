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
package com.cardinalstar.cubicchunks.mixin.early.common;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cardinalstar.cubicchunks.api.IColumn;
import com.cardinalstar.cubicchunks.api.ICube;
import com.cardinalstar.cubicchunks.util.Coords;
import com.cardinalstar.cubicchunks.world.ICubicWorld;
import com.cardinalstar.cubicchunks.world.cube.ICubeProviderInternal;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

/**
 * Contains fixes for hardcoded height checks and other height-related issues.
 */
@ParametersAreNonnullByDefault
@Mixin(World.class)
public abstract class MixinWorld_HeightLimit implements ICubicWorld {

    @Shadow
    public boolean isRemote;

    @Shadow
    protected abstract boolean chunkExists(int x, int z);

    // TODO WATCH func_147467_a
    // =================================================
    // Individual Height Limit Mixins
    // =================================================

    @Shadow
    protected IChunkProvider chunkProvider;

    // blockExists
    @ModifyConstant(
        method = "blockExists",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0))
    private int blockExists_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "blockExists", constant = @Constant(intValue = 256, ordinal = 0))
    private int blockExists_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // getBlock
    @ModifyConstant(
        method = "getBlock",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0))
    private int getBlock_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "getBlock", constant = @Constant(intValue = 256, ordinal = 0))
    private int getBlock_heightLimits_max(int original) {
        return getMaxHeight();
    }

    @Definition(id = "chunkExists", method = "Lnet/minecraft/world/World;chunkExists(II)Z")
    @Expression("this.chunkExists(?, ?)")
    @Redirect(method = "blockExists", at = @At("MIXINEXTRAS:EXPRESSION"))
    boolean redirectChunkExistsCubeExists(World instance, int p_72916_1_, int p_72916_2_,
        @Local(argsOnly = true, name = "p_72899_1_") int x, @Local(argsOnly = true, name = "p_72899_2_") int y,
        @Local(argsOnly = true, name = "p_72899_3_") int z) {
        if (chunkProvider instanceof ICubeProviderInternal cubeProvider) {
            // Vanilla server blockExists tests residency, not saved terrain. Keep the client's permissive check.
            return isRemote ? cubeExists(x >> 4, y >> 4, z >> 4)
                : cubeProvider.getLoadedCube(x >> 4, y >> 4, z >> 4) != null;
        } else {
            return chunkExists(x >> 4, z >> 4);
        }
    }

    // checkChunksExist
    @ModifyConstant(
        method = "checkChunksExist",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0))
    private int checkChunksExist_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "checkChunksExist", constant = @Constant(intValue = 256, ordinal = 0))
    private int checkChunksExist_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // setBlock
    @ModifyConstant(
        method = "setBlock",
        constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0, ordinal = 0))
    private int setBlock_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "setBlock", constant = @Constant(intValue = 256, ordinal = 0))
    private int setBlock_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // getBlockMetadata
    @ModifyConstant(
        method = "getBlockMetadata",
        constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0, ordinal = 0))
    private int getBlockMetadata_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "getBlockMetadata", constant = @Constant(intValue = 256, ordinal = 0))
    private int getBlockMetadata_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // setBlockMetadataWithNotify
    @ModifyConstant(
        method = "setBlockMetadataWithNotify",
        constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0, ordinal = 0))
    private int setBlockMetadataWithNotify_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "setBlockMetadataWithNotify", constant = @Constant(intValue = 256, ordinal = 0))
    private int setBlockMetadataWithNotify_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // getFullBlockLightValue
    @Definition(id = "y", local = @Local(argsOnly = true, ordinal = 1, type = int.class))
    @Expression("y < 0")
    @WrapOperation(method = "getFullBlockLightValue", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean getFullBlockLightValue_heightLimits_min(int left, int right, Operation<Boolean> original) {
        return left < getMinHeight();
    }

    @ModifyConstant(method = "getFullBlockLightValue", constant = @Constant(intValue = 256, ordinal = 0))
    private int getFullBlockLightValue_heightLimits_max(int original) {
        return getMaxHeight();
    }

    @ModifyConstant(method = "getFullBlockLightValue", constant = @Constant(intValue = 255, ordinal = 0))
    private int getFullBlockLightValue_heightLimits_maxDefault(int original) {
        return getMaxHeight();
    }

    // ================= getBlockLightValue_do ======================
    @Definition(id = "y", local = @Local(argsOnly = true, ordinal = 1, type = int.class))
    @Expression("y < 0")
    @WrapOperation(method = "getBlockLightValue_do", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean getBlockLightValue_do_heightLimits_min(int left, int right, Operation<Boolean> original) {
        return left < getMinHeight();
    }

    @ModifyConstant(method = "getBlockLightValue_do", constant = @Constant(intValue = 256, ordinal = 0))
    private int getBlockLightValue_do_heightLimits_max(int original) {
        return getMaxHeight();
    }

    @ModifyConstant(method = "getBlockLightValue_do", constant = @Constant(intValue = 255, ordinal = 0))
    private int getBlockLightValue_do_heightLimits_maxDefault(int original) {
        return getMaxHeight();
    }

    // getSavedLightValue
    @ModifyConstant(
        method = "getSavedLightValue",
        constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0, ordinal = 0))
    private int getSavedLightValue_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "getSavedLightValue", constant = @Constant(intValue = 0, ordinal = 0))
    private int getSavedLightValue_heightLimits_minDefault(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "getSavedLightValue", constant = @Constant(intValue = 256, ordinal = 0))
    private int getSavedLightValue_heightLimits_max(int original) {
        return getMaxHeight();
    }

    @ModifyConstant(method = "getSavedLightValue", constant = @Constant(intValue = 255, ordinal = 0))
    private int getSavedLightValue_heightLimits_maxDefault(int original) {
        return getMaxHeight();
    }

    // setLightValue
    @ModifyConstant(
        method = "setLightValue",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0))
    private int setLightValue_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "setLightValue", constant = @Constant(intValue = 256, ordinal = 0))
    private int setLightValue_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // getTileEntity
    @ModifyConstant(
        method = "getTileEntity",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0))
    private int getTileEntity_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "getTileEntity", constant = @Constant(intValue = 256, ordinal = 0))
    private int getTileEntity_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // canBlockFreezeBody
    @ModifyConstant(
        method = "canBlockFreezeBody(IIIZ)Z",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0),
        remap = false)
    private int canBlockFreezeBody_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(
        method = "canBlockFreezeBody(IIIZ)Z",
        constant = @Constant(intValue = 256, ordinal = 0),
        remap = false)
    private int canBlockFreezeBody_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // canSnowAtBody
    @ModifyConstant(
        method = "canSnowAtBody",
        constant = @Constant(
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            intValue = 0,
            ordinal = 0),
        remap = false)
    private int canSnowAtBody_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "canSnowAtBody", constant = @Constant(intValue = 256, ordinal = 0), remap = false)
    private int canSnowAtBody_heightLimits_max(int original) {
        return getMaxHeight();
    }

    // getBlockLightOpacity
    @ModifyConstant(
        method = "getBlockLightOpacity",
        constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0, ordinal = 0),
        remap = false)
    private int getBlockLightOpacity_heightLimits_min(int original) {
        return getMinHeight();
    }

    @ModifyConstant(method = "getBlockLightOpacity", constant = @Constant(intValue = 256, ordinal = 0), remap = false)
    private int getBlockLightOpacity_heightLimits_max(int original) {
        return getMaxHeight();
    }

    /**
     * Conditionally replaces isAreaLoaded with Cubic Chunks implementation
     * (continues with vanilla code if it's not a cubic chunks world).
     * World.isAreaLoaded is used to check if some things can be updated (like light).
     * If it returns false - update doesn't happen. This fixes it
     * <p>
     * NOTE: there are some methods that use it incorrectly
     * ie. by checking it at some constant height (usually 0 or 64).
     * These places need to be modified.
     *
     * @author Barteks2x
     */
    @Group(name = "exists", max = 1)
    @Inject(method = "checkChunksExist(IIIIII)Z", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void checkChunksExistInject(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd,
        @Nonnull CallbackInfoReturnable<Boolean> cbi) {
        if (!(this.chunkProvider instanceof ICubeProviderInternal)) return;
        cbi.setReturnValue(this.testForCubes(xStart, yStart, zStart, xEnd, yEnd, zEnd, Objects::nonNull));
    }

    // NOTE: This may break some things

    @Redirect(
        method = "updateEntityWithOptionalForce",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;chunkExists(II)Z", ordinal = 0))
    private boolean updateEntityWithOptionalForce_chunkExists0(World world, int chunkX, int chunkZ, Entity entity,
        boolean force) {
        if (!(this.chunkProvider instanceof ICubeProviderInternal)) return this.chunkExists(chunkX, chunkZ);
        return this.cubeExists(entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ);
    }

    @Redirect(
        method = "updateEntityWithOptionalForce",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;chunkExists(II)Z", ordinal = 1))
    private boolean updateEntityWithOptionalForce_chunkExists1(World world, int chunkX, int chunkZ, Entity entity,
        boolean force) {
        assert this == (Object) world;
        if (!(this.chunkProvider instanceof ICubeProviderInternal)) return this.chunkExists(chunkX, chunkZ);
        int x = Coords.blockToCube(entity.posX);
        int y = Coords.blockToCube(entity.posY);
        int z = Coords.blockToCube(entity.posZ);

        return this.cubeExists(x, y, z);
    }

    // TODO If we want 3D biomes this needs to get Y as well. I don't think this will be good for compat.
    // @Inject(method = "getBiomeGenForCoords", at = @At("HEAD"), cancellable = true)
    // private void getBiome(int x, int y, int z, CallbackInfoReturnable<BiomeGenBase> ci) {
    // if (!this.isCubicWorld())
    // return;
    // ICube cube =
    // this.getCubeCache().getLoadedCube(Coords.blockToCube(x),Coords.blockToCube(y),Coords.blockToCube(z));
    // /*
    // * Using return here function will keep callback not cancelled,
    // * therefore "vanilla" function, which will get biome from chunk, will
    // * be called. Since cube is null there is no way to retrieve chunk
    // * faster, than using vanilla way.
    // */
    // if (cube == null)
    // return;
    // BiomeGenBase biome = cube.getBiome(x, y, z);
    // ci.setReturnValue(biome);
    // }

    @Redirect(
        method = "spawnEntityInWorld",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;chunkExists(II)Z"))
    private boolean spawnEntity_isChunkLoaded(World world, int chunkX, int chunkZ, Entity ent) {
        assert this == (Object) world;
        return this.chunkExists(chunkX, chunkZ);
    }

    @Redirect(
        method = "markAndNotifyBlock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;func_150802_k()Z"))
    private boolean isValidForRendering(Chunk column, @Local(argsOnly = true, ordinal = 0) int x,
        @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
        ICube cube = ((IColumn) column).getCube(Coords.blockToCube(y));

        return cube != null && cube.isPopulated();
    }

    @Redirect(
        method = "getBiomeGenForCoordsBody",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;blockExists(III)Z"))
    private boolean checkColumnExists(World instance, int x, int zero, int z) {
        return chunkExists(x >> 4, z >> 4);
    }
}
