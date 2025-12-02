package com.COLLABOMOD.collabomod.command;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketSyncMagicStats;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class PsionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("psion")
                .requires(source -> source.hasPermission(2)) // 権限レベル2（OP）以上が必要

                // 1. get (確認)
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    return getPsion(context.getSource(), EntityArgument.getPlayer(context, "target"));
                                })
                        )
                )

                // 2. set (設定)
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            return setPsion(context.getSource(), EntityArgument.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "amount"));
                                        })
                                )
                        )
                )

                // 3. add (加算・減算)
                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            return addPsion(context.getSource(), EntityArgument.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "amount"));
                                        })
                                )
                        )
                )
        );
    }

    private static int getPsion(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            source.sendSuccess(new TextComponent(player.getName().getString() + "のサイオン量: " + stats.getCurrentPsion() + "/" + stats.getMaxPsion()), false);
        });
        return 1;
    }

    private static int setPsion(CommandSourceStack source, ServerPlayer player, int amount) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            stats.setCurrentPsion(amount);
            syncData(player); // 同期処理
            source.sendSuccess(new TextComponent(player.getName().getString() + "のサイオンを " + amount + " に設定しました"), true);
        });
        return 1;
    }

    private static int addPsion(CommandSourceStack source, ServerPlayer player, int amount) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int current = stats.getCurrentPsion();
            stats.setCurrentPsion(current + amount);
            syncData(player); // 同期処理
            source.sendSuccess(new TextComponent(player.getName().getString() + "のサイオンを " + amount + " 変化させました (現在: " + stats.getCurrentPsion() + ")"), true);
        });
        return 1;
    }

    // 変更をクライアントに即座に反映させるメソッド
    private static void syncData(ServerPlayer player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncMagicStats(
                            stats.getCurrentPsion(),
                            stats.getMaxPsion(),
                            stats.getCalculationArea(),
                            stats.getMentalLoad()
                    )
            );
        });
    }
}
