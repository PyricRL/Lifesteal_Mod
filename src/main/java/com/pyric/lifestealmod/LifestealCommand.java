package com.pyric.lifestealmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

        source.sendMessage(Text.literal("§6---- Lifesteal Mod Help ----"));

        if (source.hasPermissionLevel(2)) {
            source.sendMessage(Text.literal("§e/lifesteal help §7- Show this help message"));
            source.sendMessage(Text.literal("§e/lifesteal setmax <amount> §7- Set max hearts"));
            source.sendMessage(Text.literal("§e/lifesteal setmin <amount> §7- Set min hearts"));
            source.sendMessage(Text.literal("§e/lifesteal gain <amount> §7- Hearts gained per kill"));
            source.sendMessage(Text.literal("§e/lifesteal loss <amount> §7- Hearts lost per death"));
            source.sendMessage(Text.literal("§e/lifesteal regen <enable|disable> §7- Enable or disable heart regeneration"));
            source.sendMessage(Text.literal("§e/lifesteal setregenamount <amount> §7- Set amount of hearts per regen"));
            source.sendMessage(Text.literal("§e/lifesteal setregentime <time> §7- Set time in minnutes before heart regen"));
            source.sendMessage(Text.literal("§e/lifesteal withdraw <amount> §7- Withdraw an amount of hearts"));
            source.sendMessage(Text.literal("§e/lifesteal setwithdraw <enable|disable> §7- Enable or disable heart withdrawal"));
            source.sendMessage(Text.literal("§e/lifesteal setheartcrafting <enable|disable> §7- Enable or disable heart crafting"));
            source.sendMessage(Text.literal("§e/lifesteal zeroheartaction <action> §7- Set action when reaching 0 hearts"));
            source.sendMessage(Text.literal("§e/lifesteal setmobheartloss <enable|disable> §7- Enable or disable mob kill heart loss"));
            source.sendMessage(Text.literal("§e/lifesteal reset <player(s)> §7- Reset player hearts"));
            source.sendMessage(Text.literal("§e/lifesteal resetsettings §7- Reset all mod settings to default"));
        } else {
            source.sendMessage(Text.literal("§e/lifesteal withdraw <amount> §7- Withdraw an amount of hearts"));
        }

        return 1;
    }

    /**
     * runWithdraw() checks whether the heartWithdraw variable is set to true in the config,
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
            if (playerMaxHealth >= amount * 2) {

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
        }
        return 1;
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
     * runResetSettings() is a command accessible via the server, and resets all config options to default.
     */
    private static int runResetSettings(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ModConfig config = ModConfig.instance();

        int heartMin = config.defaultMinHeartCap;
        int healthMin = heartMin * 2;

        int heartMax = config.defaultMaxHeartCap;
        int healthMax = heartMax * 2;

        // reset values
        config.maxHeartCap = ModConfig.defaultMaxHeartCap;
        config.minHeartCap = ModConfig.defaultMinHeartCap;
        config.heartIncrease = ModConfig.defaultHeartIncrease;
        config.heartDecrease = ModConfig.defaultHeartDecrease;
        config.heartRegen = ModConfig.defaultHeartRegen;
        config.heartRegenAmount = ModConfig.defaultHeartRegenAmount;
        config.heartRegenTime = ModConfig.defaultHeartRegenTime;
        config.heartWithdraw = ModConfig.defaultHeartWithdraw;
        config.craftingRecipeEnabled = ModConfig.defaultCraftingRecipeEnabled;
        config.zeroHeartAction = ModConfig.defaultZeroHeartAction;
        config.mobKillHeartLoss = ModConfig.defaultMobKillHeartLoss;

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
                player.sendMessage(Text.literal("Your max hearts have been updated to " + heartMax), false);
            }
        }

        context.getSource().sendMessage(Text.literal("Reset all values to default"));

        return 1;
    }

    /**
     * runSetRegenAmount() is a command accessible via the server, and sets the amount of hearts that gained when
     * regenerating.
     */
    private static int runSetRegenAmount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ModConfig config = ModConfig.instance();

        config.heartRegenAmount = amount;
        config.save();

        context.getSource().sendMessage(Text.literal("Heart regen amount set to " + amount + " hearts."));

        return 1;
    }

    /**
     * runSetHeartCrafting() is a command accessible via the server, and sets whether you can craft a heart.
     */
    private static int runSetHeartCrafting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig config = ModConfig.instance();

        config.craftingRecipeEnabled = enabled;
        config.save();

        context.getSource().sendMessage(Text.literal("Heart crafting set to " + enabled));

        return 1;
    }

    /**
     * runSetZeroHeartAction() is a command accessible via the server, and sets what to do when someone hits 0 hearts.
     */
    private static int runSetZeroHeartAction(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String actionName = StringArgumentType.getString(context, "action"); // use your argument name

        try {
            ModConfig.ZeroHeartActions action = ModConfig.ZeroHeartActions.valueOf(actionName.toUpperCase());
            ModConfig.instance().zeroHeartAction = action; // set it in the config
            ModConfig.instance().save();
            context.getSource().sendMessage(Text.literal("Zero heart action set to " + action));
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Invalid action!"));
        }
        return 1;
    }

    /**
     * runSetMobHeartLoss() is a command accessible via the server, and sets whether a player loses a heat when a mob
     * kills them.
     */
    private static int runSetMobHeartLoss(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig config = ModConfig.instance();

        config.mobKillHeartLoss = enabled;
        config.save();

        context.getSource().sendMessage(Text.literal("Mob kill heart loss set to " + enabled));

        return 1;
    }

    /**
     * runSetPlayerHeart() is a command accessible via the server, and can set a players hearts to a certain value
     */
    private static int runSetPlayerHearts(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // get int arg for hearts
        int hearts = Math.min(IntegerArgumentType.getInteger(context, "amount"), ModConfig.instance().maxHeartCap);
        ServerCommandSource source = context.getSource();

        for (ServerPlayerEntity serverPlayerEntity : EntityArgumentType.getPlayers(context, "players")) {
            // set player max hearts * 2
            serverPlayerEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(hearts * 2);

            // set max health to hearts
            serverPlayerEntity.setHealth(serverPlayerEntity.getMaxHealth());

            // log
            serverPlayerEntity.sendMessage(Text.literal("You were given " + hearts + " hearts by an admin!"), false);
            source.sendMessage(Text.literal("You gave " + serverPlayerEntity.getName().getString() + " " + hearts + " hearts!"));
        }
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
     * Setup for setRegenCommand()
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
                .literal("setwithdraw")
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
                .literal("resetsettings")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runResetSettings)
                .build();
        return resetSettingsNode;
    }

    /**
     * Setup for setRegenAmountCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setRegenAmountCommand() {
        LiteralCommandNode<ServerCommandSource> setRegenNode = CommandManager
                .literal("setregenamount")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> setRegenAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetRegenAmount)
                .build();
        setRegenNode.addChild(setRegenAmountNode);
        return setRegenNode;
    }

    /**
     * Setup for setHeartCraftingCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setHeartCraftingCommand() {
        LiteralCommandNode<ServerCommandSource> setHeartCraftingNode = CommandManager
                .literal("setheartcrafting")
                .build();
        ArgumentCommandNode<ServerCommandSource, Boolean> setHeartCraftingBoolNode = CommandManager
                .argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetHeartCrafting)
                .build();
        setHeartCraftingNode.addChild(setHeartCraftingBoolNode);
        return setHeartCraftingNode;
    }

    /**
     * Setup for setZeroHeartActionCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setZeroHeartActionCommand() {
        LiteralCommandNode<ServerCommandSource> zeroHeartNode = CommandManager
                .literal("zeroheartaction")
                .build();
        ArgumentCommandNode<ServerCommandSource, String> zeroHeartActionNode = CommandManager
                .argument("action", StringArgumentType.word())
                .suggests((context, builder) -> {
                    // Suggest all enum names for tab completion
                    for (ModConfig.ZeroHeartActions action : ModConfig.ZeroHeartActions.values()) {
                        builder.suggest(action.name());
                    }
                    return builder.buildFuture();
                })
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetZeroHeartAction)
                .build();
        zeroHeartNode.addChild(zeroHeartActionNode);
        return zeroHeartNode;
    }

    /**
     * Setup for setMobHeartLossCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setMobHeartLossCommand() {
        LiteralCommandNode<ServerCommandSource> setMobHeartLossNode = CommandManager
                .literal("setmobheartloss")
                .build();
        ArgumentCommandNode<ServerCommandSource, Boolean> setMobHeartLossBoolNode = CommandManager
                .argument("enabled", BoolArgumentType.bool())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runSetMobHeartLoss)
                .build();
        setMobHeartLossNode.addChild(setMobHeartLossBoolNode);
        return setMobHeartLossNode;
    }

    /**
     * Setup for setPlayerHeartsCommand()
     */
    private static LiteralCommandNode<ServerCommandSource> setPlayerHeartsCommand() {
        LiteralCommandNode<ServerCommandSource> heartNode = CommandManager
                .literal("setplayerhearts")
                .requires(source -> source.hasPermissionLevel(2))
                .build();
        ArgumentCommandNode<ServerCommandSource, EntitySelector> heartPlayerNode = CommandManager
                .argument("players", EntityArgumentType.players())
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> heartAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(1))
                .executes(LifestealCommand::runSetPlayerHearts)
                .build();
        heartPlayerNode.addChild(heartAmountNode);
        heartNode.addChild(heartPlayerNode);
        return heartNode;
    }

    /**
     * registerCommands() registers all nodes to their main command identifier. This method is called in the onInitialize() method in LifestealMod.java.
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> lifestealNode = CommandManager.literal("lifesteal").build();

        dispatcher.getRoot().addChild(lifestealNode);

        // Basic commands
        lifestealNode.addChild(helpCommand());
        lifestealNode.addChild(withdrawCommand());
        lifestealNode.addChild(resetCommand());

        // Heart commands
        lifestealNode.addChild(setHeartMaxCommand());
        lifestealNode.addChild(setHeartMinCommand());
        lifestealNode.addChild(setHeartGainCommand());
        lifestealNode.addChild(setHeartLossCommand());

        // Regen commands
        lifestealNode.addChild(setRegenCommand());
        lifestealNode.addChild(setRegenTimeCommand());
        lifestealNode.addChild(setRegenAmountCommand());

        // Withdraw command
        lifestealNode.addChild(setWithdrawCommand());

        // Crafting / zero heart / mob loss commands / give player hearts
        lifestealNode.addChild(setHeartCraftingCommand());
        lifestealNode.addChild(setZeroHeartActionCommand());
        lifestealNode.addChild(setMobHeartLossCommand());
        lifestealNode.addChild(setPlayerHeartsCommand());

        // Reset settings
        lifestealNode.addChild(resetSettingsCommand());
    }
}