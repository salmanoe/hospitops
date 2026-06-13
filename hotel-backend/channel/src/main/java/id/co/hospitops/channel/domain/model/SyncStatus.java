package id.co.hospitops.channel.domain.model;

/**
 * Lifecycle of an outbox message.
 *
 * <ul>
 *   <li>{@code PENDING}  — waiting to be sent (or to be retried after a backoff).</li>
 *   <li>{@code SENT}     — delivered to the provider successfully.</li>
 *   <li>{@code FAILED}   — gave up after the maximum number of attempts (dead letter).</li>
 * </ul>
 */
public enum SyncStatus {
    PENDING,
    SENT,
    FAILED
}
