package logisticspipes.crafting.requesttable;

/** The two independent, permanent item-storage upgrade paths. */
public enum RequestTableItemUpgradeBranch {

    SLOT_COUNT("Amount of slots"),
    SLOT_SIZE("Slot size");

    private final String label;

    RequestTableItemUpgradeBranch(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
