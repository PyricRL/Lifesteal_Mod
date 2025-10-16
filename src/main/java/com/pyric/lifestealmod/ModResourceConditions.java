package com.pyric.lifestealmod;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.util.Identifier;

public class ModResourceConditions {
    public static final ResourceConditionType<ConfigEnabledCondition> CONFIG_ENABLED =
            ResourceConditionType.create(Identifier.of("lifestealmod", "config_enabled"), ConfigEnabledCondition.CODEC);
}
