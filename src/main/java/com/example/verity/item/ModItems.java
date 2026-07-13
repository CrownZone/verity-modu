package com.example.verity.item;

import com.example.verity.VerityMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public final class ModItems {

    public static final Item VERITY_SWORD = register("verity_sword",
            new SwordItem(Tiers.NETHERITE,
                    new Item.Properties().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 10.0F, 1.6F))));

    public static final Item VERITY_AXE = register("verity_axe",
            new AxeItem(Tiers.NETHERITE,
                    new Item.Properties().attributes(AxeItem.createAttributes(Tiers.NETHERITE, 12.0F, 1.0F))));

    public static final Item VERITY_PICKAXE = register("verity_pickaxe",
            new PickaxeItem(Tiers.NETHERITE,
                    new Item.Properties().attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 7.0F, 1.2F))));

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(VerityMod.MOD_ID, name), item);
    }

    public static void register() {
        // Referencing this class triggers the static fields above to run.
    }
}
