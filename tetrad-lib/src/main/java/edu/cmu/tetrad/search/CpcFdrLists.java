///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014 by Peter Spirtes, Richard Scheines, Joseph   //
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

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.data.Knowledge2;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.ChoiceGenerator;
import edu.cmu.tetrad.util.DepthChoiceGenerator;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradLogger;

import java.io.PrintStream;
import java.util.*;

/**
 * Implements the CPC-FdrLists search.
 *
 * @author Joseph Ramsey.
 */
public class CpcFdrLists implements IFas {

    /**
     * The search graph. It is assumed going in that all of the true adjacencies of x are in this graph for every node
     * x. It is hoped (i.e. true in the large sample limit) that true adjacencies are never removed.
     */
    private Graph graph;

    /**
     * The search nodes.
     */
    private List<Node> nodes;

    /**
     * The independence test. This should be appropriate to the types
     */
    private IndependenceTest test;

    /**
     * Specification of which edges are forbidden or required.
     */
    private IKnowledge knowledge = new Knowledge2();

    /**
     * The maximum number of variables conditioned on in any conditional independence test. If the depth is -1, it will
     * be taken to be the maximum value, which is 1000. Otherwise, it should be set to a non-negative integer.
     */
    private int depth = 1000;

    /**
     * The logger, by default the empty logger.
     */
    private TetradLogger logger = TetradLogger.getInstance();

    /**
     * The sepsets found during the search.
     */
    private SepsetMap sepset = new SepsetMap();

    /**
     * True iff verbose output should be printed.
     */
    private boolean verbose = false;

    /**
     * Verbose output is sent here.
     */
    private PrintStream out = System.out;

    /**
     * True if the Meek rules should prevent cycles from being oriented.
     */
    private boolean isAggressivelyPreventCycles = true;

    /**
     * The elapsed time in milliseconds.
     */
    private long elapsedtime = 0;

    /**
     * The FDR q to use for the orientation search.
     */
    private double fdrQ = 0.05;


    //==========================CONSTRUCTORS=============================//

    public CpcFdrLists(IndependenceTest test) {
        this.test = test;
        this.nodes = test.getVariables();
    }

    //==========================PUBLIC METHODS===========================//

    /**
     * Discovers all adjacencies in data.  The procedure is to remove edges in the graph which connect pairs of
     * variables which are independent conditional on some other set of variables in the graph (the "sepset"). These are
     * removed in tiers.  First, edges which are independent conditional on zero other variables are removed, then edges
     * which are independent conditional on one other variable are removed, then two, then three, and so on, until no
     * more edges can be removed from the graph.  The edges which remain in the graph after this procedure are the
     * adjacencies in the data.
     *
     * @return a SepSet, which indicates which variables are independent conditional on which other variables
     */
    public Graph search() {
        this.logger.log("info", "Starting Fast Adjacency Search.");
        return mainLoop();
    }

    public void setDepth(int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException("Depth must be -1 (unlimited) or >= 0.");
        }

        if (depth == -1) depth = 1000;
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public IKnowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(IKnowledge knowledge) {
        if (knowledge == null) {
            throw new NullPointerException("Cannot set knowledge to null");
        }
        this.knowledge = knowledge;
    }

    public int getNumIndependenceTests() {
        return 0;
    }

    public void setTrueGraph(Graph trueGraph) {
        throw new UnsupportedOperationException("The true graph is not used in this class.");
    }

    public int getNumFalseDependenceJudgments() {
        return 0;
    }

    public int getNumDependenceJudgments() {
        return 0;
    }

