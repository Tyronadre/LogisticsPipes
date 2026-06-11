package logisticspipes.routing.request;

import java.util.UUID;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import lombok.Getter;

/**
 * Attached to every routed item that belongs to a tracked {@link RequestJob}.
 * When the item arrives at the requester pipe, the pipe calls
 * {@link RequestJobManager#notifyItemTaken} through this reference so the job
 * can update its accounting and eventually reach
 * {@link RequestJobState#COMPLETED}.
 */
public class JobTargetInformation implements IAdditionalTargetInformation {

    @Getter
    private final UUID jobId;

    @Getter
    private final RequestJobManager jobManager;

    public JobTargetInformation(UUID jobId, RequestJobManager jobManager) {
        this.jobId = jobId;
        this.jobManager = jobManager;
    }
}