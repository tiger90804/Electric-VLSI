/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellIdTest.java
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
package com.sun.electric.database;


import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;

import java.util.Arrays;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class CellIdTest {
    
    IdManager idManager;
    CellId cellId0;
    CellId cellId1;
    CellId cellId2;
    CellUsage u0_2;
    CellUsage u0_1;
    CellUsage u1_2;
    String nameA = "A";
    ExportId e1_A;
    
    @Before public void setUp() throws Exception {
        idManager = new IdManager();
        cellId0 = idManager.newCellId();
        cellId1 = idManager.newCellId();
        cellId2 = idManager.newCellId();
        u0_2 = cellId0.getUsageIn(cellId2);
        u0_1 = cellId0.getUsageIn(cellId1);
        u1_2 = cellId1.getUsageIn(cellId2);
        e1_A = cellId1.newExportId(nameA);
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(CellIdTest.class);
    }

    /**
     * Test of numUsagesIn method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNumUsagesIn() {
        System.out.println("numUsagesIn");
        
        int expResult = 2;
        int result = cellId0.numUsagesIn();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUsageIn method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetUsageIn() {
        System.out.println("getUsageIn");
        
        int i = 0;
        CellId instance = cellId0;
        
        CellUsage expResult = u0_2;
        CellUsage result = instance.getUsageIn(i);
        assertEquals(expResult, result);
    }

    /**
     * Test of numUsagesOf method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNumUsagesOf() {
        System.out.println("numUsagesOf");
        
        CellId instance = cellId2;
        
        int expResult = 2;
        int result = instance.numUsagesOf();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUsageOf method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetUsageOf() {
        System.out.println("getUsageOf");
        
        int i = 1;
        CellId instance = cellId2;
        
        CellUsage expResult = u1_2;
        CellUsage result = instance.getUsageOf(i);
        assertEquals(expResult, result);
    }

    /**
     * Test of findExportId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testFindExportId() {
        System.out.println("findExportId");
        
        String name = nameA;
        CellId instance = cellId1;
        
        ExportId expResult = e1_A;
        ExportId result = instance.findExportId(name);
        assertEquals(expResult, result);
        
        assertNull( instance.findExportId("B") );
        assertEquals( 1, cellId1.numExportIds() );
    }
    
//    /**
//     * Test of findExportId method, of class com.sun.electric.database.CellId.
//     */
//    public void testConcurrentFindExportId() {
//        System.out.println("findExportId concurrently");
//        
//        Thread writer = new Thread() {
//            public void run() {
//                for (;;) {
//                    cellId1.newExportId(nameA);
//                }
//            }
//        };
//        
//        writer.start();
//        
//        Name nameB = Name.findName("B");
//        for (;;) {
//            assertNull( cellId1.findExportId(nameB) );
//        }
//    }
    
    /**
     * Test of numExportIds method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNumExportIds() {
        System.out.println("numExportIds");
        
        CellId instance = cellId1;
        
        int expResult = 1;
        int result = instance.numExportIds();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPortId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetPortId() {
        System.out.println("getPortId");
        
        int chronIndex = 0;
        CellId instance = cellId1;
        
        ExportId expResult = e1_A;
        ExportId result = instance.getPortId(chronIndex);
        assertEquals(expResult, result);
    }

    /**
     * Test of getExportIds method, of class com.sun.electric.database.CellId.
     */
    @Test public void testGetExportIds() {
        System.out.println("getExportIds");
        
        CellId instance = cellId1;
        
        ExportId[] expResult = new ExportId[] { e1_A };
        ExportId[] result = instance.getExportIds();
        assertTrue(Arrays.equals(expResult, result));
    }

    /**
     * Test of newExportId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewExportId() {
        System.out.println("newExportId");
        
        String suggestedName = "B";
        CellId instance = cellId1;
        
        ExportId result = instance.newExportId(suggestedName);
        assertSame(cellId1, result.parentId);
        assertSame(suggestedName, result.externalId);
        assertEquals(1, result.chronIndex);
        
        assertEquals(2, cellId1.numExportIds());
        assertSame(e1_A, cellId1.getPortId(0));
        assertSame(result, cellId1.getPortId(1));
    }

    /**
     * Test of newExportId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testDuplicateExporId() {
        System.out.println("duplicateExportId");
        
        String suggestedName = "bus[1:2]";
        CellId instance = cellId1;
        
        ExportId result = instance.newExportId(suggestedName);
        ExportId result1 = instance.newExportId(suggestedName);
        assertSame(cellId1, result.parentId);
        assertEquals(1, result.chronIndex);
        assertSame(suggestedName, result.externalId);
        assertNotSame(suggestedName, result1.externalId);
    }

    /**
     * Test of newExportIds method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewExportIds() {
        System.out.println("newExportIds");
        
        String[] externalIds = { "C", "B"};
        cellId1.newExportIds(externalIds);
        
        assertEquals( 3, cellId1.numExportIds() );
        assertSame(e1_A, cellId1.getPortId(0));
        ExportId e1_C = cellId1.getPortId(1);
        ExportId e1_B = cellId1.getPortId(2);
        assertSame(externalIds[0], e1_C.externalId );
        assertSame(externalIds[1], e1_B.externalId );
    }

    /**
     * Test of newExportIds method, of class com.sun.electric.database.CellId.
     */
    @Test(expected = IllegalArgumentException.class) public void testDuplicateExportIds() {
        System.out.println("newExportIds");
        
        String[] externalIds = { "C", "C" };
        cellId1.newExportIds(externalIds);
    }

    /**
     * Test of newNodeId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewNodeId() {
        System.out.println("newNodeId");
        
        CellId instance = cellId2;
        
        int expResult = 0;
        int result = instance.newNodeId();
        assertEquals(expResult, result);
    }

    /**
     * Test of newArcId method, of class com.sun.electric.database.CellId.
     */
    @Test public void testNewArcId() {
        System.out.println("newArcId");
        
        CellId instance = cellId1;
        
        int expResult = 0;
        int result = instance.newArcId();
        assertEquals(expResult, result);
    }

    /**
     * Test of inDatabase method, of class com.sun.electric.database.CellId.
     */
    @Test public void testInDatabase() {
        System.out.println("inDatabase");
        
        EDatabase database = new EDatabase(idManager);
        CellId instance = cellId0;
        
        Cell expResult = null;
        Cell result = instance.inDatabase(database);
        assertEquals(expResult, result);
    }

    /**
     * Test of toString method, of class com.sun.electric.database.CellId.
     */
    @Test public void testToString() {
        System.out.println("toString");
        
        CellId instance = cellId0;
        
        String expResult = "CellId#0";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of check method, of class com.sun.electric.database.CellId.
     */
    @Test public void testCheck() {
        System.out.println("check");
        
        CellId instance = cellId1;
        
        instance.check();
    }
}
