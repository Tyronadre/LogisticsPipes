package logisticspipes.routing.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import logisticspipes.interfaces.routing.ICraft;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import lombok.Getter;

/**
 * Represents a single crafting request job managed by a {@link RequestJobManager}.
 * <p>
 * A job is analogous to a crafting CPU job in Applied Energistics: it tracks the
 * top-level resource being requested, all sub-requests (ingredients / intermediate
 * crafting steps), item accounting (needed / taken / produced / missing), and the
 * overall execution state. Each pipe can run multiple jobs concurrently; the limit
 * is enforced by {@link RequestJobManager}.
 */
public class RequestJob {

    /** Unique identifier for this job (runtime-stable). */
    @Getter
    private final UUID jobId;

    /** The top-level resource this job was created to produce. */
    @Getter
    private final IResource requestedResource;

    /** Total number of items/units that must be produced to satisfy this job. */
    @Getter
    private final int amountNeeded;

    /** Number of items/units that have been taken from the network so far. */
    @Getter
    private int amountTaken;

    /** Number of items/units that have been produced (crafted) so far. */
    @Getter
    private int amountProduced;

    /** Current execution state of this job. */
    @Getter
    private RequestJobState state;

    /**
     * The order list produced when this job was fulfilled via the request tree.
     * May be {@code null} until the job has been dispatched.
     */
    @Getter
    private LinkedLogisticsOrderList orderList;

    /** All sub-requests that must be satisfied to complete this job. */
    private final List<SubRequestEntry> subRequests = new ArrayList<>();

    public RequestJob(IResource requestedResource) {
        if (requestedResource == null) {
            throw new NullPointerException("requestedResource must not be null");
        }
        this.jobId = UUID.randomUUID();
        this.requestedResource = requestedResource;
        this.amountNeeded = requestedResource.getRequestedAmount();
        this.amountTaken = 0;
        this.amountProduced = 0;
        this.state = RequestJobState.PENDING;
    }

    // -------------------------------------------------------------------------
    // Sub-request management
    // -------------------------------------------------------------------------

    /**
     * Adds a new sub-request for a crafting step.
     *
     * @param resource        the ingredient / intermediate resource needed
     * @param crafter         the crafter pipe responsible (may be {@code null} for provider-only)
     * @param craftingTemplate the template used by the crafter (may be {@code null})
     * @param amountNeeded    how many units of {@code resource} are required
     * @return the created {@link SubRequestEntry}
     */
    public SubRequestEntry addSubRequest(IResource resource, ICraft crafter, ICraftingTemplate craftingTemplate,
            int amountNeeded) {
        SubRequestEntry entry = new SubRequestEntry(resource, crafter, craftingTemplate, amountNeeded);
        subRequests.add(entry);
        return entry;
    }

    /** Returns an unmodifiable view of all sub-requests for this job. */
    public List<SubRequestEntry> getSubRequests() {
        return Collections.unmodifiableList(subRequests);
    }

    // -------------------------------------------------------------------------
    // Item accounting
    // -------------------------------------------------------------------------

    /**
     * Records that {@code amount} items have been taken from the network toward
     * this job's top-level result.
     *
     * @param amount number of items taken
     */
    public void notifyTaken(int amount) {
        amountTaken = Math.min(amountTaken + amount, amountNeeded);
        updateState();
    }

    /**
     * Records that {@code amount} items have been produced (crafted) toward
     * this job's top-level result.
     *
     * @param amount number of items produced
     */
    public void notifyProduced(int amount) {
        amountProduced = Math.min(amountProduced + amount, amountNeeded);
        updateState();
    }

    /** Returns how many items are still missing (neither taken nor produced). */
    public int getMissing() {
        return Math.max(0, amountNeeded - amountTaken - amountProduced);
    }

    /** Returns {@code true} when all needed items have been accounted for. */
    public boolean isSatisfied() {
        return amountTaken + amountProduced >= amountNeeded;
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    /** Transitions this job from {@link RequestJobState#PENDING} to {@link RequestJobState#IN_PROGRESS}. */
    public void start() {
        if (state == RequestJobState.PENDING) {
            state = RequestJobState.IN_PROGRESS;
        }
    }

    /** Marks this job as failed and propagates failure to all unfinished sub-requests. */
    public void fail() {
        state = RequestJobState.FAILED;
        for (SubRequestEntry sub : subRequests) {
            if (sub.getState() != RequestJobState.COMPLETED) {
                sub.fail();
            }
        }
    }

    /**
     * Attaches the {@link LinkedLogisticsOrderList} produced by the request tree
     * when this job was dispatched into the pipe network.
     *
     * @param orderList the order list to attach
     */
    public void setOrderList(LinkedLogisticsOrderList orderList) {
        this.orderList = orderList;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    /** Returns {@code true} if this job has reached a terminal state (completed or failed). */
    public boolean isFinished() {
        return state == RequestJobState.COMPLETED || state == RequestJobState.FAILED;
    }

    @Override
    public String toString() {
        return "RequestJob{id=" + jobId
                + ", resource=" + requestedResource.getDisplayItem()
                + ", needed=" + amountNeeded
                + ", taken=" + amountTaken
                + ", produced=" + amountProduced
                + ", missing=" + getMissing()
                + ", subRequests=" + subRequests.size()
                + ", state=" + state + "}";
    }
}
