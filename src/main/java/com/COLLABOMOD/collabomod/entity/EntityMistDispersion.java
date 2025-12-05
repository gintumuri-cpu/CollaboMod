package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.register.EntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public class EntityMistDispersion extends ThrowableProjectile{
    public EntityMistDispersion(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public EntityMistDispersion(Level level, LivingEntity shooter) {
        super(EntityRegister.MIST_DISPERSION.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 40) this.discard(); // 射程は短めでもOK（必殺技なので）
    }

    // 重力なし（直線軌道）
    @Override
    protected float getGravity() { return 0.0F; }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            if (result.getType() != HitResult.Type.ENTITY) {
                this.discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level.isClientSide) {
            BlockPos pos = result.getBlockPos();
            decomposeBlockRecursively(pos);
        }
    }

    // 連結ブロックもまとめて消すメソッド
    private void decomposeBlockRecursively(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        if (state.isAir()) return;

        // 岩盤などは除外
        if (state.getDestroySpeed(this.level, pos) < 0) return;

        // ■ 1. 連結パーツの検索と削除
        // ドア (DoorBlock)
        if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            if (this.level.getBlockState(otherPos).getBlock() == state.getBlock()) {
                destroyBlockNoDrop(otherPos); // 相方を先に消す
            }
        }
        // ベッド (BedBlock)
        else if (state.getBlock() instanceof BedBlock) {
            BedPart part = state.getValue(BedBlock.PART);
            Direction facing = state.getValue(BedBlock.FACING);
            BlockPos otherPos = (part == BedPart.FOOT) ? pos.relative(facing) : pos.relative(facing.getOpposite());
            if (this.level.getBlockState(otherPos).getBlock() == state.getBlock()) {
                destroyBlockNoDrop(otherPos);
            }
        }
        // 背の高い草・花 (DoublePlantBlock)
        else if (state.getBlock() instanceof DoublePlantBlock) {
            DoubleBlockHalf half = state.getValue(DoublePlantBlock.HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            if (this.level.getBlockState(otherPos).getBlock() == state.getBlock()) {
                destroyBlockNoDrop(otherPos);
            }
        }

        // ■ 2. 本体の削除と演出
        destroyBlockNoDrop(pos);
    }

    // ドロップなしで消去＆エフェクト発生
    private void destroyBlockNoDrop(BlockPos pos) {
        // 空気置換（フラグ2: 隣接更新なし、フラグ16: ドロップなし）
        // 念のため removeBlock(false) を使いますが、さらに setBlock で確実に消します
        this.level.removeBlock(pos, false);

        // 演出：粉になって崩れ落ちる
        spawnDenseDustEffect(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0);

        // 音
        this.level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0F, 2.0F);
    }

    // ★ここが「3連分解」の再現ロジック
    @Override
    protected void onHitEntity(EntityHitResult result) {
        // super.onHitEntityは呼ばない（貫通させないため）
        Entity target = result.getEntity();

        if (target instanceof LivingEntity livingTarget) {
            // 領域干渉・情報強化の分解
            livingTarget.removeAllEffects();
            if (livingTarget instanceof Player targetPlayer) {
                if (targetPlayer.isUsingItem() && targetPlayer.getUseItem().is(Items.SHIELD)) {
                    targetPlayer.getCooldowns().addCooldown(Items.SHIELD, 100);
                    targetPlayer.stopUsingItem();
                }
            }

            // 肉体分解（即死）
            livingTarget.hurt(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);

            // 演出
            spawnDenseDustEffect(livingTarget.getX(), livingTarget.getY() + 1.0, livingTarget.getZ(), livingTarget.getBbWidth());

            // 音：シュゥゥ...という消滅音
            this.level.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, target.getSoundSource(), 1.0F, 1.0F);
        }

        // 当たったら弾は消える
        if (!this.level.isClientSide) {
            this.discard();
        }
    }

    // ■■■ 演出強化版：大量の粉塵エフェクト ■■■
    private void spawnDenseDustEffect(double x, double y, double z, double widthScale) {
        if (this.level instanceof ServerLevel serverLevel) {

            // 1. 白い粉 (White Concrete Powder) - メインの残骸
            // 量を 30 -> 100 に増量
            BlockParticleOption dustParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_CONCRETE_POWDER.defaultBlockState());
            serverLevel.sendParticles(dustParticle, x, y, z,
                    500, widthScale * 0.5, 0.4, widthScale * 0.5, 0.0); // 範囲を少し縦長に

            // 2. 粉雪 (Snowball) - 細かい粒子感
            // アイテムパーティクルを使うと「砕け散る」感じが出ます
            ItemParticleOption snowParticle = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SNOWBALL));
            serverLevel.sendParticles(snowParticle, x, y, z,
                    500, widthScale * 0.2, 0.3, widthScale * 0.4, 0.05);

            // 3. 霧 (Cloud) - 分解された気体
            // その場に少し漂わせる
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z,
                    10, widthScale * 0.5, 0.5, widthScale * 0.5, 0.02);
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
