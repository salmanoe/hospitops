package id.co.hospitops.shared.event;

import java.time.Instant;

/**
 * R-07 FIX: Base class for all domain events.
 *
 * <p>Previously extended {@code ApplicationEvent}, which coupled the {@code shared}
 * module — a pure domain library — to Spring's application context. Since
 * Spring 4.2 {@code ApplicationEventPublisher.publishEvent(Object)} accepts any
 * POJO, the Spring dependency is not needed here.
 *
 * <p>Each event records the instant it occurred so consumers can reason about
 * ordering without relying on wall-clock time at the point of handling.
 */
public abstract class DomainEvent {

    private final Instant occurredOn;

    protected DomainEvent() {
        this.occurredOn = Instant.now();
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }
}
