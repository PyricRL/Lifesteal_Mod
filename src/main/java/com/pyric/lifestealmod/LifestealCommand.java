package com.pyric.lifestealmod;

import com.mojang.brigadier.CommandDispatcher;
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
    private static int runWithdrawAmountCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (!ModConfig.instance().heartWithdraw) {
            player.sendMessage(Text.literal("This command is disabled within the mod configuration"), false);
            return 0;
        }

        else {
            int amount = IntegerArgumentType.getInteger(context, "amount");

            double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);

            if (playerMaxHealth - amount * 2 < ModConfig.instance().minHeartCap * 2) {
                player.sendMessage(Text.literal("Cannot withdraw hearts under " + ModConfig.instance().minHeartCap + "!"), false);
                return 0;
            }

            if (playerMaxHealth * 2.0 >= amount * 2) {
                LifestealMod.decreasePlayerHealth(player, amount * 2);

                ItemStack heartStack = new ItemStack(ModItems.HEART, amount);
                player.giveItemStack(heartStack);

                player.sendMessage(Text.literal("Heart withdrawn successfully!"), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Heart withdraw failure!"), false);
            }
            return 0;
        }
    }

    private static int runResetPlayerCommandServer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        for (ServerPlayerEntity serverPlayerEntity : EntityArgumentType.getPlayers(context, "players")) {
            serverPlayerEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(20.0);
            serverPlayerEntity.setHealth(20.0f);
            serverPlayerEntity.sendMessage(Text.literal("Player reset successfully"), false);
            return 1;
        }
        return 0;
    }

    private static int runResetPlayerCommandPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity serverPlayerEntity = context.getSource().getPlayerOrThrow();
        serverPlayerEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(20.0);
        serverPlayerEntity.setHealth(20.0f);
        serverPlayerEntity.sendMessage(Text.literal("Player reset successfully"), false);
        return 1;
    }

    private static LiteralCommandNode<ServerCommandSource> withdrawCommand() {
        LiteralCommandNode<ServerCommandSource> withdrawNode = CommandManager
                .literal("withdraw")
                .build();
        ArgumentCommandNode<ServerCommandSource, Integer> withdrawAmountNode = CommandManager
                .argument("amount", IntegerArgumentType.integer(0))
                .executes(LifestealCommand::runWithdrawAmountCommand)
                .build();
        withdrawNode.addChild(withdrawAmountNode);
        return withdrawNode;
    }

    private static LiteralCommandNode<ServerCommandSource> resetCommand() {
        LiteralCommandNode<ServerCommandSource> resetNode = CommandManager
                .literal("reset")
                .executes(LifestealCommand::runResetPlayerCommandPlayer)
                .build();
        ArgumentCommandNode<ServerCommandSource, EntitySelector> resetPlayerNode = CommandManager
                .argument("players", EntityArgumentType.players())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(LifestealCommand::runResetPlayerCommandServer)
                .build();
        resetNode.addChild(resetPlayerNode);
        return resetNode;
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> lifestealNode = CommandManager.literal("lifesteal").build();

        dispatcher.getRoot().addChild(lifestealNode);
        lifestealNode.addChild(withdrawCommand());
        lifestealNode.addChild(resetCommand());
    }
}