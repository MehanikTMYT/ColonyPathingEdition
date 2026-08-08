package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.minecolonies.api.blocks.AbstractBlockMinecolonies;
import com.minecolonies.core.blocks.MinecoloniesCropBlock;
import com.minecolonies.core.blocks.MinecoloniesFarmland;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(value = MinecoloniesCropBlock.class, remap = false)
public abstract class MinecoloniesCropBlockMixin extends AbstractBlockMinecolonies<MinecoloniesCropBlock> {

    @Final @Shadow(remap = false) private Block preferredFarmland;

    public MinecoloniesCropBlockMixin()
    {
        super(Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY));
    }

    /**
     * @author ARxyt
     * @reason There is bugs when add broken state into updateShape directly. So we overwrite it.
     */
    @Overwrite(remap = false)
    public @NotNull BlockState updateShape(
            @NotNull BlockState state,
            @NotNull LevelReader level,
            @NotNull ScheduledTickAccess ticks,
            @NotNull BlockPos pos,
            @NotNull Direction dir,
            @NotNull BlockPos neighborPos,
            @NotNull BlockState newState,
            @NotNull RandomSource random)
    {
        return !canSurviveOnFarmland(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, level, ticks, pos, dir, neighborPos, newState, random);
    }

    @Unique
    public boolean canSurviveOnFarmland(@NotNull BlockState state, LevelReader level, @NotNull BlockPos pos)
    {
        BlockPos blockpos = pos.below();
        boolean checkFarmland = level.getBlockState(blockpos).getBlock() == preferredFarmland || level.getBlockState(blockpos).getBlock() instanceof MinecoloniesFarmland;
        return checkFarmland && (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos));
    }
}
