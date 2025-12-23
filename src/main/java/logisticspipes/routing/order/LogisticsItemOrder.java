package logisticspipes.routing.order;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.resources.DictResource;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;

public class LogisticsItemOrder extends LogisticsOrder {

    public LogisticsItemOrder(DictResource item, IRequestItems destination, ResourceType type,
            IAdditionalTargetInformation info) {
        super(type, info);
        if (item == null) {
            throw new NullPointerException();
        }
        resource = item;
        this.destination = destination;
    }

    @Getter
    private final DictResource resource;

    @Getter
    private final IRequestItems destination;

    @Override
    public IRouter getRouter() {
        if (destination == null) {
            return null;
        }
        return destination.getRouter();
    }

    @Override
    public void sendFailed() {
        if (destination == null) {
            return;
        }
        destination.itemCouldNotBeSend(getResource().stack, getInformation());
    }

    @Override
    public ItemIdentifierStack getAsDisplayItem() {
        return resource.stack;
    }

    @Override
    public int getAmount() {
        return resource.stack.getStackSize();
    }

    @Override
    public void reduceAmountBy(int amount) {
        resource.stack.setStackSize(resource.stack.getStackSize() - amount);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound) {
        nbtTagCompound.setInteger("destination_id", this.destination.getID());
        this.resource.stack.unsafeMakeNormalStack().writeToNBT(nbtTagCompound);
        nbtTagCompound.setInteger("type",this.getType().ordinal());
        this.getInformation().writeToNBT(nbtTagCompound);
    }

    public static LogisticsItemOrder createFromNBT(NBTTagCompound nbtTagCompound) {
        var destinationID = nbtTagCompound.getInteger("destination_id");
        var type = LogisticsItemOrder.ResourceType.values()[(nbtTagCompound.getInteger("type"))];
        var information = IAdditionalTargetInformation.createFromNBT(nbtTagCompound);
        var itemStack = ItemStack.loadItemStackFromNBT(nbtTagCompound);

//        try {
//            //new LogisticsItemOrder(new DictResource(ItemIdentifierStack.getFromStack(itemStack),null), getRouter().get);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return null;
    }
}
