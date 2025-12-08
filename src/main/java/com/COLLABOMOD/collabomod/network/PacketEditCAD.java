package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.item.ICAD;
import com.COLLABOMOD.collabomod.magic.MagicComponentType;
import com.COLLABOMOD.collabomod.magic.SpellResolver;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketEditCAD {
    private final List<String> scriptLines;

    public PacketEditCAD(List<String> scriptLines) {
        this.scriptLines = scriptLines;
    }

    public PacketEditCAD(FriendlyByteBuf buf) {
        this.scriptLines = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.scriptLines.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(scriptLines.size());
        for (String s : scriptLines) {
            buf.writeUtf(s);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.containerMenu instanceof MagicConsoleMenu menu) {
                MagicConsoleBlockEntity be = menu.blockEntity;
                be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    ItemStack cadStack = handler.getStackInSlot(0);

                    if (!cadStack.isEmpty() && cadStack.getItem() instanceof ICAD) {

                        // ■ 文字列リストをそのままNBT "ScriptCode" に保存
                        ListTag nbtList = new ListTag();
                        for (String line : scriptLines) {
                            if (!line.trim().isEmpty()) {
                                nbtList.add(StringTag.valueOf(line));
                            }
                        }

                        CompoundTag tag = cadStack.getOrCreateTag();
                        tag.put("ScriptCode", nbtList);

                        // ■ 重要: 変更を確定させる
                        cadStack.setTag(tag); // 明示的にセット
                        be.setChanged(); // ブロックエンティティに変更を通知

                        // ■ 重要: サーバーからクライアントへスロットの更新を送信
                        // これをやらないと、プレイヤーがGUIを開いたままでは変更が見えません
                        player.containerMenu.broadcastChanges();

                        // 効果音
                        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 2.0F);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
