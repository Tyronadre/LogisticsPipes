package logisticspipes.crafting.requesttable;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTableFluidContainersTest {

    private final Item emptyCell = new Item();
    private final Item filledCell = new Item();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void leftClickConvertsWholeStackAndKeepsResultOnCursor(boolean filling) {
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.setItemStack(taggedCell(filling ? emptyCell : filledCell, 64, "water"));
        AtomicInteger remaining = new AtomicInteger(64);

        assertTrue(
            RequestTableFluidContainers
                .interact(inventory, 0, conversions(remaining, filling ? filledCell : emptyCell).get()));
        assertSame(filling ? filledCell : emptyCell, inventory.getItemStack().getItem());
        assertEquals(64, inventory.getItemStack().stackSize);
        assertEquals("water", inventory.getItemStack().getTagCompound().getString("fluid"));
        assertEquals(0, remaining.get());
        assertTrue(Arrays.stream(inventory.mainInventory).allMatch(stack -> stack == null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rightClickPreservesCursorKindAndPutsOneResultInInventory(boolean filling) {
        InventoryPlayer inventory = new InventoryPlayer(null);
        ItemStack cursor = taggedCell(filling ? emptyCell : filledCell, 64, "water");
        inventory.setItemStack(cursor);
        AtomicInteger remaining = new AtomicInteger(64);

        assertTrue(
            RequestTableFluidContainers
                .interact(inventory, 1, conversions(remaining, filling ? filledCell : emptyCell).get()));

        assertEquals(63, remaining.get());
        assertSame(cursor.getItem(), inventory.getItemStack().getItem());
        assertEquals(63, inventory.getItemStack().stackSize);
        assertEquals(cursor.getTagCompound(), inventory.getItemStack().getTagCompound());
        assertEquals(1, inventory.mainInventory[0].stackSize);
        assertSame(filling ? filledCell : emptyCell, inventory.mainInventory[0].getItem());
        assertEquals(64, cursor.stackSize);
    }

    @Test
    void rightClickOnLastCellPlacesResultInInventoryAndClearsCursor() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.setItemStack(taggedCell(filledCell, 1, "water"));
        AtomicInteger remaining = new AtomicInteger(1);

        assertTrue(RequestTableFluidContainers.interact(inventory, 1, conversions(remaining, emptyCell).get()));
        assertNull(inventory.getItemStack());
        assertSame(emptyCell, inventory.mainInventory[0].getItem());
        assertEquals(1, inventory.mainInventory[0].stackSize);
        assertEquals(0, remaining.get());
    }

    @Test
    void fullInventoryRejectsRightClickWithoutTransferringFluid() {
        InventoryPlayer inventory = fullInventory();
        ItemStack cursor = taggedCell(filledCell, 64, "water");
        inventory.setItemStack(cursor);
        AtomicInteger remaining = new AtomicInteger(64);

        assertFalse(RequestTableFluidContainers.interact(inventory, 1, conversions(remaining, emptyCell).get()));
        assertSame(cursor, inventory.getItemStack());
        assertEquals(64, cursor.stackSize);
        assertEquals(64, remaining.get());
    }

    @Test
    void fullInventoryStillAllowsWholeStackLeftClick() {
        InventoryPlayer inventory = fullInventory();
        inventory.setItemStack(taggedCell(filledCell, 64, "water"));
        AtomicInteger remaining = new AtomicInteger(64);

        assertTrue(RequestTableFluidContainers.interact(inventory, 0, conversions(remaining, emptyCell).get()));
        assertSame(emptyCell, inventory.getItemStack().getItem());
        assertEquals(64, inventory.getItemStack().stackSize);
        assertEquals(0, remaining.get());
    }

    @Test
    void rightClickMergesOnlyMatchingContainerTags() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.mainInventory[0] = taggedCell(filledCell, 63, "lava");
        inventory.mainInventory[1] = taggedCell(filledCell, 63, "water");
        inventory.setItemStack(taggedCell(emptyCell, 64, "water"));
        AtomicInteger remaining = new AtomicInteger(64);

        assertTrue(RequestTableFluidContainers.interact(inventory, 1, conversions(remaining, filledCell).get()));
        assertEquals(63, inventory.mainInventory[0].stackSize);
        assertEquals(64, inventory.mainInventory[1].stackSize);
        assertNull(inventory.mainInventory[2]);
    }

    @Test
    void leftClickTransfersAvailableFluidAndReturnsUntouchedCellsToInventory() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        ItemStack cursor = taggedCell(emptyCell, 64, "water");
        inventory.setItemStack(cursor);
        AtomicInteger remaining = new AtomicInteger(63);

        assertTrue(RequestTableFluidContainers.interact(inventory, 0, conversions(remaining, filledCell).get()));
        assertSame(filledCell, inventory.getItemStack().getItem());
        assertEquals(63, inventory.getItemStack().stackSize);
        assertSame(emptyCell, inventory.mainInventory[0].getItem());
        assertEquals(1, inventory.mainInventory[0].stackSize);
        assertEquals(cursor.getTagCompound(), inventory.mainInventory[0].getTagCompound());
        assertEquals(64, cursor.stackSize);
        assertEquals(0, remaining.get());
    }

    @Test
    void leftClickDoesNotLoseFluidWhenUntouchedCellsCannotFit() {
        InventoryPlayer inventory = fullInventory();
        ItemStack cursor = taggedCell(filledCell, 64, "water");
        inventory.setItemStack(cursor);
        AtomicInteger remaining = new AtomicInteger(63);

        assertFalse(RequestTableFluidContainers.interact(inventory, 0, conversions(remaining, emptyCell).get()));
        assertSame(cursor, inventory.getItemStack());
        assertEquals(64, cursor.stackSize);
        assertEquals(63, remaining.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void rejectedTransferLeavesWholeStackOnCursor(int mouseButton) {
        InventoryPlayer inventory = new InventoryPlayer(null);
        ItemStack cursor = new ItemStack(emptyCell, 64);
        inventory.setItemStack(cursor);
        AtomicInteger commits = new AtomicInteger();

        assertFalse(
            RequestTableFluidContainers.interact(
                inventory,
                mouseButton,
                new RequestTableFluidContainers.Transfer(cell -> cell, commits::incrementAndGet)));
        assertSame(cursor, inventory.getItemStack());
        assertEquals(64, cursor.stackSize);
        assertEquals(0, commits.get());
        assertTrue(Arrays.stream(inventory.mainInventory).allMatch(stack -> stack == null));
    }

    @Test
    void differentFluidAmountsCannotBeCombinedOnCursor() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        ItemStack cursor = taggedCell(emptyCell, 2, "water");
        inventory.setItemStack(cursor);
        AtomicInteger amounts = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();

        assertFalse(
            RequestTableFluidContainers.interact(inventory, 0, new RequestTableFluidContainers.Transfer(cell -> {
                ItemStack result = taggedCell(filledCell, 1, "water");
                result.getTagCompound().setInteger("amount", amounts.incrementAndGet());
                return result;
            }, commits::incrementAndGet)));
        assertSame(cursor, inventory.getItemStack());
        assertEquals(0, commits.get());
    }

    @Test
    void shiftClickConvertsStackInPlaceWithFullInventoryAndPreservesCursor() {
        InventoryPlayer inventory = fullInventory();
        inventory.mainInventory[12] = taggedCell(filledCell, 64, "water");
        ItemStack cursor = new ItemStack(emptyCell, 7);
        inventory.setItemStack(cursor);
        AtomicInteger remaining = new AtomicInteger(64);

        assertTrue(RequestTableFluidContainers.shiftClick(inventory, 12, conversions(remaining, emptyCell)));
        assertSame(emptyCell, inventory.mainInventory[12].getItem());
        assertEquals(64, inventory.mainInventory[12].stackSize);
        assertEquals(0, remaining.get());
        assertSame(cursor, inventory.getItemStack());
    }

    @Test
    void shiftClickStopsAtStorageCapacityAndKeepsUnconvertedCells() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.mainInventory[12] = taggedCell(filledCell, 5, "water");
        AtomicInteger remaining = new AtomicInteger(3);

        assertTrue(RequestTableFluidContainers.shiftClick(inventory, 12, conversions(remaining, emptyCell)));
        assertSame(filledCell, inventory.mainInventory[12].getItem());
        assertEquals(2, inventory.mainInventory[12].stackSize);
        assertSame(emptyCell, inventory.mainInventory[0].getItem());
        assertEquals(3, inventory.mainInventory[0].stackSize);
        assertEquals(0, remaining.get());
    }

    @Test
    void shiftClickDoesNotLoseFluidWhenPartialConversionByproductsCannotFit() {
        InventoryPlayer inventory = fullInventory();
        ItemStack source = taggedCell(filledCell, 5, "water");
        inventory.mainInventory[12] = source;
        AtomicInteger remaining = new AtomicInteger(3);

        assertFalse(RequestTableFluidContainers.shiftClick(inventory, 12, conversions(remaining, emptyCell)));
        assertSame(source, inventory.mainInventory[12]);
        assertEquals(5, source.stackSize);
        assertEquals(3, remaining.get());
    }

    @Test
    void shiftClickDoesNotReprocessEmptyCellsSharingTheSameItem() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.mainInventory[12] = taggedCell(filledCell, 64, "water");
        AtomicInteger remaining = new AtomicInteger(100);

        assertTrue(RequestTableFluidContainers.shiftClick(inventory, 12, () -> {
            int available = remaining.get();
            AtomicInteger staged = new AtomicInteger(available);
            return new RequestTableFluidContainers.Transfer(cell -> {
                staged.decrementAndGet();
                return new ItemStack(filledCell, 1, 1);
            }, () -> remaining.set(staged.get()));
        }));
        assertSame(filledCell, inventory.mainInventory[12].getItem());
        assertEquals(1, inventory.mainInventory[12].getItemDamage());
        assertEquals(64, inventory.mainInventory[12].stackSize);
        assertEquals(36, remaining.get());
    }

    private InventoryPlayer fullInventory() {
        InventoryPlayer inventory = new InventoryPlayer(null);
        Arrays.fill(inventory.mainInventory, new ItemStack(emptyCell, 64));
        return inventory;
    }

    private ItemStack taggedCell(Item item, int count, String fluid) {
        ItemStack stack = new ItemStack(item, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("fluid", fluid);
        stack.setTagCompound(tag);
        return stack;
    }

    private Supplier<RequestTableFluidContainers.Transfer> conversions(AtomicInteger remaining, Item resultItem) {
        return () -> {
            AtomicInteger staged = new AtomicInteger(remaining.get());
            return new RequestTableFluidContainers.Transfer(cell -> {
                assertEquals(1, cell.stackSize);
                if (staged.get() <= 0) {
                    return cell;
                }
                staged.decrementAndGet();
                ItemStack result = new ItemStack(resultItem, 1);
                result.setTagCompound((NBTTagCompound) cell.getTagCompound().copy());
                return result;
            }, () -> remaining.set(staged.get()));
        };
    }

    @Test
    void stagedStorageChangesNotifyViewersOnlyWhenCommitted() {
        RequestTableFluidStorage storage = new RequestTableFluidStorage(1, "test", 1000);
        AtomicInteger changes = new AtomicInteger();
        storage.addListener(inventory -> changes.incrementAndGet());
        InventoryPlayer inventory = fullInventory();
        inventory.setItemStack(new ItemStack(filledCell, 1));

        assertFalse(
            RequestTableFluidContainers
                .interact(inventory, 1, RequestTableFluidContainers.stage(storage, (staged, cell) -> {
                    staged.applySnapshot(20000, Arrays.asList(null, null, null));
                    return new ItemStack(emptyCell, 1);
                })));
        assertEquals(1000, storage.getSlotCapacity());
        assertEquals(1, storage.getSizeInventory());
        assertEquals(0, changes.get());

        assertTrue(
            RequestTableFluidContainers
                .interact(inventory, 0, RequestTableFluidContainers.stage(storage, (staged, cell) -> {
                    staged.applySnapshot(20000, Arrays.asList(null, null, null));
                    return new ItemStack(emptyCell, 1);
                })));
        assertEquals(20000, storage.getSlotCapacity());
        assertEquals(3, storage.getSizeInventory());
        assertEquals(1, changes.get());
    }

    @Test
    void fluidSnapshotNotifiesListenersAndPreservesEmptySlotIndexes() {
        RequestTableFluidStorage storage = new RequestTableFluidStorage(1, "test", 1000);
        AtomicInteger changes = new AtomicInteger();
        storage.addListener(inventory -> changes.incrementAndGet());

        storage.applySnapshot(20000, Arrays.asList(null, null, null));
        assertEquals(1, changes.get());
        assertEquals(3, storage.getSizeInventory());
        assertEquals(20000, storage.getSlotCapacity());
        storage.setInventorySlotContents(1, null);
        assertEquals(2, changes.get());
    }
}
