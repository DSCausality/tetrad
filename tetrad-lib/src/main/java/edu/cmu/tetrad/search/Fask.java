///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (c) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
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

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.data.Knowledge2;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.regression.RegressionDataset;
import edu.cmu.tetrad.regression.RegressionResult;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradLogger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static edu.cmu.tetrad.util.StatUtils.*;
import static java.lang.Math.*;

/**
 * Runs the FASK (Fast Adjacency Skewnmess) algorithm.
 *
 * @author Joseph Ramsey
 */
public final class Fask implements GraphSearch {

    // The score to be used for the FAS adjacency search.
    private final IndependenceTest test;

    // An initial graph to orient, skipping the adjacency step.
    private Graph initialGraph = null;

    // Elapsed time of the search, in milliseconds.
    private long elapsed = 0;

    // The data sets being analyzed. They must all have the same variables and the same
    // number of records.
    private final DataSet dataSet;

    // For the Fast Adjacency Search.
    private int depth = -1;

    // Knowledge the the search will obey, of forbidden and required edges.
    private IKnowledge knowledge = new Knowledge2();

    // A threshold for including extra adjacencies due to skewness. Default is 0 (no skew edges).
    private double skewEdgeThreshold = 0;

    // A theshold for making 2-cycles. Default is 0 (no 2-cycles.)
    private double twoCycleThreshold = 0;

    // True if FAS adjacencies should be included in the output.
    private boolean useFasAdjacencies = true;

    // Conditioned correlations are checked to make sure they are different from zero (since if they
    // are zero, the FASK theory doesn't apply).
    private double lr;
    private boolean linearityAssumed = true;

    /**
     * @param dataSet These datasets must all have the same variables, in the same order.
     */
    public Fask(DataSet dataSet, IndependenceTest test) {
        if (!dataSet.isContinuous()) {
            throw new IllegalArgumentException("For FASK, the dataset must be entirely continuous");
        }

        this.dataSet = dataSet;
        this.test = test;
    }

    public Fask(DataSet dataSet, Graph initialGraph) {
        if (!dataSet.isContinuous()) {
            throw new IllegalArgumentException("For FASK, the dataset must be entirely continuous");
        }

        this.dataSet = dataSet;
        this.initialGraph = initialGraph;
        this.test = null;

    }

    //======================================== PUBLIC METHODS ====================================//

