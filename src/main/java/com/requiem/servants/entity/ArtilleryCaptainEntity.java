package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.requiem.servants.item.ArtilleryHornItem;
import com.requiem.servants.item.CompanyStandardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 炮兵队长：指挥一支炮兵小队的军官。
 *
 * <p>本实体是"炮兵小队"的锚点——具体的小队管理、炮位分配、开火号令由 KubeJS 脚本
 * （{@code goety_storyline/60_artillery_captain.js}）驱动；这里只负责注册实体、属性与渲染。
 */
public class ArtilleryCaptainEntity extends VindicatorServant {
    public ArtilleryCaptainEntity(EntityType<? extends ArtilleryCaptainEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    /** 炮兵队长默认为小队队长（goety 袭击队长机制）。 */
    @Override
    public boolean canBeLeader() {
        return true;
    }

    /** 号角要在 goety 自己的交互逻辑之前截获。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof ArtilleryHornItem) {
            if (!this.level().isClientSide()) {
                ArtilleryHornItem.bindTo(stack, this, player);
                this.inheritSquad();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.getItem() instanceof CompanyStandardItem) {
            if (!this.level().isClientSide()) {
                CompanyStandardItem.toggleSupportCaptain(stack, this, player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        // 空手右键：goety 会下掉队长旗，这里拦截，炮兵队长必须一直扛旗才能当小队长
        if (stack.isEmpty()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    /**
     * 继承旧小队：把附近"编入过炮兵小队、但队长已阵亡/失联"的炮兵重新指派给自己。
     *
     * <p>炮兵通过 goety 的 {@code setCommandPosEntity} 跟随队长；队长阵亡后
     * {@code getCommandPosEntity()} 返回 null，但炮兵身上持久的 {@code artilleryCaptainUuid}
     * 仍在。新队长被号角绑定时，扫一遍周围这些孤儿炮兵并重新收编。
     */
    private void inheritSquad() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = this.getBoundingBox().inflate(48.0D);
        for (ArtilleryEntity artillery : level.getEntitiesOfClass(ArtilleryEntity.class, box,
                e -> !e.getArtilleryCaptainUuid().isEmpty() && e.getCommandPosEntity() == null)) {
            artillery.setCommandPosEntity(this);
        }
    }

    /** 玩家吹号时置位的开火命令标记（布尔，供 KubeJS 炮兵 AI 读取）。 */
    public void setHornFireTick(long tick) {
        this.getPersistentData().putBoolean("HornFire", true);
    }

    public boolean consumeHornFire() {
        if (!this.getPersistentData().getBoolean("HornFire")) {
            return false;
        }
        this.getPersistentData().putBoolean("HornFire", false);
        return true;
    }

    /** 设定炮兵阵地（号角右键地面写入）。 */
    public void setBattery(BlockPos pos) {
        this.getPersistentData().putString("Battery",
                "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]");
    }

    /** 读取炮兵阵地；未设定时返回 {@code null}。 */
    public BlockPos getBattery() {
        String s = this.getPersistentData().getString("Battery");
        if (s.isEmpty()) {
            return null;
        }
        try {
            String[] parts = s.substring(1, s.length() - 1).split(",");
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // 炮兵队长不主动索敌：它是军官，开火由号角/标记地点控制
    @Override
    public void targetSelectGoal() {
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
