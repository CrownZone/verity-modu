package com.example.verity.entity;

import com.example.verity.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TotemGuardianEntity extends Zombie {

    public TotemGuardianEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        // Level 1 Regeneration, effectively permanent (huge duration).
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 72000000, 0, false, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.VERITY_SWORD), 0.0F);
    }
}