    /**
     * Runs the search on the concatenated data, returning a graph, possibly cyclic, possibly with
     * two-cycles. Runs the fast adjacency search (FAS, Spirtes et al., 2000) follows by a modification
     * of the robust skew rule (Pairwise Likelihood Ratios for Estimation of Non-Gaussian Structural
     * Equation Models, Smith and Hyvarinen), together with some heuristics for orienting two-cycles.
     *
     * @return the graph. Some of the edges may be undirected (though it shouldn't be many in most cases)
     * and some of the adjacencies may be two-cycles.
     */
    public Graph search() {
        long start = System.currentTimeMillis();
        NumberFormat nf = new DecimalFormat("0.000");

        DataSet dataSet = DataUtils.standardizeData(this.dataSet);

        List<Node> variables = dataSet.getVariables();
        double[][] colData = dataSet.getDoubleData().transpose().toArray();

        TetradLogger.getInstance().forceLogMessage("FASK v. 2.0");
        TetradLogger.getInstance().forceLogMessage("");
        TetradLogger.getInstance().forceLogMessage("# variables = " + dataSet.getNumColumns());
        TetradLogger.getInstance().forceLogMessage("N = " + dataSet.getNumRows());
        TetradLogger.getInstance().forceLogMessage("Skewness edge threshold = " + skewEdgeThreshold);
        TetradLogger.getInstance().forceLogMessage("2-cycle threshold = " + twoCycleThreshold);
        TetradLogger.getInstance().forceLogMessage("");

        Graph G0;

        if (isUseFasAdjacencies()) {
            TetradLogger.getInstance().forceLogMessage("Running FAS-Stable, alpha = " + test.getAlpha());

            FasStable fas = new FasStable(test);
            fas.setDepth(getDepth());
            fas.setVerbose(false);
            fas.setKnowledge(knowledge);
            G0 = fas.search();
        } else if (getInitialGraph() != null) {
            TetradLogger.getInstance().forceLogMessage("Using initial graph.");

            Graph g1 = new EdgeListGraph(getInitialGraph().getNodes());

            for (Edge edge : getInitialGraph().getEdges()) {
                Node x = edge.getNode1();
                Node y = edge.getNode2();

                if (!g1.isAdjacentTo(x, y)) g1.addUndirectedEdge(x, y);
            }

            g1 = GraphUtils.replaceNodes(g1, dataSet.getVariables());

            G0 = g1;
        } else {
            G0 = new EdgeListGraph(dataSet.getVariables());
        }

        TetradLogger.getInstance().forceLogMessage("");

        SearchGraphUtils.pcOrientbk(knowledge, G0, G0.getNodes());

        Graph graph = new EdgeListGraph(G0.getNodes());

        TetradLogger.getInstance().forceLogMessage("X\tY\tMethod\tLR\tEdge");

        int V = variables.size();
        int method = linearityAssumed ? 8 : 11;

        double[] ee = new double[2 * (V * (V - 1) / 2)];
        int count = 0;

        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (i == j) continue;

                // Centered
                double[] x = colData[i];
                double[] y = colData[j];
                ee[count++] = cov(x, y, x)[method];
            }
        }

        double mean = 0;
        double sd = sd(ee);
        double zStar = StatUtils.getZForAlpha(skewEdgeThreshold);
        double thresh = zStar * sd / sqrt(count);

        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (i == j) continue;

                Node X = variables.get(i);
                Node Y = variables.get(j);

                if (graph.isAdjacentTo(X, Y)) continue;

                // Centered
                double[] x = colData[i];
                double[] y = colData[j];

                double c = cov(x, y, x)[method] / V;

                if ((isUseFasAdjacencies() && G0.isAdjacentTo(X, Y)) || (abs(c - mean) > thresh)) {
                    double lrxy = leftRight(x, y, method);

                    this.lr = lrxy;

                    if (edgeForbiddenByKnowledge(X, Y)) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tknowledge_forbidden"
                                + "\t" + nf.format(lrxy)
                                + "\t" + X + "<->" + Y
                        );
                        continue;
                    }

                    if (knowledgeOrients(X, Y)) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tknowledge"
                                + "\t" + nf.format(lrxy)
                                + "\t" + X + "-->" + Y
                        );
                        graph.addDirectedEdge(X, Y);
                    } else if (knowledgeOrients(Y, X)) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tknowledge"
                                + "\t" + nf.format(lrxy)
                                + "\t" + X + "<--" + Y
                        );
                        graph.addDirectedEdge(Y, X);
                    } else if (abs(lrxy) < twoCycleThreshold) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\t2-cycle"
                                + "\t" + nf.format(lrxy)
                                + "\t" + X + "<=>" + Y
                        );
                        graph.addDirectedEdge(X, Y);
                        graph.addDirectedEdge(Y, X);
                    } else {
                        if (lrxy > 0) {
                            TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tleft-right"
                                    + "\t" + nf.format(lrxy)
                                    + "\t" + X + "-->" + Y
                            );
                            graph.addDirectedEdge(X, Y);
                        } else {
                            TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tleft-right"
                                    + "\t" + nf.format(lrxy)
                                    + "\t" + X + "<--" + Y
                            );
                            graph.addDirectedEdge(Y, X);
                        }
                    }
                }
            }
        }

        if (!useFasAdjacencies) {
            double zStar2 = StatUtils.getZForAlpha(skewEdgeThreshold / (10));
            double thresh2 = zStar2 * sd / sqrt(count);

            for (int d = 1; d < 10; d++) {
                List<Edge> toRemove = new ArrayList<>();

                for (Edge edge : graph.getEdges()) {
                    Node X = edge.getNode1();
                    Node Y = edge.getNode2();

                    graph.removeEdge(edge);

                    if (graph.isAncestorOf(X, Y)) {
                        double[] x = colData[variables.indexOf(X)];
                        double[] y = colData[variables.indexOf(Y)];

                        double c = cov(x, y, x)[method] / V;

                        Node h = Edges.getDirectedEdgeHead(edge);

                        List<Node> par = graph.getParents(h);
                        int p = par.size();

                        if (p > 0 && p <= d) {
                            if (abs(c - mean) < thresh2) {
                                toRemove.add(edge);
                            }
                        }
                    }

                    graph.addEdge(edge);
                }

                for (Edge edge : toRemove) {
                    graph.removeEdge(edge);
                }
            }
        }

        long stop = System.currentTimeMillis();
        this.elapsed = stop - start;

        return graph;
    }

    private double leftRight(double[] x, double[] y, int method) {
        double skx = skewness(x);
        double sky = skewness(y);
        double r = correlation(x, y);

        // E(x, y | x > 0) - E(x, y | y > 0)
//        double lr = E(x, y, x) - E(x, y, y);
        double lr = cov(x, y, x)[method] - cov(x, y, y)[method];

        if (signum(skx) * signum(sky) * signum(r) < 0 && (signum(skx) > 0 == signum(sky) > 0)) {
            lr *= -1;
        }

        return lr;
    }

