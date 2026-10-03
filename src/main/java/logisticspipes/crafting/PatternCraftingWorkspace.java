package logisticspipes.crafting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import logisticspipes.crafting.pattern.PatternRecipeSnapshot;
import logisticspipes.crafting.patternStack.IPatternStack;
import logisticspipes.crafting.patternStack.PatternStackHelper;

/**
 * Conservative admission: reserve the entire finite dependency plan before requesting any inputs. Waiting jobs own no
 * target reservations. Admitted jobs can stage all of their dependencies without competing for the space needed to
 * finish an already-started job.
 */
final class PatternCraftingWorkspace {

    private final ModulePatternCrafting module;
    private final Map<UUID, Budget> reservations = new LinkedHashMap<>();

    PatternCraftingWorkspace(ModulePatternCrafting module) {
        this.module = module;
    }

    boolean admit(PatternCraftingOrder order) {
        UUID instance = order.reference().instanceId();
        if (reservations.containsKey(instance)) return true;
        if (order.usesBatchExecution()) {
            java.util.List<ModulePatternCrafting> network = PatternCraftingMonitorRegistry
                    .networkPatternModules(module.getRouter());
            for (ModulePatternCrafting provider : network) {
                if (provider.hasLegacyOrders()) {
                    module.debugEventThrottled(
                            "ADMISSION",
                            "request %s queued until legacy saved requests drain",
                            instance);
                    return false;
                }
            }
        }
        Map<ModulePatternCrafting, Budget> plan = new LinkedHashMap<>();
        if (order.branch != null) order.branch.collectWorkspace(plan);
        if (!plan.containsKey(module)) addRecipe(plan, module, order.patternSlot, Math.max(1, order.remainingSets));
        for (Map.Entry<ModulePatternCrafting, Budget> entry : plan.entrySet()) {
            Budget budget = entry.getValue();
            if (!budget.fits()) {
                module.debugEvent(
                        "ADMISSION",
                        "request %s exceeds bounded workspace at router=%s: %s; split the request",
                        instance,
                        entry.getKey().getRouter(),
                        budget);
                PatternCraftingInstanceRegistry.cancelInstance(instance);
                return false;
            }
            PatternCraftingWorkspace workspace = entry.getKey().workspace();
            if (!workspace.reservations.containsKey(instance) && !workspace.occupied().plus(budget).fits()) {
                module.debugEventThrottled(
                        "ADMISSION",
                        "request %s queued for workspace at router=%s",
                        instance,
                        entry.getKey().getRouter());
                return false;
            }
        }
        // All checks precede all mutations; request fulfillment can reenter the scheduler afterwards.
        for (Map.Entry<ModulePatternCrafting, Budget> entry : plan.entrySet()) {
            entry.getKey().workspace().reservations.putIfAbsent(instance, entry.getValue());
            if (order.usesBatchExecution()) entry.getKey().batchOutputs().manageJob(instance);
            entry.getKey().markCraftingStateDirty();
        }
        return true;
    }

    static void addRecipe(Map<ModulePatternCrafting, Budget> plan, ModulePatternCrafting provider, int slot, int sets) {
        if (sets <= 0 || provider.getPatternStack(slot) == null) return;
        PatternRecipeSnapshot recipe = provider.getPatternRecipe(provider.getPatternStack(slot));
        Budget budget = plan.computeIfAbsent(provider, ignored -> new Budget());
        for (int i = 0; i < recipe.getIngredientSlotCount(); i++) budget.add(recipe.getInput(i), sets, false);
        for (int i = 0; i < recipe.getResultSlotCount(); i++) budget.add(recipe.getOutput(i), sets, true);
    }

    private Budget occupied() {
        Budget occupied = new Budget();
        for (Budget budget : reservations.values()) occupied = occupied.plus(budget);
        occupied.inputItems += module.unreservedInputs(reservations.keySet(), false);
        occupied.inputFluids += module.unreservedInputs(reservations.keySet(), true);
        occupied.outputItems += module.batchOutputs().unreservedAmount(reservations.keySet(), false);
        occupied.outputFluids += module.batchOutputs().unreservedAmount(reservations.keySet(), true);
        return occupied;
    }

    void cleanup() {
        for (UUID instance : new java.util.ArrayList<>(reservations.keySet())) {
            boolean active = !PatternCraftingInstanceRegistry.ordersForInstance(instance).isEmpty();
            for (ModulePatternCrafting provider : PatternCraftingMonitorRegistry
                    .networkPatternModules(module.getRouter()))
                active |= provider.hasWorkspaceWork(instance);
            if (!active) {
                reservations.remove(instance);
                module.batchOutputs().forgetJob(instance);
                module.markCraftingStateDirty();
            }
        }
    }

    void readFromNBT(NBTTagCompound tag) {
        reservations.clear();
        NBTTagList list = tag.getTagList("patternWorkspace", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            UUID instance = UUID.fromString(entry.getString("instance"));
            Budget budget = new Budget();
            budget.inputItems = entry.getLong("inputItems");
            budget.inputFluids = entry.getLong("inputFluids");
            budget.outputItems = entry.getLong("outputItems");
            budget.outputFluids = entry.getLong("outputFluids");
            reservations.put(instance, budget);
        }
    }

    void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Budget> reservation : reservations.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("instance", reservation.getKey().toString());
            Budget budget = reservation.getValue();
            entry.setLong("inputItems", budget.inputItems);
            entry.setLong("inputFluids", budget.inputFluids);
            entry.setLong("outputItems", budget.outputItems);
            entry.setLong("outputFluids", budget.outputFluids);
            list.appendTag(entry);
        }
        tag.setTag("patternWorkspace", list);
    }

    void appendDebugState(StringBuilder out) {
        for (Map.Entry<UUID, Budget> entry : reservations.entrySet()) out.append("  workspace instance=")
                .append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
    }

    boolean queued(UUID instance) {
        return !reservations.containsKey(instance);
    }

    static final class Budget {

        long inputItems;
        long inputFluids;
        long outputItems;
        long outputFluids;

        void add(IPatternStack stack, int sets, boolean output) {
            if (stack == null || stack.getAmount() <= 0) return;
            long amount = (long) stack.getAmount() * sets;
            if (output) {
                if (PatternStackHelper.isFluid(stack)) outputFluids += amount;
                else outputItems += amount;
            } else {
                if (PatternStackHelper.isFluid(stack)) inputFluids += amount;
                else inputItems += amount;
            }
        }

        Budget plus(Budget other) {
            Budget result = new Budget();
            result.inputItems = inputItems + other.inputItems;
            result.inputFluids = inputFluids + other.inputFluids;
            result.outputItems = outputItems + other.outputItems;
            result.outputFluids = outputFluids + other.outputFluids;
            return result;
        }

        boolean fits() {
            return inputItems <= PatternCraftingBatchOutputs.ITEM_CAPACITY
                    && outputItems <= PatternCraftingBatchOutputs.ITEM_CAPACITY
                    && inputFluids <= PatternCraftingBatchOutputs.FLUID_CAPACITY
                    && outputFluids <= PatternCraftingBatchOutputs.FLUID_CAPACITY;
        }

        @Override
        public String toString() {
            return "inputItems=" + inputItems
                    + " inputFluid="
                    + inputFluids
                    + " outputItems="
                    + outputItems
                    + " outputFluid="
                    + outputFluids;
        }
    }
}
