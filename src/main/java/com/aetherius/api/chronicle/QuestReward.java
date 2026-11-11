package com.aetherius.api.chronicle;

import com.aetherius.api.magic.MagicUser;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.BiConsumer;

/**
 * A quest payoff. Functional so content files can declare rewards inline:
 * <pre>{@code
 * QuestReward.ofRune(Rune.FLARIS)
 * }</pre>
 */
public interface QuestReward {

    void grant(ServerPlayerEntity player, MagicUser magic);

    String describeKey();

    static QuestReward ofLore(net.minecraft.util.Identifier loreId) {
        return of("aetherius.reward.lore", (player, magic) -> magic.getChronicle().unlockLore(loreId));
    }

    static QuestReward ofTensionResistance(float amount) {
        return of("aetherius.reward.tension_resistance", (player, magic) -> {
            if (magic instanceof com.aetherius.api.magic.PlayerMagicCapability capability) {
                capability.addTensionResistance(amount);
            }
        });
    }

    static QuestReward ofItem(ItemStack stack) {
        return new QuestReward() {
            @Override
            public void grant(ServerPlayerEntity player, MagicUser magic) {
                ItemStack copy = stack.copy();
                if (!player.getInventory().insertStack(copy)) {
                    player.dropItem(copy, false);
                }
            }

            @Override
            public String describeKey() {
                return stack.getItem().getTranslationKey();
            }
        };
    }

    static QuestReward ofRune(Rune rune) {
        return of("aetherius.reward.rune", (player, magic) -> magic.unlockRune(rune));
    }

    static QuestReward ofMana(float maxManaBonus) {
        return of("aetherius.reward.max_mana", (player, magic) ->
                magic.setMaxMana(magic.getMaxMana() + maxManaBonus));
    }

    static QuestReward ofMastery(SpellSchool school, int levels) {
        return of("aetherius.reward.mastery", (player, magic) -> {
            for (int i = 0; i < levels; i++) {
                magic.setSchoolMastery(school, magic.getSchoolMastery(school) + 1);
            }
        });
    }

    static QuestReward ofExperience(int levels) {
        return of("aetherius.reward.experience", (player, magic) -> player.addExperienceLevels(levels));
    }

    static QuestReward of(String describeKey, BiConsumer<ServerPlayerEntity, MagicUser> action) {
        return new QuestReward() {
            @Override
            public void grant(ServerPlayerEntity player, MagicUser magic) {
                action.accept(player, magic);
            }

            @Override
            public String describeKey() {
                return describeKey;
            }
        };
    }
}
