package com.pyric.lifestealmod;

import com.pyric.lifestealmod.item.ModItems;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;

public class LifestealMod implements ModInitializer {
    public static final String MOD_ID = "lifestealmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    final HashMap<UUID, Integer> newHeartCooldown = new HashMap<>();	// hash map for storing cooldowns of heart additions for each player

    /**
     * onInitialize() calls all the methods associated with registering events
     */
    @Override
    public void onInitialize() {
        registerEvents();
        ModItems.registerModItems();

        CommandRegistrationCallback.EVENT.register(LifestealCommand::registerCommands);
    }

    /**
     * registerEvents() has the event "ALLOW_DEATH" which handles the calling of the method that gives and
     * takes player hearts away
     */
    private void registerEvents() {

        // fatal damage taken
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {

            // if entity that took damage is player
            if (entity instanceof PlayerEntity) {

                ItemStack heartStack = new ItemStack(ModItems.HEART);

                // create a player variable that stores the entity and get the max health of the player
                PlayerEntity player = (PlayerEntity) entity;
                double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

                // if damage done was by a player
                if (source.getAttacker() instanceof PlayerEntity) {
                    PlayerEntity attacker = (PlayerEntity) source.getAttacker();
                    double attackerMaxHealth = attacker.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

                    // if attacker health is less than max heart cap
                    if (attackerMaxHealth < ModConfig.maxHealthCap) {

                        // if killed player health cap is above min heart cap
                        if (playerMaxHealth > ModConfig.minHealthCap) {

                            // increase player health and send a message to the player
                            increasePlayerHealth(attacker, ModConfig.healthIncrease);
                            attacker.sendMessage(Text.literal("You gained a heart!"), false);
                        }

                        // if killed player health cap is equal or below min heart cap
                        else {
                            attacker.sendMessage(Text.literal("The player you killed does not have the minimum required hearts to give you."), false);
                        }
                    }

                    if (attackerMaxHealth >= ModConfig.maxHealthCap) {
                        if (playerMaxHealth > ModConfig.minHealthCap) {

                            // increase player health, send message to player, and drop heart
                            decreasePlayerHealth(player, ModConfig.healthDecrease);
                            attacker.sendMessage(Text.literal("You have reached the maximum heart limit, a heart has been dropped!"), false);
                            attacker.giveItemStack(heartStack);
                        }

                        // if killed player health cap is equal or below min heart cap
                        else {
                            attacker.sendMessage(Text.literal("The player you killed does not have the minimum required hearts to give you."), false);
                        }
                    }

                    // if killed player health is greater than min health cap
                    if (playerMaxHealth > ModConfig.minHealthCap) {

                        // decrease player health and send a message to the player
                        player.sendMessage(Text.literal("You lost a heart!"), false);
                    }

                    // if killed player health is less than or equal to min health cap
                    else {
                        player.sendMessage(Text.literal("Your heart count is too low to lose a heart."), false);
                    }
                }
                // if you didn't die to player
                else {
                    player.sendMessage(Text.literal("You didn't lose a heart due to not dying by a player"), false);
                }
            }
            // return false so that it won't do anything if it doesn't pass any checks
            return false;
        });

        // Register the player holding an item (parameters give you info about which event)
        UseItemCallback.EVENT.register((player, world, hand) -> {

            // Get the stack that the player is holding
            ItemStack itemStack = player.getStackInHand(hand);
            ItemStack heartStack = new ItemStack(ModItems.HEART);

            // if the player is holding my custom heart item and the name of it is "Heart"
            if (itemStack.getItem() == ModItems.HEART
                    && itemStack.getName().getString().equals("Heart")) {

                // if person holding is player
                if (player instanceof ServerPlayerEntity) {

                    // get max health of player
                    double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

                    // if player max health is greater or equal to max health cap
                    if (playerMaxHealth >= ModConfig.maxHealthCap) {

                        // send message and cancel action
                        player.sendMessage(Text.literal("You have reached the maximum heart limit!"), false);
                        return ActionResult.SUCCESS;
                    }

                    // increase player health and tell player that it happened
                    increasePlayerHealth(player, ModConfig.healthIncrease);
                    player.sendMessage(Text.literal("You gained a heart!"), false);

                    // take away the item
                    itemStack.decrement(1);

                    // return success (skips through the rest of the EVENT.register)
                    return ActionResult.SUCCESS;
                }
            }

            // pass if no conditions are met
            return ActionResult.PASS;

        });

        // Registers each tick end as an event
        ServerTickEvents.END_SERVER_TICK.register((server) -> {

            // if heart regen is turned on
            if (ModConfig.heartRegen) {
                // for each player in the server
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                    // create cooldown variable with hashmap or set to 0
                    int cooldown = newHeartCooldown.getOrDefault(player.getUuid(), 0);

                    // if player max health is less set max health
                    if (player.getMaxHealth() < ModConfig.healthRegenAmount) {

                        // increment cooldown and check if its greater or equal to heartRegenTime * 1200 (used so the config is easier to configure)
                        if (++cooldown >= ModConfig.heartRegenTime * 1200) {

                            // increase player health, send message, and reset cooldown
                            increasePlayerHealth(player, ModConfig.healthIncrease);
                            player.sendMessage(Text.literal("You gained a heart!"), false);
                            cooldown = 0;
                        }
                    }

                    // add player UUID to hashmap with their cooldown
                    newHeartCooldown.put(player.getUuid(), cooldown);
                }
            }

            for (UUID key : newHeartCooldown.keySet()) {
                boolean playerInKey = false;
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (key == player.getUuid()) {
                        playerInKey = true;
                    }
                }
                if (!playerInKey) {
                    newHeartCooldown.remove(key);
                }
            }
        });
    }

    /**
     * increasePlayerHealth() takes the argument of player, then gets the health of the player originally
     * and adds the value of healthIncrease from the file "ModConfig"
     */
    public static void increasePlayerHealth(PlayerEntity player, double amount) {
        double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH); // store player max health in a double
        player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(playerMaxHealth + amount); // decrease playerMaxHealth by healthDecrease
    }

    /**
     * decreasePlayerHealth() takes the argument of player, then gets the health of the player originally
     * and adds the negative value of healthIncrease from the file "ModConfig"
     */
    public static void decreasePlayerHealth(PlayerEntity player, double amount) {
        double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH); // store player max health in a double
        player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(playerMaxHealth + -amount); // decrease playerMaxHealth by healthDecrease
    }
}
