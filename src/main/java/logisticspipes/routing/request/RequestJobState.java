package logisticspipes.routing.request;

/**
 * Represents the execution state of a {@link RequestJob} or a {@link SubRequestEntry}.
 */
public enum RequestJobState {
    /** The request has been created but crafting has not started yet. */
    PENDING,
    /** Ingredients are being gathered / items are in transit. */
    IN_PROGRESS,
    /** All required items have been produced and delivered. */
    COMPLETED,
    /** The request could not be fulfilled and has been cancelled. */
    FAILED
}
