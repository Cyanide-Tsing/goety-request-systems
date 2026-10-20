package com.requiem.servants.command;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.requiem.servants.entity.ArtilleryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 与 KubeJS 炮兵脚本对接的桥梁。
 *
 * <h2>背景</h2>
 * 炮兵的开火逻辑不在本附属里，而在整合包的
 * {@code server_scripts/goety_storyline/40_artillery.js}（{@code global.artilleryAI}）。
 * 脚本把炮位、弹药木桶、打击坐标以 JSON 存在实体持久化数据的 {@value #POSITIONS_KEY} 键下：
 * <pre>
 * {"cannon":[x,y,z],"barrels":[[x,y,z],...],"strikeTarget":[x,y,z],"cannonYaw":0.0}
 * </pre>
 *
 * <h2>脚本的三条硬性规则</h2>
 * <ol>
 *   <li><b>只有"驻守"姿态才操炮</b>：{@code if (!hostile && !entity.isGuardingArea()) return;}。
 *       而 goety 的 {@code isGuardingArea()} 实质就是"{@code boundPos != null} 且维度相同"，
 *       {@code setFollowing()} 会把 {@code boundPos} 置空 —— 所以把炮兵编成"跟随"
 *       等于直接让它停火。见 {@link #emplace} 与 {@link #withdraw}。</li>
 *   <li><b>炮兵自己走去炮位</b>：{@code entity.navigation.moveTo(pos.cannon)}。
 *       所以连队不给它派阵型槽位，否则两边抢寻路。</li>
 *   <li><b>打击坐标优先于索敌</b>：{@code loadAndFire} 先看 {@code pos.strikeTarget}，
 *       有就按坐标轰（弹药优先级为炮弹 &gt; 燃烧弹 &gt; 炸弹）；没有才去找目标，
 *       而找目标时又优先采用 goety 的"优先目标"。
 *       于是连队有两种指挥手段：{@link #setStrikeTarget} 面杀伤、优先目标点杀。</li>
 * </ol>
 *
 * <p>所有解析/写入都是防御式的：脚本可能没装、键可能缺失、格式可能变化，
 * 任何异常都当作"没有该数据"处理，并且<b>绝不</b>在解析失败时覆盖原值。
 */
public final class ArtilleryEmplacement {
    /** KubeJS 脚本存放方块位置的持久化数据键（见 00_library.js 的 readPositions）。 */
    public static final String POSITIONS_KEY = "Positions";

    private static final String KEY_CANNON = "cannon";
    private static final String KEY_STRIKE = "strikeTarget";

    /** Supplementaries 大炮方块的注册名。 */
    private static final ResourceLocation CANNON_BLOCK =
            new ResourceLocation("supplementaries", "cannon");

    private ArtilleryEmplacement() {
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    private static JsonObject readPositions(Mob mob) {
        String raw = mob.getPersistentData().getString(POSITIONS_KEY);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static BlockPos readPos(JsonObject positions, String key) {
        if (positions == null) {
            return null;
        }
        try {
            JsonElement element = positions.get(key);
            if (element == null || !element.isJsonArray()) {
                return null;
            }
            JsonArray coords = element.getAsJsonArray();
            if (coords.size() < 3) {
                return null;
            }
            return new BlockPos(coords.get(0).getAsInt(), coords.get(1).getAsInt(), coords.get(2).getAsInt());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 该仆从绑定的炮位；没有则返回 {@code null}。 */
    public static BlockPos getCannonPos(Mob mob) {
        return mob instanceof ArtilleryEntity artillery ? artillery.getCompanyCannon() : null;
    }

    /** 脚本当前的打击坐标；没有则返回 {@code null}。 */
    public static BlockPos getStrikeTarget(Mob mob) {
        return readPos(readPositions(mob), KEY_STRIKE);
    }

    /**
     * 该仆从是否是一门"有炮位的火炮"。
     *
     * <p>有炮位的单位归炮位管，连队不发站位令，只负责指定打击目标。
     */
    public static boolean hasCannon(Mob mob) {
        return getCannonPos(mob) != null;
    }

    // ------------------------------------------------------------------
    // 架炮 / 撤炮
    // ------------------------------------------------------------------

    /**
     * 让炮兵"就位"：把它的驻守点绑定到自己的炮位。
     *
     * <p>这比 goety 的 {@code setGuarding()} 更合适——后者绑定的是<b>当前站立位置</b>，
     * 而这里直接绑到炮位，于是：
     * <ul>
     *   <li>{@code isGuardingArea()} 为真，脚本愿意操炮；</li>
     *   <li>goety 自己的"返回驻守点"目标也会把炮兵拉回炮位，
     *       跟脚本的 {@code moveTo(cannon)} 方向一致，不再互相拉扯。</li>
     * </ul>
     *
     * @return 是否成功就位（没有炮位则返回 false）
     */
    public static boolean emplace(Mob mob) {
        BlockPos cannon = getCannonPos(mob);
        if (cannon == null || !(mob instanceof IServant servant)) {
            return false;
        }
        servant.setWandering(false);
        servant.setStaying(false);
        servant.setBoundPos(cannon);
        servant.setBoundDim(mob.level().dimension());
        return true;
    }

    /**
     * 撤炮：解除驻守绑定，炮兵随即停止操炮并可以跟着连队走。
     *
     * <p>用于炮阵被近身时把炮组撤到阵后。同时清掉连队下的打击坐标，
     * 免得它撤退途中还惦记着轰炸。
     */
    public static void withdraw(Mob mob) {
        clearCaptainStrike(mob);
        if (mob instanceof IServant servant) {
            servant.setFollowing();
        }
    }

    /** 该仆从当前是否处于可以操炮的状态。 */
    public static boolean isManningCannon(Mob mob) {
        return mob instanceof IServant servant && hasCannon(mob) && servant.isGuardingArea();
    }

    /**
     * 找一个紧贴炮位、能站人的位置，作为炮组的操作位。
     *
     * <p><b>为什么必须由本附属主动把炮兵送过去</b>：脚本里"前往炮位"的那段代码排在
     * 目标检查<b>之后</b>——
     * <pre>
     * if (!target &amp;&amp; !hasStrike) return;   // 没敌人就直接返回
     * if (dist² &gt; WORK_DIST_SQ) moveTo(cannon);
     * </pre>
     * 也就是说周围没有敌人时，脚本根本不会把炮兵挪到炮位上。而开火又要求炮兵
     * 位于炮位 {@code WORK_DIST_SQ}（2.5 格）之内。结果就是炮兵平时散在各处，
     * 等敌人出现时还要先跑一段，甚至因为被阵型牵制而永远到不了位。
     *
     * <p>所以连队会给炮组专门下一条"钉死在炮位旁"的站位令。目标点与脚本的
     * {@code moveTo(cannon)} 指向同一处，两者不会互相拉扯。
     *
     * <p>炮位方块本身是实心的，站不进去，所以按"炮位本身 → 四正方向 → 四斜角"
     * 的顺序、在上下各一格的范围内找第一个能站的落点。
     *
     * @return 操作位中心坐标；实在找不到就退回炮位中心（至少方向是对的）
     */
    public static Vec3 findCrewStand(Level level, BlockPos cannon) {
        int[][] offsets = {
                {0, 0}, {0, -1}, {0, 1}, {-1, 0}, {1, 0},
                {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
        };
        for (int dy : new int[]{0, 1, -1}) {
            for (int[] offset : offsets) {
                BlockPos candidate = cannon.offset(offset[0], dy, offset[1]);
                if (canStand(level, candidate)) {
                    return new Vec3(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                }
            }
        }
        return Vec3.atCenterOf(cannon);
    }

    private static boolean canStand(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        BlockPos above = pos.above();
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }

    // ------------------------------------------------------------------
    // 打击坐标
    // ------------------------------------------------------------------

    /**
     * 下达面杀伤坐标。
     *
     * <p>读—改—写整个 {@value #POSITIONS_KEY} JSON，只替换 {@value #KEY_STRIKE} 一个键，
     * 其余（炮位、木桶、炮口朝向）原样保留。
     *
     * <p>如果当前的打击坐标<b>不是</b>连队下的（也就是玩家用"炮射地图"手动指定的），
     * 则不覆盖——玩家的手动命令优先。
     *
     * @return 是否写入成功
     */
    public static boolean setStrikeTarget(Mob mob, BlockPos target) {
        if (!FormationData.isCaptainStrike(mob) && getStrikeTarget(mob) != null) {
            // 玩家手动设过打击坐标，不去抢
            return false;
        }
        JsonObject positions = readPositions(mob);
        if (positions == null) {
            // 没有炮位数据就不要凭空造一个，否则会干扰脚本
            return false;
        }
        JsonArray coords = new JsonArray();
        coords.add(target.getX());
        coords.add(target.getY());
        coords.add(target.getZ());
        positions.add(KEY_STRIKE, coords);
        mob.getPersistentData().putString(POSITIONS_KEY, positions.toString());
        FormationData.setCaptainStrike(mob, true);
        return true;
    }

    /** 该方块是否是 Supplementaries 大炮。 */
    public static boolean isCannon(Level level, BlockPos pos) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        return id != null && CANNON_BLOCK.equals(id);
    }

    /**
     * 由连队长给炮兵指派炮位（写入 {@code CompanyCannon}，走 Java getter/setter 通道，
     * 与 {@code CompanyStrike} 一致，KubeJS 炮兵 AI 通过 {@code getCompanyCannon()} 读取）。
     *
     * <p>原本炮位只能由玩家手持传送石一个个绑。连队长可以代劳：读出周围的炮位，
     * 把没有炮位的炮兵分配到空着的炮上，于是一队炮兵能自动铺开到整个炮兵阵地。
     *
     * @return 是否写入成功
     */
    public static boolean assignCannon(Mob mob, BlockPos cannon) {
        if (!(mob instanceof ArtilleryEntity artillery)) {
            return false;
        }
        artillery.setCompanyCannon(cannon);
        return true;
    }

    /** 收回连队下达的打击坐标，让炮兵回到自动索敌（玩家手动设的不动）。 */
    public static void clearCaptainStrike(Mob mob) {
        if (!FormationData.isCaptainStrike(mob)) {
            return;
        }
        FormationData.setCaptainStrike(mob, false);
        JsonObject positions = readPositions(mob);
        if (positions == null || !positions.has(KEY_STRIKE)) {
            return;
        }
        positions.remove(KEY_STRIKE);
        mob.getPersistentData().putString(POSITIONS_KEY, positions.toString());
    }
}
