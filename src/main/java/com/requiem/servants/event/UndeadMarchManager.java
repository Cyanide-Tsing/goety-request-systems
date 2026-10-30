package com.requiem.servants.event;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.common.entities.ModEntityType;
import com.Polarice3.Goety.common.entities.neutral.AbstractNecromancer;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.MobUtil;
import com.requiem.servants.ai.MarchTowardsPlayerGoal;
import com.requiem.servants.RequiemServants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 「亡者行军」挑战事件：一支古老亡灵军队主动向玩家行军，独立于灵魂能量系统。
 *
 * <p>触发：每 3 个游戏日对每个存活玩家判定一次（20–25%），玩家附近亡灵仆从较多时概率翻倍；
 * 仅主世界/下界，创造/旁观不触发。事件持续 5–8 分钟，或击杀行军统领提前终结。
 */
public final class UndeadMarchManager {
    private static final long ROLL_INTERVAL = 72000L;
    private static final int UNDEAD_SERVANT_THRESHOLD = 4;
    private static final UUID COMMANDER_HEALTH_ID = UUID.fromString("a2f3b9c0-1234-4a5b-9c8d-1e2f3a4b5c6d");

    private static boolean enabled = true;
    private static final Map<ResourceKey<Level>, Long> NEXT_ROLL = new HashMap<>();
    private static final Map<ResourceKey<Level>, ActiveMarch> ACTIVE = new HashMap<>();

    private UndeadMarchManager() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static boolean isActive(ServerLevel level) {
        return ACTIVE.containsKey(level.dimension());
    }

    /** 强制触发（/requiem events undead_march）。 */
    public static void forceTrigger(ServerLevel level, ServerPlayer player) {
        ActiveMarch existing = ACTIVE.get(level.dimension());
        if (existing != null) {
            existing.end();
        }
        ActiveMarch march = new ActiveMarch(level, player);
        ACTIVE.put(level.dimension(), march);
        march.start();
    }

    /** 每个服务器刻调用一次。 */
    static void onServerTick(ServerLevel level) {
        if (!enabled) {
            return;
        }
        ResourceKey<Level> dim = level.dimension();
        long now = level.getGameTime();
        long next = NEXT_ROLL.computeIfAbsent(dim, d -> now + ROLL_INTERVAL);

        ActiveMarch active = ACTIVE.get(dim);
        if (active != null) {
            active.tick();
        }

        if (now >= next) {
            NEXT_ROLL.put(dim, now + ROLL_INTERVAL);
            if (active == null || !ACTIVE.containsKey(dim)) {
                rollForPlayers(level);
            }
        }
    }

