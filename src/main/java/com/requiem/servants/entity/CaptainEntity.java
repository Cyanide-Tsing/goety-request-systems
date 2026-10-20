package com.requiem.servants.entity;

import com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant.IllagerServantArmPose;
import com.Polarice3.Goety.common.entities.ally.illager.VindicatorServant;
import com.requiem.servants.ModItems;
import com.requiem.servants.ai.CaptainMarchGoal;
import com.requiem.servants.ai.CaptainRetreatGoal;
import com.requiem.servants.command.CompanyController;
import com.requiem.servants.command.CompanyStance;
import com.requiem.servants.command.FormationData;
import com.requiem.servants.command.FormationType;
import com.requiem.servants.item.AssaultOrderItem;
import com.requiem.servants.item.CompanyBinding;
import com.requiem.servants.item.CompanyStandardItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 连队长：能把周围的 goety 仆从编成一个连队，并下令列出各种阵型。
 *
 * <p>实体本身只是个壳：所有指挥逻辑都在 {@link CompanyController} 里，每刻在服务端跑一次。
 * 连队长不靠特殊的移动目标站位——它给自己也分配一个"指挥位"槽位，
 * 和普通成员一样由 {@link com.requiem.servants.ai.FormationSlotGoal} 带到位，
 * 所以指挥官与部队用的是同一套移动机制。
 *
 * <p>阵型与姿态额外同步到客户端，这样连旗的提示文本不需要自定义网络包就能显示当前状态。
 */
public class CaptainEntity extends VindicatorServant {
    private static final EntityDataAccessor<Integer> DATA_FORMATION =
            SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STANCE =
            SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MEMBERS =
            SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.INT);

    private final CompanyController company = new CompanyController(this);

    /** 自主作战时的指挥位，由 {@link CompanyController} 每阶段更新。不存档：重载后会重算。 */
    private Vec3 commandPost;

    public CaptainEntity(EntityType<? extends CaptainEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VindicatorServant.setCustomAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 5.0D)
                // 指挥官要能看到更远的威胁，才好提前调整姿态
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FORMATION, FormationType.LINE.ordinal());
        this.entityData.define(DATA_STANCE, CompanyStance.FORM_UP.ordinal());
        this.entityData.define(DATA_MEMBERS, 0);
    }

    /** 持旗姿势：双手分开，便于渲染手持物品。 */
    @Override
    public IllagerServantArmPose getArmPose() {
        return IllagerServantArmPose.NEUTRAL;
    }

    /** 连队长生成即持苍白军刀作为近战武器，覆盖卫道士默认的斧头。 */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALE_SABER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    /**
     * 连队长不主动索敌。
     *
     * <p>goety 的 {@code AbstractIllagerServant.targetSelectGoal()} 会加上"主动攻击村民与铁傀儡"
     * 之类的索敌目标，{@code VindicatorServant} 还会再加一个。对指挥官来说这些都是送死行为——
     * 一旦选中目标就会扑上去，而它应该待在阵后。这里整个覆盖成空实现。
     *
     * <p>注意这<b>不会</b>让它变成完全的靶子：被打时的反击来自
     * {@code Summoned.targetRetaliateGoal()}，那是另一个方法，仍然正常生效。
     */
    @Override
    public void targetSelectGoal() {
        // 故意留空：指挥官只反击，不主动挑事
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 优先级 0 / 1：必须压过 goety 继承来的那堆目标，否则指挥官会被杂事绊住。
        // AbstractIllagerServant 在优先级 2 加了"存食物进箱子"、优先级 3 加了
        // "分发多余食物/丢垃圾"，这些都占用移动控制权；行军目标如果排在它们之后，
        // 连队长就会站在原地整理背包，导致作战卡在逼近阶段。
        this.goalSelector.addGoal(0, new CaptainRetreatGoal(this));
        this.goalSelector.addGoal(1, new CaptainMarchGoal(this));
    }

    /**
     * 自主作战时连队长该站的位置；非作战期间为 {@code null}，
     * 此时它靠 goety 原本的跟随主人 AI 移动。
     */
    public Vec3 getCommandPost() {
        return this.commandPost;
    }

    public void setCommandPost(Vec3 commandPost) {
        this.commandPost = commandPost;
    }

    public CompanyController getCompany() {
        return this.company;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.company.tick();
            this.syncCompanyState();
        }
        ServantRotation.tick(this);
    }

    private void syncCompanyState() {
        int formation = this.company.getFormation().ordinal();
        if (this.entityData.get(DATA_FORMATION) != formation) {
            this.entityData.set(DATA_FORMATION, formation);
        }
        int stance = this.company.getStance().ordinal();
        if (this.entityData.get(DATA_STANCE) != stance) {
            this.entityData.set(DATA_STANCE, stance);
        }
        int members = this.company.getMemberCount();
        if (this.entityData.get(DATA_MEMBERS) != members) {
            this.entityData.set(DATA_MEMBERS, members);
        }
    }

    /** 当前阵型。客户端读同步数据，服务端读权威状态。 */
    public FormationType getFormation() {
        return this.level().isClientSide()
                ? FormationType.byOrdinal(this.entityData.get(DATA_FORMATION))
                : this.company.getFormation();
    }

    /** 当前姿态。 */
    public CompanyStance getStance() {
        return this.level().isClientSide()
                ? CompanyStance.byOrdinal(this.entityData.get(DATA_STANCE))
                : this.company.getStance();
    }

    /** 当前连队人数（不含连队长）。 */
    public int getMemberCount() {
        return this.level().isClientSide()
                ? this.entityData.get(DATA_MEMBERS)
                : this.company.getMemberCount();
    }

    /**
     * 连旗要在 goety 自己的交互逻辑（装备物品、开背包）之前截获，
     * 因为 {@code Player.interactOn} 会先调用实体的 mobInteract，再才轮到物品的 interactLivingEntity。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof CompanyStandardItem) {
            if (!this.level().isClientSide()) {
                CompanyStandardItem.bindTo(stack, this, player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.getItem() instanceof AssaultOrderItem) {
            if (!this.level().isClientSide()) {
                CompanyBinding.bind(stack, this);
                player.displayClientMessage(Component.translatable("requiem.order.bound",
                        this.company.getSquadCount(), this.company.getMemberCount()), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource cause) {
        // 指挥官阵亡，部队就地解散（槽位租约本身也会在几秒内过期，这里只是让它立刻生效）
        if (!this.level().isClientSide()) {
            this.company.disband();
        }
        super.die(cause);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag companyTag = new CompoundTag();
        this.company.save(companyTag);
        tag.put("Company", companyTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Company")) {
            this.company.load(tag.getCompound("Company"));
        }
        // 连队长自己不占阵型槽位。早期版本曾给它分配过"指挥位"，
        // 这里清掉可能残留的编制数据，避免它被自己的槽位拖着走。
        if (FormationData.isEnlisted(this)) {
            FormationData.discharge(this);
        }
    }
}
