package com.cardinalstar.cubicchunks.api.registry;

import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

@SuppressWarnings("unchecked")
public class AbstractRegistryEntry<TSelf> implements IRegistryEntry<TSelf> {

    public UniqueIdentifier registryName;

    @Override
    public TSelf setRegistryName(UniqueIdentifier name) {
        registryName = name;
        return (TSelf) this;
    }

    public TSelf setUnlocalizedName(String name) {
        return (TSelf) this;
    }
}
