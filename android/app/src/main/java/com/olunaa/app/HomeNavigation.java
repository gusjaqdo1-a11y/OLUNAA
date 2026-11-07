package com.olunaa.app;

/** Canonical destinations for the primary OLUNAA navigation.
 * UI labels may evolve independently from the existing API route names.
 */
public final class HomeNavigation {
    private HomeNavigation() {}

    private static final String[] ROUTES = {
        "Chats", "Updates", "Communities", "Calls", "Settings"
    };

    public static String[] routes() { return ROUTES.clone(); }

    public static String labelForRoute(String route) {
        if ("Updates".equals(route)) return "Stories";
        if ("Communities".equals(route)) return "Groups";
        return route == null ? "" : route;
    }

    public static String routeForLabel(String label) {
        if ("Stories".equals(label)) return "Updates";
        if ("Groups".equals(label)) return "Communities";
        for (String route : ROUTES) {
            if (route.equals(label)) return route;
        }
        return "";
    }
}
