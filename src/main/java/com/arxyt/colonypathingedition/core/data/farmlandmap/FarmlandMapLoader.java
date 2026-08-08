package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FarmlandMapLoader extends SimpleJsonResourceReloadListener<List<FarmlandMapLoader.SeedFarmlandEntry>> {

    /**
     * One entry of the "values" array in a farmland_map JSON file. Same on-disk shape as before
     * the 26.1.2 Gson -> Codec migration of {@link SimpleJsonResourceReloadListener}.
     */
    public record SeedFarmlandEntry(Identifier seed, Identifier farmland) {
        public static final Codec<SeedFarmlandEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("seed").forGetter(SeedFarmlandEntry::seed),
                Identifier.CODEC.fieldOf("farmland").forGetter(SeedFarmlandEntry::farmland)
        ).apply(instance, SeedFarmlandEntry::new));
    }

    // Decodes { "values": [ { "seed": "...", "farmland": "..." }, ... ] } - unchanged file format.
    private static final Codec<List<SeedFarmlandEntry>> CODEC =
            SeedFarmlandEntry.CODEC.listOf().fieldOf("values").codec();

    public static final FarmlandMapLoader INSTANCE = new FarmlandMapLoader();

    private static final Map<Identifier, Identifier> mappings = new LinkedHashMap<>();

    private FarmlandMapLoader() {
        super(CODEC, FileToIdConverter.json("farmland_map"));
    }

    @Override
    protected void apply(
            Map<Identifier, List<SeedFarmlandEntry>> object,
            @NotNull ResourceManager manager,
            @NotNull ProfilerFiller profiler) {

        mappings.clear();

        object.forEach((id, entries) -> {
            try {
                for (SeedFarmlandEntry entry : entries) {
                    mappings.put(entry.seed(), entry.farmland());
                }
            } catch (Exception e) {
                ColonyPathingEdition.LOGGER.error(
                        "[SpecialSeeds] Failed to parse {}: {}", id, e.getMessage());
            }
        });

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Loaded {} mappings", mappings.size());

        SpecialSeedManager.rebuildFromMappings(mappings);
    }

    public static Map<Identifier, Identifier> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }
}
