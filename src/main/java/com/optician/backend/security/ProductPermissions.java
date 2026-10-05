package com.optician.backend.security;

public final class ProductPermissions {

    private ProductPermissions() {}

    public static final String PRODUCT_VIEW = "PRODUCT_VIEW";
    public static final String PRODUCT_CREATE = "PRODUCT_CREATE";
    public static final String PRODUCT_UPDATE = "PRODUCT_UPDATE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";
    public static final String PRODUCT_EXPORT = "PRODUCT_EXPORT";

    // Expressive SpEL expressions for @PreAuthorize
    public static final String HAS_VIEW_PERMISSION = "hasAnyAuthority('" + PRODUCT_VIEW + "', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN') or permitAll()";
    public static final String HAS_CREATE_PERMISSION = "hasAnyAuthority('" + PRODUCT_CREATE + "', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_UPDATE_PERMISSION = "hasAnyAuthority('" + PRODUCT_UPDATE + "', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_DELETE_PERMISSION = "hasAnyAuthority('" + PRODUCT_DELETE + "', 'ROLE_MANAGER', 'ROLE_ADMIN')";
    public static final String HAS_EXPORT_PERMISSION = "hasAnyAuthority('" + PRODUCT_EXPORT + "', 'ROLE_OPTICIEN', 'ROLE_MANAGER', 'ROLE_ADMIN')";
}