    private static void rollForPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            ResourceKey<Level> dim = player.level().dimension();
            if (dim != Level.OVERWORLD && dim != Level.NETHER) {
                continue;
            }
            double chance = 0.20 + level.random.nextDouble() * 0.05;
            if (hasManyUndeadServants(player)) {
                chance *= 2.0;
            }
            if (level.random.nextDouble() < chance) {
                forceTrigger(level, player);
                return;
            }
        }
    }

    private static boolean hasManyUndeadServants(Player player) {
        AABB box = player.getBoundingBox().inflate(32.0);
        int count = 0;
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box,
                e -> e.getMobType() == MobType.UNDEAD && MobUtil.getOwner(e) == player)) {
            if (++count >= UNDEAD_SERVANT_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private static int marchPower(ServerLevel level, Player player) {
        int soulPower = readSoulPower(level.getServer());
        if (soulPower >= 0) {
            if (soulPower < 50000) {
                return 0;
            }
            if (soulPower < 55000) {
                return 1;
            }
            if (soulPower < 65000) {
                return 2;
            }
            if (soulPower < 80000) {
                return 3;
            }
            return 4;
        }
        // 兜底：没有 soul power 时按游戏天数增强
        long days = level.getDayTime() / 24000L;
        if (days < 10) {
            return 0;
        }
        if (days < 25) {
            return 1;
        }
        if (days < 50) {
            return 2;
        }
        if (days < 100) {
            return 3;
        }
        return 4;
    }

    private static int readSoulPower(MinecraftServer server) {
        try {
            java.lang.reflect.Method method = MinecraftServer.class.getDeclaredMethod("kjs$getPersistentData");
            method.setAccessible(true);
            Object result = method.invoke(server);
            if (result instanceof CompoundTag tag) {
                return tag.contains("soul_power") ? tag.getInt("soul_power") : -1;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static Variant variantFor(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.NETHER) {
            return Variant.WITHER;
        }
        Biome biome = level.getBiome(pos).value();
        String path = level.getBiome(pos).unwrapKey().map(key -> key.location().getPath()).orElse("");
        if (biome.coldEnoughToSnow(pos) || biome.getBaseTemperature() < 0.2F) {
            return Variant.STRAY;
        }
        if (biome.getBaseTemperature() > 1.0F || path.contains("desert") || path.contains("badlands") || path.contains("mesa")) {
            return Variant.CAIRN;
        }
        if (path.contains("jungle") || path.contains("swamp") || path.contains("mangrove")) {
            return Variant.MOSSY;
        }
        if (path.contains("ocean") || path.contains("river") || path.contains("beach")) {
            return Variant.DROWNED;
        }
        return Variant.NORMAL;
    }

    private static BlockPos findRallyPos(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double distance = 128.0 + level.random.nextDouble() * 64.0;
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);
            int y = findSurfaceY(level, x, z);
            if (y > level.getMinBuildHeight()) {
                return new BlockPos(x, y, z);
            }
        }
        return player.blockPosition().offset(128, 0, 0);
    }

    private static int findSurfaceY(ServerLevel level, int x, int z) {
        int y = level.getMaxBuildHeight() - 1;
        while (y > level.getMinBuildHeight()) {
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            BlockState here = level.getBlockState(new BlockPos(x, y, z));
            BlockState above = level.getBlockState(new BlockPos(x, y + 1, z));
            if (!below.isAir() && below.isSolid() && here.isAir() && above.isAir()) {
                return y;
            }
            y--;
        }
        return level.getMinBuildHeight();
    }

    private enum Variant {
        STRAY(ModEntityType.NECROMANCER),
        CAIRN(ModEntityType.CAIRN_NECROMANCER),
        MOSSY(ModEntityType.MOSSY_NECROMANCER),
        DROWNED(ModEntityType.DROWNED_NECROMANCER_SERVANT),
        NORMAL(ModEntityType.NECROMANCER),
        WITHER(ModEntityType.WITHER_NECROMANCER);

        private final Supplier<? extends EntityType<?>> type;

        Variant(Supplier<? extends EntityType<?>> type) {
            this.type = type;
        }

        EntityType<?> type() {
            return type.get();
        }
    }

    private static final class ActiveMarch {
        private final ServerLevel level;
        private final ServerPlayer target;
        private final Set<UUID> units = new HashSet<>();
        private final List<UUID> necromancers = new ArrayList<>();
        private UUID commander;
        private final ServerBossEvent bossEvent;
        private int ticksRemaining;
        private boolean ended;
        private PlayerTeam team;
        private final int power;

        ActiveMarch(ServerLevel level, ServerPlayer target) {
            this.level = level;
            this.target = target;
            this.ticksRemaining = 6000 + level.random.nextInt(3600);
            this.power = marchPower(level, target);
            this.bossEvent = new ServerBossEvent(Component.translatable("requiem.march.commander"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        }

        void start() {
            BlockPos rally = findRallyPos(level, target);
            Variant variant = variantFor(level, rally);

            this.team = level.getScoreboard().addPlayerTeam("requiem_march_" + Integer.toHexString(level.random.nextInt()));

            int necroCount = 2 + power / 2;
            for (int i = 0; i < necroCount; i++) {
                spawnNecromancer(rally, variant, i == 0);
            }

            int eliteCount = 8 + power;
            for (int i = 0; i < eliteCount; i++) {
                spawnElite(rally, i % 2 == 0);
            }

            int gruntCount = 15 + power * 3;
            for (int i = 0; i < gruntCount; i++) {
                spawnGrunt(rally, variant);
            }

            int rareCount = power / 2;
            for (int i = 0; i < rareCount; i++) {
                spawnRare(rally);
            }

            level.playSound(null, rally.getX(), rally.getY(), rally.getZ(),
                    ModSounds.THUNDER_STRIKE_EPIC.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            level.playSound(null, target.blockPosition(),
                    ModSounds.THUNDER_STRIKE_EPIC.get(), SoundSource.HOSTILE, 2.0F, 1.0F);

            bossEvent.setVisible(true);
            bossEvent.addPlayer(target);
        }

        private void joinTeam(Mob mob) {
            if (team != null) {
                level.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), team);
            }
        }

        private void equipHelmet(Mob mob) {
            Item[] helmets = {Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET};
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmets[level.random.nextInt(helmets.length)]));
        }

        private void applyPowerGear(Mob mob) {
            if (power <= 0) {
                return;
            }
            if (mob instanceof AbstractSkeleton) {
                ItemStack bow = new ItemStack(Items.BOW);
                bow.enchant(Enchantments.POWER_ARROWS, power);
                mob.setItemSlot(EquipmentSlot.MAINHAND, bow);
            } else if (mob instanceof Zombie) {
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                sword.enchant(Enchantments.SHARPNESS, power);
                mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
            }
            ItemStack helmet = mob.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, power);
            }
        }

        private void equipNecromancerArmor(Mob mob) {
            boolean canWear = !(mob instanceof AbstractNecromancer) || ((AbstractNecromancer) mob).canWearArmor();
            if (!canWear) {
                // 死灵法师无法穿戴盔甲：退而求其次，用抗火避免被烧死
                mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
                return;
            }
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.BLACK_IRON_HELMET()));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BLACK_IRON_CHESTPLATE()));
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.BLACK_IRON_LEGGINGS()));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.BLACK_IRON_BOOTS()));
        }

        private void setupMarchAI(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinder)) {
                return;
            }
            PathNavigation nav = pathfinder.getNavigation();
            nav.setMaxVisitedNodesMultiplier(32.0F);
            if (nav instanceof GroundPathNavigation ground) {
                ground.setCanOpenDoors(true);
                ground.setCanPassDoors(true);
            }
            AttributeInstance followRange = pathfinder.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) {
                followRange.setBaseValue(Math.max(followRange.getBaseValue(), 128.0D));
            }
            pathfinder.goalSelector.addGoal(1, new MarchTowardsPlayerGoal(pathfinder, target, 1.0D));
        }

        private void spawnNecromancer(BlockPos center, Variant variant, boolean asCommander) {
            EntityType<?> type = variant.type();
            if (type == null) {
                return;
            }
            Entity e = type.create(level);
            if (!(e instanceof Mob mob)) {
                return;
            }
            BlockPos pos = scatter(center, 8);
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(target);
            necromancers.add(mob.getUUID());
            units.add(mob.getUUID());
            joinTeam(mob);
            equipNecromancerArmor(mob);
            setupMarchAI(mob);
            if (asCommander) {
                commander = mob.getUUID();
                mob.setCustomName(Component.translatable("requiem.march.commander"));
                mob.setCustomNameVisible(true);
                mob.setGlowingTag(true);
                AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
                if (health != null) {
                    double target = 80.0 + level.random.nextInt(21);
                    double bonus = target - health.getBaseValue();
                    if (bonus > 0.0) {
                        health.addPermanentModifier(new AttributeModifier(COMMANDER_HEALTH_ID,
                                "requiem_march_commander_health", bonus, AttributeModifier.Operation.ADDITION));
                    }
                    mob.setHealth(mob.getMaxHealth());
                }
            }
            level.addFreshEntity(mob);
        }

        private void spawnElite(BlockPos center, boolean vanguard) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                    new ResourceLocation("goetyawaken", vanguard ? "skeleton_vanguard" : "zombie_darkguard"));
            if (type == null) {
                type = vanguard ? EntityType.SKELETON : EntityType.ZOMBIE;
            }
            Entity e = type.create(level);
            if (!(e instanceof Mob mob)) {
                return;
            }
            BlockPos pos = scatter(center, 12);
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(target);
            equipHelmet(mob);
            applyPowerGear(mob);
            joinTeam(mob);
            setupMarchAI(mob);
            units.add(mob.getUUID());
            level.addFreshEntity(mob);
        }

        private void spawnGrunt(BlockPos center, Variant variant) {
            // 所有群系都可能刷新的特殊单位（玩偶匣僵尸/攀岩僵尸）
            if (level.random.nextFloat() < 0.2F) {
                spawnSpecialGrunt(center);
                return;
            }
            EntityType<?> type = gruntTypeFor(variant);
            Entity e = type.create(level);
            if (!(e instanceof Mob mob)) {
                return;
            }
            BlockPos pos = scatter(center, 16);
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(target);
            equipHelmet(mob);
            applyPowerGear(mob);
            joinTeam(mob);
            setupMarchAI(mob);
            if (mob instanceof Zombie zombie) {
                zombie.setCanBreakDoors(true);
            }
            units.add(mob.getUUID());
            level.addFreshEntity(mob);
        }

        private void spawnSpecialGrunt(BlockPos center) {
            EntityType<?> type = level.random.nextBoolean()
                    ? lookupOr(new ResourceLocation("goetyawaken", "jitb_zombie_servant"), EntityType.ZOMBIE)
                    : lookupOr(new ResourceLocation("goetyawaken", "bouldering_zombie"), EntityType.ZOMBIE);
            Entity e = type.create(level);
            if (!(e instanceof Mob mob)) {
                return;
            }
            // 玩偶匣僵尸是仆从，需要设定为敌对
            if (mob instanceof IOwned owned) {
                owned.setHostile(true);
            }
            BlockPos pos = scatter(center, 16);
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(target);
            equipHelmet(mob);
            applyPowerGear(mob);
            joinTeam(mob);
            setupMarchAI(mob);
            if (mob instanceof Zombie zombie) {
                zombie.setCanBreakDoors(true);
            }
            units.add(mob.getUUID());
            level.addFreshEntity(mob);
        }

        private EntityType<?> gruntTypeFor(Variant variant) {
            return switch (variant) {
                case STRAY -> level.random.nextBoolean() ? EntityType.STRAY
                        : lookupOr(new ResourceLocation("goetyawaken", "frozen_zombie"), EntityType.ZOMBIE);
                case CAIRN -> level.random.nextBoolean()
                        ? lookupOr(new ResourceLocation("goetyawaken", "parched"), EntityType.HUSK)
                        : EntityType.HUSK;
                case MOSSY -> level.random.nextBoolean()
                        ? lookupOr(new ResourceLocation("goetyawaken", "jungle_zombie"), EntityType.ZOMBIE)
                        : EntityType.SKELETON;
                case DROWNED -> EntityType.DROWNED;
                case NORMAL -> level.random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
                case WITHER -> level.random.nextBoolean() ? EntityType.ZOMBIFIED_PIGLIN : EntityType.WITHER_SKELETON;
            };
        }

        private EntityType<?> lookupOr(ResourceLocation id, EntityType<?> fallback) {
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(id);
            return t != null ? t : fallback;
        }

        private void spawnRare(BlockPos center) {
            boolean wraith = level.random.nextBoolean();
            EntityType<?> type = wraith ? ModEntityType.WRAITH.get() : ModEntityType.REAPER.get();
            Entity e = type.create(level);
            if (!(e instanceof Mob mob)) {
                return;
            }
            BlockPos pos = scatter(center, 14);
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(target);
            joinTeam(mob);
            setupMarchAI(mob);
            units.add(mob.getUUID());
            level.addFreshEntity(mob);
        }

        private BlockPos scatter(BlockPos center, int radius) {
            for (int attempt = 0; attempt < 8; attempt++) {
                int x = center.getX() + level.random.nextInt(radius * 2 + 1) - radius;
                int z = center.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
                int y = findSurfaceY(level, x, z);
                if (y > level.getMinBuildHeight()) {
                    return new BlockPos(x, y, z);
                }
            }
            return center;
        }

        void tick() {
            if (ended) {
                return;
            }
            Entity commanderEntity = commander == null ? null : level.getEntity(commander);
            if (commanderEntity instanceof LivingEntity living && living.isAlive()) {
                bossEvent.setProgress(living.getHealth() / living.getMaxHealth());
                applyAura(living);
            } else if (commander != null) {
                commanderDied();
                return;
            }

            ticksRemaining--;
            if (ticksRemaining <= 0) {
                end();
            }
        }

        private void applyAura(LivingEntity commander) {
            AABB box = commander.getBoundingBox().inflate(32.0);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box, e -> units.contains(e.getUUID()))) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
                mob.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }

        void commanderDied() {
            for (UUID necroId : necromancers) {
                Entity necro = level.getEntity(necroId);
                if (necro instanceof LivingEntity living) {
                    dropLoot(living);
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, living.getBoundingBox().inflate(64.0),
                            e -> MobUtil.getOwner(e) == living)) {
                        mob.discard();
                    }
                }
            }
            end();
        }

        private void dropLoot(LivingEntity living) {
            living.spawnAtLocation(new ItemStack(ModItems.ECTOPLASM(), 2 + level.random.nextInt(3)));
            living.spawnAtLocation(new ItemStack(ModItems.GRAVE_DUST(), 1 + level.random.nextInt(2)));
            living.spawnAtLocation(new ItemStack(ModItems.FORBIDDEN_FRAGMENT(), 1));
            living.spawnAtLocation(new ItemStack(ModItems.CURSED_METAL_INGOT(), 2 + level.random.nextInt(2)));
            living.spawnAtLocation(new ItemStack(ModItems.EMPTY_FOCUS(), 1));
            living.spawnAtLocation(new ItemStack(Items.GHAST_TEAR, 1));
            if (level.random.nextBoolean()) {
                living.spawnAtLocation(new ItemStack(ModItems.BURIED_SCROLL(), 1));
            }
            if (level.random.nextBoolean()) {
                living.spawnAtLocation(new ItemStack(ModItems.FRONT_SCROLL(), 1));
            }
            if (level.random.nextFloat() < 0.5F) {
                living.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL, 1));
            }
            if (level.random.nextFloat() < 0.5F) {
                living.spawnAtLocation(new ItemStack(Items.ZOMBIE_HEAD, 1));
            }
            if (level.random.nextFloat() < 0.35F) {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(randomEnchantment(),
                        4 + level.random.nextInt(2)));
                living.spawnAtLocation(book);
            }
        }

        private Enchantment randomEnchantment() {
            Enchantment[] pool = {
                    Enchantments.SHARPNESS, Enchantments.ALL_DAMAGE_PROTECTION, Enchantments.POWER_ARROWS,
                    Enchantments.BLOCK_EFFICIENCY, Enchantments.UNBREAKING
            };
            return pool[level.random.nextInt(pool.length)];
        }

        void end() {
            if (ended) {
                return;
            }
            ended = true;
            bossEvent.removeAllPlayers();
            if (team != null) {
                level.getScoreboard().removePlayerTeam(team);
                team = null;
            }
            for (UUID id : units) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
            units.clear();
            necromancers.clear();
            ACTIVE.remove(level.dimension());
        }
    }

    /** 复刻 goety 物品的间接引用，避免在静态初始化早期直接触碰 RegistryObject.get()。 */
    private static final class ModItems {
        static net.minecraft.world.item.Item ECTOPLASM() {
            return com.Polarice3.Goety.common.items.ModItems.ECTOPLASM.get();
        }

        static net.minecraft.world.item.Item GRAVE_DUST() {
            return com.Polarice3.Goety.common.items.ModItems.GRAVE_DUST.get();
        }

        static net.minecraft.world.item.Item FORBIDDEN_FRAGMENT() {
            return com.Polarice3.Goety.common.items.ModItems.FORBIDDEN_FRAGMENT.get();
        }

        static net.minecraft.world.item.Item CURSED_METAL_INGOT() {
            return com.Polarice3.Goety.common.items.ModItems.CURSED_METAL_INGOT.get();
        }

        static net.minecraft.world.item.Item EMPTY_FOCUS() {
            return com.Polarice3.Goety.common.items.ModItems.EMPTY_FOCUS.get();
        }

        static net.minecraft.world.item.Item BLACK_IRON_HELMET() {
            return com.Polarice3.Goety.common.items.ModItems.BLACK_IRON_HELMET.get();
        }

        static net.minecraft.world.item.Item BLACK_IRON_CHESTPLATE() {
            return com.Polarice3.Goety.common.items.ModItems.BLACK_IRON_CHESTPLATE.get();
        }

        static net.minecraft.world.item.Item BLACK_IRON_LEGGINGS() {
            return com.Polarice3.Goety.common.items.ModItems.BLACK_IRON_LEGGINGS.get();
        }

        static net.minecraft.world.item.Item BLACK_IRON_BOOTS() {
            return com.Polarice3.Goety.common.items.ModItems.BLACK_IRON_BOOTS.get();
        }

        static net.minecraft.world.item.Item BURIED_SCROLL() {
            return com.Polarice3.Goety.common.items.ModItems.BURIED_SCROLL.get();
        }

        static net.minecraft.world.item.Item FRONT_SCROLL() {
            return com.Polarice3.Goety.common.items.ModItems.FRONT_SCROLL.get();
        }
    }
}
