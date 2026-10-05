package com.optician.backend.security;

public final class TryOnPermissions {

    private TryOnPermissions() {}

    public static final String TRY_ON_VIEW = "TRY_ON_VIEW";
    public static final String TRY_ON_CREATE = "TRY_ON_CREATE";
    public static final String TRY_ON_UPDATE = "TRY_ON_UPDATE";
    public static final String TRY_ON_VALIDATE = "TRY_ON_VALIDATE";
    public static final String TRY_ON_PUBLISH = "TRY_ON_PUBLISH";
    public static final String TRY_ON_DELETE = "TRY_ON_DELETE";

    public static final String HAS_VIEW_PERMISSION = "hasAnyAuthority('" + TRY_ON_VIEW + "', 'ROLE_CLIENT', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN') or permitAll()";
    public static final String HAS_CREATE_PERMISSION = "hasAnyAuthority('" + TRY_ON_CREATE + "', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_UPDATE_PERMISSION = "hasAnyAuthority('" + TRY_ON_UPDATE + "', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_VALIDATE_PERMISSION = "hasAnyAuthority('" + TRY_ON_VALIDATE + "', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_PUBLISH_PERMISSION = "hasAnyAuthority('" + TRY_ON_PUBLISH + "', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_DELETE_PERMISSION = "hasAnyAuthority('" + TRY_ON_DELETE + "', 'ROLE_MANAGER', 'ROLE_ADMIN')";
}
