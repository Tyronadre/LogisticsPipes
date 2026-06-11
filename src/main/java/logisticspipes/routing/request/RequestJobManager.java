package logisticspipes.routing.request;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import logisticspipes.interfaces.routing.ICraft;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IProvide;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.IPromise;
import logisticspipes.request.RequestLog;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LinkedLogisticsOrderList;

/**
 * Manages all active {@link RequestJob}s for a single pipe.
 * <p>
 * Each routed pipe that participates in crafting owns one {@code RequestJobManager}.
 * It is analogous to the set of crafting CPUs in Applied Energistics, but a single
 * pipe can execute more than one job at a time (the concurrent-job limit is
 * configurable via {@link #setMaxConcurrentJobs(int)}).
 * <p>
 * Typical lifecycle:
 * <ol>
 *   <li>Call {@link #createJob(IResource)} to register a new job and receive its
 *       {@link RequestJob} handle.</li>
 *   <li>Populate sub-requests via {@link RequestJob#addSubRequest}.</li>
 *   <li>Dispatch the job into the pipe network with
 *       dispatchJob(RequestJob, IRequestItems).</li>
 *   <li>Notify the manager as items arrive via
 *       {@link #notifyItemTaken(UUID, int)} /
 *       {@link #notifyItemProduced(UUID, int)}.</li>
 *   <li>Finished jobs are automatically removed during {@link #tick()}.</li>
 * </ol>
 */
public class RequestJobManager {

    /** Default maximum number of jobs that may run concurrently on one pipe. */
    public static final int DEFAULT_MAX_CONCURRENT_JOBS = 1;

