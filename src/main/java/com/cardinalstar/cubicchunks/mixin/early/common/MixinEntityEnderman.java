package com.cardinalstar.cubicchunks.mixin.early.common;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cardinalstar.cubicchunks.world.ICubicWorld;
import com.cardinalstar.cubicchunks.world.cube.ICubeProvider;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

@Mixin(EntityEnderman.class)
public abstract class MixinEntityEnderman extends EntityMob {

    protected MixinEntityEnderman(World world) {
        super(world);
    }

    @WrapOperation(
        method = "teleportTo",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;blockExists(III)Z"),
        require = 1)
    private boolean cubicChunks$checkStart(World world, int x, int y, int z, Operation<Boolean> original,
        @Share("minGroundY") LocalIntRef minGroundY) {
        if (!(world.getChunkProvider() instanceof ICubeProvider)) return original.call(world, x, y, z);
        // Capture the event-adjusted starting height before vanilla starts descending.
        minGroundY.set((int) Math.max(((ICubicWorld) world).getMinHeight(), (long) y - 256));
        return cubicChunks$isBlockLoaded(x, y, z);
    }

    @Expression("? > 0")
    @WrapOperation(method = "teleportTo", at = @At("MIXINEXTRAS:EXPRESSION"), require = 1)
    private boolean cubicChunks$canDescend(int y, int zero, Operation<Boolean> original,
        @Share("minGroundY") LocalIntRef minGroundY) {
        if (!(worldObj.getChunkProvider() instanceof ICubeProvider)) return original.call(y, zero);
        return y > minGroundY.get()
            && cubicChunks$isBlockLoaded(MathHelper.floor_double(posX), y - 1, MathHelper.floor_double(posZ));
    }

    @WrapOperation(
        method = "teleportTo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getCollidingBoundingBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"),
        require = 1)
    private List<AxisAlignedBB> cubicChunks$checkDestination(World world, Entity entity, AxisAlignedBB box,
        Operation<List<AxisAlignedBB>> original) {
        if (world.getChunkProvider() instanceof ICubeProvider && !cubicChunks$isDestinationLoaded(box)) {
            // A nonempty result takes vanilla's failure/rollback path and skips its liquid reads.
            return Collections.singletonList(box);
        }
        return original.call(world, entity, box);
    }

    private boolean cubicChunks$isBlockLoaded(int x, int y, int z) {
        ICubicWorld cubic = (ICubicWorld) worldObj;
        return x >= -30000000 && x < 30000000
            && z >= -30000000
            && z < 30000000
            && y >= cubic.getMinHeight()
            && y < cubic.getMaxHeight()
            && worldObj.checkChunksExist(x, y, z, x, y, z);
    }

    private boolean cubicChunks$isDestinationLoaded(AxisAlignedBB box) {
        ICubicWorld cubic = (ICubicWorld) worldObj;
        // Collision shapes read neighbours, including door halves beyond the support/head
        // rows. Out-of-world block reads return air without touching a cube.
        double minX = Math.floor(box.minX) - 1;
        double minY = Math.max(Math.floor(box.minY) - 2, cubic.getMinHeight());
        double minZ = Math.floor(box.minZ) - 1;
        double maxX = Math.floor(box.maxX + 1);
        double maxY = Math.min(Math.floor(box.maxY + 1), (double) cubic.getMaxHeight() - 1);
        double maxZ = Math.floor(box.maxZ + 1);
        if (!(minX >= -30000000 && maxX < 30000000
            && minZ >= -30000000
            && maxZ < 30000000
            && minY >= cubic.getMinHeight()
            && maxY < cubic.getMaxHeight()
            && minX <= maxX
            && minY <= maxY
            && minZ <= maxZ)) return false;
        return worldObj.checkChunksExist((int) minX, (int) minY, (int) minZ, (int) maxX, (int) maxY, (int) maxZ);
    }
}
