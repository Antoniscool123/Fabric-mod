package com.aetherius.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Lazy;

/**
 * Custom armour material for the Aethersight Monocle.
 *
 * <p>The repair ingredient deliberately points at amethyst shards (vanilla) instead of
 * {@code ModItems.AETHER_SHARD}: this enum is initialised while {@link com.aetherius.registry.ModItems}
 * is still being built, and referencing an Aetherius item here would create a
 * class-initialisation cycle that silently produces {@code null} ingredients.</p>
 */
public enum AetherArmorMaterials implements ArmorMaterial {

    AETHERSIGHT("aetherius_aethersight", 12, new int[]{2, 5, 4, 2, 3}, 22,
            SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 0.0f, 0.0f,
            () -> Ingredient.ofItems(Items.AMETHYST_SHARD));

    private static final int[] BASE_DURABILITY = new int[]{13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Lazy<Ingredient> repairIngredient;

    AetherArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmounts,
                         int enchantability, SoundEvent equipSound, float toughness,
                         float knockbackResistance, java.util.function.Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = new Lazy<>(repairIngredient);
    }

    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()] * durabilityMultiplier;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return protectionAmounts[type.getEquipmentSlot().getEntitySlotId()];
    }

    @Override
    public int getEnchantability() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
