package com.arxyt.colonypathingedition.core.network.message;

import com.arxyt.colonypathingedition.api.network.ICPEMessage;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SpecialSeedSyncMessage implements ICPEMessage<SpecialSeedSyncMessage> {

    public static final Type<SpecialSeedSyncMessage> TYPE =
            ICPEMessage.createType(SpecialSeedSyncMessage.class);

    public static final StreamCodec<RegistryFriendlyByteBuf, SpecialSeedSyncMessage> CODEC =
            ICPEMessage.streamCodec(SpecialSeedSyncMessage::new);

    private final Map<Identifier, Identifier> mappings;

    public SpecialSeedSyncMessage(Map<Identifier, Identifier> mappings) {
        this.mappings = mappings;
    }

    public SpecialSeedSyncMessage(RegistryFriendlyByteBuf buf) {

        int size = buf.readInt();

        this.mappings = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            Identifier seed = buf.readIdentifier();
            Identifier soil = buf.readIdentifier();
            mappings.put(seed, soil);
        }
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {

        buf.writeInt(mappings.size());

        mappings.forEach((seed, soil) -> {
            buf.writeIdentifier(seed);
            buf.writeIdentifier(soil);
        });
    }

    @Override
    public void handle(IPayloadContext context) {

        context.enqueueWork(() ->
                SpecialSeedManager.rebuildFromMappings(mappings)
        );
    }

    @Override
    public @NotNull Type<SpecialSeedSyncMessage> type() {
        return TYPE;
    }
}
