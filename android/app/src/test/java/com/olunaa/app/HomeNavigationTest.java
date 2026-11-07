package com.olunaa.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeNavigationTest {
    @Test public void exposesAllPrimaryDestinationsInStableOrder() {
        assertArrayEquals(new String[]{"Chats","Updates","Communities","Calls","Settings"}, HomeNavigation.routes());
    }

    @Test public void mapsProductLabelsToExistingBackendRoutes() {
        assertEquals("Updates", HomeNavigation.routeForLabel("Stories"));
        assertEquals("Communities", HomeNavigation.routeForLabel("Groups"));
        assertEquals("Stories", HomeNavigation.labelForRoute("Updates"));
        assertEquals("Groups", HomeNavigation.labelForRoute("Communities"));
    }

    @Test public void preservesUnchangedDestinations() {
        for (String route : HomeNavigation.routes()) {
            assertEquals(route, HomeNavigation.routeForLabel(HomeNavigation.labelForRoute(route)));
        }
    }

    @Test public void returnsDefensiveRouteArray() {
        String[] routes = HomeNavigation.routes();
        routes[0] = "Corrupted";
        assertEquals("Chats", HomeNavigation.routes()[0]);
    }

    @Test public void unknownLabelsDoNotInventRoutes() {
        assertEquals("", HomeNavigation.routeForLabel("Made up"));
        assertEquals("", HomeNavigation.routeForLabel(null));
    }
}
