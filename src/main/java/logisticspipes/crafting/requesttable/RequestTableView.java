package logisticspipes.crafting.requesttable;

/**
 * Selects which scrollable panel is shown in the upper part of the request table GUI.
 */
public enum RequestTableView {

    /**
     * Shows requestable items and fluids from the logistics network.
     */
    NETWORK,

    /**
     * Shows the request table's internal item inventory.
     */
    ITEM_STORAGE,

    /**
     * Shows the request table's internal fluid inventory.
     */
    FLUID_STORAGE;

    boolean transfersFluids(boolean filledCell) {
        return this != ITEM_STORAGE && filledCell;
    }

    boolean transfersItems(boolean filledCell) {
        return this == ITEM_STORAGE || (this == NETWORK && !filledCell);
    }
}
