package com.pyric.lifestealmod;

import com.pyric.lifestealmod.item.ModItems;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class LifestealMod implements ModInitializer {
    public static final String MOD_ID = "lifestealmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // hash map for storing cooldowns of heart additions for each player
    final HashMap<UUID, Integer> newHeartCooldown = new HashMap<>();

    /**
     * onInitialize() calls all the methods associated with registering events.
     */
    @Override
    public void onInitialize() {
        registerEvents();
        ModItems.registerModItems();

        CommandRegistrationCallback.EVENT.register(LifestealCommand::registerCommands);
        ModConfig.instance().save();
    }

    /**
     * Register the recipes depending on a variable
     */
    public static final Identifier HEART_RECIPE_ID = Identifier.of("lifestealmod", "heart");

    /**
     * registerEvents() has the event "ALLOW_DEATH" which handles the calling of the method that gives and
     * takes player hearts away.
     */
    private void registerEvents() {

        // fatal damage taken
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof PlayerEntity player)) return;

            double minHealth = ModConfig.instance().minHeartCap * 2;
            double maxHealth = ModConfig.instance().maxHeartCap * 2;
            double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

            // confirm attacker is an entity, and set type
            PlayerEntity attacker = (source.getAttacker() instanceof PlayerEntity p) ? p : null;

            if (attacker != null) {
                double attackerMaxHealth = attacker.getMaxHealth();

                if (playerMaxHealth > minHealth) {
                    // decrease killed player health
                    decreasePlayerHealth(player, ModConfig.instance().heartDecrease * 2);

                    // increase attacker health if not at cap
                    if (attackerMaxHealth < maxHealth) {
                        increasePlayerHealth(attacker, ModConfig.instance().heartIncrease * 2);
                        attacker.sendMessage(Text.literal("You gained a heart!"), false);
                        LifestealMod.LOGGER.info("Player " + attacker.getName().getString() + " gained a heart.");
                    } else {
                        // attacker is at max heart cap, drop a heart item
                        attacker.giveItemStack(new ItemStack(ModItems.HEART));
                        attacker.sendMessage(Text.literal("You gained a heart! It was dropped to you because you hit the heart cap."), false);
                        LifestealMod.LOGGER.info("Player " + attacker.getName().getString() + " gained a heart.");
                    }

                    player.sendMessage(Text.literal("You lost a heart!"), false);
                    LifestealMod.LOGGER.info("Player " + player.getName().getString() + " lost a heart.");
                } else {
                    player.sendMessage(Text.literal("Your heart count is too low to lose a heart."), false);
                    attacker.sendMessage(Text.literal("The player you killed did not have a sufficient amount of hearts to give you."), false);
                }

                // handle what happens when 0 hearts
                if (player.getMaxHealth() <= 1 && player instanceof ServerPlayerEntity serverPlayer) {
                    MinecraftServer server = serverPlayer.getServer();
                    if (server != null && !server.isSingleplayer()) {
                        switch (ModConfig.instance().zeroHeartAction) {
                            case BAN -> {
                                BannedPlayerEntry entry = new BannedPlayerEntry(player.getGameProfile(), new Date(), "Server", null, "Lost all hearts");
                                server.getPlayerManager().getUserBanList().add(entry);
                                serverPlayer.networkHandler.disconnect(Text.literal("You lost all of your hearts, now you are banned."));
                                LifestealMod.LOGGER.info("Player " + player.getName().getString() + " was banned for losing all hearts.");
                            }
                            case CREATIVE -> {
                                serverPlayer.changeGameMode(GameMode.CREATIVE);
                                serverPlayer.sendMessage(Text.literal("You lost all of your hearts, now you are in creative."));
                                LifestealMod.LOGGER.info("Player " + player.getName().getString() + " was put in creative for losing all hearts");
                            }
                            case SPECTATOR -> {
                                serverPlayer.changeGameMode(GameMode.SPECTATOR);
                                serverPlayer.sendMessage(Text.literal("You lost all of your hearts, now you are in spectator."));
                                LifestealMod.LOGGER.info("Player " + player.getName().getString() + " was put in spectator for losing all hearts.");
                            }
                            case RESET -> {
                                player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(10);
                                player.setHealth(10);
                                serverPlayer.sendMessage(Text.literal("You lost all of your hearts, now you are reset back to 10."));
                                LifestealMod.LOGGER.info("Player " + player.getName().getString() + " was reset for losing all hearts.");
                            }
                        }
                    }
                }
            }

            // handle deaths from mobs
            else if (source.getAttacker() instanceof MobEntity && ModConfig.instance().mobKillHeartLoss) {
                if (playerMaxHealth > minHealth) {
                    decreasePlayerHealth(player, ModConfig.instance().heartDecrease * 2);
                    player.sendMessage(Text.literal("You lost a heart! Current max health: " + player.getMaxHealth()), false);
                    LifestealMod.LOGGER.info("Player " + player.getName().getString() + " lost a heart to a mob.");
                } else {
                    player.sendMessage(Text.literal("Your heart count is too low to lose a heart."), false);
                }
            }
        });

        // Register the player holding an item (parameters give you info about which event)
        UseItemCallback.EVENT.register((player, world, hand) -> {

            // Get the stack that the player is holding
            ItemStack itemStack = player.getStackInHand(hand);
            ItemStack heartStack = new ItemStack(ModItems.HEART);

            // if the player is holding my custom heart item and the name of it is "Heart"
            if (itemStack.getItem() == ModItems.HEART && itemStack.isOf(ModItems.HEART)) {

                // if person holding is player
                if (player instanceof ServerPlayerEntity) {

                    // get max health of player
                    double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

                    // if player max health is greater or equal to max health cap
                    if (playerMaxHealth >= ModConfig.instance().maxHeartCap * 2) {

                        // send message and cancel action
                        player.sendMessage(Text.literal("You have reached the maximum heart limit!"), false);
                        return ActionResult.SUCCESS;
                    }

                    // increase player health and tell player that it happened
                    increasePlayerHealth(player, (double) ModConfig.instance().heartIncrease * 2);
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
            if (ModConfig.instance().heartRegen) {

                // for each player in the server
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                    // create cooldown variable with hashmap or set to 0
                    int cooldown = newHeartCooldown.getOrDefault(player.getUuid(), 0);

                    // if player max health is less set max health
                    if (player.getMaxHealth() < (double) ModConfig.instance().heartRegenAmount * 2) {

                        // increment cooldown and check if its greater or equal to heartRegenTime * 1200 (used so the config is easier to configure)
                        if (++cooldown >= ModConfig.instance().heartRegenTime * 1200) {

                            // increase player health, send message, and reset cooldown
                            increasePlayerHealth(player, (double) ModConfig.instance().heartIncrease * 2);
                            player.sendMessage(Text.literal("You gained a heart!"), false);
                            cooldown = 0;
                        }
                    }

                    // add player UUID to hashmap with their cooldown
                    newHeartCooldown.put(player.getUuid(), cooldown);
                }
            }

            // checks if player is not online and removes their UUID and cooldown from the hashmap
            newHeartCooldown.keySet().removeIf((uuid) -> server.getPlayerManager().getPlayer(uuid) == null);
        });
    }

    /**
     * increasePlayerHealth() takes the argument of player, then gets the health of the player originally
     * and adds the value of healthIncrease from the file "ModConfig".
     */
    public static void increasePlayerHealth(PlayerEntity player, double amount) {
        double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH); // store player max health in a double
        player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(playerMaxHealth + amount); // increase playerMaxHealth by healthIncrease
    }

    /**
     * decreasePlayerHealth() takes the argument of player, then gets the health of the player originally
     * and adds the negative value of healthIncrease from the file "ModConfig".
     */
    public static void decreasePlayerHealth(PlayerEntity player, double amount) {
        double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH); // store player max health in a double
        player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(playerMaxHealth + -amount); // decrease playerMaxHealth by healthDecrease
    }
}
