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

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetrad.util.TetradMatrix;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.SingularMatrixException;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static edu.cmu.tetrad.util.MathUtils.logChoose;
import static edu.cmu.tetrad.util.StatUtils.getZForAlpha;
import static java.lang.Math.*;

/**
 * Checks conditional independence of variable in a continuous data set using Fisher's Z test. See Spirtes, Glymour, and
 * Scheines, "Causation, Prediction and Search," 2nd edition, page 94.
 *
 * @author Joseph Ramsey
 * @author Frank Wimberly adapted IndTestCramerT for Fisher's Z
 */
public final class IndTestFisherZSkew implements IndependenceTest {

    /**
     * The covariance matrix.
     */
    private final ICovarianceMatrix covMatrix;

    /**
     * The variables of the covariance matrix, in order. (Unmodifiable list.)
     */
    private List<Node> variables;

    /**
     * The significance level of the independence tests.
     */
    private double alpha;

    /**
     * Stores a reference to the dataset being analyzed.
     */
    private DataSet dataSet;

    private Map<Node, Integer> indexMap;
    private Map<String, Node> nameMap;
    private boolean verbose = true;
    private double fisherZ = Double.NaN;
    private double cutoff = Double.NaN;
    private NormalDistribution normal = new NormalDistribution(0, 1);
    private boolean fastFDR = false;
    // Data as a double[][].
    private final double[][] data;

    //==========================CONSTRUCTORS=============================//

    /**
     * Constructs a new Independence test which checks independence facts based on the correlation matrix implied by the
     * given data set (must be continuous). The given significance level is used.
     *
     * @param dataSet A data set containing only continuous columns.
     * @param alpha   The alpha level of the test.
     */
    public IndTestFisherZSkew(DataSet dataSet, double alpha) {
        if (!(dataSet.isContinuous())) {
            throw new IllegalArgumentException("Data set must be continuous.");
        }

        if (!(alpha >= 0 && alpha <= 1)) {
            throw new IllegalArgumentException("Alpha mut be in [0, 1]");
        }

        this.covMatrix = new CovarianceMatrixOnTheFly(dataSet);
        List<Node> nodes = covMatrix.getVariables();

        this.variables = Collections.unmodifiableList(nodes);
        this.indexMap = indexMap(variables);
        this.nameMap = nameMap(variables);
        setAlpha(alpha);

        this.dataSet = DataUtils.standardizeData(dataSet);
        data = this.dataSet.getDoubleData().transpose().toArray();
    }

    /**
     * Constructs a new Fisher Z independence test with  the listed arguments.
     *
     * @param data      A 2D continuous data set with no missing values.
     * @param variables A list of variables, a subset of the variables of <code>data</code>.
     * @param alpha     The significance cutoff level. p values less than alpha will be reported as dependent.
     */
    public IndTestFisherZSkew(TetradMatrix data, List<Node> variables, double alpha) {
        this.dataSet = ColtDataSet.makeContinuousData(variables, data);
        this.covMatrix = new CovarianceMatrixOnTheFly(dataSet);
        this.variables = Collections.unmodifiableList(variables);
        this.indexMap = indexMap(variables);
        this.nameMap = nameMap(variables);
        setAlpha(alpha);
        this.dataSet = DataUtils.standardizeData(this.dataSet);
        this.data = this.dataSet.getDoubleData().transpose().toArray();
    }

    /**
     * Constructs a new independence test that will determine conditional independence facts using the given correlation
     * matrix and the given significance level.
     */
    public IndTestFisherZSkew(ICovarianceMatrix covMatrix, double alpha) {
        this.covMatrix = covMatrix;
        this.variables = covMatrix.getVariables();
        this.indexMap = indexMap(variables);
        this.nameMap = nameMap(variables);
        setAlpha(alpha);
        this.dataSet = DataUtils.standardizeData(this.dataSet);
        this.data = this.dataSet.getDoubleData().transpose().toArray();

    }

    //==========================PUBLIC METHODS=============================//

