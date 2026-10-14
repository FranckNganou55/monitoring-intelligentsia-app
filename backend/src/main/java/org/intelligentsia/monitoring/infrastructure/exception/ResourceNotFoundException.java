package org.intelligentsia.monitoring.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource.toUpperCase() + "_NOT_FOUND",
              resource + " avec l'identifiant '" + id + "' introuvable.",
              HttpStatus.NOT_FOUND);
    }
}
