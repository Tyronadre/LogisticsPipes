package logisticspipes.network.packets.crafting.requesttable;

import logisticspipes.crafting.requesttable.RequestTableContainer;
import logisticspipes.crafting.requesttable.RequestTablePipe;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.network.PacketGuards;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Synchronizes the open table's layout, items, actual fluids, and cursor using integer amounts. */
public class RequestTableInventoryPacket extends CoordinatesPacket {

    private int windowId;
    private int itemSlots;
    private int itemStackLimit;
    private int fluidSlots;
    private int fluidSlotCapacity;
    private List<ItemStack> contents = new ArrayList<>();
    private List<FluidStack> fluids = new ArrayList<>();
    private ItemStack cursor;

    public RequestTableInventoryPacket(int id) {
        super(id);
    }

    public RequestTableInventoryPacket setInventory(RequestTableContainer container, EntityPlayer player) {
        RequestTablePipe table = container.getTable();
        windowId = container.windowId;
        itemSlots = table.inv.getSizeInventory();
        itemStackLimit = table.inv.getInventoryStackLimit();
        fluidSlots = table.getFluidStorage().getSizeInventory();
        fluidSlotCapacity = table.getFluidStorage().getSlotCapacity();
        contents = new ArrayList<>();
        for (Object entry : container.inventorySlots) {
            Slot slot = (Slot) entry;
            ItemStack stack = slot.inventory == table.getFluidStorage() ? null : slot.getStack();
            contents.add(stack == null ? null : stack.copy());
        }
        fluids = new ArrayList<>();
        for (int slot = 0; slot < fluidSlots; slot++) {
            fluids.add(table.getFluidStorage().getFluid(slot));
        }
        ItemStack held = player.inventory.getItemStack();
        cursor = held == null ? null : held.copy();
        setTilePos(table.container);
        return this;
    }

    @Override
    public ModernPacket template() {
        return new RequestTableInventoryPacket(getId());
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (!PacketGuards.isOnClient(player) || !(player.openContainer instanceof RequestTableContainer container)
            || container.windowId != windowId) {
            return;
        }
        RequestTablePipe table = container.getTable();
        if (table.getX() != getPosX() || table.getY() != getPosY() || table.getZ() != getPosZ()) {
            return;
        }
        if (fluids.size() != fluidSlots) {
            return;
        }
        container.applyInventory(itemSlots, itemStackLimit, fluidSlotCapacity, contents, fluids);
        player.inventory.setItemStack(cursor);
    }

    @Override
    public void writeData(LPDataOutputStream data) throws IOException {
        super.writeData(data);
        data.writeInt(windowId);
        data.writeInt(itemSlots);
        data.writeInt(itemStackLimit);
        data.writeInt(fluidSlots);
        data.writeInt(fluidSlotCapacity);
        data.writeList(contents, LPDataOutputStream::writeItemStack);
        data.writeList(fluids, LPDataOutputStream::writeFluidStack);
        data.writeItemStack(cursor);
    }

    @Override
    public void readData(LPDataInputStream data) throws IOException {
        super.readData(data);
        windowId = data.readInt();
        itemSlots = data.readInt();
        itemStackLimit = data.readInt();
        fluidSlots = data.readInt();
        fluidSlotCapacity = data.readInt();
        contents = data.readList(LPDataInputStream::readItemStack);
        fluids = data.readList(LPDataInputStream::readFluidStack);
        cursor = data.readItemStack();
    }
}