    /**
     * Creates a new independence test instance for a subset of the variables.
     */
    public IndependenceTest indTestSubset(List<Node> vars) {
        if (vars.isEmpty()) {
            throw new IllegalArgumentException("Subset may not be empty.");
        }

        for (Node var : vars) {
            if (!variables.contains(var)) {
                throw new IllegalArgumentException(
                        "All vars must be original vars");
            }
        }

        int[] indices = new int[vars.size()];

        for (int i = 0; i < indices.length; i++) {
            indices[i] = indexMap.get(vars.get(i));
        }

        ICovarianceMatrix newCovMatrix = covMatrix.getSubmatrix(indices);

        double alphaNew = getAlpha();
        return new IndTestFisherZSkew(newCovMatrix, alphaNew);
    }

    /**
     * Determines whether variable x is independent of variable y given a list of conditioning variables z.
     *
     * @param x the one variable being compared.
     * @param y the second variable being compared.
     * @param z the list of conditioning variables.
     * @return true iff x _||_ y | z.
     * @throws RuntimeException if a matrix singularity is encountered.
     */
    public boolean isIndependent(Node x, Node y, List<Node> z) {
        int n = sampleSize();
        double r;

        try {
            r = partialCorrelation(x, y, z);
        } catch (SingularMatrixException e) {
            System.out.println(SearchLogUtils.determinismDetected(z, x));
            this.fisherZ = Double.POSITIVE_INFINITY;
            return false;
        }

        double fisherZ = Math.sqrt(n - 3 - z.size()) * 0.5 * (Math.log(1.0 + r) - Math.log(1.0 - r));
        this.fisherZ = fisherZ;

        // Centered
        final double[] _x = data[variables.indexOf(x)];
        final double[] _y = data[variables.indexOf(y)];

        double[][] _Z = new double[z.size()][];

        for (int f = 0; f < z.size(); f++) {
            Node _z = z.get(f);
            int column = dataSet.getColumn(_z);
            _Z[f] = data[column];
        }

        double pc1 = partialCorrelation(_x, _y, _Z, _x, 0, +1);
        double pc2 = partialCorrelation(_x, _y, _Z, _y, 0, +1);

        int nc1 = StatUtils.getRows(_x, _x, 0, +1).size();
        int nc2 = StatUtils.getRows(_y, _y, 0, +1).size();

        double z1 = 0.5 * (log(1.0 + pc1) - log(1.0 - pc1));
        double z2 = 0.5 * (log(1.0 + pc2) - log(1.0 - pc2));

        double zv1 = (z1 - z2) / sqrt((1.0 / ((double) nc1 - 3) + 1.0 / ((double) nc2 - 3)));

//        if(fastFDR) {
//            final int d1 = 0; // reference
//            final int d2 = z.size();
//            final int v = variables.size() - 2;
//
//            double alpha2 = (exp(log(alpha) + logChoose(v, d1) - logChoose(v, d2)));
//            cutoff = getZForAlpha(alpha2);
//        }

//        System.out.println(zv1);

        double cutoff = 0.05;

        if (abs(z1) < cutoff && abs(z2) < cutoff) {
            return true;
        }
//        else if (abs(fisherZ) < this.cutoff) {
//            return true;
//        }
        else {
            return false;
        }

//        return abs(zv1) < .5;

//        return Math.max(abs(zv1),  abs(fisherZ)) < .1;
    }

    private double partialCorrelation(Node x, Node y, List<Node> z) throws SingularMatrixException {
        if (z.isEmpty()) {
            double a = covMatrix.getValue(indexMap.get(x), indexMap.get(y));
            double b = covMatrix.getValue(indexMap.get(x), indexMap.get(x));
            double c = covMatrix.getValue(indexMap.get(y), indexMap.get(y));

            if (b * c == 0) throw new SingularMatrixException();

            return -a / Math.sqrt(b * c);
        } else {
            int[] indices = new int[z.size() + 2];
            indices[0] = indexMap.get(x);
            indices[1] = indexMap.get(y);
            for (int i = 0; i < z.size(); i++) indices[i + 2] = indexMap.get(z.get(i));
            TetradMatrix submatrix = covMatrix.getSubmatrix(indices).getMatrix();
            return StatUtils.partialCorrelation(submatrix);
        }
    }

    public boolean isIndependent(Node x, Node y, Node... z) {
        return isIndependent(x, y, Arrays.asList(z));
    }

    public boolean isDependent(Node x, Node y, List<Node> z) {
        return !isIndependent(x, y, z);
    }

