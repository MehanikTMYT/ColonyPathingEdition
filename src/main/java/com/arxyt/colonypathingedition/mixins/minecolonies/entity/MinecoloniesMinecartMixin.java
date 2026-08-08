package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.minecolonies.api.research.util.ResearchConstants.WALKING;
import static net.minecraft.world.entity.Entity.RemovalReason.DISCARDED;

/**
 * Since 26.1.2, rail movement (gravity, powered rails, moveAlongTrack, comeOffTrack, fluid/block
 * interaction, interpolation) is fully owned by vanilla's MinecartBehavior (via
 * {@link com.minecolonies.api.entity.other.MinecoloniesMinecart#tick()}, which just calls
 * {@code super.tick()}). We no longer copy that loop. This mixin only adds the two pieces of
 * CPE-specific behaviour that MineColonies' own class does not provide:
 *   - a passenger-aware speed bonus applied through the {@code getMaxSpeed} hook that
 *     OldMinecartBehavior#moveAlongTrack consults for its final speed clamp;
 *   - ghost-citizen-passenger cleanup, appended after MineColonies' own empty-passenger discard.
 */
@Mixin(value = MinecoloniesMinecart.class, remap = false)
public abstract class MinecoloniesMinecartMixin extends Minecart {
    /**
     * Verified from 1.21.1's {@code IAbstractMinecartExtension#getMaxCartSpeedOnRail()} /
     * {@code getCurrentCartSpeedCapOnRail()} (both defaulted to this same constant; neither CPE
     * nor MineColonies ever called the corresponding setters). Per that interface's own javadoc:
     * "Carts going faster than 1.1 cause issues with chunk loading. Carts can't traverse slopes or
     * corners at greater than 0.5 - 0.6." This is the real historical ceiling, not an invented one.
     */
    @Unique
    private static final double LEGACY_MAX_CART_SPEED_CAP = 1.2D;

    @Unique
    private static volatile boolean pathFindEdition$warnedAboutNewMinecartBehavior = false;

    public MinecoloniesMinecartMixin(final EntityType<?> type, final Level world)
    {
        super(type, world);
    }

    /**
     * @author ARxyt
     * @reason Restore the exact 1.21.1 CPE speed formula: railSpeed * speedFactor, clamped by the
     * cart's absolute speed cap. In 1.21.1, "railSpeed" came from
     * BaseRailBlock#getRailMaxSpeed(...), whose default implementation for a non-furnace cart on a
     * normal rail is {@code isInWater() ? 0.2 : 0.4} - byte-for-byte the same formula
     * OldMinecartBehavior#getMaxSpeed(ServerLevel) uses in 26.1.2. We therefore read the railSpeed
     * baseline from {@code getBehavior().getMaxSpeed(level)} (the sole surviving, public,
     * behavior-polymorphic equivalent of the old per-rail-type lookup) instead of duplicating the
     * water check ourselves, and clamp with the verified historical 1.2 ceiling instead of
     * collapsing cap == railSpeed (which made speedFactor a no-op).
     */
    @Overwrite(remap = false)
    protected double getMaxSpeed(final ServerLevel level)
    {
        double railSpeed = this.getBehavior().getMaxSpeed(level);

        if (this.getBehavior() instanceof NewMinecartBehavior && !pathFindEdition$warnedAboutNewMinecartBehavior)
        {
            pathFindEdition$warnedAboutNewMinecartBehavior = true;
            ColonyPathingEdition.LOGGER.warn(
                    "[ColonyPathingEdition] This world has the experimental 'minecart_improvements' feature flag enabled. "
                    + "The CPE minecart passenger speed bonus (SPEED effect / WALKING research / deliveryman Agility) was "
                    + "verified against the default OldMinecartBehavior speed model only; NewMinecartBehavior uses a "
                    + "gamerule-driven speed (GameRules.MAX_MINECART_SPEED) that this bonus has not been validated against.");
        }

        double speedFactor = pathFindEdition$calculateCitizenRailSpeedFactor();

        return Math.min(railSpeed * speedFactor, LEGACY_MAX_CART_SPEED_CAP);
    }

    /**
     * 1.0 + SPEED effect amplifier * 0.3 + WALKING research strength * 2 + deliveryman Agility *
     * 0.03. Unchanged from upstream CPE 1.21.1: amplifier is used as-is, without +1.
     */
    @Unique
    private double pathFindEdition$calculateCitizenRailSpeedFactor()
    {
        double speedFactor = 1;
        if (!this.getPassengers().isEmpty() && this.getPassengers().getFirst() instanceof AbstractEntityCitizen citizen)
        {
            MobEffectInstance effect = citizen.getEffect(MobEffects.SPEED);
            if (effect != null) {
                speedFactor += effect.getAmplifier() * 0.3;
            }
            if (citizen.getCitizenColonyHandler() != null && citizen.getCitizenColonyHandler().getColony() != null
                    && citizen.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(WALKING) > 0) {
                speedFactor += citizen.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(WALKING) * 2;
            }
            if (citizen.getCitizenData() != null && citizen.getCitizenData().getJob() instanceof JobDeliveryman) {
                speedFactor += citizen.getCitizenData().getCitizenSkillHandler().getLevel(Skill.Agility) * 0.03;
            }
        }
        return speedFactor;
    }

    /**
     * Ghost-citizen-passenger cleanup. MineColonies' own {@code tick()} already discards the cart
     * every 20 ticks when it has no passengers at all; this only adds the CPE-specific case of a
     * passenger that is itself a ghost citizen (civilianID == 0).
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void pathFindEdition$ghostCartCleanup(CallbackInfo ci)
    {
        if (this.tickCount % 20 == 19 && !this.getPassengers().isEmpty())
        {
            for (Entity passenger : this.getPassengers())
            {
                if (passenger instanceof EntityCitizen citizen && citizen.getCivilianID() == 0)
                {
                    citizen.stopRiding();
                    this.remove(DISCARDED);
                    break;
                }
            }
        }
    }
}
