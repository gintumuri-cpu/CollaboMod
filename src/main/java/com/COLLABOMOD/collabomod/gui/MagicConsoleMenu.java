package com.COLLABOMOD.collabomod.gui;

import com.COLLABOMOD.collabomod.block.MagicConsoleBlock;
import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.item.*;
import com.COLLABOMOD.collabomod.item.ICAD;
import com.COLLABOMOD.collabomod.item.ItemCAD;
import com.COLLABOMOD.collabomod.item.ItemSilverHorn;
import com.COLLABOMOD.collabomod.item.ItemEmptyCAD;
import com.COLLABOMOD.collabomod.item.ItemSpellComponent;
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

    // クライアント側コンストラクタ
    public MagicConsoleMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    // サーバー側コンストラクタ
    public MagicConsoleMenu(int id, Inventory inv, BlockEntity entity) {
        super(MenuTypeRegister.MAGIC_CONSOLE_MENU.get(), id);
        this.blockEntity = (MagicConsoleBlockEntity) entity;
        this.access = ContainerLevelAccess.create(inv.player.level, entity.getBlockPos());

        this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
            // ■ Slot 0: CAD配置スロット (左側)
            this.addSlot(new SlotItemHandler(handler, 0, 26, 35) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof ICAD;
                }
            });

            // ■ Slot 1-3: コンポーネントスロット (中央～右)
            for (int i = 0; i < 3; i++) {
                this.addSlot(new SlotItemHandler(handler, 1 + i, 80 + (i * 18), 35) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return stack.getItem() instanceof ItemSpellComponent;
                    }
                });
            }
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // プレイヤーインベントリの配置 (Y座標などはバニラ標準)
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BlockRegister.MAGIC_CONSOLE.get());
    }

    // Shiftクリック時の挙動（簡易実装）
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 一旦無効化（実装すると長くなるため）
    }
}
