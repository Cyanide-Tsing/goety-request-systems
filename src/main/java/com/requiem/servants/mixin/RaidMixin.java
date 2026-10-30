package com.requiem.servants.mixin;

import com.requiem.servants.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 让敌对药剂师出现在袭击里：把袭击生成的女巫按概率替换成敌对药剂师。
 */
@Mixin(Raid.class)
public abstract class RaidMixin {

    @ModifyVariable(method = "spawnGroup", at = @At(value = "STORE", ordinal = 0))
    private Raider requiem$spawnBoticario(Raider raider) {
        if (raider.getType() == EntityType.WITCH && raider.level().random.nextBoolean()) {
            Entity entity = ModEntities.BOTICARIO_HOSTILE.get().create(raider.level());
            if (entity instanceof Raider boticario) {
                return boticario;
            }
        }
        return raider;
    }
}
