/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Route.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */

package com.sun.electric.tool.routing;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Specifies a route to be created.  Note that the order if items
 * in a route is meaningless.  The only thing that specifies order is the
 * start and end of the route.
 * <p>
 * Author: gainsley
 */

public class Route extends ArrayList {

    private RouteElementPort routeStart;       // start of route
    private RouteElementPort routeEnd;         // end of route

    // ---------------------- Constructors ---------------------------

    /** Constructs an empty route */
    public Route() {
        super();
        routeStart = null;
        routeEnd = null;
    }

    /** Constructs a route containing the elements of the passed route,
     * in the order they are returned by the route iterator, and having
     * the same start and end RouteElement (if Collection is a Route).
     */
    public Route(Collection c) {
        super(c);
        if (c instanceof Route) {
            Route r = (Route)c;
            routeStart = r.getStart();
            routeEnd = r.getEnd();
        } else {
            routeStart = null;
            routeEnd = null;
        }
    }

    /** Constructs an empty route with the specified initial capacity */
    public Route(int initialCapacity) {
        super(initialCapacity);
        routeStart = null;
        routeEnd = null;
    }


    // ------------------------------- Route Methods -----------------------------------


    /** Sets the start of the Route */
    public void setStart(RouteElementPort startRE) {
        if (!contains(startRE)) {
            add(startRE);
            //System.out.println("Route.setStart Error: argument not part of list");
            //return;
        }
        routeStart = startRE;
    }

    /** Get the start of the Route */
    public RouteElementPort getStart() { return routeStart; }

    /** Sets the end of the Route */
    public void setEnd(RouteElementPort endRE) {
        if (!contains(endRE)) {
            add(endRE);
            //System.out.println("Route.setEnd Error: argument not part of list");
            //return;
        }
        routeEnd = endRE;
    }

    /** Get the end of the Route */
    public RouteElementPort getEnd() { return routeEnd; }

    /**
     * Reverse the Route. This just swaps and the start and end
     * RouteElements, because the order of the list does not matter.
     */
    public void reverseRoute() {
        RouteElementPort re = routeStart;
        routeStart = routeEnd;
        routeEnd = re;
    }

}
