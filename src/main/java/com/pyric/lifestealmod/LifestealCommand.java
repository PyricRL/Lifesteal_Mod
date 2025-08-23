package com.pyric.lifestealmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pyric.lifestealmod.item.ModItems;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LifestealCommand {

    /**
     * runHelp() displays text about each command and what they can do.
     */
    private static int runHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        player.sendMessage(Text.literal("§6---- Lifesteal Mod Help ----"), false);
        player.sendMessage(Text.literal("§e/lifesteal set <player> <min|max> <amount> §7- Set min or max hearts"), false);
        player.sendMessage(Text.literal("§e/lifesteal ondeath <gain|lose> <amount> §7- Hearts gained/lost on death"), false);
        player.sendMessage(Text.literal("§e/lifesteal onkill <gain|lose> <amount> §7- Hearts gained/lost on kill"), false);
        player.sendMessage(Text.literal("§e/lifesteal regen <enable|disable> §7- Enable or disable heart regeneration"), false);
        player.sendMessage(Text.literal("§e/lifesteal regentime <seconds> §7- Time to regenerate one heart"), false);
        player.sendMessage(Text.literal("§e/lifesteal withdraw <enable|disable> §7- Enable or disable heart withdrawal"), false);

        return 1;
    }

    /**
     * runWithdrawHeart() checks whether the heartWithdraw variable is set to true in the config,
     * then runs a series of checks and takes a set amount of player hearts away and puts them into the
     * players inventory.
     */
    private static int runWithdraw(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        // if heart withdraw not enabled
        if (!ModConfig.instance().heartWithdraw) {
            player.sendMessage(Text.literal("This command is disabled within the mod configuration"), false);
            return 0;
        }

        // if heart withdraw enabled
        else {
            int amount = 1;
            try {
                amount = IntegerArgumentType.getInteger(context, "amount");
            } catch (IllegalArgumentException ignored) {}

            double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

            // if player withdraws more hearts than they have
            if (playerMaxHealth - amount * 2 < ModConfig.instance().minHeartCap * 2) {
                player.sendMessage(Text.literal("Cannot withdraw hearts under " + ModConfig.instance().minHeartCap + "!"), false);
                return 0;
            }

            // if player health is greater than the amount they want to withdraw
            if (playerMaxHealth >= amount) {

                // decrease player health, drop item, and send message to player
                LifestealMod.decreasePlayerHealth(player, amount * 2);
                ItemStack heartStack = new ItemStack(ModItems.HEART, amount);
                player.giveItemStack(heartStack);

                player.sendMessage(Text.literal("Heart withdrawn successfully!"), false);
                return 1;
            }

            // if required arguments fail
            else {
                player.sendMessage(Text.literal("Heart withdraw failure!"), false);
            }
            return 0;
        }
    }

    /**
     * runResetPlayer() is a command only accessible via the server, and allows the server
     * to reset all the players hearts or reset a certain players hearts.
     */
    private static int runResetPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ModConfig config = ModConfig.instance();

        // for each player in the game
        for (ServerPlayerEntity serverPlayerEntity : EntityArgumentType.getPlayers(context, "players")) {

            // set max health to max health value, set their health to max health value, and send message
            serverPlayerEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(config.maxHeartCap * 2);
            serverPlayerEntity.setHealth(20.0f);
            serverPlayerEntity.sendMessage(Text.literal("Player reset successfully"), false);
            return 1;
        }
        return 0;
    }

    /**
     * runSetHeartMax() is a command accessible via the server, and sets all players max heart
     * value to the given amount.
     */
    private static int runSetHeartMax(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int hearts = IntegerArgumentType.getInteger(context, "amount");
        int health = hearts * 2;
        ModConfig config = ModConfig.instance();

        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            // Only adjust current health if it's higher than new max
            if (player.getHealth() > health) {
                // Set max health
                player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(health);

                // Modify health amount
                player.setHealth(health);
            }

            player.sendMessage(Text.literal("Your max hearts have been updated to " + hearts), false);
        }

        // Update config
        config.maxHeartCap = hearts;
        config.save();

        context.getSource().sendMessage(Text.literal("Max heart cap updated to " + hearts));
        return 1;
    }

    /**
     * runSetHeartMin() is a command accessible via the server, and sets all players min heart
     * value to the given amount.
     */
    private static int runSetHeartMin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int hearts = IntegerArgumentType.getInteger(context, "amount");
        int health = hearts * 2;
        ModConfig config = ModConfig.instance();

        // Only adjust health of players that are lower than changed value
        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            if (player.getHealth() < health) {
                // Set min health
                player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(health);

                // Modify health amount
                player.setHealth(health);

                player.sendMessage(Text.literal("Your min hearts have been updated to " + hearts), false);
            }
        }

        // Update config
        config.minHeartCap = hearts;
        config.save();

        context.getSource().sendMessage(Text.literal("Min heart cap updated to " + hearts));
        return 1;
    }

    /**
     * runSetHeartGain() is a command accessible via the server, and sets the amount of
     * hearts that are gained per player kill.
     */
    private static int runSetHeartGain(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int hearts = IntegerArgumentType.getInteger(context, "amount");
        ModConfig config = ModConfig.instance();

        config.heartIncrease = hearts;
        config.save();

        context.getSource().sendMessage(Text.literal("Hearts gained per kill set to " + hearts));

        return 1;
    }

    /**
     * runSetHeartLost() is a command accessible via the server, and sets the amount of
     * hearts that are lost per death.
     */
    private static int runSetHeartLoss(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int hearts = IntegerArgumentType.getInteger(context, "amount");
        ModConfig config = ModConfig.instance();

        config.heartDecrease = hearts;
        config.save();

        context.getSource().sendMessage(Text.literal("Hearts lost per death set to " + hearts));

        return 1;
    }

    /**
     * runSetRegen() is a command accessible via the server, and sets health regen to be on/off.
     */
    private static int runSetRegen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig config = ModConfig.instance();

        config.heartRegen = enabled;
        config.save();

        context.getSource().sendMessage(Text.literal("Heart regen set to " + enabled));

        return 1;
    }

    /**
     * runSetRegenTime() is a command accessible via the server, and sets the time for hearts to
     * regen in minutes.
     */
    private static int runSetRegenTime(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int time = IntegerArgumentType.getInteger(context, "time");
        ModConfig config = ModConfig.instance();

        config.heartRegenTime = time;
        config.save();

        context.getSource().sendMessage(Text.literal("Heart regen time set to " + time + " minutes"));

        return 1;
    }

    /**
     * runSetWithdraw() is a command accessible via the server, and sets the ability to withdraw hearts.
     */
    private static int runSetWithdraw(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig config = ModConfig.instance();

        config.heartWithdraw = enabled;
        config.save();

        context.getSource().sendMessage(Text.literal("Heart withdraw set to " + enabled));

        return 1;
    }

    /**
     * runReset() is a command accessible via the server, and resets all config options to default.
     */
    private static int runResetSettings(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ModConfig config = ModConfig.instance();

        int heartMin = config.defaultMinHeartCap;
        int healthMin = heartMin * 2;

        int heartMax = config.defaultMaxHeartCap;
        int healthMax = heartMax * 2;

        // reset values
        config.maxHeartCap = ModConfig.defaultMaxHeartCap;
        config.heartIncrease = ModConfig.defaultHeartIncrease;
        config.heartDecrease = ModConfig.defaultHeartDecrease;
        config.heartRegen = ModConfig.defaultHeartRegen;
        config.heartRegenAmount = ModConfig.defaultHeartRegenAmount;
        config.heartRegenTime = ModConfig.defaultHeartRegenTime;
        config.heartWithdraw = ModConfig.defaultHeartWithdraw;

        config.save();

        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            // Only adjust current health if it's lower than new min
            if (player.getHealth() < healthMin) {
                // Set min health
                player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(healthMin);

                // Modify health amount
                player.setHealth(healthMin);
                player.sendMessage(Text.literal("Your min hearts have been updated to " + heartMin), false);
            }

            // Only adjust current health if it's higher than new max
            if (player.getHealth() > healthMax) {
                // Set max health
                player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(healthMax);

                // Modify health amount
                player.setHealth(healthMax);
                player.sendMessage(Text.literal("Your min hearts have been updated to " + heartMax), false);
            }
        }

        context.getSource().sendMessage(Text.literal("Reset all values to default"));

        return 1;
    }

    /**
     * Setup for helpCommand
     */
    private static LiteralCommandNode<ServerCommandSource> helpCommand() {
        LiteralCommandNode<ServerCommandSource> helpNode = CommandManager
                .literal("help")
                .executes(LifestealCommand::runHelp)
                .build();
        return helpNode;
    }

    /**
     * Setup for withdrawCommand
     */
    private static LiteralCommandNode<ServerCommandSource> withdrawCommand() {
        LiteralCommandNode<ServerCommandSource> withdrawNode = CommandManager
                .literal("withdraw")
                .executes(LifestealCommand::runWithdraw)
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> withdrawAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(0))
                .executes(LifestealCommand::runWithdraw)
                .build();
        withdrawNode.addChild(withdrawAmountNode);
        return withdrawNode;
    }

    /**
     * Setup for resetCommand
     */
    private static LiteralCommandNode<ServerCommandSource> resetCommand() {
        LiteralCommandNode<ServerCommandSource> resetNode = CommandManager
                .literal("reset")
                .build();
        ArgumentCommandNode<ServerCommandSource, EntitySelector> resetPlayerNode = CommandManager
                .argument("players", EntityArgumentType.players())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runResetPlayer)
                .build();
        resetNode.addChild(resetPlayerNode);
        return resetNode;
    }

    /**
     * Setup for setHeartMaxCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setHeartMaxCommand() {
        LiteralCommandNode<ServerCommandSource> setHeartMaxNode = CommandManager
                .literal("setmax")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> setHeartMaxAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(0))
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetHeartMax)
                .build();
        setHeartMaxNode.addChild(setHeartMaxAmountNode);
        return setHeartMaxNode;
    }

    /**
     * Setup for setHeartMinCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setHeartMinCommand() {
        LiteralCommandNode<ServerCommandSource> setHeartMinNode = CommandManager
                .literal("setmin")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> setHeartMinAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(0))
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetHeartMin)
                .build();
        setHeartMinNode.addChild(setHeartMinAmountNode);
        return setHeartMinNode;
    }

    /**
     * Setup for setHeartGainCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setHeartGainCommand() {
        LiteralCommandNode<ServerCommandSource> setHeartGainNode = CommandManager
                .literal("gain")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> setHeartGainAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(0))
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetHeartGain)
                .build();
        setHeartGainNode.addChild(setHeartGainAmountNode);
        return setHeartGainNode;
    }

    /**
     * Setup for setHeartLossCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setHeartLossCommand() {
        LiteralCommandNode<ServerCommandSource> setHeartLossNode = CommandManager
                .literal("loss")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> setHeartLossAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(0))
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetHeartLoss)
                .build();
        setHeartLossNode.addChild(setHeartLossAmountNode);
        return setHeartLossNode;
    }

    /**
     * Setup for setHeartRegenCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setRegenCommand() {
        LiteralCommandNode<ServerCommandSource> setRegenNode = CommandManager
                .literal("regen")
                .build();
        ArgumentCommandNode<ServerCommandSource, Boolean> setRegenBoolNode = CommandManager
                .argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetRegen)
                .build();
        setRegenNode.addChild(setRegenBoolNode);
        return setRegenNode;
    }

    /**
     * Setup for setRegenTimeCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setRegenTimeCommand() {
        LiteralCommandNode<ServerCommandSource> setRegenNode = CommandManager
                .literal("regentime")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> setRegenTimeNode = CommandManager
                .argument("time", IntegerArgumentType.integer(0))
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetRegenTime)
                .build();
        setRegenNode.addChild(setRegenTimeNode);
        return setRegenNode;
    }

    /**
     * Setup for setWithdrawCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setWithdrawCommand() {
        LiteralCommandNode<ServerCommandSource> setWithdrawNode = CommandManager
                .literal("withdraw")
                .build();
        ArgumentCommandNode<ServerCommandSource, Boolean> setWithdrawBoolNode = CommandManager
                .argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetWithdraw)
                .build();
        setWithdrawNode.addChild(setWithdrawBoolNode);
        return setWithdrawNode;
    }

    /**
     * Setup for resetSettingsCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> resetSettingsCommand() {
        LiteralCommandNode<ServerCommandSource> resetSettingsNode = CommandManager
                .literal("reset")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runResetSettings)
                .build();
        return resetSettingsNode;
    }

    /**
     * registerCommands() registers all nodes to their main command identifier. This method is called in the onInitialize() method in LifestealMod.java.
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> lifestealNode = CommandManager.literal("lifesteal").build();

        dispatcher.getRoot().addChild(lifestealNode);

        lifestealNode.addChild(helpCommand());
        lifestealNode.addChild(withdrawCommand());
        lifestealNode.addChild(resetCommand());
        lifestealNode.addChild(setHeartMaxCommand());
        lifestealNode.addChild(setHeartMinCommand());
        lifestealNode.addChild(setHeartGainCommand());
        lifestealNode.addChild(setHeartLossCommand());
        lifestealNode.addChild(setRegenCommand());
        lifestealNode.addChild(setRegenTimeCommand());
        lifestealNode.addChild(setWithdrawCommand());
        lifestealNode.addChild(resetSettingsCommand());
    }
}