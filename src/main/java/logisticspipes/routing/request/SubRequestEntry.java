package logisticspipes.routing.request;

import logisticspipes.interfaces.routing.ICraft;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.resources.IResource;
import lombok.Getter;

/**
 * Tracks a single sub-request within a {@link RequestJob}.
 * <p>
 * Each sub-request corresponds to one ingredient or intermediate crafting step
 * that must be satisfied before the parent job can complete. It records which
 * crafter is responsible, the crafting template being used, how many items are
 * needed, how many have been taken from the network, how many have been
 * produced, and the current execution state.
 */
public class SubRequestEntry {

    /** The resource (item/fluid + amount) that this sub-request is asking for. */
    @Getter
    private final IResource resource;

    /** The crafter pipe that will handle this sub-request, or {@code null} if provided directly. */
    @Getter
    private final ICraft crafter;

    /** The crafting template used by the crafter, or {@code null} for provider-only sub-requests. */
    @Getter
    private final ICraftingTemplate craftingTemplate;

    /** Total number of items/units needed to satisfy this sub-request. */
    @Getter
    private final int amountNeeded;

    /** Number of items/units that have been extracted from the network so far. */
    @Getter
    private int amountTaken;

    /** Number of items/units that have been produced (crafted) so far. */
    @Getter
    private int amountProduced;

    /** Current execution state of this sub-request. */
    @Getter
    private RequestJobState state;

    public SubRequestEntry(IResource resource, ICraft crafter, ICraftingTemplate craftingTemplate, int amountNeeded) {
        if (resource == null) {
            throw new NullPointerException("resource must not be null");
        }
        this.resource = resource;
        this.crafter = crafter;
        this.craftingTemplate = craftingTemplate;
        this.amountNeeded = amountNeeded;
        this.amountTaken = 0;
        this.amountProduced = 0;
        this.state = RequestJobState.PENDING;
    }

    /** Marks this sub-request as actively being processed. */
    public void start() {
        if (state == RequestJobState.PENDING) {
            state = RequestJobState.IN_PROGRESS;
        }
    }

    /**
     * Records that {@code amount} items have been taken from the network.
     *
     * @param amount number of items taken
     */
    public void notifyTaken(int amount) {
        amountTaken = Math.min(amountTaken + amount, amountNeeded);
        updateState();
    }

    /**
     * Records that {@code amount} items have been produced by the crafter.
     *
     * @param amount number of items produced
     */
    public void notifyProduced(int amount) {
        amountProduced = Math.min(amountProduced + amount, amountNeeded);
        updateState();
    }

    /** Marks this sub-request as failed. */
    public void fail() {
        state = RequestJobState.FAILED;
    }

    /** Returns how many items are still missing (neither taken nor produced). */
    public int getMissing() {
        return Math.max(0, amountNeeded - amountTaken - amountProduced);
    }

    /** Returns {@code true} when all needed items have been accounted for. */
    public boolean isSatisfied() {
        return amountTaken + amountProduced >= amountNeeded;
    }

    private void updateState() {
        if (state == RequestJobState.FAILED) {
            return;
        }
        if (isSatisfied()) {
            state = RequestJobState.COMPLETED;
        } else if (state == RequestJobState.PENDING) {
            state = RequestJobState.IN_PROGRESS;
        }
    }

    @Override
    public String toString() {
        return "SubRequestEntry{resource=" + resource.getDisplayItem()
                + ", needed=" + amountNeeded
                + ", taken=" + amountTaken
                + ", produced=" + amountProduced
                + ", missing=" + getMissing()
                + ", state=" + state + "}";
    }
}
