package com.COLLABOMOD.collabomod.gui;

import com.COLLABOMOD.collabomod.block.MagicConsoleBlock;
import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.item.*;
import com.COLLABOMOD.collabomod.item.ICAD;
import com.COLLABOMOD.collabomod.item.ItemCAD;
import com.COLLABOMOD.collabomod.item.ItemSilverHorn;
import com.COLLABOMOD.collabomod.item.ItemEmptyCAD;
import com.COLLABOMOD.collabomod.item.ItemThirdEye;
import com.COLLABOMOD.collabomod.register.BlockRegister;
import com.COLLABOMOD.collabomod.register.MenuTypeRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MagicConsoleMenu extends AbstractContainerMenu {

    public final MagicConsoleBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public MagicConsoleMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    public MagicConsoleMenu(int id, Inventory inv, BlockEntity entity) {
        super(MenuTypeRegister.MAGIC_CONSOLE_MENU.get(), id);
        this.blockEntity = (MagicConsoleBlockEntity) entity;
        this.access = ContainerLevelAccess.create(inv.player.level, entity.getBlockPos());

        this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {

            // ■ 修正: CADスロットを右側（モニターの下、INSTALLボタンの横）に移動
            // x=190, y=113 (ボタンの左隣あたり)
            this.addSlot(new SlotItemHandler(handler, 0, 190, 113) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof ICAD;
                }
            });
        });

        layoutPlayerInventory(inv, 48, 140);
    }

    // ... (layoutPlayerInventory, stillValid, quickMoveStack は変更なし) ...
    private void layoutPlayerInventory(Inventory playerInventory, int leftCol, int topRow) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, leftCol + l * 18, topRow + i * 18));
            }
        }
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
        return ItemStack.EMPTY;
    }
}
