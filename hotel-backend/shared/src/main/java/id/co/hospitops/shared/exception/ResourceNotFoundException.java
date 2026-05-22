package id.co.hospitops.shared.exception;

import java.util.UUID;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " not found: " + id);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
