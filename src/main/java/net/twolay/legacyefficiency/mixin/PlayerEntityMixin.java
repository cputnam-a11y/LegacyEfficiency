package net.twolay.legacyefficiency.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Final
    @Shadow
    PlayerInventory inventory;

    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * @author 2lay, uperehostile
     * @reason overrides block breaking speeds
     */


    @Overwrite
    public float getBlockBreakingSpeed(BlockState block) {
        float speed = this.inventory.getBlockBreakingSpeed(block);

        ItemStack heldItem = this.getMainHandStack();
        int efficiencyLevel = this.getRegistryManager()
                .getOptional(RegistryKeys.ENCHANTMENT)
                .map(r -> r.getEntry(Enchantments.EFFICIENCY))
                .map(optionalEfficiency ->
                        optionalEfficiency.map(
                                efficiency -> EnchantmentHelper.getLevel(efficiency, heldItem)
                        ).orElse(0)
                )
                .orElse(0);
        ;

        if (efficiencyLevel > 0 && !heldItem.isEmpty()) {
            speed += (float) (efficiencyLevel * efficiencyLevel + 1);
        }
        if (this.hasStatusEffect(StatusEffects.HASTE)) {
            speed *= 1.0f + (float) (Objects.requireNonNull(this.getStatusEffect(StatusEffects.HASTE)).getAmplifier() + 1) * 0.2f;
        }
        if (this.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float fatigueMultiplier = switch (Objects.requireNonNull(this.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            };
            speed *= fatigueMultiplier;
        }

        if (this.isSubmergedIn(FluidTags.WATER) && this.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT)
                .map(
                        r -> r.getEntry(Enchantments.AQUA_AFFINITY)
                ).map(
                        optionalAquaAffinity -> optionalAquaAffinity.map(
                                aquaAffinity -> EnchantmentHelper.getLevel(aquaAffinity, this.getEquippedStack(EquipmentSlot.HEAD))
                        ).orElse(0)
                ).orElse(0) > 0) {
            speed /= 5.0f;
        }
        if (!this.isOnGround()) {
            speed /= 5.0f;
        }
        return speed;
    }
}