//    private double leftRightScaled(double[] x, double[] y) {
//        double skx = skewness(x);
//        double sky = skewness(y);
//        double r = correlation(x, y);
//
//        // E(x, y | x > 0) / sqrt(E(x, x | x > 0) E(x, x | y > 0))
//        //      - E(x, x | y > 0) / sqrt(E(y, y | x > 0) E(y, y | y > 0))
//        // Assumes linearity
//        double lr = cov(x, y, x)[8] - cov(x, y, y)[8];
//
//        if (signum(skx) * signum(sky) * signum(r) < 0 && (signum(skx) > 0 == signum(sky) > 0)) {
//            lr *= -1;
//        }
//
//        return lr;
//    }

//    private static double E(double[] x, double[] y, double[] condition) {
//        double exy = 0.0;
//
//        int n = 0;
//
//        for (int k = 0; k < x.length; k++) {
//            if (condition[k] > 0) {
//                exy += x[k] * y[k];
//                n++;
//            }
//        }
//
//        return exy / n;
//    }

//    private double robustSkew(double[] x, double[] y) {
////        if (true) {
////            x = correctSkewness(x, skewness(x));
////            y = correctSkewness(y, skewness(y));
////        }
//
//        double rho = correlation(x, y);
//
//        x = Arrays.copyOf(x, x.length);
//        y = Arrays.copyOf(y, y.length);
//
//        double[] xx = new double[x.length];
//
//        for (int i = 0; i < x.length; i++) {
//            if (Thread.currentThread().isInterrupted()) {
//                break;
//            }
//
//            double xi = x[i];
//            double yi = y[i];
//
//            double s1 = (g(xi) * yi) - (xi * g(yi));
//
//            xx[i] = s1;
//        }
//
//        double mxx = mean(xx);
//
//        return rho * mxx;
//    }

