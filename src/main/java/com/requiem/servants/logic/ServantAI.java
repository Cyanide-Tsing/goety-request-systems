package com.requiem.servants.logic;

import com.Polarice3.Goety.api.blocks.entities.IBarrack;
import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.common.entities.ally.illager.Neollager;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.SEHelper;
import com.mojang.logging.LogUtils;
import com.requiem.servants.ModItems;
import com.requiem.servants.block.DemandWorkBlockEntity;
import com.requiem.servants.block.HorseFarmRegisterBlockEntity;
import com.requiem.servants.block.WorkingBlockEntity;
import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.FletcherEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.GrenadierEntity;
import com.requiem.servants.entity.GroomEntity;
import com.requiem.servants.entity.LightCavalryEntity;
import com.requiem.servants.entity.RecruiterEntity;
import com.requiem.servants.entity.WorkServantEntity;
import com.requiem.servants.item.DemolitionScrollItem;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * 仆从 AI 分发与各兵种 AI（从 KubeJS {@code 10~95_*.js} 移植）。
 */
public final class ServantAI {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] MAGIC_TORCH_TYPES =
            {"warlock", "evoker", "heretic", "iceologer", "geomancer", "wind_caller"};
    private static final String[] MELEE_TORCH_TYPES =
            {"pillager", "vindicator", "artillery", "grenadier", "conqueror", "boticario",
                    "apprentice", "gardener", "groom", "fletcher", "recruiter"};
    private static final String[] FLETCH_OUTPUTS =
            {"minecraft:arrow", "minecraft:spectral_arrow", "minecraft:bow", "minecraft:crossbow"};
    private static final String[] GARDENER_OUTPUTS =
            {"minecraft:sugar", "minecraft:paper"};
    private static final String[] APPRENTICE_OUTPUTS =
            {"minecraft:iron_block", "minecraft:gold_block", "minecraft:diamond_block",
                    "minecraft:emerald_block", "minecraft:lapis_block", "minecraft:copper_block",
                    "minecraft:coal_block", "minecraft:redstone_block", "minecraft:netherite_block"};

    private ServantAI() {
    }

    /** 每 20 刻由 {@link ServantTickHandler} 调用一次。 */
    public static void serverTick(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            try {
                if (entity instanceof RecruiterEntity recruiter) {
                    recruiterAI(recruiter);
                } else if (entity instanceof GroomEntity groom) {
                    groomAI(groom);
                } else if (entity instanceof GrenadierEntity grenadier) {
                    grenadierAI(grenadier);
                } else if (entity instanceof LightCavalryEntity cavalry) {
                    lightCavalryAI(cavalry);
                } else if (entity instanceof ApprenticeEntity apprentice) {
                    apprenticeAI(apprentice);
                } else if (entity instanceof FletcherEntity fletcher) {
                    fletcherAI(fletcher);
                } else if (entity instanceof GardenerEntity gardener) {
                    gardenerAI(gardener);
                } else if (entity instanceof ArtilleryEntity artillery) {
                    artilleryAI(artillery);
                } else if (entity instanceof ArtilleryCaptainEntity captain) {
                    artilleryCaptainAI(captain);
                } else if (isPreacher(entity)) {
                    preacherAI(entity);
                } else if (isPrisoner(entity)) {
                    prisonerAI(entity);
                }
            } catch (RuntimeException e) {
                // 与 KubeJS 一致：单个实体出错不影响其它实体
            }
        }
    }

    // ============================================================
    //  征兵官（30_recruiter.js）
    // ============================================================

    private static void recruiterAI(RecruiterEntity entity) {
        if (!entity.isGuardingArea()) {
            ServantLogic.setTask(entity, "跟随主人");
            return;
        }
        ServerLevel level = (ServerLevel) entity.level();
        List<BlockPos> torches = ServantLogic.readBlockPosList(entity, "Torches");
        if (torches.isEmpty()) {
            idleWander(level, entity, entity.blockPosition());
            ServantLogic.setTask(entity, "空闲（未设置不祥火炬）");
            return;
        }
        Neollager illager = findUntrainedIllager(level, entity);
        if (illager == null) {
            idleWander(level, entity, torches.get(0));
            ServantLogic.setTask(entity, "空闲（附近没有未训练新刌民）");
            return;
        }
        boolean magic = illager.isMagic();
        Torch torch = findTorch(level, torches, magic);
        if (torch == null) {
            idleWander(level, entity, torches.get(0));
            ServantLogic.setTask(entity, "空闲（无可用" + (magic ? "魔法" : "近战") + "火炬）");
            return;
        }
        BlockPos pos = torch.pos;
        double dx = entity.getX() - (pos.getX() + 0.5);
        double dy = entity.getY() - pos.getY();
        double dz = entity.getZ() - (pos.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 6.25) {
            entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
            ServantLogic.setTask(entity, "前往 " + torch.type + " 火炬分配");
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IBarrack barrack) {
            barrack.addTrainable(illager, level, pos);
            illager.setTrainPos(pos);
        }
        markAssigned(entity, illager);
        ServantLogic.tellOwner(entity, "已将 " + (magic ? "魔法" : "近战") + "新刌民分配到 " + torch.type + " 火炬");
        ServantLogic.setTask(entity, "分配 " + torch.type);
    }

    private static Neollager findUntrainedIllager(ServerLevel level, RecruiterEntity recruiter) {
        List<Neollager> list = level.getEntitiesOfClass(Neollager.class,
                new AABB(recruiter.getX() - 16, recruiter.getY() - 16, recruiter.getZ() - 16,
                        recruiter.getX() + 16, recruiter.getY() + 16, recruiter.getZ() + 16),
                e -> !isAssigned(recruiter, e));
        for (Neollager e : list) {
            if (e.isTraining()) {
                continue;
            }
            return e;
        }
        return null;
    }

    private static boolean isAssigned(RecruiterEntity recruiter, Neollager illager) {
        ListTag list = recruiter.getPersistentData().getList("Assigned", Tag.TAG_STRING);
        String uuid = illager.getStringUUID();
        for (int i = 0; i < list.size(); i++) {
            if (uuid.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private static void markAssigned(RecruiterEntity recruiter, Neollager illager) {
        ListTag list = recruiter.getPersistentData().getList("Assigned", Tag.TAG_STRING);
        String uuid = illager.getStringUUID();
        for (int i = 0; i < list.size(); i++) {
            if (uuid.equals(list.getString(i))) {
                return;
            }
        }
        list.add(StringTag.valueOf(uuid));
        if (list.size() > 100) {
            while (list.size() > 100) {
                list.remove(0);
            }
        }
        recruiter.getPersistentData().put("Assigned", list);
    }

    private static Torch findTorch(ServerLevel level, List<BlockPos> torches, boolean magic) {
        String[] types = magic ? MAGIC_TORCH_TYPES : MELEE_TORCH_TYPES;
        for (String type : types) {
            for (BlockPos p : torches) {
                if (!torchUsable(level, p)) {
                    continue;
                }
                String t = torchTypeOf(level, p);
                if (!t.isEmpty() && t.contains(type)) {
                    return new Torch(p, type);
                }
            }
        }
        return null;
    }

    private static String torchTypeOf(ServerLevel level, BlockPos p) {
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof IBarrack barrack) {
            String s = barrack.getCurrentMob();
            return s == null ? "" : s.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static boolean torchUsable(ServerLevel level, BlockPos p) {
        BlockEntity be = level.getBlockEntity(p);
        if (!(be instanceof IBarrack barrack)) {
            return false;
        }
        if (torchTypeOf(level, p).isEmpty()) {
            return false;
        }
        int limit = barrack.trainLimit();
        return limit <= 0 || barrack.getCurrentAmount() < limit;
    }

    // ============================================================
    //  马倌（80_groom.js）
    // ============================================================

    private static void groomAI(GroomEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        BlockPos station = ServantLogic.readBlockPos(entity, "Station");
        if (station == null) {
            station = findHorseFarmRegister(level, entity);
            if (station == null) {
                ServantLogic.setTask(entity, "未找到马场登记站");
                return;
            }
            ServantLogic.writeBlockPos(entity, "Station", station);
        }
        BlockEntity be = level.getBlockEntity(station);
        if (!(be instanceof HorseFarmRegisterBlockEntity register) || !register.isBreeding()) {
            idleWander(level, entity, station);
            ServantLogic.setTask(entity, "繁殖关闭");
            return;
        }

        // 给新生马加成 + 驯化附近无主马
        for (Horse horse : findHorses(level, entity, 8)) {
            if (horse.isBaby() && !isBoosted(horse)) {
                boostNewborn(horse);
                markBoosted(horse);
            }
        }
        tameNearbyHorses(entity);

        // 饲料
        boolean hasFood = entity.hasItem("minecraft:wheat", 1) || entity.hasItem("minecraft:carrot", 1);
        if (!hasFood) {
            ServantLogic.setTask(entity, "需求饲料");
            return;
        }

        // 喂小马
        List<Horse> foals = findHorses(level, entity, 8).stream().filter(Horse::isBaby).toList();
        if (!foals.isEmpty()) {
            Item food = consumeFood(entity);
            if (food != null) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(food));
                feedBaby(foals.get(0));
            }
            ServantLogic.setTask(entity, "喂小马");
            return;
        }

        // 繁殖
        List<Horse> adults = findHorses(level, entity, 10).stream()
                .filter(h -> !h.isBaby() && !h.isInLove()).toList();
        if (adults.size() < 2) {
            idleWander(level, entity, station);
            ServantLogic.setTask(entity, "等待马匹");
            return;
        }
        adults.get(0).setInLove(null);
        adults.get(1).setInLove(null);
        Item breedFood = consumeFood(entity);
        entity.setItemSlot(EquipmentSlot.MAINHAND,
                new ItemStack(breedFood != null ? breedFood : Items.WHEAT));
        ServantLogic.setTask(entity, "繁殖马匹");
    }

    private static BlockPos findHorseFarmRegister(ServerLevel level, GroomEntity groom) {
        String uid = groom.getStringUUID();
        BlockPos center = groom.blockPosition();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -32; dz <= 32; dz++) {
                for (int dx = -32; dx <= 32; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof HorseFarmRegisterBlockEntity register) {
                        var sid = register.getAssignedServantId();
                        if (sid != null && sid.toString().equals(uid)) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<Horse> findHorses(ServerLevel level, GroomEntity groom, int radius) {
        return level.getEntitiesOfClass(Horse.class,
                new AABB(groom.getX() - radius, groom.getY() - radius, groom.getZ() - radius,
                        groom.getX() + radius, groom.getY() + radius, groom.getZ() + radius),
                Horse::isAlive);
    }

    private static void boostNewborn(Horse horse) {
        AttributeInstance maxHealth = horse.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = horse.getAttribute(Attributes.JUMP_STRENGTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.min(30.0, maxHealth.getBaseValue() * 1.10));
        }
        if (speed != null) {
            speed.setBaseValue(Math.min(0.3375, speed.getBaseValue() * 1.10));
        }
        if (jump != null) {
            jump.setBaseValue(Math.min(1.0, jump.getBaseValue() * 1.15));
        }
    }

    private static boolean isBoosted(Horse horse) {
        return horse.getPersistentData().getBoolean("requiemGroomBoosted");
    }

    private static void markBoosted(Horse horse) {
        horse.getPersistentData().putBoolean("requiemGroomBoosted", true);
    }

    private static void tameNearbyHorses(GroomEntity entity) {
        LivingEntity owner = entity.getTrueOwner();
        if (!(owner instanceof Player player)) {
            return;
        }
        ServerLevel level = (ServerLevel) entity.level();
        List<Horse> horses = level.getEntitiesOfClass(Horse.class,
                new AABB(entity.getX() - 12, entity.getY() - 4, entity.getZ() - 12,
                        entity.getX() + 12, entity.getY() + 4, entity.getZ() + 12),
                h -> h.getOwnerUUID() == null);
        for (Horse horse : horses) {
            horse.setTamed(true);
            horse.setOwnerUUID(player.getUUID());
        }
    }

    private static Item consumeFood(GroomEntity entity) {
        if (entity.removeItem("minecraft:wheat", 1) >= 1) {
            return Items.WHEAT;
        }
        if (entity.removeItem("minecraft:carrot", 1) >= 1) {
            return Items.CARROT;
        }
        return null;
    }

    private static void feedBaby(Horse horse) {
        int age = horse.getAge();
        if (age < 0) {
            int newAge = (int) Math.round(age * 0.9);
            if (newAge >= 0) {
                newAge = -1;
            }
            horse.setAge(newAge);
        }
    }

    // ============================================================
    //  掷弹兵（50_grenadier.js）
    // ============================================================

    private static void grenadierAI(GrenadierEntity entity) {
        boolean hostile = entity.getType() == com.requiem.servants.ModEntities.GRENADIER_HOSTILE.get();
        ServerLevel level = (ServerLevel) entity.level();

        LivingEntity melee = findGrenadierTarget(level, entity, hostile, 2.0);
        updateGrenadierWeapon(entity, melee != null);
        if (melee != null) {
            entity.setTarget(melee);
            ServantLogic.setTask(entity, "近战反击");
            return;
        }
        entity.setTarget(null);

        LivingEntity target = findGrenadierTarget(level, entity, hostile, 20.0);
        if (target == null) {
            ServantLogic.setTask(entity, "待命");
            return;
        }

        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        if (dx * dx + dz * dz < 9.0) {
            entity.getNavigation().moveTo(entity.getX() - dx, entity.getY(), entity.getZ() - dz, 1.2);
            ServantLogic.setTask(entity, "保持距离");
            return;
        }

        long now = level.getGameTime();
        long last = entity.getPersistentData().getLong("grenadierLastThrow");
        if (now - last < 40) {
            ServantLogic.setTask(entity, "装填");
            return;
        }
        entity.getPersistentData().putLong("grenadierLastThrow", now);
        throwBomb(level, entity, target);
        ServantLogic.setTask(entity, "投弹");
    }

    private static LivingEntity findGrenadierTarget(ServerLevel level, GrenadierEntity entity,
            boolean hostile, double range) {
        AABB aabb = new AABB(entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range);
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (e == entity || !e.isAlive()) {
                continue;
            }
            boolean ok = hostile ? isHostileTarget(e) : isEnemyTarget(e);
            if (!ok) {
                continue;
            }
            double d = entity.distanceToSqr(e);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static void updateGrenadierWeapon(GrenadierEntity entity, boolean sword) {
        boolean cur = entity.getPersistentData().getBoolean("grenadierSword");
        if (cur == sword) {
            return;
        }
        entity.getPersistentData().putBoolean("grenadierSword", sword);
        entity.setItemSlot(EquipmentSlot.MAINHAND,
                sword ? new ItemStack(ModItems.PALE_SABER.get()) : bombStack());
    }

    private static ItemStack bombStack() {
        Item bomb = ForgeRegistries.ITEMS.getValue(new ResourceLocation("supplementaries", "bomb"));
        return bomb == null ? ItemStack.EMPTY : new ItemStack(bomb);
    }

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    }

    private static boolean isEnemyTarget(LivingEntity e) {
        if (e instanceof Enemy) {
            return true;
        }
        return e instanceof IOwned owned && owned.isHostile();
    }

    private static void throwBomb(ServerLevel level, GrenadierEntity entity, LivingEntity target) {
        try {
            Class<?> bombClass = loadClass("net.mehvahdjukaar.supplementaries.common.entities.BombEntity");
            Class<?> bombTypeClass = loadClass(
                    "net.mehvahdjukaar.supplementaries.common.entities.BombEntity$BombType");
            Object normal = Enum.valueOf((Class) bombTypeClass, "NORMAL");
            Constructor<?> ctor = bombClass.getConstructor(Level.class, LivingEntity.class, bombTypeClass);
            Object bombObj = ctor.newInstance(level, entity, normal);
            double dx = target.getX() - entity.getX();
            double dy = (target.getY() + 1.0) - entity.getEyeY();
            double dz = target.getZ() - entity.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.01) {
                return;
            }
            dy += len * 0.08;
            if (bombObj instanceof Projectile bomb) {
                bomb.shoot(dx, dy, dz, 1.5f, 0.6f);
            }
            level.addFreshEntity((Entity) bombObj);
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            entity.setYRot(yaw);
            entity.setYHeadRot(yaw);
        } catch (Exception e) {
            LOGGER.error("Failed to throw supplementaries bomb", e);
        }
    }

    // ============================================================
    //  轻骑兵（70_light_cavalry.js）
    // ============================================================

    private static void lightCavalryAI(LightCavalryEntity entity) {
        if (!entity.isPassenger()) {
            ServantLogic.setTask(entity, "下马近战");
        } else if (entity.getTarget() != null) {
            ServantLogic.setTask(entity, "射击");
        } else {
            ServantLogic.setTask(entity, "待命");
        }
    }

    // ============================================================
    //  传教士（95_preacher.js）
    // ============================================================

    private static void preacherAI(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        LivingEntity owner = entity instanceof IOwned owned ? owned.getTrueOwner() : null;
        if (!(owner instanceof Player player) || !SEHelper.hasResearch(player, DemolitionScrollItem.DEMOLITION)) {
            return;
        }
        ServerLevel level = (ServerLevel) entity.level();
        AABB aabb = new AABB(entity.getX() - 16, entity.getY() - 16, entity.getZ() - 16,
                entity.getX() + 16, entity.getY() + 16, entity.getZ() + 16);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (e == entity || !e.isAlive()) {
                continue;
            }
            if (e.getType().toString().contains("artillery")) {
                continue;
            }
            if (!MobUtil.areAllies(entity, e)) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
    }

    // ============================================================
    //  囚犯挖矿（prisoner_mining_upd.js）
    // ============================================================

    private static void prisonerAI(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        Item pickaxe = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", "pale_metal_pickaxe"));
        if (pickaxe == null || living.getMainHandItem().getItem() != pickaxe) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, false, false));
    }

    private static boolean isPreacher(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "goetyawaken".equals(key.getNamespace()) && "preacher_servant".equals(key.getPath());
    }

    private static boolean isPrisoner(Entity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "goety".equals(key.getNamespace()) && "prisoner".equals(key.getPath());
    }

    // ============================================================
    //  制箭师（90_fletcher.js）
    // ============================================================

    private static void fletcherAI(FletcherEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        if (!entity.isGuardingArea()) {
            ServantLogic.setTask(entity, "跟随主人");
            return;
        }
        BlockPos station = findWorkBlock(level, entity, "minecraft:fletching_table");
        if (station == null) {
            ServantLogic.setTask(entity, "未找到制箭台");
            return;
        }
        if (ServantDemand.processDemand(entity)) {
            return;
        }

        storeExcess(level, entity, FLETCH_OUTPUTS);
        storeExcess(level, entity, "minecraft:chicken");

        if (handleChickens(level, entity)) {
            return;
        }

        String demand = getDemandSlotItem(level, station);
        if (demand != null) {
            if (craftFletch(entity, demand)) {
                ServantLogic.setTask(entity, "制作 " + demand);
            } else {
                ServantDemand.requestMaterials(entity, demand, fletchInputs(demand));
                ServantLogic.setTask(entity, "需求原料 " + String.join("/", fletchInputs(demand)));
            }
            return;
        }

        if (craftFletch(entity, "minecraft:arrow")) {
            ServantLogic.setTask(entity, "制作箭矢");
        } else {
            ServantDemand.requestMaterials(entity, "minecraft:arrow", fletchInputs("minecraft:arrow"));
            ServantLogic.setTask(entity, "需求箭矢原料");
        }
    }

    /** 养鸡：需求种子 / 宰杀超过 4 只的成年鸡 / 喂鸡繁殖。返回 true 表示本 tick 专注养鸡。 */
    private static boolean handleChickens(ServerLevel level, FletcherEntity entity) {
        AABB aabb = new AABB(entity.getX() - 8, entity.getY() - 4, entity.getZ() - 8,
                entity.getX() + 8, entity.getY() + 4, entity.getZ() + 8);
        List<Chicken> adults = new ArrayList<>();
        for (Entity e : level.getEntitiesOfClass(Entity.class, aabb)) {
            if (e instanceof Chicken chicken && chicken.isAlive() && !chicken.isBaby()) {
                adults.add(chicken);
            }
        }
        if (!entity.hasItem("minecraft:wheat_seeds", 1)) {
            ServantDemand.requestMaterials(entity, "minecraft:wheat_seeds", "minecraft:wheat_seeds");
            ServantLogic.setTask(entity, "需求种子");
            return true;
        }
        if (adults.size() > 4) {
            int toKill = adults.size() - 4;
            for (int i = 0; i < toKill && i < adults.size(); i++) {
                adults.get(i).hurt(level.damageSources().generic(), 999999.0F);
            }
            ServantLogic.setTask(entity, "宰杀 " + toKill + " 只成年鸡");
            return true;
        }
        List<Chicken> available = new ArrayList<>();
        for (Chicken c : adults) {
            if (!c.isInLove()) {
                available.add(c);
            }
        }
        if (available.size() >= 2) {
            available.get(0).setInLove(null);
            available.get(1).setInLove(null);
            entity.removeItem("minecraft:wheat_seeds", 1);
            ServantLogic.setTask(entity, "喂鸡繁殖");
            return true;
        }
        ServantLogic.setTask(entity, "等待鸡群");
        return false;
    }

    private static String[] fletchInputs(String out) {
        return switch (out) {
            case "minecraft:arrow" -> new String[]{"minecraft:flint", "minecraft:stick", "minecraft:feather"};
            case "minecraft:spectral_arrow" -> new String[]{"minecraft:arrow", "minecraft:arrow",
                    "minecraft:glowstone_dust", "minecraft:glowstone_dust", "minecraft:glowstone_dust", "minecraft:glowstone_dust"};
            case "minecraft:bow" -> new String[]{"minecraft:stick", "minecraft:stick", "minecraft:stick",
                    "minecraft:string", "minecraft:string", "minecraft:string"};
            case "minecraft:crossbow" -> new String[]{"minecraft:stick", "minecraft:stick", "minecraft:stick",
                    "minecraft:string", "minecraft:string", "minecraft:iron_ingot", "minecraft:tripwire_hook"};
            default -> new String[0];
        };
    }

    private static boolean craftFletch(FletcherEntity entity, String out) {
        Item result;
        int amount;
        switch (out) {
            case "minecraft:arrow" -> { result = Items.ARROW; amount = 4; }
            case "minecraft:spectral_arrow" -> { result = Items.SPECTRAL_ARROW; amount = 2; }
            case "minecraft:bow" -> { result = Items.BOW; amount = 1; }
            case "minecraft:crossbow" -> { result = Items.CROSSBOW; amount = 1; }
            default -> { return false; }
        }
        String[] inputs = fletchInputs(out);
        for (String id : inputs) {
            if (!entity.hasItem(id, 1)) {
                return false;
            }
        }
        for (String id : inputs) {
            entity.removeItem(id, 1);
        }
        entity.addItem(new ItemStack(result, amount));
        return true;
    }

    // ============================================================
    //  园丁（20_gardener.js，简化：收割成熟作物+补种+瓜类+甘蔗）
    // ============================================================

    private static void gardenerAI(GardenerEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        if (!entity.isGuardingArea()) {
            ServantLogic.setTask(entity, "跟随主人");
            return;
        }
        BlockPos station = findWorkBlock(level, entity, "requiem:gardening_table");
        if (station == null) {
            ServantLogic.setTask(entity, "未找到园艺台");
            return;
        }
        if (ServantDemand.processDemand(entity)) {
            return;
        }

        storeExcess(level, entity, GARDENER_OUTPUTS);

        if (craftGardenerMaterials(entity)) {
            ServantLogic.setTask(entity, "处理甘蔗");
            return;
        }

        GardenTarget target = findGardenTarget(level, entity, station);
        if (target == null) {
            idleWander(level, entity, station);
            ServantLogic.setTask(entity, "空闲");
            return;
        }
        double dx = entity.getX() - (target.pos.getX() + 0.5);
        double dy = entity.getY() - target.pos.getY();
        double dz = entity.getZ() - (target.pos.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 6.25) {
            entity.getNavigation().moveTo(target.pos.getX() + 0.5, target.pos.getY(), target.pos.getZ() + 0.5, 1.0);
            ServantLogic.setTask(entity, "前往" + target.kind.task);
            return;
        }
        harvestGarden(level, entity, target);
        ServantLogic.setTask(entity, target.kind.task);
    }

    /** 用甘蔗合成糖 / 纸（低于阈值才继续合成）。 */
    private static boolean craftGardenerMaterials(GardenerEntity entity) {
        boolean did = false;
        int sugarcane = entity.countItem("minecraft:sugar_cane");
        while (sugarcane >= 1 && entity.countItem("minecraft:sugar") < 32) {
            entity.removeItem("minecraft:sugar_cane", 1);
            entity.addItem(new ItemStack(Items.SUGAR));
            sugarcane--;
            did = true;
        }
        while (sugarcane >= 1 && entity.countItem("minecraft:paper") < 32) {
            entity.removeItem("minecraft:sugar_cane", 1);
            entity.addItem(new ItemStack(Items.PAPER));
            sugarcane--;
            did = true;
        }
        return did;
    }

    private static GardenTarget findGardenTarget(ServerLevel level, GardenerEntity entity, BlockPos center) {
        int half = 16;
        // 成熟作物
        for (int dy = -3; dy <= 3; dy++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dx = -half; dx <= half; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(p);
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        return new GardenTarget(p, GardenKind.CROP);
                    }
                }
            }
        }
        // 瓜类（西瓜/南瓜）
        for (int dy = -3; dy <= 3; dy++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dx = -half; dx <= half; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    Block block = level.getBlockState(p).getBlock();
                    if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
                        return new GardenTarget(p, GardenKind.GOURD);
                    }
                }
            }
        }
        // 甘蔗（高度 >= 3 才收获）
        for (int dy = -3; dy <= 3; dy++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dx = -half; dx <= half; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() != Blocks.SUGAR_CANE) {
                        continue;
                    }
                    int height = 0;
                    BlockPos b = p;
                    while (level.getBlockState(b).getBlock() == Blocks.SUGAR_CANE && height < 4) {
                        height++;
                        b = b.above();
                    }
                    if (height >= 3) {
                        return new GardenTarget(p, GardenKind.SUGAR_CANE);
                    }
                }
            }
        }
        return null;
    }

    private static void harvestGarden(ServerLevel level, GardenerEntity entity, GardenTarget target) {
        switch (target.kind) {
            case CROP -> harvestCrop(level, entity, target.pos);
            case GOURD -> harvestGourd(level, entity, target.pos);
            case SUGAR_CANE -> harvestSugarCane(level, entity, target.pos);
        }
    }

    /** 收获成熟作物并补种。 */
    private static void harvestCrop(ServerLevel level, GardenerEntity entity, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, ItemStack.EMPTY);
        level.destroyBlock(pos, false, null);
        for (ItemStack drop : drops) {
            entity.addItem(drop);
        }
        // 补种
        String seed = seedForCrop(block);
        String cropId = cropForSeed(seed);
        if (seed != null && cropId != null && entity.removeItem(seed, 1) >= 1
                && level.getBlockState(pos.below()).getBlock() == Blocks.FARMLAND) {
            Block cropBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(cropId));
            if (cropBlock != null) {
                level.setBlock(pos, cropBlock.defaultBlockState(), 3);
            }
        }
    }

    /** 收获瓜类（破坏整个方块，藤蔓保留继续结瓜）。 */
    private static void harvestGourd(ServerLevel level, GardenerEntity entity, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, ItemStack.EMPTY);
        level.destroyBlock(pos, false, null);
        for (ItemStack drop : drops) {
            entity.addItem(drop);
        }
    }

    /** 收获甘蔗：破坏顶部两段，保留底段继续生长。 */
    private static void harvestSugarCane(ServerLevel level, GardenerEntity entity, BlockPos pos) {
        BlockPos top = pos;
        while (level.getBlockState(top.above()).getBlock() == Blocks.SUGAR_CANE) {
            top = top.above();
        }
        for (int i = 0; i < 2; i++) {
            BlockPos cur = top.below(i);
            if (level.getBlockState(cur).getBlock() != Blocks.SUGAR_CANE) {
                break;
            }
            // 保留最底部一段
            if (level.getBlockState(cur.below()).getBlock() != Blocks.SUGAR_CANE && i > 0) {
                break;
            }
            BlockState state = level.getBlockState(cur);
            List<ItemStack> drops = Block.getDrops(state, level, cur, null, null, ItemStack.EMPTY);
            level.destroyBlock(cur, false, null);
            for (ItemStack drop : drops) {
                entity.addItem(drop);
            }
        }
    }

    private static String seedForCrop(Block block) {
        if (block == Blocks.WHEAT) return "minecraft:wheat_seeds";
        if (block == Blocks.CARROTS) return "minecraft:carrot";
        if (block == Blocks.POTATOES) return "minecraft:potato";
        if (block == Blocks.BEETROOTS) return "minecraft:beetroot_seeds";
        return null;
    }

    private static String cropForSeed(String seed) {
        return switch (seed) {
            case "minecraft:wheat_seeds" -> "minecraft:wheat";
            case "minecraft:carrot" -> "minecraft:carrots";
            case "minecraft:potato" -> "minecraft:potatoes";
            case "minecraft:beetroot_seeds" -> "minecraft:beetroots";
            default -> null;
        };
    }

    private enum GardenKind {
        CROP("收割作物"),
        GOURD("采摘瓜果"),
        SUGAR_CANE("收割甘蔗");

        final String task;

        GardenKind(String task) {
            this.task = task;
        }
    }

    private static final class GardenTarget {
        final BlockPos pos;
        final GardenKind kind;

        GardenTarget(BlockPos pos, GardenKind kind) {
            this.pos = pos;
            this.kind = kind;
        }
    }

    // ============================================================
    //  工作方块查找
    // ============================================================

    /** 空闲时在工作方块旁边闲逛。 */
    private static void idleWander(ServerLevel level, WorkServantEntity entity, BlockPos anchor) {
        if (entity.getNavigation().isInProgress()) {
            return;
        }
        if (entity.getRandom().nextInt(5) != 0) {
            return;
        }
        int dx = entity.getRandom().nextInt(9) - 4;
        int dz = entity.getRandom().nextInt(9) - 4;
        BlockPos target = anchor.offset(dx, 0, dz);
        entity.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.7);
    }

    /** 把指定 id 的过剩物品存入记事本分配的仓库（木桶/箱子）。 */
    private static void storeExcess(ServerLevel level, WorkServantEntity entity, String... itemIds) {
        List<BlockPos> barrels = ServantLogic.readBlockPosList(entity, "Barrels");
        if (barrels.isEmpty()) {
            return;
        }
        for (BlockPos p : barrels) {
            if (!(level.getBlockEntity(p) instanceof Container container)) {
                continue;
            }
            for (int i = 0; i < entity.getWorkInventory().getContainerSize(); i++) {
                ItemStack stack = entity.getWorkInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key == null) {
                    continue;
                }
                boolean match = false;
                for (String want : itemIds) {
                    if (want.equals(key.toString())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                int left = entity.depositToContainer(container, stack);
                if (left <= 0) {
                    entity.getWorkInventory().setItem(i, ItemStack.EMPTY);
                } else {
                    stack.setCount(left);
                }
            }
        }
    }

    private static BlockPos findWorkBlock(ServerLevel level, WorkServantEntity entity, String blockId) {
        Block target = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
        if (target == null) {
            return null;
        }
        String uid = entity.getStringUUID();
        BlockPos center = entity.blockPosition();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -32; dz <= 32; dz++) {
                for (int dx = -32; dx <= 32; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!level.getBlockState(p).is(target)) {
                        continue;
                    }
                    if (level.getBlockEntity(p) instanceof WorkingBlockEntity wb) {
                        var sid = wb.getAssignedServantId();
                        if (sid != null && sid.toString().equals(uid)) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String getDemandSlotItem(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DemandWorkBlockEntity dw && dw.isDemandEnabled()) {
            ItemStack s = dw.getDemandContainer().getItem(0);
            if (!s.isEmpty()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
                return key == null ? null : key.toString();
            }
        }
        return null;
    }

    // ============================================================
    //  炮兵（40_artillery.js，简化：大炮 BE 反射）
    // ============================================================

    private static void artilleryAI(ArtilleryEntity entity) {
        boolean hostile = entity.getType() == com.requiem.servants.ModEntities.ARTILLERY_HOSTILE.get();
        ServerLevel level = (ServerLevel) entity.level();

        LivingEntity melee = findArtilleryTarget(level, entity, hostile, 4.0);
        updateArtilleryWeapon(entity, melee != null);
        if (melee != null) {
            entity.setTarget(melee);
            ServantLogic.setTask(entity, "近战反击");
            return;
        }
        entity.setTarget(null);

        if (!hostile && !entity.isGuardingArea()) {
            ServantLogic.setTask(entity, "跟随主人");
            return;
        }

        BlockPos cannon = ServantLogic.readBlockPos(entity, "Cannon");
        if (cannon == null && hostile) {
            cannon = findNearestCannon(level, entity);
            if (cannon != null) {
                ServantLogic.writeBlockPos(entity, "Cannon", cannon);
            }
        }
        if (cannon == null) {
            artilleryDebug(entity, "无炮位");
            ServantLogic.setTask(entity, hostile ? "未找到炮位" : "空闲（未设置炮位）");
            return;
        }

        if ("cease".equals(entity.getPersistentData().getString("captainFireMode"))) {
            ServantLogic.setTask(entity, "停火");
            return;
        }

        // 定点打击坐标优先（炮射地图/连队），否则自动索敌
        double tx, ty, tz;
        BlockPos strike = readArtilleryStrike(entity);
        if (strike != null) {
            tx = strike.getX() + 0.5;
            ty = strike.getY() + 0.5;
            tz = strike.getZ() + 0.5;
        } else {
            LivingEntity target = findArtilleryTarget(level, entity, hostile, 48.0);
            if (target == null) {
                artilleryDebug(entity, "无目标");
                ServantLogic.setTask(entity, "空闲（无目标）");
                return;
            }
            tx = target.getX();
            ty = target.getY() + 0.5;
            tz = target.getZ();
        }

        BlockPos stand = findCannonStand(level, cannon);
        double dx = entity.getX() - (stand.getX() + 0.5);
        double dy = entity.getY() - stand.getY();
        double dz = entity.getZ() - (stand.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 0.5) {
            entity.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.0);
            ServantLogic.setTask(entity, "前往炮位");
            return;
        }

        loadAndFire(level, entity, cannon, tx, ty, tz);
        ServantLogic.setTask(entity, strike != null ? "定点打击" : "装填开火");
    }

    private static BlockPos readArtilleryStrike(ArtilleryEntity entity) {
        BlockPos company = entity.getCompanyStrike();
        if (company != null) {
            return company;
        }
        return ServantLogic.readBlockPos(entity, "StrikeTarget");
    }

    private static void artilleryDebug(ArtilleryEntity entity, String msg) {
        long now = entity.level().getGameTime();
        long last = entity.getPersistentData().getLong("artilleryDbg");
        if (now - last >= 60) {
            entity.getPersistentData().putLong("artilleryDbg", now);
            ServantLogic.tellOwner(entity, "§7[炮兵] " + msg);
            LOGGER.info("[Artillery {}] {}", entity.getStringUUID().substring(0, 8), msg);
        }
    }

    private static LivingEntity findArtilleryTarget(ServerLevel level, ArtilleryEntity entity,
            boolean hostile, double range) {
        AABB aabb = new AABB(entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range);
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (e == entity || !e.isAlive()) {
                continue;
            }
            boolean ok = hostile ? isHostileTarget(e) : isEnemyTarget(e);
            if (!ok) {
                continue;
            }
            double d = entity.distanceToSqr(e);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static boolean isHostileTarget(LivingEntity e) {
        if (e instanceof Player) {
            return true;
        }
        if (e instanceof IOwned owned && !owned.isHostile()) {
            return true;
        }
        return e instanceof IronGolem || e instanceof Villager;
    }

    private static void updateArtilleryWeapon(ArtilleryEntity entity, boolean sword) {
        boolean cur = entity.getPersistentData().getBoolean("artillerySword");
        if (cur == sword) {
            return;
        }
        entity.getPersistentData().putBoolean("artillerySword", sword);
        entity.setItemSlot(EquipmentSlot.MAINHAND,
                sword ? new ItemStack(ModItems.PALE_SABER.get()) : new ItemStack(Items.TORCH));
    }

    private static List<BlockPos> findCannonsNear(ServerLevel level, BlockPos center) {
        Block cannon = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("supplementaries", "cannon"));
        List<BlockPos> list = new ArrayList<>();
        if (cannon == null) {
            return list;
        }
        for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dx = -16; dx <= 16; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(cannon)) {
                        list.add(p);
                    }
                }
            }
        }
        return list;
    }

    private static BlockPos findNearestCannon(ServerLevel level, ArtilleryEntity entity) {
        Block cannon = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("supplementaries", "cannon"));
        if (cannon == null) {
            return null;
        }
        BlockPos center = entity.blockPosition();
        BlockPos best = null;
        double bestD = 16 * 16;
        for (int dy = -16; dy <= 16; dy++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dx = -16; dx <= 16; dx++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!level.getBlockState(p).is(cannon)) {
                        continue;
                    }
                    double d = p.distSqr(center);
                    if (d < bestD) {
                        bestD = d;
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    private static BlockPos findCannonStand(ServerLevel level, BlockPos cannon) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos p = cannon.offset(dx, 0, dz);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                    return p;
                }
            }
        }
        return cannon;
    }

    private static void loadAndFire(ServerLevel level, ArtilleryEntity entity, BlockPos cannon,
            double tx, double ty, double tz) {
        try {
            Class<?> cannonClass = loadClass(
                    "net.mehvahdjukaar.supplementaries.common.block.tiles.CannonBlockTile");
            BlockEntity be = level.getBlockEntity(cannon);
            if (be == null) {
                artilleryDebug(entity, "炮位无 BE: " + cannon);
                return;
            }
            if (!cannonClass.isInstance(be)) {
                artilleryDebug(entity, "炮位 BE 不是大炮: " + be.getClass().getSimpleName());
                return;
            }
            Method isFiring = cannonClass.getMethod("isFiring");
            if ((boolean) isFiring.invoke(be)) {
                return;
            }
            Method hasFuelAndProj = cannonClass.getMethod("hasFuelAndProjectiles");
            Method setFuel = cannonClass.getMethod("setFuel", ItemStack.class);
            Method setProjectile = cannonClass.getMethod("setProjectile", ItemStack.class);
            Method readyToFire = cannonClass.getMethod("readyToFire");
            Method setAttributes = cannonClass.getMethod("setAttributes",
                    float.class, float.class, byte.class, boolean.class, Player.class);
            Method ignite = cannonClass.getMethod("ignite", Entity.class);

            if (!(boolean) hasFuelAndProj.invoke(be)) {
                setFuel.invoke(be, new ItemStack(Items.GUNPOWDER, 4));
                Item bomb = ForgeRegistries.ITEMS.getValue(new ResourceLocation("supplementaries", "bomb"));
                setProjectile.invoke(be, bomb == null ? ItemStack.EMPTY : new ItemStack(bomb));
                artilleryDebug(entity, "已装填火药+炸弹");
            }
            if (!(boolean) readyToFire.invoke(be)) {
                artilleryDebug(entity, "readyToFire=false（冷却中或弹药不足）");
                return;
            }

            double cx = cannon.getX() + 0.5;
            double cy = cannon.getY() + 0.5;
            double cz = cannon.getZ() + 0.5;
            double dx = tx - cx;
            double dy = ty - cy;
            double dz = tz - cz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.5) {
                artilleryDebug(entity, "目标过近，不开火");
                return;
            }
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            float pitch = (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            setAttributes.invoke(be, yaw, pitch, (byte) 4, false, null);
            ignite.invoke(be, entity);
            artilleryDebug(entity, "已点火 -> (" + (int) tx + "," + (int) ty + "," + (int) tz + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to operate supplementaries cannon", e);
            artilleryDebug(entity, "反射异常: " + e.getMessage());
        }
    }

    // ============================================================
    //  炮兵队长（60_artillery_captain.js）
    // ============================================================

    private static void artilleryCaptainAI(ArtilleryCaptainEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        String mode = entity.getPersistentData().getString("captainFireMode");
        if (mode.isEmpty()) {
            mode = "free";
        }

        List<ArtilleryEntity> squad = findSquadArtillery(level, entity);

        if (entity.consumeHornFire()) {
            for (ArtilleryEntity a : squad) {
                a.getPersistentData().putInt("hornFireTimer", 12);
                a.getPersistentData().putInt("artilleryRetreat", 0);
            }
        }

        BlockPos battery = entity.getBattery();
        if (battery != null) {
            List<BlockPos> cannons = findCannonsNear(level, battery);
            for (ArtilleryEntity a : squad) {
                if (ServantLogic.readBlockPos(a, "Cannon") == null && !cannons.isEmpty()) {
                    ServantLogic.writeBlockPos(a, "Cannon", cannons.remove(0));
                }
            }
        }

        BlockPos companyStrike = entity.getCompanyStrike();
        for (ArtilleryEntity a : squad) {
            if (companyStrike != null) {
                a.setCompanyStrike(companyStrike);
                a.getPersistentData().putString("captainFireMode", "marked");
            } else {
                a.clearCompanyStrike();
                a.getPersistentData().putString("captainFireMode", mode);
            }
        }

        String task = mode.equals("cease") ? "停火" : (mode.equals("marked") ? "射击标记地点" : "自由射击");
        ServantLogic.setTask(entity, task);
    }

    private static List<ArtilleryEntity> findSquadArtillery(ServerLevel level, ArtilleryCaptainEntity captain) {
        String captainUuid = captain.getStringUUID();
        AABB box = new AABB(captain.getX() - 64, captain.getY() - 64, captain.getZ() - 64,
                captain.getX() + 64, captain.getY() + 64, captain.getZ() + 64);
        return level.getEntitiesOfClass(ArtilleryEntity.class, box, e -> {
            String cu = e.getArtilleryCaptainUuid();
            return (cu != null && cu.equals(captainUuid)) || e.getLeader() == captain;
        });
    }

    // ============================================================
    //  学徒（10_apprentice.js，简化：冶炼 + 矿物块合成，不含种植）
    // ============================================================

    private static final Map<String, String> MINERAL_TO_BLOCK = Map.of(
            "minecraft:iron_ingot", "minecraft:iron_block",
            "minecraft:gold_ingot", "minecraft:gold_block",
            "minecraft:diamond", "minecraft:diamond_block",
            "minecraft:emerald", "minecraft:emerald_block",
            "minecraft:lapis_lazuli", "minecraft:lapis_block",
            "minecraft:copper_ingot", "minecraft:copper_block",
            "minecraft:coal", "minecraft:coal_block",
            "minecraft:redstone", "minecraft:redstone_block",
            "minecraft:netherite_ingot", "minecraft:netherite_block");

    private static final Map<String, String> BLOCK_TO_MINERAL = reverseMap(MINERAL_TO_BLOCK);

    private static final String[] SMELTABLES = {
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:raw_iron",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:raw_gold",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore", "minecraft:raw_copper",
            "minecraft:ancient_debris", "minecraft:nether_gold_ore"};

    private static final String[] SMOKER_INPUTS = {
            "minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
            "minecraft:mutton", "minecraft:rabbit", "minecraft:cod",
            "minecraft:salmon", "minecraft:potato", "minecraft:kelp"};

    private static final String[] SMELT_FUELS = {
            "minecraft:coal", "minecraft:charcoal", "minecraft:coal_block"};

    private static Map<String, String> reverseMap(Map<String, String> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            result.put(e.getValue(), e.getKey());
        }
        return result;
    }

    private static void apprenticeAI(ApprenticeEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        if (!entity.isGuardingArea()) {
            ServantLogic.setTask(entity, "跟随主人");
            return;
        }
        BlockPos station = findWorkBlock(level, entity, "requiem:processing_table");
        if (station == null) {
            ServantLogic.setTask(entity, "未找到加工台");
            return;
        }
        if (ServantDemand.processDemand(entity)) {
            return;
        }

        storeExcess(level, entity, APPRENTICE_OUTPUTS);
        withdrawWorkMaterials(level, entity);

        if (smeltOres(level, entity, station)) {
            ServantLogic.setTask(entity, "冶炼矿石");
            return;
        }

        String demand = getDemandSlotItem(level, station);
        if (demand != null) {
            if (craftMineral(entity, demand)) {
                ServantLogic.setTask(entity, "制作 " + demand);
            } else {
                ServantDemand.requestMaterials(entity, demand, mineralInputs(demand));
                ServantLogic.setTask(entity, "需求原料 " + demand);
            }
            return;
        }

        if (craftMineralAuto(entity)) {
            ServantLogic.setTask(entity, "合成矿物块");
            return;
        }
        idleWander(level, entity, station);
        ServantLogic.setTask(entity, "空闲");
    }

    private static String[] mineralInputs(String demand) {
        String ingot = BLOCK_TO_MINERAL.get(demand);
        if (ingot != null) {
            return new String[]{ingot, ingot, ingot, ingot, ingot, ingot, ingot, ingot, ingot};
        }
        String block = MINERAL_TO_BLOCK.get(demand);
        if (block != null) {
            return new String[]{block};
        }
        return new String[]{demand};
    }

    private static boolean craftMineral(ApprenticeEntity entity, String demand) {
        String ingot = BLOCK_TO_MINERAL.get(demand);
        if (ingot != null && entity.hasItem(ingot, 9)) {
            entity.removeItem(ingot, 9);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(demand));
            if (item != null) {
                entity.addItem(new ItemStack(item));
            }
            return true;
        }
        String block = MINERAL_TO_BLOCK.get(demand);
        if (block != null && entity.hasItem(block, 1)) {
            entity.removeItem(block, 1);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(demand));
            if (item != null) {
                entity.addItem(new ItemStack(item, 9));
            }
            return true;
        }
        return false;
    }

    private static boolean craftMineralAuto(ApprenticeEntity entity) {
        for (Map.Entry<String, String> e : MINERAL_TO_BLOCK.entrySet()) {
            if (entity.hasItem(e.getKey(), 9)) {
                entity.removeItem(e.getKey(), 9);
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.getValue()));
                if (item != null) {
                    entity.addItem(new ItemStack(item));
                }
                return true;
            }
        }
        for (Map.Entry<String, String> e : BLOCK_TO_MINERAL.entrySet()) {
            if (entity.hasItem(e.getKey(), 1)) {
                entity.removeItem(e.getKey(), 1);
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.getValue()));
                if (item != null) {
                    entity.addItem(new ItemStack(item, 9));
                }
                return true;
            }
        }
        return false;
    }

    /** 从记事本分配的仓库取燃料与可烧炼材料（供学徒使用）。 */
    private static void withdrawWorkMaterials(ServerLevel level, ApprenticeEntity entity) {
        List<BlockPos> barrels = ServantLogic.readBlockPosList(entity, "Barrels");
        if (barrels.isEmpty()) {
            return;
        }
        for (BlockPos p : barrels) {
            if (!(level.getBlockEntity(p) instanceof Container container)) {
                continue;
            }
            for (String fuel : SMELT_FUELS) {
                int have = entity.countItem(fuel);
                if (have < 8) {
                    entity.withdrawFromContainer(container, fuel, 8 - have);
                }
            }
            for (String ore : SMELTABLES) {
                int have = entity.countItem(ore);
                if (have < 16) {
                    entity.withdrawFromContainer(container, ore, 16 - have);
                }
            }
            for (String food : SMOKER_INPUTS) {
                int have = entity.countItem(food);
                if (have < 16) {
                    entity.withdrawFromContainer(container, food, 16 - have);
                }
            }
        }
    }

    private static boolean smeltOres(ServerLevel level, ApprenticeEntity entity, BlockPos station) {
        List<BlockPos> furnaces = findFurnaces(level, entity, station);
        if (furnaces.isEmpty()) {
            return false;
        }
        boolean didWork = false;
        for (BlockPos p : furnaces) {
            if (!(level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity furnace)) {
                continue;
            }
            ItemStack output = furnace.getItem(2);
            if (!output.isEmpty()) {
                entity.addItem(output.copy());
                furnace.setItem(2, ItemStack.EMPTY);
                didWork = true;
            }
            if (furnace.getItem(1).isEmpty()) {
                for (String fuel : SMELT_FUELS) {
                    if (entity.hasItem(fuel, 1)) {
                        entity.removeItem(fuel, 1);
                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(fuel));
                        furnace.setItem(1, item == null ? ItemStack.EMPTY : new ItemStack(item));
                        didWork = true;
                        break;
                    }
                }
            }
            if (furnace.getItem(0).isEmpty()) {
                String in = findFurnaceInput(entity, level.getBlockState(p).getBlock());
                if (in != null) {
                    entity.removeItem(in, 1);
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(in));
                    furnace.setItem(0, item == null ? ItemStack.EMPTY : new ItemStack(item));
                    didWork = true;
                }
            }
        }
        return didWork;
    }

    private static String findFurnaceInput(ApprenticeEntity entity, Block type) {
        if (type == Blocks.SMOKER) {
            for (String id : SMOKER_INPUTS) {
                if (entity.hasItem(id, 1)) return id;
            }
        } else if (type == Blocks.BLAST_FURNACE) {
            for (String id : SMELTABLES) {
                if (entity.hasItem(id, 1)) return id;
            }
        } else {
            for (String id : SMELTABLES) {
                if (entity.hasItem(id, 1)) return id;
            }
            for (String id : SMOKER_INPUTS) {
                if (entity.hasItem(id, 1)) return id;
            }
        }
        return null;
    }

    private static List<BlockPos> findFurnaces(ServerLevel level, ApprenticeEntity entity, BlockPos station) {
        List<BlockPos> result = new ArrayList<>(ServantLogic.readBlockPosList(entity, "Furnaces"));
        BlockPos single = ServantLogic.readBlockPos(entity, "Furnace");
        if (single != null) {
            result.add(single);
        }
        if (!result.isEmpty()) {
            return result;
        }
        // 记事本未分配熔炉时，回退到加工台（station）附近搜索
        for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dx = -16; dx <= 16; dx++) {
                    BlockPos p = station.offset(dx, dy, dz);
                    Block b = level.getBlockState(p).getBlock();
                    if (b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) {
                        result.add(p);
                    }
                }
            }
        }
        return result;
    }

    private static final class Torch {
        final BlockPos pos;
        final String type;

        Torch(BlockPos pos, String type) {
            this.pos = pos;
            this.type = type;
        }
    }
}
