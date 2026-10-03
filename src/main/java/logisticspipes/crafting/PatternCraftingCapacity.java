package logisticspipes.crafting;

import net.minecraft.item.ItemStack;

import logisticspipes.crafting.pattern.PatternHandler;
import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.crafting.patternStack.PatternItemStack;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/** Routing accepts only outstanding deliveries whose space has already been reserved by admission. */
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

    int spaceForItem(ItemIdentifier item, boolean includeInTransit) {
        long amount = module.batchOutputs().incoming(new PatternItemStack(new ItemIdentifierStack(item, 1)));
        for (int slot = 0; slot < patterns.size(); slot++) {
            ItemStack pattern = patterns.getConfiguredPatternStack(slot);
            if (pattern != null) amount += ingredients.requestedItemAmount(slot, pattern, item);
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
