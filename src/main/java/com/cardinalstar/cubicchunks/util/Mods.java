package com.cardinalstar.cubicchunks.util;

import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import cpw.mods.fml.common.Loader;

public enum Mods implements IMod, ITargetMod {

    Angelica(ModIDs.ANGELICA, "com.gtnewhorizons.angelica.loading.AngelicaTweaker"),
    ChunkAPI(ModIDs.CHUNK_API, "com.falsepattern.chunk.internal.core.CoreLoadingPlugin"),
    EtFuturumRequiem(ModIDs.ET_FUTURUM_REQUIEM),
    JourneyMap(ModIDs.JOURNEY_MAP),

    Minecraft(ModIDs.MINECRAFT) {

        @Override
        public boolean isModLoaded() {
            return true;
        }
    },

    ;

    public static class ModIDs {

        public static final String ANGELICA = "angelica";
        public static final String CHUNK_API = "chunkapi";
        public static final String ET_FUTURUM_REQUIEM = "etfuturum";
        public static final String JOURNEY_MAP = "journeymap";
        public static final String MINECRAFT = "minecraft";
    }

    public final String ID;
    public final String resourceDomain;
    protected boolean checkedMod, modLoaded;
    protected final TargetModBuilder builder;

    Mods(String ID) {
        this.ID = ID;
        this.resourceDomain = ID.toLowerCase(Locale.ENGLISH);
        this.builder = new TargetModBuilder().setModId(getEffectiveModID());
    }

    Mods(String ID, String coremodClass) {
        this.ID = ID;
        this.resourceDomain = ID.toLowerCase(Locale.ENGLISH);
        this.builder = new TargetModBuilder().setModId(getEffectiveModID())
            .setCoreModClass(coremodClass);
    }

    @Override
    public @Nonnull TargetModBuilder getBuilder() {
        return builder;
    }

    @Override
    public String getID() {
        return ID;
    }

    protected String getEffectiveModID() {
        return ID;
    }

    public boolean isModLoaded() {
        if (!checkedMod) {
            this.modLoaded = Loader.isModLoaded(getEffectiveModID());
            checkedMod = true;
        }
        return this.modLoaded;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }

    public String getResourcePath(String... path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public ResourceLocation getResourceLocation(String... path) {
        return new ResourceLocation(this.resourceDomain, String.join("/", path));
    }
}
