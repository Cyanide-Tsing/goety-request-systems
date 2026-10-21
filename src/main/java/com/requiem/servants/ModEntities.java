package com.requiem.servants;

import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.CaptainEntity;
import com.requiem.servants.entity.ConquerorEntity;
import com.requiem.servants.entity.DrummerEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.GrenadierEntity;
import com.requiem.servants.entity.RecruiterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RequiemServants.MOD_ID);

    public static final RegistryObject<EntityType<ApprenticeEntity>> APPRENTICE = ENTITY_TYPES.register("apprentice",
            () -> EntityType.Builder.of(ApprenticeEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:apprentice"));

    public static final RegistryObject<EntityType<GardenerEntity>> GARDENER = ENTITY_TYPES.register("gardener",
            () -> EntityType.Builder.of(GardenerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:gardener"));

    public static final RegistryObject<EntityType<RecruiterEntity>> RECRUITER = ENTITY_TYPES.register("recruiter",
            () -> EntityType.Builder.of(RecruiterEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:recruiter"));

    public static final RegistryObject<EntityType<ArtilleryEntity>> ARTILLERY = ENTITY_TYPES.register("artillery",
            () -> EntityType.Builder.of(ArtilleryEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:artillery"));

    // 连队长：编组周围的 goety 仆从并指挥其列阵
    public static final RegistryObject<EntityType<CaptainEntity>> CAPTAIN = ENTITY_TYPES.register("captain",
            () -> EntityType.Builder.of(CaptainEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    // 指挥官要在更远处就被客户端跟踪，阵型才不会在视野边缘错位
                    .clientTrackingRange(24)
                    .updateInterval(3)
                    .build("requiem:captain"));

    public static final RegistryObject<EntityType<ArtilleryEntity>> ARTILLERY_HOSTILE = ENTITY_TYPES.register("artillery_hostile",
            () -> EntityType.Builder.of(ArtilleryEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:artillery_hostile"));

    // 掷弹兵：投掷 Supplementaries 炸弹
    public static final RegistryObject<EntityType<GrenadierEntity>> GRENADIER = ENTITY_TYPES.register("grenadier",
            () -> EntityType.Builder.of(GrenadierEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:grenadier"));

    public static final RegistryObject<EntityType<GrenadierEntity>> GRENADIER_HOSTILE = ENTITY_TYPES.register("grenadier_hostile",
            () -> EntityType.Builder.of(GrenadierEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:grenadier_hostile"));

    // 征服者：持弩的重装线列步兵，可替代掠夺者/掷弹兵编入线列
    public static final RegistryObject<EntityType<ConquerorEntity>> CONQUEROR = ENTITY_TYPES.register("conqueror",
            () -> EntityType.Builder.of(ConquerorEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:conqueror"));

    // 炮兵队长：指挥一支炮兵小队的军官
    public static final RegistryObject<EntityType<ArtilleryCaptainEntity>> ARTILLERY_CAPTAIN = ENTITY_TYPES.register("artillery_captain",
            () -> EntityType.Builder.of(ArtilleryCaptainEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:artillery_captain"));

    // 鼓手：持小军鼓，为连队提供节奏增益
    public static final RegistryObject<EntityType<DrummerEntity>> DRUMMER = ENTITY_TYPES.register("drummer",
            () -> EntityType.Builder.of(DrummerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:drummer"));

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
    }
}
