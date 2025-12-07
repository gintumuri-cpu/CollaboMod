package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.item.ICAD;
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
    private final List<String> components;

    public PacketEditCAD(List<String> components) {
        this.components = components;
    }

    public PacketEditCAD(FriendlyByteBuf buf) {
        this.components = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.components.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(components.size());
        for (String s : components) {
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
                    // スロット0 (CAD) を取得
                    ItemStack cadStack = handler.getStackInSlot(0);

                    if (!cadStack.isEmpty() && cadStack.getItem() instanceof ICAD) {

                        // リストをNBTに変換
                        ListTag nbtList = new ListTag();
                        for (String compName : components) {
                            // "EMPTY" などのダミーは保存しない
                            if (!compName.equals("NONE")) {
                                nbtList.add(StringTag.valueOf(compName));
                            }
                        }

                        // 書き込み
                        CompoundTag tag = cadStack.getOrCreateTag();
                        tag.put("Components", nbtList);

                        // 演出音（キーボードを叩いてエンターッ！という音）
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
