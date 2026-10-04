package logisticspipes.crafting;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import logisticspipes.config.Configs;
import logisticspipes.crafting.patternStack.IPatternStack;
import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.crafting.patternStack.PatternItemStack;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LogisticsFluidOrder;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.utils.item.ItemIdentifierStack;

/** Drains recorded production, then distributes complete result sets to their existing orders. */
final class PatternCraftingResultExtractor {

    private final ModulePatternCrafting module;
    private final PipeItemsPatternCraftingLogistics pipe;
    private final AdjacentInventoryHandler adjacent;
    private final PatternByproductExtractionTargetCache satellites;

    PatternCraftingResultExtractor(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe,
            AdjacentInventoryHandler adjacent) {
        this.module = module;
        this.pipe = pipe;
        this.adjacent = adjacent;
        this.satellites = new PatternByproductExtractionTargetCache(pipe);
    }

    void tick() {
        if (!pipe.isNthTick(6)) return;
        module.batchOutputs().collect();
        distributeItems();
        distributeFluids();
        module.batchOutputs().returnUnclaimedOutputs();
        module.batchOutputs().cleanup();
        module.requestIngredientsForStagedCrafts();
    }

    private void distributeItems() {
        var manager = pipe.getItemOrderManager();
        int remaining = 64;
        int attempts = manager.getAllOrders().size();
        while (remaining > 0 && attempts-- > 0 && manager.hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
            LogisticsItemOrder order = manager.peekAtTopRequest(ResourceType.CRAFTING, ResourceType.EXTRA);
            if (order == null) break;
            int wanted = Math
                    .min(remaining, Math.min(order.getAmount(), order.getResource().getItem().getMaxStackSize()));
            if (order.getType() == ResourceType.EXTRA)
                wanted = Math.min(wanted, Math.max(0, order.getAmount() - module.batchOutputs().futureClaims(order)));
            if (wanted <= 0) {
                manager.deferSend();
                continue;
            }
            IPatternStack result = module.batchOutputs().take(order, wanted);
            if (result == null && !module.batchOutputs().manages(order)) {
                // Compatibility for orders saved before producing-batch accounting existed.
                PatternByproductTarget target = order.getByproductTarget();
                if (target != null && target.isConfigured()) {
                    var extracted = satellites.extractItem(
                            target,
                            order.getResource().getItem(),
                            wanted,
                            order.getRouter() == null ? -1 : order.getRouter().getSimpleID(),
                            order.getInformation());
                    if (extracted.amount() > 0) {
                        manager.sendSuccessfull(extracted.amount(), false, extracted.routedItem());
                        remaining -= extracted.amount();
                        continue;
                    }
                } else {
                    ItemStack extracted = adjacent.hasConnectedTE() ? adjacent.extract(order.getResource(), wanted)
                            : null;
                    if (extracted != null) result = new PatternItemStack(ItemIdentifierStack.getFromStack(extracted));
                }
            }
            if (result == null) {
                manager.deferSend();
                continue;
            }
            if (module.isOrderDestinationThisModule(order)) {
                // Arrival can schedule another recursive order and move extras in this same queue.
                // Finish the current order before handing its result back to the module.
                manager.sendSuccessfull(result.getAmount(), false, null);
                route(order, result, true);
            } else {
                manager.sendSuccessfull(result.getAmount(), false, route(order, result, false));
            }
            remaining -= result.getAmount();
        }
    }

    private void distributeFluids() {
        var manager = pipe.getPatternFluidOrderManager();
        int remaining = Configs.MAX_LOGISTICS_FLUID_TRANSPORT_INNER_CAPACITY / 2;
        int attempts = 0;
        for (LogisticsFluidOrder ignored : manager) attempts++;
        while (remaining > 0 && attempts-- > 0 && manager.hasOrders(ResourceType.CRAFTING, ResourceType.EXTRA)) {
            LogisticsFluidOrder order = manager.peekAtTopRequest(ResourceType.CRAFTING, ResourceType.EXTRA);
            if (order == null) break;
            int wanted = Math.min(remaining, order.getAmount());
            if (order.getType() == ResourceType.EXTRA)
                wanted = Math.min(wanted, Math.max(0, order.getAmount() - module.batchOutputs().futureClaims(order)));
            if (wanted <= 0) {
                manager.deferSend();
                continue;
            }
            IPatternStack result = module.batchOutputs().take(order, wanted);
            if (result == null && !module.batchOutputs().manages(order)) {
                PatternByproductTarget target = order.getByproductTarget();
                if (target != null && target.isConfigured()) {
                    var extracted = satellites.extractFluid(
                            target,
                            order.getFluid(),
                            wanted,
                            order.getRouter() == null ? -1 : order.getRouter().getSimpleID(),
                            order.getInformation());
                    if (extracted.amount() > 0) {
                        manager.sendSuccessfull(extracted.amount(), false, extracted.routedItem());
                        remaining -= extracted.amount();
                        continue;
                    }
                } else for (var tile : adjacent.locateFluidHandlers()) {
                    FluidStack extracted = adjacent
                            .extractFluid(tile, new PatternFluidStack(order.getFluid(), wanted), wanted);
                    if (extracted != null && extracted.amount > 0) {
                        result = PatternFluidStack.fromFluidStack(extracted);
                        break;
                    }
                }
            }
            if (result == null) {
                manager.deferSend();
                continue;
            }
            if (module.isOrderDestinationThisModule(order)) {
                manager.sendSuccessfull(result.getAmount(), false, null);
                route(order, result, true);
            } else {
                manager.sendSuccessfull(result.getAmount(), false, route(order, result, false));
            }
            remaining -= result.getAmount();
        }
    }

    private IRoutedItem route(LogisticsOrder order, IPatternStack result, boolean samePipe) {
        ItemIdentifierStack stack = ItemIdentifierStack.getFromStack(result.makePatternStack());
        if (samePipe && order.getInformation() instanceof PatternTargetInformation) {
            module.itemArrived(stack, order.getInformation());
            if (stack.getStackSize() > 0)
                pipe.sendStack(stack.makeNormalStack(), -1, CoreRoutedPipe.ItemSendMode.Normal, null);
            return null;
        }
        if (order.getRouter() == null)
            return pipe.sendStack(stack.makeNormalStack(), -1, CoreRoutedPipe.ItemSendMode.Normal, null);
        IRoutedItem item = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
        item.setDestination(order.getRouter().getSimpleID());
        item.setTransportMode(TransportMode.Active);
        item.setAdditionalTargetInformation(order.getInformation());
        pipe.queueRoutedItem(item, ForgeDirection.UNKNOWN);
        return item;
    }
}
