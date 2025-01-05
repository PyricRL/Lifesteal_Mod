package com.pyric.lifestealmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    // create heart as an item with a stack of 1
    public static final Item HEART = registerItem("heart", Item::new, new Item.Settings().maxCount(1));

    /**
     * registerItem() allows to create an item with a set name, function, and settings.
     */
    public static Item registerItem(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("lifestealmod", path));
        return Items.register(registryKey, factory, settings);
    }

    /**
     * registerModItems registers the HEART item into the group INGREDIENTS. This method is called in the onInitialize() method in LifestealMod.java.
     */
    public static void registerModItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(HEART);
        });
    }

}
