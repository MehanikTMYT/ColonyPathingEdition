package com.arxyt.colonypathingedition.mixins.minecraft;

import com.arxyt.colonypathingedition.api.extras.FurnaceBlockEntityExtra;
import com.minecolonies.api.util.constant.Constants;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin implements FurnaceBlockEntityExtra {
    @Final @Shadow private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;
    @Shadow int litTimeRemaining;
    @Shadow int cookingTimer;
    @Shadow int cookingTotalTime;
    @Shadow int litTotalTime;

    @Shadow public abstract List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 popVec);

    @Unique private int pathFindEdition$workerID = -1;
    @Unique private int pathFindEdition$pickerID = -1;
    @Unique private int pathFindEdition$fuelerID = -1;
    @Unique private int pathFindEdition$protectTime = 0;

    // 增加进度
    @Unique
    public int pathFindEdition$addProgress(int adder){
        cookingTimer += adder;
        if(cookingTimer >= cookingTotalTime){
            int left = cookingTimer - cookingTotalTime;
            cookingTimer =  cookingTotalTime - 1;
            litTimeRemaining ++;
            return left;
        }
        return 0;
    }

    // 增加燃料点燃时间
    @Unique
    public void pathFindEdition$addLitTime(int adder){
        if(litTimeRemaining == 0){
            return;
        }
        litTimeRemaining += adder;
        if(litTotalTime < litTimeRemaining){
            litTimeRemaining = litTotalTime;
        }
    }

    @Unique
    public void pathFindEdition$tickProtect(){
        if(pathFindEdition$protectTime > 0){
            pathFindEdition$protectTime--;
        }
    }

    @Unique
    public void pathFindEdition$setPickup(AbstractFurnaceBlockEntity pBlockEntity){
        if(cookingTotalTime == cookingTimer + 1 && pBlockEntity.getItem(Constants.SMELTABLE_SLOT).getCount() == 1){
            pathFindEdition$setFurnacePicker(pathFindEdition$workerID);
        }
    }

    @Unique
    public int pathFindEdition$getFurnaceWorker() {
        return pathFindEdition$workerID;
    }

    @Unique
    public void pathFindEdition$setFurnaceWorker(int workerID) {
        this.pathFindEdition$workerID = workerID;
        if(workerID < 0){
            pathFindEdition$protectTime = 0;
        }
        else{
            pathFindEdition$protectTime = 60; //保护三秒
        }
    }

    @Unique
    public int pathFindEdition$getFurnacePicker(){
        return pathFindEdition$pickerID;
    }

    @Unique
    public void pathFindEdition$setFurnacePicker(int pickerID){
        this.pathFindEdition$pickerID = pickerID;
        if(pickerID < 0){
            pathFindEdition$protectTime = 0;
        }
        else{
            pathFindEdition$protectTime = 40; //保护两秒
        }
    }

    /**
     * @return citizen Civilian ID
     */
    @Unique
    public int pathFindEdition$getFurnaceFueler() {
        return pathFindEdition$fuelerID;
    }

    /**
     * @param fuelerID: Civilian ID
     */
    @Unique
    public void pathFindEdition$setFurnaceFueler(int fuelerID) {
        this.pathFindEdition$fuelerID = fuelerID;
        if(fuelerID >= 0){
            pathFindEdition$protectTime = 40; //保护两秒
        }
    }

    @Unique
    public void pathFindEdition$setProtectTime(int protectTime){
        this.pathFindEdition$protectTime = protectTime;
    }

    @Unique
    public boolean pathFindEdition$atProtectTime() {
        return pathFindEdition$protectTime > 0;
    }

    @Unique
    public void pathFindEdition$clearUsedRecipesAndPopExperience(ServerLevel world, Entity worker) {
        if(world == null) return;
        this.getRecipesToAwardAndPopExperience(world, worker.position());
        this.recipesUsed.clear();
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void onSave(ValueOutput output, CallbackInfo ci) {
        output.putInt("WorkerID", pathFindEdition$workerID);
        output.putInt("PickerID", pathFindEdition$pickerID);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void onLoad(ValueInput input, CallbackInfo ci) {
        pathFindEdition$workerID = input.getIntOr("WorkerID", -1);
        pathFindEdition$pickerID = input.getIntOr("PickerID", -1);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void afterServerTick(ServerLevel pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtra)pBlockEntity).pathFindEdition$tickProtect();
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void beforeServerTick(ServerLevel pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pBlockEntity, CallbackInfo ci){
        ((FurnaceBlockEntityExtra)pBlockEntity).pathFindEdition$setPickup(pBlockEntity);
    }
}
