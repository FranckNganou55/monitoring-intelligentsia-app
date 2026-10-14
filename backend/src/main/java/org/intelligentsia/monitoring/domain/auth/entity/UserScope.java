package org.intelligentsia.monitoring.domain.auth.entity;

public enum UserScope {
    /** Sees only own institution's data */
    INSTITUTION,
    /** Sees all institutions' data, no platform admin rights */
    GLOBAL,
    /** Full access — platform administration */
    ADMIN
}
