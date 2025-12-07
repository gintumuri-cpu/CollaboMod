package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.magic.MagicComponentType;
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

import java.util.function.Supplier;

public class PacketInstallSpell {

    public PacketInstallSpell() {}
    public PacketInstallSpell(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 開いているメニューが調整台かチェック
            if (player.containerMenu instanceof MagicConsoleMenu menu) {
                MagicConsoleBlockEntity be = menu.blockEntity;

                be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    ItemStack cadStack = handler.getStackInSlot(0); // CAD

                    // CADがなければ終了
                    if (cadStack.isEmpty()) return;

                    // コンポーネントリストを作成
                    ListTag compList = new ListTag();

                    // スロット1〜3を走査
//                    for (int i = 1; i <= 5; i++) {
//                        ItemStack compStack = handler.getStackInSlot(i);
//                        if (!compStack.isEmpty() && compStack.getItem() instanceof ItemSpellComponent compItem) {
//                            MagicComponentType type = compItem.getComponentType();
//                            compList.add(StringTag.valueOf(type.name()));
//                        }
//                    }

                    // コンポーネントが1つ以上あれば書き込む
                    if (!compList.isEmpty()) {
                        CompoundTag tag = cadStack.getOrCreateTag();
                        tag.put("Components", compList);

                        // 完了音
                        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.5F);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}