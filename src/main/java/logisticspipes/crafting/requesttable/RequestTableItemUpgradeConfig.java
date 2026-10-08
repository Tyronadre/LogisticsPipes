package logisticspipes.crafting.requesttable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Editable cumulative tier bonuses and ore-dictionary circuit costs, also sent to the upgrade GUI. */
public final class RequestTableItemUpgradeConfig {

    public static final String CATEGORY = "requesttable.itemupgrades";
    public static final String[] TIERS = {"LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV"};
    public static final int TIER_COUNT = TIERS.length;
    private static final int[] DEFAULT_SLOTS = {9, 18, 27, 36, 45, 54, 63, 72, 81, 90};
    private static final int[] DEFAULT_SIZES = {64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};
    private static final String[] DEFAULT_CIRCUITS = {"circuitBasic", "circuitGood", "circuitAdvanced", "circuitData",
        "circuitElite", "circuitMaster", "circuitUltimate", "circuitSuperconductor", "circuitInfinite",
        "circuitBio"};
    private static RequestTableItemUpgradeConfig configured = new RequestTableItemUpgradeConfig();
    private final int[] slots = DEFAULT_SLOTS.clone();
    private final int[] sizes = DEFAULT_SIZES.clone();
    private final String[] circuits = DEFAULT_CIRCUITS.clone();
    private final int[] costs = new int[TIER_COUNT];
    private int baseSlots = 27;
    private int baseSlotSize = 64;

    private RequestTableItemUpgradeConfig() {
        java.util.Arrays.fill(costs, 1);
    }

    public static RequestTableItemUpgradeConfig getConfigured() {
        return configured;
    }

    public static void load(Configuration config) {
        RequestTableItemUpgradeConfig values = new RequestTableItemUpgradeConfig();
        values.baseSlots = Math.max(1, config.get(CATEGORY, "baseSlots", 27, "Base item-storage slot count.").getInt());
        values.baseSlotSize = Math
            .max(1, config.get(CATEGORY, "baseSlotSize", 64, "Base items per storage slot.").getInt());
        for (int i = 0; i < TIER_COUNT; i++) {
            String category = CATEGORY + "." + TIERS[i].toLowerCase(java.util.Locale.ROOT);
            values.slots[i] = Math.max(
                0,
                config.get(
                    category,
                    "additionalSlots",
                    DEFAULT_SLOTS[i],
                    "Added once when this slot-count tier is consumed.").getInt());
            values.sizes[i] = Math.max(
                0,
                config.get(
                    category,
                    "additionalSlotSize",
                    DEFAULT_SIZES[i],
                    "Added once when this slot-size tier is consumed.").getInt());
            values.circuits[i] = config
                .get(
                    category,
                    "circuitOreName",
                    DEFAULT_CIRCUITS[i],
                    "Accepted circuit ore-dictionary name; any matching circuit variant can be used.")
                .getString();
            values.costs[i] = Math.max(
                1,
                Math.min(
                    256,
                    config.get(
                        category,
                        "circuitCount",
                        1,
                        "Circuits consumed by either branch at this tier (1-256).").getInt()));
        }
        configured = values;
    }

    public static boolean isValidTier(int tier) {
        return tier >= 1 && tier <= TIER_COUNT;
    }

    public static String getTierName(int tier) {
        return tier == 0 ? "Base" : TIERS[Math.max(1, Math.min(TIER_COUNT, tier)) - 1];
    }

    public static RequestTableItemUpgradeConfig readData(DataInput input) throws IOException {
        RequestTableItemUpgradeConfig values = new RequestTableItemUpgradeConfig();
        values.baseSlots = Math.max(1, input.readInt());
        values.baseSlotSize = Math.max(1, input.readInt());
        for (int i = 0; i < TIER_COUNT; i++) {
            values.slots[i] = Math.max(0, input.readInt());
            values.sizes[i] = Math.max(0, input.readInt());
            values.circuits[i] = input.readUTF();
            values.costs[i] = Math.max(1, Math.min(256, input.readInt()));
        }
        return values;
    }

    public int getBonus(RequestTableItemUpgradeBranch branch, int tier) {
        return (branch == RequestTableItemUpgradeBranch.SLOT_COUNT ? slots : sizes)[tier - 1];
    }

    public int getTotal(RequestTableItemUpgradeBranch branch, int tier) {
        long total = branch == RequestTableItemUpgradeBranch.SLOT_COUNT ? baseSlots : baseSlotSize;
        for (int i = 1; i <= Math.min(TIER_COUNT, Math.max(0, tier)); i++) {
            total += getBonus(branch, i);
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int getCost(int tier) {
        return costs[tier - 1];
    }

    public String getCircuitOreName(int tier) {
        return circuits[tier - 1];
    }

    public boolean matchesCircuit(ItemStack stack, int tier) {
        if (stack == null || !isValidTier(tier)) return false;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(id).equals(getCircuitOreName(tier))) return true;
        }
        return false;
    }

    public ItemStack getExampleCircuit(int tier) {
        java.util.List<ItemStack> options = OreDictionary.getOres(getCircuitOreName(tier));
        for (ItemStack option : options) {
            if (option != null && option.getItem() != null && option.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
                ItemStack result = option.copy();
                result.stackSize = 1;
                return result;
            }
        }
        return null;
    }

    public void writeData(DataOutput output) throws IOException {
        output.writeInt(baseSlots);
        output.writeInt(baseSlotSize);
        for (int i = 0; i < TIER_COUNT; i++) {
            output.writeInt(slots[i]);
            output.writeInt(sizes[i]);
            output.writeUTF(circuits[i]);
            output.writeInt(costs[i]);
        }
    }
}
