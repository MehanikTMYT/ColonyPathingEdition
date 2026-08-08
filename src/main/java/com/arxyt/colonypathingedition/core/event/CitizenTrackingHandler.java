package com.arxyt.colonypathingedition.core.event;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;


public class CitizenTrackingHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if(event.getEntity().level().isClientSide()) {
            return;
        }

        if(!(event.getTarget() instanceof AbstractEntityCitizen)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();

        // ClientboundTeleportEntityPacket no longer has an Entity-argument constructor in 26.1.2;
        // rebuild the same absolute teleport packet from the target entity's current state.
        final Entity target = event.getTarget();
        player.connection.send(ClientboundTeleportEntityPacket.teleport(
                target.getId(),
                PositionMoveRotation.of(target),
                Set.of(),
                target.onGround()
        ));
    }
}
