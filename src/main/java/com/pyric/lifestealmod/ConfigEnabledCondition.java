package com.pyric.lifestealmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;

public record ConfigEnabledCondition(boolean value) implements ResourceCondition {
    public static final Identifier ID = Identifier.of("lifestealmod", "config_enabled");

    public static final MapCodec<ConfigEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("value").forGetter(ConfigEnabledCondition::value)
    ).apply(instance, ConfigEnabledCondition::new));

    // The registered condition type (you’ll register this once in your init)
    public static final ResourceConditionType<ConfigEnabledCondition> TYPE =
            ResourceConditionType.create(ID, CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(@Nullable RegistryOps.RegistryInfoGetter registryInfo) {
        return value && ModConfig.instance().craftingRecipeEnabled;
    }
}