    private final List<RequestJob> activeJobs = new LinkedList<>();
    private int maxConcurrentJobs = DEFAULT_MAX_CONCURRENT_JOBS;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** Returns the maximum number of jobs that may run concurrently on this pipe. */
    public int getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    /**
     * Sets the maximum number of jobs that may run concurrently on this pipe.
     *
     * @param max must be &gt;= 1
     */
    public void setMaxConcurrentJobs(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("maxConcurrentJobs must be >= 1");
        }
        this.maxConcurrentJobs = max;
    }

    // -------------------------------------------------------------------------
    // Job creation & dispatch
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this manager can accept another job right now.
     */
    public boolean canAcceptJob() {
        int running = 0;
        for (RequestJob job : activeJobs) {
            if (!job.isFinished()) {
                running++;
            }
        }
        return running < maxConcurrentJobs;
    }

    /**
     * Creates a new {@link RequestJob} for the given resource and registers it
     * with this manager.
     * <p>
     * The job starts in {@link RequestJobState#PENDING}. Call
     * dispatchJob(RequestJob, IRequestItems) to actually
     * send it into the pipe network.
     *
     * @param resource the top-level resource to produce
     * @return the newly created job
     * @throws IllegalStateException if the concurrent-job limit has been reached
     */
    public RequestJob createJob(IResource resource) {
        if (!canAcceptJob()) {
            throw new IllegalStateException(
                    "Cannot create a new job: concurrent job limit (" + maxConcurrentJobs + ") reached");
        }
        RequestJob job = new RequestJob(resource);
        activeJobs.add(job);
        return job;
    }

    /**
     * Dispatches a previously created job into the pipe network via the
     * {@link RequestTree}, populates its sub-requests by walking the resolved
     * request tree, attaches the resulting {@link LinkedLogisticsOrderList}, and
     * transitions the job to {@link RequestJobState#IN_PROGRESS}.
     * <p>
     * Sub-requests are created for every node in the resolved tree that has at
     * least one crafting promise — i.e. every ingredient or intermediate step
     * that a crafter pipe must produce. Provider-only nodes (items pulled
     * directly from the network) are recorded as sub-requests without a crafter.
     * <p>
     * If the request tree cannot satisfy the job the job is marked as
     * {@link RequestJobState#FAILED}.
     *
     * @param job       the job to dispatch (must have been created by this manager)
     * @param requester the pipe that is requesting the items (used by the request tree)
     * @param info      additional routing target information, may be {@code null}
     * @return the {@link LinkedLogisticsOrderList} produced by the request tree,
     *         or {@code null} if the request could not be fulfilled
     */
    public LinkedLogisticsOrderList dispatchJob(RequestJob job, IRequestItems requester,
            IAdditionalTargetInformation info) {
        if (job == null) {
            throw new NullPointerException("job must not be null");
        }
        if (requester == null) {
            throw new NullPointerException("requester must not be null");
        }

        final LinkedLogisticsOrderList[] orderListHolder = new LinkedLogisticsOrderList[1];

        // Attach a JobTargetInformation so that every order in the resolved tree
        // carries the job reference. When items arrive at the requester pipe,
        // IRequireReliableTransport.itemArrived receives this info and calls
        // notifyItemTaken, driving the job toward COMPLETED.
        final JobTargetInformation jobInfo = new JobTargetInformation(job.getJobId(), this);

        RequestTree resolvedTree = RequestTree.requestAndReturnTree(
                job.getRequestedResource().getDisplayItem().clone(),
                requester,
                new RequestLog() {
                    @Override
                    public void handleMissingItems(java.util.List<IResource> resources) {}

                    @Override
                    public void handleSucessfullRequestOf(IResource item, LinkedLogisticsOrderList parts) {
                        orderListHolder[0] = parts;
                    }

                    @Override
                    public void handleSucessfullRequestOfList(java.util.List<IResource> resources,
                            LinkedLogisticsOrderList parts) {
                        orderListHolder[0] = parts;
                    }
                },
                jobInfo);

        if (resolvedTree != null) {
            // Walk the resolved tree and register every node as a sub-request on the job.
            // Skip the root node itself (it represents the top-level result, already tracked
            // by the job's own amountNeeded/amountProduced counters).
            populateSubRequests(job, resolvedTree);

            LinkedLogisticsOrderList orderList = orderListHolder[0];
            if (orderList == null) {
                orderList = new LinkedLogisticsOrderList();
            }
            job.setOrderList(orderList);
            job.start();
            return orderList;
        } else {
            job.fail();
            return null;
        }
    }

    /**
     * Walks the resolved request tree and adds a {@link SubRequestEntry} to {@code job}
     * for every child node (ingredient / intermediate crafting step).
     * <p>
     * The root node is skipped because it represents the top-level result already
     * tracked by the job itself. For each child node the crafter is determined from
     * the first crafting template committed to that node (if any); provider-only
     * nodes have a {@code null} crafter.
     */
    private void populateSubRequests(RequestJob job, RequestTreeNode root) {
        for (RequestTreeNode child : root.getChildNodes()) {
            populateSubRequestsRecursive(job, child);
        }
    }

    private void populateSubRequestsRecursive(RequestJob job, RequestTreeNode node) {
        IResource resource = node.getRequestType();
        int amountNeeded = resource.getRequestedAmount();

        // Determine the crafter and template from the first crafting promise on this node.
        ICraft crafter = null;
        ICraftingTemplate template = null;
        for (IPromise promise : node.getPromises()) {
            if (promise.getType() == ResourceType.CRAFTING) {
                IProvide provider = promise.getProvider();
                if (provider instanceof ICraft) {
                    crafter = (ICraft) provider;
                }
                // Find the matching template from usedCrafters
                for (ICraftingTemplate t : node.getUsedCrafters()) {
                    if (t.getCrafter() == crafter) {
                        template = t;
                        break;
                    }
                }
                break;
            }
        }

        job.addSubRequest(resource, crafter, template, amountNeeded);

        // Recurse into children (sub-ingredients of this step)
        for (RequestTreeNode child : node.getChildNodes()) {
            populateSubRequestsRecursive(job, child);
        }
    }

    // -------------------------------------------------------------------------
    // Item accounting notifications
    // -------------------------------------------------------------------------

    /**
     * Notifies the manager that {@code amount} items have been taken from the
     * network toward the job identified by {@code jobId}.
     *
     * @param jobId  the job's unique identifier
     * @param amount number of items taken
     */
    public void notifyItemTaken(UUID jobId, int amount) {
        RequestJob job = findJob(jobId);
        if (job != null) {
            job.notifyTaken(amount);
        }
    }

    /**
     * Notifies the manager that {@code amount} items have been produced (crafted)
     * toward the job identified by {@code jobId}.
     *
     * @param jobId  the job's unique identifier
     * @param amount number of items produced
     */
    public void notifyItemProduced(UUID jobId, int amount) {
        RequestJob job = findJob(jobId);
        if (job != null) {
            job.notifyProduced(amount);
        }
    }

    /**
     * Notifies the manager that {@code amount} items have been taken from the
     * network toward the given sub-request of the job identified by
     * {@code jobId}.
     *
     * @param jobId        the job's unique identifier
     * @param subRequest   the sub-request entry to update
     * @param amount       number of items taken
     */
    public void notifySubRequestTaken(UUID jobId, SubRequestEntry subRequest, int amount) {
        if (subRequest != null) {
            subRequest.notifyTaken(amount);
        }
    }

    /**
     * Notifies the manager that {@code amount} items have been produced toward
     * the given sub-request of the job identified by {@code jobId}.
     *
     * @param jobId        the job's unique identifier
     * @param subRequest   the sub-request entry to update
     * @param amount       number of items produced
     */
    public void notifySubRequestProduced(UUID jobId, SubRequestEntry subRequest, int amount) {
        if (subRequest != null) {
            subRequest.notifyProduced(amount);
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Should be called once per tick by the owning pipe.
     * Removes all jobs that have reached a terminal state
     * ({@link RequestJobState#COMPLETED} or {@link RequestJobState#FAILED}).
     */
    public void tick() {
        Iterator<RequestJob> iter = activeJobs.iterator();
        while (iter.hasNext()) {
            RequestJob job = iter.next();
            if (job.isFinished()) {
                iter.remove();
            }
        }
    }

    /**
     * Immediately cancels and removes all active jobs.
     * Intended for use when the pipe is removed from the world.
     */
    public void cancelAll() {
        for (RequestJob job : activeJobs) {
            if (!job.isFinished()) {
                job.fail();
            }
        }
        activeJobs.clear();
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns an unmodifiable view of all currently tracked jobs (including finished ones not yet ticked away). */
    public List<RequestJob> getActiveJobs() {
        return Collections.unmodifiableList(activeJobs);
    }

    /** Returns the number of jobs currently tracked (including finished ones not yet ticked away). */
    public int getJobCount() {
        return activeJobs.size();
    }

    /** Returns the number of jobs that are still running (not yet in a terminal state). */
    public int getRunningJobCount() {
        int count = 0;
        for (RequestJob job : activeJobs) {
            if (!job.isFinished()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Finds a job by its unique identifier.
     *
     * @param jobId the job UUID
     * @return the matching {@link RequestJob}, or {@code null} if not found
     */
    public RequestJob findJob(UUID jobId) {
        for (RequestJob job : activeJobs) {
            if (job.getJobId().equals(jobId)) {
                return job;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Debug
    // -------------------------------------------------------------------------

    /** Dumps a human-readable summary of all active jobs to stdout. */
    public void dump() {
        System.out.println("########## RequestJobManager ##########");
        System.out.println("Running: " + getRunningJobCount() + " / " + maxConcurrentJobs);
        for (RequestJob job : activeJobs) {
            System.out.println("  " + job);
            for (SubRequestEntry sub : job.getSubRequests()) {
                System.out.println("    " + sub);
            }
        }
        System.out.println("#######################################");
    }
}
