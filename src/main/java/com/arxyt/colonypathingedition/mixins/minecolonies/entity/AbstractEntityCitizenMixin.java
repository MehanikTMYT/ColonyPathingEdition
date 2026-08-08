package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenJobHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractEntityCitizen.class, remap = false)
public abstract class AbstractEntityCitizenMixin extends AbstractCivilianEntity {

    @Shadow(remap = false) public abstract ICitizenJobHandler getCitizenJobHandler();

    public AbstractEntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);

        //市民职业信息
        if(getCitizenJobHandler() != null && getCitizenJobHandler().getColonyJob() != null) {
            output.putString("citizenJob", getCitizenJobHandler().getColonyJob().getModel().getPath());
        }
        else{
            output.putString("citizenJob", "unemployed");
        }
    }
}
