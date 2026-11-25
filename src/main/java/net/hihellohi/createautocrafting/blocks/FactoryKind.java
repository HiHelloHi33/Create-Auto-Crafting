package net.hihellohi.createautocrafting.blocks;

import net.minecraft.util.StringRepresentable;

/** Which factory a formed multiblock is (decides the look of its body). */
public enum FactoryKind implements StringRepresentable {
    NONE("none"),
    ETCHER("etcher"),
    ASSEMBLER("assembler");

    private final String name;

    FactoryKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
