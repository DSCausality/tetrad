///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.test;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.RandomUtil;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class TestCellTable {
    private CellTable table;
    private final int[] dims = new int[]{2, 2, 2, 2};

    private final int[][] data = new int[][]{{1, 1, 1, 0}, {0, 0, 1, 0},
            {1, 1, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 0, 1, 0}, {1, 1, 1, 1}, {0, 1, 0, 0},
            {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 0}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 0},
            {0, 0, 0, 1}, {0, 0, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 1, 1}, {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 1, 0},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 1, 0, 0}, {0, 1, 0, 1},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 0}, {0, 1, 1, 0},
            {0, 0, 0, 1}, {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 1, 0}, {0, 0, 1, 0},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {1, 1, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 1, 1}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {0, 1, 1, 0}, {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 0},
            {0, 0, 0, 0}, {0, 1, 0, 0}, {0, 1, 0, 1}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 1, 1}, {0, 1, 1, 0},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 1, 1, 0},
            {0, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 1}, {0, 1, 0, 1},
            {0, 1, 0, 0}, {0, 1, 0, 0}, {0, 1, 0, 1}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 1, 0, 0}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 1}, {0, 1, 0, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 0, 1, 1},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {0, 0, 1, 1}, {0, 0, 0, 1}, {1, 1, 0, 0}, {0, 0, 1, 0},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 0}, {0, 1, 0, 0},
            {0, 0, 1, 0}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 0},
            {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0},
            {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {1, 1, 1, 0}, {0, 1, 1, 0}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 1, 1, 0},
            {0, 0, 1, 0}, {0, 1, 1, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 1, 0, 1}, {1, 0, 1, 0},
            {0, 0, 1, 1}, {0, 0, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 0},
            {1, 1, 0, 1}, {1, 1, 0, 1}, {1, 1, 0, 1}, {1, 1, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 1, 0}, {0, 1, 0, 0},
            {0, 0, 0, 1}, {0, 1, 1, 0}, {1, 1, 0, 1}, {1, 1, 1, 0},
            {0, 0, 0, 0}, {0, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 1, 0, 0},
            {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 0, 1, 1},
            {0, 0, 1, 0}, {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 0},
            {1, 1, 0, 0}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 1, 0},
            {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {1, 1, 0, 1}, {0, 1, 0, 1},
            {0, 0, 1, 0}, {1, 1, 1, 0}, {0, 0, 1, 1}, {0, 1, 0, 0},
            {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 0, 1, 1}, {0, 0, 1, 1},
            {1, 1, 1, 0}, {0, 1, 1, 0}, {0, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 1, 0, 1},
            {0, 0, 1, 0}, {0, 1, 0, 0}, {0, 0, 1, 1}, {0, 0, 0, 1},
            {0, 0, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 0}, {1, 1, 0, 1},
            {0, 1, 0, 1}, {0, 1, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 1, 1}, {1, 1, 1, 0},
            {0, 0, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 1, 0},
            {0, 0, 1, 0}, {0, 1, 0, 1}, {1, 0, 1, 1}, {0, 0, 1, 0},
            {0, 1, 0, 0}, {0, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 1, 1},
            {0, 1, 0, 1}, {1, 1, 0, 1}, {1, 1, 1, 0}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {1, 1, 1, 0}, {0, 1, 0, 1}, {1, 1, 1, 0},
            {0, 0, 1, 0}, {0, 0, 1, 1}, {0, 0, 1, 0}, {0, 0, 0, 1},
            {1, 1, 1, 0}, {0, 1, 1, 0}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 1, 1},
            {0, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {0, 0, 1, 1}, {0, 1, 1, 0}, {0, 1, 0, 0}, {0, 0, 0, 1},
            {0, 0, 1, 0}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 1, 0, 1},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 0, 1, 1}, {0, 1, 1, 0}, {1, 0, 1, 0},
            {0, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {1, 1, 0, 1}, {0, 0, 1, 0}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {1, 0, 1, 1}, {1, 1, 1, 0}, {1, 1, 1, 1},
            {1, 1, 1, 0}, {0, 1, 0, 0}, {0, 1, 1, 0}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 1},
            {0, 1, 0, 1}, {0, 1, 0, 0}, {0, 0, 0, 0}, {1, 1, 0, 0},
            {0, 0, 1, 1}, {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 0},
            {1, 1, 1, 0}, {0, 1, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 0, 1, 1}, {0, 1, 0, 1},
            {0, 0, 1, 0}, {1, 1, 0, 1}, {0, 1, 1, 0}, {0, 1, 0, 1},
            {0, 1, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 1, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {1, 1, 0, 1}, {0, 1, 0, 1},
            {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 1, 0, 0},
            {1, 1, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 0},
            {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 1, 0, 0}, {0, 0, 0, 0}, {0, 0, 1, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 1, 1},
            {0, 1, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {1, 0, 1, 1}, {0, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {0, 0, 1, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 0, 1}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 0, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 1, 0},
            {0, 0, 1, 1}, {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 1},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 1, 1, 1},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 1, 0},
            {0, 0, 0, 0}, {0, 1, 1, 0}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {0, 0, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1}, {1, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 1, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0},
            {0, 0, 1, 1}, {1, 1, 1, 0}, {0, 0, 1, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 1, 0, 1},
            {1, 1, 1, 0}, {0, 1, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {1, 1, 1, 1}, {0, 1, 0, 1},
            {0, 0, 1, 1}, {0, 1, 0, 0}, {0, 1, 1, 0}, {0, 0, 0, 0},
            {1, 1, 0, 1}, {1, 1, 1, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {1, 1, 0, 1}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 1, 1, 1}, {0, 1, 1, 0}, {0, 1, 0, 0},
            {0, 0, 0, 1}, {0, 1, 1, 0}, {0, 0, 0, 0}, {1, 1, 0, 1},
            {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 1, 1}, {0, 1, 0, 1},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 1, 0},
            {0, 1, 1, 1}, {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 1, 1},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 1, 0, 0},
            {0, 1, 0, 1}, {0, 1, 0, 0}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 1, 1, 0}, {0, 1, 0, 0},
            {0, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 1, 1, 0},
            {1, 1, 1, 0}, {1, 1, 1, 1}, {0, 1, 0, 1}, {0, 0, 1, 1},
            {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {0, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 1, 1, 0},
            {0, 1, 1, 0}, {0, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {0, 0, 1, 0}, {1, 1, 1, 0}, {1, 1, 0, 1}, {0, 0, 1, 1},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 0, 1}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 0},
            {0, 1, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {0, 0, 0, 0}, {0, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 1, 1},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 0, 0, 0},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 0, 1}, {0, 0, 1, 1},
            {0, 1, 0, 1}, {0, 0, 1, 1}, {1, 1, 0, 0}, {0, 1, 1, 0},
            {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 1, 1, 0}, {1, 1, 0, 1},
            {1, 1, 0, 0}, {0, 0, 0, 1}, {0, 1, 1, 0}, {0, 1, 1, 0},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 1, 0},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 0}, {1, 1, 1, 0},
            {0, 0, 0, 1}, {0, 1, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 1, 0}, {0, 1, 1, 0},
            {0, 0, 0, 0}, {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {1, 1, 0, 0}, {0, 0, 1, 0}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {0, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {1, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 1, 0, 1}, {1, 1, 0, 1},
            {0, 0, 1, 1}, {0, 1, 0, 1}, {0, 0, 1, 1}, {1, 1, 0, 1},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {0, 0, 0, 0}, {0, 1, 0, 1}, {1, 0, 1, 1},
            {0, 0, 0, 1}, {0, 0, 0, 0}, {1, 0, 1, 0}, {0, 0, 1, 1},
            {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1},
            {1, 1, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 1, 0}, {0, 1, 1, 0},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 1, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 1, 1}, {0, 1, 1, 0},
            {1, 0, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 1, 1},
            {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 1, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {1, 1, 0, 1}, {0, 0, 1, 1}, {0, 0, 0, 1},
            {1, 1, 1, 1}, {0, 0, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 1}, {1, 1, 0, 1}, {0, 1, 1, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 1, 1, 0}, {0, 1, 0, 1},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 1, 1}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1},
            {0, 1, 0, 0}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 1, 1, 0},
            {0, 0, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1}, {1, 1, 0, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 1, 0, 1},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {1, 1, 0, 0}, {0, 0, 1, 0},
            {1, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {0, 0, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 0},
            {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 1},
            {0, 1, 1, 1}, {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 1, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 0, 1, 1},
            {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 1},
            {0, 1, 0, 0}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 1, 1, 0}, {0, 0, 1, 1}, {0, 1, 0, 1}, {0, 1, 0, 1},
            {0, 1, 0, 0}, {0, 0, 0, 0}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {1, 0, 1, 1}, {1, 1, 1, 0}, {1, 0, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 0, 0, 1}, {0, 0, 1, 1}, {0, 1, 1, 0}, {0, 0, 1, 0},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {1, 0, 1, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 0},
            {1, 1, 1, 0}, {0, 0, 0, 1}, {0, 1, 0, 0}, {1, 1, 1, 0},
            {1, 1, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0}, {1, 1, 1, 0},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 1, 0, 0},
            {0, 0, 0, 1}, {1, 1, 1, 0}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {1, 1, 0, 1}, {0, 1, 0, 1}, {0, 1, 0, 1}, {0, 0, 0, 1},
            {0, 1, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 1, 1, 0}, {0, 0, 1, 0}, {0, 0, 0, 1},
            {0, 0, 1, 1}, {0, 0, 1, 1}, {0, 1, 1, 0}, {0, 1, 0, 1},
            {0, 1, 1, 0}, {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 1, 0, 1},
            {0, 0, 1, 0}, {0, 1, 0, 1}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 0}, {0, 1, 0, 1}, {0, 0, 1, 0},
            {1, 1, 1, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 0, 0, 1},
            {0, 1, 0, 1}, {0, 0, 1, 1}, {0, 1, 0, 0}, {0, 1, 0, 0},
            {0, 0, 1, 1}, {0, 0, 0, 1}, {0, 0, 0, 1}, {0, 1, 0, 0},
            {0, 0, 1, 0}, {0, 0, 0, 1}, {0, 0, 0, 0}, {1, 0, 0, 1},
            {0, 0, 1, 1}, {0, 1, 1, 0}, {0, 0, 1, 1}, {0, 0, 0, 0},
            {1, 1, 1, 0}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 1, 0}, {0, 1, 0, 1},
            {0, 0, 1, 0}, {0, 0, 0, 0}, {1, 1, 1, 0}, {0, 0, 0, 0},
            {0, 0, 0, 1}, {0, 1, 1, 0}, {0, 0, 1, 1}, {1, 1, 1, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 1, 0, 1},
            {0, 1, 0, 0}, {0, 0, 0, 1}, {0, 0, 0, 1}, {1, 1, 1, 0},
            {0, 0, 1, 0}, {0, 1, 0, 1}, {1, 1, 1, 0}, {0, 1, 0, 1},
            {0, 1, 0, 1}, {0, 0, 0, 1}};

    public final void setUp() {

        this.table = new CellTable(dims);

//        // Add data to table.
        List<Node> variables = new LinkedList<>();
        variables.add(new DiscreteVariable("X1", 2));
        variables.add(new DiscreteVariable("X2", 2));
        variables.add(new DiscreteVariable("X3", 2));
        variables.add(new DiscreteVariable("X4", 2));

        DataSet dataSet = new BoxDataSet(new DoubleDataBox(data.length, variables.size()), variables);

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                dataSet.setInt(i, j, data[i][j]);
            }
        }

        int[] indices = new int[]{0, 1, 2, 3};

        this.table.addToTable(dataSet, indices);
    }

    @Test
    public final void testCount() {
        setUp();

        // Pick 8 random cells, count those cells, test the counts in
        // the cell count.
        int[] testCell;

        for (int c = 0; c < 8; c++) {
            testCell = pickRandomCell(4);

            int myCount = 0;

            for (int[] aData : data) {
                boolean inCell = true;

                for (int j = 0; j < data[0].length; j++) {
                    if (aData[j] != testCell[j]) {
                        inCell = false;
                    }
                }

                if (inCell) {
                    myCount++;
                }
            }

            assertEquals(myCount, this.table.getValue(testCell));
        }
    }

    @Test
    public final void testMargins() {
        setUp();

        // Test 15 margin calculations.
        for (int m = 0; m < 15; m++) {

            // Pick a random cell. Keep this cell, but make (a) an
            // array of variable indices to marginalize and (b) a
            // "wildcard" version of this cell with those indices
            // replaced by -1.
            int[] cell = pickRandomCell(4);

            // The indices to marginalize. (No repeats.)
            int numMargin = RandomUtil.getInstance().nextInt(4);
            int[] marginVars = new int[numMargin];

            for (int i = 0; i < numMargin; i++) {
                marginVars[i] = RandomUtil.getInstance().nextInt(4);

                for (int j = 0; j < i; j++) {
                    if (marginVars[j] == marginVars[i]) {
                        i--;
                    }
                }
            }

            // The wildcard version of the test cell.
            int[] testCell = new int[4];

            System.arraycopy(cell, 0, testCell, 0, 4);

            for (int i = 0; i < numMargin; i++) {
                testCell[marginVars[i]] = -1;
            }

            // Count the data, using the -1 as a wildcard.
            int myCount = 0;

            for (int[] aData : data) {
                boolean inMargin = true;

                for (int j = 0; j < data[0].length; j++) {
                    if (testCell[j] == -1) {
                        // Do nothing.
                    }
                    else if (aData[j] != testCell[j]) {
                        inMargin = false;
                    }
                }

                if (inMargin) {
                    myCount++;
                }
            }

            // Test using both the wildcard and the variable indices
            // version of the calcMargin method.
            assertEquals(myCount, this.table.calcMargin(testCell));
            assertEquals(myCount, this.table.calcMargin(cell, marginVars));
        }
    }

    private static int[] pickRandomCell(int size) {

        int[] cell = new int[size];

        for (int i = 0; i < size; i++) {
            cell[i] = RandomUtil.getInstance().nextInt(2);
        }

        return cell;
    }
}




