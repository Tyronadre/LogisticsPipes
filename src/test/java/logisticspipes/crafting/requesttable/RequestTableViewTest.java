package logisticspipes.crafting.requesttable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestTableViewTest {

    @ParameterizedTest
    @CsvSource({"NETWORK, true, true, false", "NETWORK, false, false, true", "ITEM_STORAGE, true, false, true",
        "ITEM_STORAGE, false, false, true", "FLUID_STORAGE, true, true, false",
        "FLUID_STORAGE, false, false, false"})
    void shiftClickDestinationsFollowTheOpenView(RequestTableView view, boolean filledCell, boolean transferFluids,
                                                 boolean transferItems) {
        assertEquals(transferFluids, view.transfersFluids(filledCell));
        assertEquals(transferItems, view.transfersItems(filledCell));
    }
}