//    private double g(double x) {
//        return Math.log(Math.cosh(Math.max(x, 0)));
//    }

    /**
     * @return The depth of search for the Fast Adjacency Search (FAS).
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param depth The depth of search for the Fast Adjacency Search (S). The default is -1.
     *              unlimited. Making this too high may results in statistical errors.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * @return The elapsed time in milliseconds.
     */
    public long getElapsedTime() {
        return elapsed;
    }

    /**
     * @return the current knowledge.
     */
    public IKnowledge getKnowledge() {
        return knowledge;
    }

    /**
     * @param knowledge Knowledge of forbidden and required edges.
     */
    public void setKnowledge(IKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    public Graph getInitialGraph() {
        return initialGraph;
    }

    public void setInitialGraph(Graph initialGraph) {
        this.initialGraph = initialGraph;
    }

    public void setSkewEdgeThreshold(double skewEdgeThreshold) {
        this.skewEdgeThreshold = skewEdgeThreshold;
    }

    public boolean isUseFasAdjacencies() {
        return useFasAdjacencies;
    }

    public void setUseFasAdjacencies(boolean useFasAdjacencies) {
        this.useFasAdjacencies = useFasAdjacencies;
    }

    public void setTwoCycleThreshold(double twoCycleThreshold) {
        this.twoCycleThreshold = twoCycleThreshold;
    }

    public double getLr() {
        return lr;
    }

    public void setLinearityAssumed(boolean linearityAssumed) {
        this.linearityAssumed = linearityAssumed;
    }

    public enum RegressionType {LINEAR, NONLINEAR}

    /**
     * Calculates the residuals of y regressed nonparametrically onto y. Left public
     * so it can be accessed separately.
     * <p>
     * Here we want residuals of x regressed onto y. I'll tailor the method to that.
     *
     * @return the nonlinear residuals of y regressed onto x.
     */
    public static double[] residuals(final double[] y, final double[] x, RegressionType regressionType) {
        double[] residuals;

        if (regressionType == RegressionType.LINEAR) {
            RegressionResult result = RegressionDataset.regress(y, new double[][]{x});
            residuals = result.getResiduals().toArray();
        } else {
            int N = y.length;
            residuals = new double[N];
            double[] sum = new double[N];
            double[] totalWeight = new double[N];
            double h = h1(x);

            for (int j = 0; j < N; j++) {
                double yj = y[j];

                for (int i = 0; i < N; i++) {
                    double d = distance(x, i, j);
                    double k = kernelGaussian(d, h);
                    sum[i] += k * yj;
                    totalWeight[i] += k;
                }
            }

            for (int i = 0; i < N; i++) {
                residuals[i] = y[i] - sum[i] / totalWeight[i];
            }
        }

        return residuals;
    }

    //======================================== PRIVATE METHODS ====================================//

    private boolean knowledgeOrients(Node left, Node right) {
        return knowledge.isForbidden(right.getName(), left.getName()) || knowledge.isRequired(left.getName(), right.getName());
    }

    private boolean edgeForbiddenByKnowledge(Node left, Node right) {
        return knowledge.isForbidden(right.getName(), left.getName()) && knowledge.isForbidden(left.getName(), right.getName());
    }

    private static double h1(double[] xCol) {
        int N = xCol.length;
        double w;

        if (N < 200) {
            w = 0.8;
        } else if (N < 1200) {
            w = 0.5;
        } else {
            w = 0.3;
        }

        return w;
    }

    private static double distance(double[] data, int i, int j) {
        double sum = 0.0;

        double d = (data[i] - data[j]) / 2.0;

        if (!Double.isNaN(d)) {
            sum += d * d;
        }

        return sqrt(sum);
    }

    private static double kernelGaussian(double z, double h) {
        z /= 1 * h;
        return exp(-z * z);
    }

    private static double[] cov(double[] x, double[] y, double[] condition) {
        double exy = 0.0;
        double exx = 0.0;
        double eyy = 0.0;

        double ex = 0.0;
        double ey = 0.0;

        int n = 0;

        for (int k = 0; k < x.length; k++) {
            if (condition[k] > 0) {
                exy += x[k] * y[k];
                exx += x[k] * x[k];
                eyy += y[k] * y[k];
                ex += x[k];
                ey += y[k];
                n++;
            }
        }

        exy /= n;
        exx /= n;
        eyy /= n;
        ex /= n;
        ey /= n;

        double sxy = exy - ex * ey;
        double sx = exx - ex * ex;
        double sy = eyy - ey * ey;

        return new double[]{sxy, sxy / sqrt(sx * sy), sx, sy, (double) n, ex, ey, sxy / sx, exy / sqrt(exx * eyy), exx, eyy, exy};
    }

//    private double[] correctSkewness(double[] data, double sk) {
//        data = Arrays.copyOf(data, data.length);
//        double[] data2 = new double[data.length];
//        for (int i = 0; i < data.length; i++) data2[i] = data[i] * Math.signum(sk);
//        return data2;
//    }
}






