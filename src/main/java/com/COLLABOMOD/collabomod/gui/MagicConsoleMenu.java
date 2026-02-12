package com.COLLABOMOD.collabomod.gui;

import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.item.ICAD;
import com.COLLABOMOD.collabomod.register.BlockRegister;
import com.COLLABOMOD.collabomod.register.MenuTypeRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MagicConsoleMenu extends AbstractContainerMenu {

    public final MagicConsoleBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    // ■ 修正: クライアント側コンストラクタ
    public MagicConsoleMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(MenuTypeRegister.MAGIC_CONSOLE_MENU.get(), id);

        BlockPos pos = extraData.readBlockPos();
        this.blockEntity = (MagicConsoleBlockEntity) inv.player.level.getBlockEntity(pos);
        this.access = ContainerLevelAccess.create(inv.player.level, pos);

        // クライアント側でもTileEntityから正しいHandlerを取得する
        // 取得できない場合のみダミーを使う（通常は同期されているはず）
        IItemHandler handler = null;
        if (this.blockEntity != null) {
            handler = this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(null);
        }
        // 万が一取得失敗した場合は、操作できないように空のダミーではなくnull扱いでエラー回避してもいいが、
        // ここでは安全策としてTileEntityが無ければスロットを追加しない等の分岐も可能。
        // 一旦取得できた前提で進める。

        if (handler != null) {
            addConsoleSlots(handler);
        }

        layoutPlayerInventory(inv, 48, 174);
    }

    // ■ 修正: サーバー側コンストラクタ
    public MagicConsoleMenu(int id, Inventory inv, IItemHandler handler, BlockEntity entity) {
        super(MenuTypeRegister.MAGIC_CONSOLE_MENU.get(), id);
        this.blockEntity = (MagicConsoleBlockEntity) entity;
        this.access = ContainerLevelAccess.create(inv.player.level, entity.getBlockPos());

        addConsoleSlots(handler);
        layoutPlayerInventory(inv, 48, 174);
    }

    // 共通のスロット追加ロジック
    private void addConsoleSlots(IItemHandler handler) {
        this.addSlot(new SlotItemHandler(handler, 0, 42, 115) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                // ICADのみ許可
                //return stack.getItem() instanceof ICAD;
                return true;
            }
        });
    }

    private void layoutPlayerInventory(Inventory playerInventory, int leftCol, int topRow) {
        // メインインベントリ
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, leftCol + l * 18, topRow + i * 18));
            }
        }
        // ホットバー
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, leftCol + i * 18, topRow + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BlockRegister.MAGIC_CONSOLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 0) { // CAD -> Player
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player -> CAD
                // SlotItemHandler(index 0) に移動を試みる
                if (itemstack1.getItem() instanceof ICAD) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 28) {
                    if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 37 && !this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}