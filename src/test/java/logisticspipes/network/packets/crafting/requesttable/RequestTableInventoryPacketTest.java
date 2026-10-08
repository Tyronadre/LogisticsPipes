package logisticspipes.network.packets.crafting.requesttable;

import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTableInventoryPacketTest {

    private static final Item ITEM = new Item();

    @BeforeAll
    static void registerItem() throws ReflectiveOperationException {
        // The headless test runner has no Forge LaunchClassLoader. Only the codec's ID lookup is needed here.
        Field ids = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
        ids.setAccessible(true);
        ((ObjectIntIdentityMap) ids.get(Item.itemRegistry)).func_148746_a(ITEM, 32000);
    }

    @Test
    void upgradedStorageCountsAndItemTagsSurvivePacketRoundTrip() throws IOException {
        ItemStack stored = new ItemStack(ITEM, 192, 7);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", "Stored item");
        stored.setTagCompound(tag);
        ItemStack cursor = new ItemStack(ITEM, 64, 7);

        LPDataOutputStream input = new LPDataOutputStream();
        input.writeInt(10);
        input.writeInt(64);
        input.writeInt(-20);
        input.writeInt(3);
        input.writeInt(36);
        input.writeInt(192);
        input.writeInt(18);
        input.writeInt(20000);
        input.writeInt(123456);
        input.writeList(Arrays.asList(stored, null, cursor), LPDataOutputStream::writeItemStack);
        input.writeList(Collections.nCopies(18, null), LPDataOutputStream::writeFluidStack);
        input.writeItemStack(cursor);

        RequestTableInventoryPacket packet = new RequestTableInventoryPacket(0);
        packet.readData(new LPDataInputStream(input.toByteArray()));
        LPDataOutputStream output = new LPDataOutputStream();
        packet.writeData(output);

        LPDataInputStream result = new LPDataInputStream(output.toByteArray());
        assertEquals(10, result.readInt());
        assertEquals(64, result.readInt());
        assertEquals(-20, result.readInt());
        assertEquals(3, result.readInt());
        assertEquals(36, result.readInt());
        assertEquals(192, result.readInt());
        assertEquals(18, result.readInt());
        assertEquals(20000, result.readInt());
        assertEquals(123456, result.readInt());
        assertEquals(3, result.readInt());
        assertTrue(ItemStack.areItemStacksEqual(stored, result.readItemStack()));
        assertNull(result.readItemStack());
        assertTrue(ItemStack.areItemStacksEqual(cursor, result.readItemStack()));
        assertEquals(18, result.readInt());
        for (int slot = 0; slot < 18; slot++) {
            assertNull(result.readFluidStack());
        }
        assertTrue(ItemStack.areItemStacksEqual(cursor, result.readItemStack()));
        assertEquals(0, result.available());
    }

    @Test
    void emptyInventoryAndCursorSurvivePacketRoundTrip() throws IOException {
        RequestTableInventoryPacket packet = new RequestTableInventoryPacket(0);
        LPDataOutputStream output = new LPDataOutputStream();
        packet.writeData(output);
        RequestTableInventoryPacket received = new RequestTableInventoryPacket(0);
        received.readData(new LPDataInputStream(output.toByteArray()));
        LPDataOutputStream retransmitted = new LPDataOutputStream();
        received.writeData(retransmitted);
        assertArrayEquals(output.toByteArray(), retransmitted.toByteArray());
    }
}
