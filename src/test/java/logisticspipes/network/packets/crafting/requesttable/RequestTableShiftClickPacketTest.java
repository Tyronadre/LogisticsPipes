package logisticspipes.network.packets.crafting.requesttable;

import logisticspipes.crafting.requesttable.RequestTableView;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestTableShiftClickPacketTest {

    @ParameterizedTest
    @EnumSource(RequestTableView.class)
    void viewAndPlayerInventorySlotSurvivePacketRoundTrip(RequestTableView view) throws IOException {
        RequestTableShiftClickPacket packet = new RequestTableShiftClickPacket(0).setClick(17, 35, view);
        LPDataOutputStream output = new LPDataOutputStream();
        packet.writeData(output);
        RequestTableShiftClickPacket received = new RequestTableShiftClickPacket(0);
        received.readData(new LPDataInputStream(output.toByteArray()));
        LPDataOutputStream retransmitted = new LPDataOutputStream();
        received.writeData(retransmitted);

        LPDataInputStream result = new LPDataInputStream(retransmitted.toByteArray());
        assertEquals(17, result.readInt());
        assertEquals(35, result.readInt());
        assertEquals(view, result.readEnum(RequestTableView.class));
        assertEquals(0, result.available());
    }
}
