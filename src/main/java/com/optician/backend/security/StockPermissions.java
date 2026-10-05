package com.optician.backend.security;

/**
 * Constantes et expressions SpEL pour la sécurité du module Stock.
 *
 * Utilisées avec @PreAuthorize dans les controllers.
 *
 * Authentification actuelle : session Spring Security (utilisateur local).
 * Compatibilité Keycloak prévue : les rôles Keycloak seront mappés
 * aux mêmes autorités (ROLE_ADMIN, ROLE_MANAGER, etc.).
 */
public final class StockPermissions {

    private StockPermissions() {}

    // Constantes de permission
    public static final String STOCK_VIEW     = "STOCK_VIEW";
    public static final String STOCK_ENTRY    = "STOCK_ENTRY";
    public static final String STOCK_EXIT     = "STOCK_EXIT";
    public static final String STOCK_TRANSFER = "STOCK_TRANSFER";
    public static final String STOCK_ADJUST   = "STOCK_ADJUST";
    public static final String STOCK_INVENTORY = "STOCK_INVENTORY";
    public static final String STOCK_EXPORT   = "STOCK_EXPORT";

    // Expressions SpEL pour @PreAuthorize
    public static final String HAS_VIEW_PERMISSION =
            "hasAnyAuthority('STOCK_VIEW','ROLE_OPTICIEN','ROLE_MANAGER','ROLE_ADMIN') or permitAll()";

    public static final String HAS_ENTRY_PERMISSION =
            "hasAnyAuthority('STOCK_ENTRY','ROLE_MANAGER','ROLE_ADMIN')";

    public static final String HAS_EXIT_PERMISSION =
            "hasAnyAuthority('STOCK_EXIT','ROLE_OPTICIEN','ROLE_MANAGER','ROLE_ADMIN')";

    public static final String HAS_TRANSFER_PERMISSION =
            "hasAnyAuthority('STOCK_TRANSFER','ROLE_MANAGER','ROLE_ADMIN')";

    public static final String HAS_ADJUST_PERMISSION =
            "hasAnyAuthority('STOCK_ADJUST','ROLE_MANAGER','ROLE_ADMIN')";

    public static final String HAS_INVENTORY_PERMISSION =
            "hasAnyAuthority('STOCK_INVENTORY','ROLE_MANAGER','ROLE_ADMIN')";

    public static final String HAS_EXPORT_PERMISSION =
            "hasAnyAuthority('STOCK_EXPORT','ROLE_MANAGER','ROLE_ADMIN')";
}
