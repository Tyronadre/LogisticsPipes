package logisticspipes.crafting;

import net.minecraft.item.ItemStack;

import logisticspipes.crafting.pattern.PatternHandler;
import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.crafting.patternStack.PatternItemStack;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/** Per-pattern ordering allowance; arrivals consume their existing ingredient claims without another capacity gate. */
final class PatternCraftingCapacity {

    private final ModulePatternCrafting module;
    private final PipeItemsPatternCraftingLogistics pipe;
    private final PatternHandler patterns;
    private final PatternStackRequestHandler requested;
    private final PatternCraftingIngredientPlanner ingredients;

    PatternCraftingCapacity(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe,
            PatternHandler patterns, PatternStackRequestHandler requested,
            PatternCraftingIngredientPlanner ingredients) {
        this.module = module;
        this.pipe = pipe;
        this.patterns = patterns;
        this.requested = requested;
        this.ingredients = ingredients;
    }

    int orderableSets(PatternCraftingOrder order) {
        ItemStack pattern = order.pattern();
        if (pattern == null) return 0;
        int pending = module.pendingPatternSets(order.patternSlot);
        int target = module.getEffectiveBlockingMode() == PipeItemsPatternCraftingLogistics.BlockingMode.BLOCKING ? 0
                : module.maxDispatchablePatternSets(order.reference(), pattern, Integer.MAX_VALUE - 64);
        if (target > 0 && !module.batchOutputs().canOrderIntoTargets(order)) target = 0;
        return (int) Math
                .max(0, Math.min(Integer.MAX_VALUE, Math.max(0L, 64L + target - pending) + order.partialSets()));
    }

    int spaceForItem(ItemIdentifier item, boolean includeInTransit) {
        long amount = module.batchOutputs().incoming(new PatternItemStack(new ItemIdentifierStack(item, 1)));
        for (PatternCraftingOrder order : PatternCraftingInstanceRegistry.ordersForModule(module)) {
            if (order.pattern() != null)
                amount += ingredients.requestedItemAmount(order.reference(), order.pattern(), item);
        }
        if (includeInTransit) amount -= pipe.countOnRoute(item);
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, amount));
    }

    int spaceForFluid(FluidIdentifier fluid, boolean includeInTransit) {
        long amount = module.batchOutputs().incoming(new PatternFluidStack(fluid, 1));
        for (int slot = 0; slot < patterns.size(); slot++) amount += requested.amount(slot, fluid);
        // LP container item counts do not represent fluid amounts in mB.
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }
}
