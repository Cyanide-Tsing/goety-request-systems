package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.requiem.servants.ModItems;
import com.requiem.servants.item.CavalryStandardItem;
import com.requiem.servants.item.CompanyStandardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 骑兵队长仆从：骑兵编制的指挥军官，持苍白军刀。
 */
public class CavalryCaptainEntity extends VindicatorServant {
    public CavalryCaptainEntity(EntityType<? extends CavalryCaptainEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes().add(Attributes.MAX_HEALTH, 34.0D);
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALE_SABER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public boolean canBeLeader() {
        return true;
    }

    /** 骑兵军旗要在 goety 自己的交互逻辑之前截获。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof CavalryStandardItem) {
            if (!this.level().isClientSide()) {
                CavalryStandardItem.bindTo(stack, this, player);
                this.inheritSquad();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.getItem() instanceof CompanyStandardItem) {
            if (!this.level().isClientSide()) {
                CompanyStandardItem.toggleCavalrySupport(stack, this, player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        // 空手右键：goety 会下掉队长旗，这里拦截，骑兵队长必须一直扛旗才能当小队长
        if (stack.isEmpty()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    /**
     * 收编附近"编入过骑兵队、但队长已失联"的轻骑兵：把它们的指挥者重新指回自己。
     */
    private void inheritSquad() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = this.getBoundingBox().inflate(48.0D);
        for (LightCavalryEntity cavalry : level.getEntitiesOfClass(LightCavalryEntity.class, box,
                e -> !e.getCavalryCaptainUuid().isEmpty() && e.getLeader() == null)) {
            cavalry.setCommandPosEntity(this);
        }
    }

    /** 切换冲锋号令，返回切换后的状态（true=冲锋）。 */
    public boolean toggleChargeOrder() {
        boolean next = !this.getChargeOrder();
        this.getPersistentData().putBoolean("cavalryCharge", next);
        return next;
    }

    public boolean getChargeOrder() {
        return this.getPersistentData().getBoolean("cavalryCharge");
    }

    /** 连队下达冲锋/收拢号令（骑兵掩护）。 */
    public void setChargeOrder(boolean charge) {
        this.getPersistentData().putBoolean("cavalryCharge", charge);
    }

    /** 指定骑兵驻扎地点。 */
    public void setGarrison(BlockPos pos) {
        this.getPersistentData().putString("cavalryGarrison",
                "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]");
    }

    /** 读取骑兵驻扎地点；未设定返回 {@code null}。 */
    public BlockPos getGarrison() {
        String s = this.getPersistentData().getString("cavalryGarrison");
        if (s.isEmpty()) {
            return null;
        }
        try {
            String[] parts = s.substring(1, s.length() - 1).split(",");
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 解除骑兵驻扎。 */
    public void clearGarrison() {
        this.getPersistentData().remove("cavalryGarrison");
    }

    /** 是否正在驻扎。 */
    public boolean isGarrisoning() {
        return this.getPersistentData().contains("cavalryGarrison");
    }

    @Override
    public void targetSelectGoal() {
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
    }
}