    public boolean isDependent(Node x, Node y, Node... z) {
        List<Node> zList = Arrays.asList(z);
        return isDependent(x, y, zList);
    }

    /**
     * @return the probability associated with the most recently computed independence test.
     */
    public double getPValue() {
        return 2.0 * (1.0 - normal.cumulativeProbability(abs(fisherZ)));
    }

    /**
     * Sets the significance level at which independence judgments should be made.  Affects the cutoff for partial
     * correlations to be considered statistically equal to zero.
     */
    public void setAlpha(double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Significance out of range: " + alpha);
        }

        this.alpha = alpha;
        this.cutoff = StatUtils.getZForAlpha(alpha);
    }

    /**
     * Gets the getModel significance level.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * @return the list of variables over which this independence checker is capable of determinine independence
     * relations-- that is, all the variables in the given graph or the given data set.
     */
    public List<Node> getVariables() {
        return this.variables;
    }

    /**
     * @return the variable with the given name.
     */
    public Node getVariable(String name) {
        return nameMap.get(name);
    }

    /**
     * @return the list of variable varNames.
     */
    public List<String> getVariableNames() {
        List<Node> variables = getVariables();
        List<String> variableNames = new ArrayList<>();
        for (Node variable1 : variables) {
            variableNames.add(variable1.getName());
        }
        return variableNames;
    }

    /**
     * If <code>isDeterminismAllowed()</code>, deters to IndTestFisherZD; otherwise throws
     * UnsupportedOperationException.
     */
    public boolean determines(List<Node> z, Node x) throws UnsupportedOperationException {
        int[] parents = new int[z.size()];

        for (int j = 0; j < parents.length; j++) {
            parents[j] = covMatrix.getVariables().indexOf(z.get(j));
        }

        if (parents.length > 0) {

            // Regress z onto i, yielding regression coefficients b.
            TetradMatrix Czz = covMatrix.getSelection(parents, parents);

            try {
                Czz.inverse();
            } catch (SingularMatrixException e) {
                System.out.println(SearchLogUtils.determinismDetected(z, x));
                return true;
            }
        }

        return false;
    }

    /**
     * @return the data set being analyzed.
     */
    public DataSet getData() {
        return dataSet;
    }

    /**
     * @return a string representation of this test.
     */
    public String toString() {
        return "Fisher Z, alpha = " + new DecimalFormat("0.0E0").format(getAlpha());
    }

    //==========================PRIVATE METHODS============================//

    private int sampleSize() {
        return covMatrix().getSampleSize();
    }

    private ICovarianceMatrix covMatrix() {
        return covMatrix;
    }

    private Map<String, Node> nameMap(List<Node> variables) {
        Map<String, Node> nameMap = new ConcurrentHashMap<>();

        for (Node node : variables) {
            nameMap.put(node.getName(), node);
        }

        return nameMap;
    }

    private Map<Node, Integer> indexMap(List<Node> variables) {
        Map<Node, Integer> indexMap = new ConcurrentHashMap<>();

        for (int i = 0; i < variables.size(); i++) {
            indexMap.put(variables.get(i), i);
        }

        return indexMap;
    }

    public void setVariables(List<Node> variables) {
        if (variables.size() != this.variables.size()) throw new IllegalArgumentException("Wrong # of variables.");
        this.variables = new ArrayList<>(variables);
        covMatrix.setVariables(variables);
    }

    public ICovarianceMatrix getCov() {
        return covMatrix;
    }

    @Override
    public List<DataSet> getDataSets() {

        List<DataSet> dataSets = new ArrayList<>();

        dataSets.add(dataSet);

        return dataSets;
    }

    @Override
    public int getSampleSize() {
        return covMatrix.getSampleSize();
    }

    @Override
    public List<TetradMatrix> getCovMatrices() {
        return null;
    }

    @Override
    public double getScore() {
        return Math.abs(fisherZ) - cutoff;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setFastFDR(boolean fastFDR) {
        this.fastFDR = fastFDR;
    }

    private double partialCorrelation(double[] x, double[] y, double[][] z, double[] condition, double threshold, double direction) throws SingularMatrixException {
        double[][] cv = StatUtils.covMatrix(x, y, z, condition, threshold, direction);
        TetradMatrix m = new TetradMatrix(cv).transpose();
        return StatUtils.partialCorrelation(m);
    }
}




