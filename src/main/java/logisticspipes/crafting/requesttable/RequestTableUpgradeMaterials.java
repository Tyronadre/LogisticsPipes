package logisticspipes.crafting.requesttable;

import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.ItemList;
import logisticspipes.LogisticsPipes;

/** Shared material recipes for the server escrow and the upgrade board's cost previews. */
final class RequestTableUpgradeMaterials {

    private RequestTableUpgradeMaterials() {}

    static List<Requirement> getRecipe(RequestTableUpgradeBranch branch, int tier,
            RequestTableStorageUpgradeConfig config) {
        return switch (branch) {
            case FLUID_CONTROLLER -> Arrays.asList(
                    lpChip(4, "Blank Upgrade"),
                    gregTech("Electric_Pump_MV", "MV Electric Pump", 4),
                    gregTech("Cover_FluidDetector", "Fluid Detector Cover", 1),
                    circuit(config, 2, 4));
            case CRAFTING_MONITORING -> Arrays.asList(
                    lpChip(6, "Diamond Upgrade Chip"),
                    circuit(config, 3, 2),
                    gregTech("Cover_Screen", "Computer Monitor Cover", 1),
                    gregTech("Cover_Metrics_Transmitter", "Metrics Transmitter Cover", 1));
            case SLOT_COUNT, SLOT_SIZE, FLUID_SLOT_COUNT, FLUID_SLOT_SIZE -> storageRecipe(branch, tier, config);
        };
    }

    private static List<Requirement> storageRecipe(RequestTableUpgradeBranch branch, int tier,
            RequestTableStorageUpgradeConfig config) {
        String tierName = RequestTableStorageUpgradeConfig.getTierName(tier);
        return Arrays.asList(
                circuit(config, tier, config.getCost(tier)),
                branch.isFluid() ? new Requirement("Empty Bucket", 1, new ItemStack(Items.bucket), null)
                        : new Requirement("Chest", 1, new ItemStack(Blocks.chest), null),
                gregTech(
                        (branch.isFluid() ? "Electric_Pump_" : "Robot_Arm_") + tierName,
                        tierName + (branch.isFluid() ? " Electric Pump" : " Robot Arm"),
                        1),
                switch (tier) {
                case 1, 2, 3 -> lpChip(4, "Blank Upgrade");
                case 4, 5, 6, 7 -> lpChip(5, "Gold Upgrade Chip");
                case 8, 9, 10 -> lpChip(6, "Diamond Upgrade Chip");
                default -> throw new IllegalArgumentException("Invalid storage upgrade tier: " + tier);
                });
    }

    private static Requirement circuit(RequestTableStorageUpgradeConfig config, int tier, int count) {
        return new Requirement(
                RequestTableStorageUpgradeConfig.getTierName(tier) + " circuit",
                count,
                config.getExampleCircuit(tier),
                config.getCircuitOreName(tier));
    }

    private static Requirement lpChip(int damage, String label) {
        return new Requirement(label, 1, new ItemStack(LogisticsPipes.LogisticsParts, 1, damage), null);
    }

    private static Requirement gregTech(String name, String label, int count) {
        // Keep GregTech's optional classes unloaded when the mod is absent.
        ItemStack example = Loader.isModLoaded("gregtech") ? GregTechItems.get(name) : null;
        return new Requirement(label, count, example, null);
    }

    private static final class GregTechItems {

        private static ItemStack get(String name) {
            ItemList item = ItemList.valueOf(name);
            return item.hasBeenSet() ? item.get(1L) : null;
        }
    }

    static final class Requirement {

        private final String label;
        private final int count;
        private final ItemStack example;
        private final String oreName;

        private Requirement(String label, int count, ItemStack example, String oreName) {
            this.label = label;
            this.count = count;
            this.example = example == null ? null : example.copy();
            this.oreName = oreName;
        }

        String getLabel() {
            return label;
        }

        int getCount() {
            return count;
        }

        ItemStack getExample() {
            return example == null ? null : example.copy();
        }

        String getOreName() {
            return oreName;
        }

        boolean matches(ItemStack stack) {
            if (stack == null || stack.stackSize <= 0) return false;
            if (oreName != null) {
                for (int id : OreDictionary.getOreIDs(stack)) {
                    if (oreName.equals(OreDictionary.getOreName(id))) return true;
                }
                return false;
            }
            return example != null && stack.isItemEqual(example);
        }
    }
}
