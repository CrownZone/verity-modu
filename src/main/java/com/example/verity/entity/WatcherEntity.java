package com.example.verity.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * The Watcher.
 *
 * Extends Zombie purely to reuse its skeleton/model/animation system, but all
 * default zombie behaviour is stripped out. She never moves, attacks, or makes
 * noise on her own -- every bit of her behaviour is scripted externally by
 * {@link com.example.verity.rule.RuleManager}. She cannot be damaged; the only
 * way to end an encounter with her is to follow (or break) the rule.
 */
public class WatcherEntity extends Zombie {

    public WatcherEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setSilent(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty. She does not act on her own -- the RuleManager
        // fully controls her position and behaviour every tick.
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // She cannot be fought. Only the rule can end the encounter.
        return true;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Never naturally despawns -- her lifecycle is fully managed by RuleManager.
        return false;
    }
}
