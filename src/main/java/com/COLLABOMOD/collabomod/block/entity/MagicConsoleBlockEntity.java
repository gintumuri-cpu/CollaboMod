package com.COLLABOMOD.collabomod.block.entity;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.register.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class MagicConsoleBlockEntity extends BlockEntity implements MenuProvider {

    // アイテムスロット: 1個（CADを置く用）
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public MagicConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.MAGIC_CONSOLE_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent("魔法式調整台");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagicConsoleMenu(containerId, playerInventory, this.itemHandler, this);
    }

    /**
     * GUIから呼び出されるコンパイル処理
     * スクリプト文字列を受け取り、解析結果をCADに書き込む
     */
    public void compileScript(String script) {
        ItemStack cadStack = itemHandler.getStackInSlot(0);
        if (cadStack.isEmpty()) return;

        // 1. スクリプトの解析
        List<String> lines = Arrays.asList(script.split("\n"));
        float[] attributes = AnalysisEngine.analyzeScript(lines);
        PhysicsMetadata physics = AnalysisEngine.derivePhysicsFromScript(lines, attributes);
        VisualMetadata visual = AnalysisEngine.deriveVisualsFromPhysics(physics, attributes, script.hashCode());

        // 2. コマンドリストの生成 (プリコンパイル)
        ListTag commandList = AnalysisEngine.generateCommandList(physics, attributes);

        // 3. NBTへの書き込み
        CompoundTag tag = cadStack.getOrCreateTag();
        tag.putString("Script", script);
        tag.put("PhysicsData", physics.toNBT());
        tag.put("VisualData", visual.toNBT());
        tag.put("CommandData", commandList); // 事前計算されたコマンドリストを保存

        setChanged();
    }

    // --- Capability (インベントリ機能の提供) ---
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    // --- データの保存/読み込み ---
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
    }
}