    public SepsetMap getSepsets() {
        return sepset;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isAggressivelyPreventCycles() {
        return isAggressivelyPreventCycles;
    }

    @Override
    public void setAggressivelyPreventCycles(boolean aggressivelyPreventCycles) {
        this.isAggressivelyPreventCycles = aggressivelyPreventCycles;
    }

    @Override
    public IndependenceTest getIndependenceTest() {
        return test;
    }

    @Override
    public Graph search(List<Node> nodes) {
        return null;
    }

    @Override
    public long getElapsedTime() {
        return elapsedtime;
    }

    @Override
    public List<Node> getNodes() {
        return test.getVariables();
    }

    @Override
    public List<Triple> getAmbiguousTriples(Node node) {
        return null;
    }

    @Override
    public void setOut(PrintStream out) {
        this.out = out;
    }

    //==============================PRIVATE METHODS======================//

    private Graph mainLoop() {
        long start = System.currentTimeMillis();

        sepset = new SepsetMap();

        graph = new EdgeListGraph(nodes);
        graph = GraphUtils.completeGraph(graph);

        findAdjacencies();

        orientTriples(depth, graph);

        MeekRules meekRules = new MeekRules();
        meekRules.setKnowledge(knowledge);
        meekRules.setAggressivelyPreventCycles(isAggressivelyPreventCycles());
        meekRules.orientImplied(graph);

        long stop = System.currentTimeMillis();
        this.elapsedtime = stop - start;

        return graph;
    }

    /**
     * The FDR q to use for the orientation search.
     */
    public double getFdrQ() {
        return fdrQ;
    }

    /**
     * The FDR q to use for the orientation search.
     */
    public void setFdrQ(double fdrQ) {
        if (fdrQ < 0 || fdrQ > 1) {
            throw new IllegalArgumentException("FDR q must be in [0, 1]: " + fdrQ);
        }

        this.fdrQ = fdrQ;
    }

    private static class PValue {
        private double p;
        private Set<Node> sepset;

        PValue(double p, List<Node> sepset) {
            this.p = p;
            this.sepset = new HashSet<>(sepset);
        }

        public Set<Node> getSepset() {
            return sepset;
        }

        public double getP() {
            return p;
        }

        public boolean equals(Object o) {
            if (!(o instanceof PValue)) return false;
            PValue _o = (PValue) o;
            return _o.getP() == getP() && _o.getSepset().equals(getSepset());
        }
    }

    private void findAdjacencies() {
        FasStable fas = new FasStable(test);
        this.graph = fas.search();
    }

    private void orientTriples(int depth, Graph graph) {
        List<Node> nodes1 = graph.getNodes();

        for (Node b : nodes1) {
            List<Node> adjacentNodes = graph.getAdjacentNodes(b);

            if (adjacentNodes.size() < 2) {
                continue;
            }

            ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                Node a = adjacentNodes.get(combination[0]);
                Node c = adjacentNodes.get(combination[1]);

                if (graph.isAdjacentTo(a, c)) {
                    continue;
                }

                if (verbose) {
                    out.println("Calculating max avg for " + a + " --- " + b + " --- " + c + " depth = " + depth);
                }

                List<PValue> pValues = getAllPValues(a, c, depth, this.graph, test);

                List<PValue> bPvals = new ArrayList<>();
                List<PValue> notbPvals = new ArrayList<>();

                for (PValue p : pValues) {
                    if (p.getSepset().contains(b)) {
                        bPvals.add(p);
                    } else {
                        notbPvals.add(p);
                    }
                }

                boolean existsb = existsSepsetFromList(bPvals, getFdrQ());
                boolean existsnotb = existsSepsetFromList(notbPvals, getFdrQ());

                if (existsb && !existsnotb) {
                    graph.addUnderlineTriple(a, b, c);
                } else if (!existsb && existsnotb) {
                    graph.removeEdge(a, b);
                    graph.removeEdge(c, b);
                    graph.addDirectedEdge(a, b);
                    graph.addDirectedEdge(c, b);
                } else {
                    graph.addAmbiguousTriple(a, b, c);
                }
            }
        }
    }

    private List<PValue> getAllPValues(Node a, Node c, int depth, Graph graph, IndependenceTest test) {
        List<Node> adja = graph.getAdjacentNodes(a);
        List<Node> adjc = graph.getAdjacentNodes(c);

        adja.remove(c);
        adjc.remove(a);

        List<List<Node>> adj = new ArrayList<>();
        adj.add(adja);
        adj.add(adjc);

        List<PValue> pValues = new ArrayList<>();

        for (List<Node> _adj : adj) {
            DepthChoiceGenerator cg1 = new DepthChoiceGenerator(_adj.size(), depth);
            int[] comb2;

            while ((comb2 = cg1.next()) != null) {
                List<Node> s = GraphUtils.asList(comb2, _adj);

                test.isIndependent(a, c, s);
                PValue _p = new PValue(test.getPValue(), s);
                if (pValues.contains(_p)) continue;
                pValues.add(_p);
            }
        }

        return pValues;
    }

    private boolean existsSepsetFromList(List<PValue> pValues, double alpha) {
        if (pValues.isEmpty()) return false;

        List<Double> _pValues = new ArrayList<>();

        for (PValue p : pValues) {
            _pValues.add(p.getP());
        }

        double cutoff = StatUtils.fdrCutoff(alpha, _pValues, false, false);

        for (double p : _pValues) {
            if (p > cutoff) return true;
        }

        return false;
    }
}
