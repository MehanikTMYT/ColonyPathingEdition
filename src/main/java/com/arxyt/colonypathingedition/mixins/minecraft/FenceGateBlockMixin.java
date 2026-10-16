package com.arxyt.colonypathingedition.mixins.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(FenceGateBlock.class)
public abstract class FenceGateBlockMixin extends HorizontalDirectionalBlock {

    @Shadow
    @Final
    private static Map<Direction.Axis, VoxelShape> SHAPE_COLLISION;

    public FenceGateBlockMixin(Properties props) {
        super(props);
    }

    @Inject(
            method = "getCollisionShape",
            at = @At("HEAD"),
            cancellable = true
    )
    private void blockPennedEntities(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext, CallbackInfoReturnable<VoxelShape> cir) {

        if (!(pContext instanceof EntityCollisionContext ecc)) return;

        Entity entity = ecc.getEntity();
        if (!(entity instanceof Animal animal) || animal.isLeashed() || !animal.getPassengers().isEmpty()) return;

        if (animal.getPersistentData().getBooleanOr("ColonyPathingEdition_Penned", false)) cir.setReturnValue(SHAPE_COLLISION.get(pState.getValue(FACING).getAxis()));
    }
}
