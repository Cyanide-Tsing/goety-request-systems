package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.Polarice3.Goety.common.entities.ally.illager.raider.RaiderServant;
import com.requiem.servants.ModItems;
import com.requiem.servants.item.CompanyStandardItem;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 鼓手仆从：持小军鼓，跟随连队长，为整个连队敲出节奏与增益。
 *
 * <p>小军鼓是持续敲击的背景音——只要鼓手活着就按固定节拍一直敲。
 * 被连旗绑定后，鼓手会保持在连队长身边，增益由
 * {@link com.requiem.servants.command.CompanyController} 根据连队姿态发放。
 */
public class DrummerEntity extends VindicatorServant {
    /** 小军鼓敲击间隔（刻），约 120 BPM 的行军节奏。 */
    private static final int DRUM_INTERVAL = 10;
    /** 鼓手跟随连队长的检查间隔（刻）。 */
    private static final int FOLLOW_INTERVAL = 20;

    private int drumTick;
    private int followTick;

    public DrummerEntity(EntityType<? extends DrummerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes();
    }

    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SNARE_DRUM.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public void targetSelectGoal() {
    }

    /** 连旗要在 goety 自己的交互逻辑之前截获，用于绑定/解绑鼓手槽。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof CompanyStandardItem) {
            if (!this.level().isClientSide()) {
                CompanyStandardItem.toggleDrummer(stack, this, player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        ServantRotation.tick(this);
        if (!this.level().isClientSide()) {
            this.drumTick++;
            if (this.drumTick % DRUM_INTERVAL == 0) {
                this.playSnare();
            }
            this.followTick++;
            if (this.followTick % FOLLOW_INTERVAL == 0) {
                this.followCaptain();
            }
        }
    }

    /** 绑定到的连队长 UUID（持久化，跨存档保持跟随关系）。 */
    public void setCaptainUuid(UUID uuid) {
        if (uuid == null) {
            this.getPersistentData().remove("drummerCaptainUuid");
        } else {
            this.getPersistentData().putUUID("drummerCaptainUuid", uuid);
        }
    }

    public UUID getCaptainUuid() {
        return this.getPersistentData().hasUUID("drummerCaptainUuid")
                ? this.getPersistentData().getUUID("drummerCaptainUuid")
                : null;
    }

    private void followCaptain() {
        UUID captainId = this.getCaptainUuid();
        if (captainId == null || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        // 把连队长设成 goety 的“小队队长”，让 FollowOwnerGoal 直接跟它，
        // 而不是在“跟随连队长”和“跟随主人”之间来回切。
        if (server.getEntity(captainId) instanceof RaiderServant captain && captain.isAlive()) {
            if (this.getLeader() != captain) {
                this.setLeader(captain);
            }
        } else {
            this.setLeader(null);
        }
    }

    private void playSnare() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.NEUTRAL, 0.7F, 1.0F);
    }
}
