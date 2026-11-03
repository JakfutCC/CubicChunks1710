package com.cardinalstar.cubicchunks.world.convert.adapter;

import net.minecraft.nbt.NBTTagCompound;

public interface SectionAdapter {

    void writeSectionData(CubeData data, NBTTagCompound section);

    void readSectionData(NBTTagCompound section, CubeData data);

}
