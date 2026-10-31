package com.requiem.servants;

import com.requiem.servants.entity.ApprenticeEntity;
import com.requiem.servants.entity.ArtilleryCaptainEntity;
import com.requiem.servants.entity.ArtilleryEntity;
import com.requiem.servants.entity.BoticarioEntity;
import com.requiem.servants.entity.BoticarioHostileEntity;
import com.requiem.servants.entity.CaptainEntity;
import com.requiem.servants.entity.CavalryCaptainEntity;
import com.requiem.servants.entity.ConquerorEntity;
import com.requiem.servants.entity.DrummerEntity;
import com.requiem.servants.entity.FletcherEntity;
import com.requiem.servants.entity.GardenerEntity;
import com.requiem.servants.entity.GrenadierEntity;
import com.requiem.servants.entity.GroomEntity;
import com.requiem.servants.entity.LightCavalryEntity;
import com.requiem.servants.entity.RecruiterEntity;
import com.requiem.servants.entity.RequiemIceologerEntity;
import com.requiem.servants.entity.projectile.RequiemAcidBall;
import com.requiem.servants.entity.projectile.RequiemBolt;
import com.requiem.servants.entity.projectile.RequiemSnareWeb;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
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

    // 轻骑兵：骑马的机动单位
    public static final RegistryObject<EntityType<LightCavalryEntity>> LIGHT_CAVALRY = ENTITY_TYPES.register("light_cavalry",
            () -> EntityType.Builder.of(LightCavalryEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:light_cavalry"));

    // 骑兵队长：骑兵编制的指挥军官
    public static final RegistryObject<EntityType<CavalryCaptainEntity>> CAVALRY_CAPTAIN = ENTITY_TYPES.register("cavalry_captain",
            () -> EntityType.Builder.of(CavalryCaptainEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:cavalry_captain"));

    // 马倌：管理马场登记站的功能性仆从
    public static final RegistryObject<EntityType<GroomEntity>> GROOM = ENTITY_TYPES.register("groom",
            () -> EntityType.Builder.of(GroomEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:groom"));

    // 制箭师：管理制箭台的功能性仆从
    public static final RegistryObject<EntityType<FletcherEntity>> FLETCHER = ENTITY_TYPES.register("fletcher",
            () -> EntityType.Builder.of(FletcherEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:fletcher"));

    // 药剂师仆从：清除己方仆从负面效果的支援单位，可编入连队
    public static final RegistryObject<EntityType<BoticarioEntity>> BOTICARIO = ENTITY_TYPES.register("boticario",
            () -> EntityType.Builder.of(BoticarioEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:boticario"));

    // 敌对药剂师：清除己方灾厄村民负面效果，袭击中出现
    public static final RegistryObject<EntityType<BoticarioHostileEntity>> BOTICARIO_HOSTILE = ENTITY_TYPES.register("boticario_hostile",
            () -> EntityType.Builder.of(BoticarioHostileEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:boticario_hostile"));

    // 怪物型冰术师：默认敌对玩家的灾厄村民，与掠夺者/卫道士同阵营
    public static final RegistryObject<EntityType<RequiemIceologerEntity>> ICEOLOGER = ENTITY_TYPES.register("iceologer",
            () -> EntityType.Builder.of(RequiemIceologerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("requiem:iceologer"));

    // 电流投掷/虚空飞弹共用飞弹
    public static final RegistryObject<EntityType<RequiemBolt>> REQUIEM_BOLT = ENTITY_TYPES.register("requiem_bolt",
            () -> EntityType.Builder.of((EntityType<RequiemBolt> type, Level level) -> new RequiemBolt(type, level), MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("requiem:requiem_bolt"));

    // 酸液球
    public static final RegistryObject<EntityType<RequiemAcidBall>> REQUIEM_ACID_BALL = ENTITY_TYPES.register("requiem_acid_ball",
            () -> EntityType.Builder.of((EntityType<RequiemAcidBall> type, Level level) -> new RequiemAcidBall(type, level), MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("requiem:requiem_acid_ball"));

    // 凝滞粘液网
    public static final RegistryObject<EntityType<RequiemSnareWeb>> REQUIEM_SNARE_WEB = ENTITY_TYPES.register("requiem_snare_web",
            () -> EntityType.Builder.of((EntityType<RequiemSnareWeb> type, Level level) -> new RequiemSnareWeb(type, level), MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("requiem:requiem_snare_web"));

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
    }
}
