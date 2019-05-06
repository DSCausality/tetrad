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

package edu.cmu.tetrad.study;

import edu.cmu.tetrad.algcomparison.Comparison;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithms;
import edu.cmu.tetrad.algcomparison.algorithm.multi.Fask;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pag.*;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.FAS;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.Fges;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.PcAll;
import edu.cmu.tetrad.algcomparison.algorithm.pairwise.R3;
import edu.cmu.tetrad.algcomparison.algorithm.pairwise.Skew;
import edu.cmu.tetrad.algcomparison.graph.RandomForward;
import edu.cmu.tetrad.algcomparison.independence.FisherZ;
import edu.cmu.tetrad.algcomparison.independence.SemBicTest;
import edu.cmu.tetrad.algcomparison.score.FisherZScore;
import edu.cmu.tetrad.algcomparison.score.SemBicScore;
import edu.cmu.tetrad.algcomparison.simulation.LinearFisherModel;
import edu.cmu.tetrad.algcomparison.simulation.SemSimulation;
import edu.cmu.tetrad.algcomparison.simulation.Simulations;
import edu.cmu.tetrad.algcomparison.statistic.*;
import edu.cmu.tetrad.util.Parameters;

import static edu.cmu.tetrad.performance.ComparisonParameters.Algorithm.GFCI;

/**
 * An example script to simulate data and run a comparison analysis on it.
 *
 * @author jdramsey
 */
public class ExampleCompareSimulationContinuousPag {
    enum Type {LinearGaussian, LinearNongaussian}

    public static void main(String... args) {
        Parameters parameters = new Parameters();
        int sampleSize = 500;
        int numMeasures = 40;
        int numLatents = 5;
        int avgDegree = 4;
        ExampleCompareSimulationContinuousPag.Type type = ExampleCompareSimulationContinuousPag.Type.LinearNongaussian;

        parameters.set("numRuns", 5);
        parameters.set("numMeasures", numMeasures);
        parameters.set("numLatents", numLatents);
        parameters.set("avgDegree", avgDegree);
        parameters.set("sampleSize", sampleSize); // This varies.
        parameters.set("differentGraphs", true);

        parameters.set("alpha", 0.05);
        parameters.set("penaltyDiscount", 2); // tookout 1
//        parameters.set("colliderDiscoveryRule", 2, 3); // took out 1
//        parameters.set("conflictRule", /*1,*/ 3); // took out 2

        parameters.set("coefLow", 0.2);
        parameters.set("coefHigh", 0.7);

        parameters.set("varLow", 1.0);
        parameters.set("varHigh", 3.0);

        parameters.set("discretise", true, false);

        parameters.set("maxDegree", 4);

        parameters.set("depth", 4);

        Statistics statistics = new Statistics();

        statistics.add(new ParameterColumn("numMeasures"));
        statistics.add(new ParameterColumn("avgDegree"));
        statistics.add(new ParameterColumn("sampleSize"));
        statistics.add(new ParameterColumn("colliderDiscoveryRule"));
        statistics.add(new ParameterColumn("conflictRule"));
        statistics.add(new ParameterColumn("alpha"));
        statistics.add(new ParameterColumn("penaltyDiscount"));
        statistics.add(new ParameterColumn("maxDegree"));

        statistics.add(new AdjacencyPrecision());
        statistics.add(new AdjacencyRecall());
        statistics.add(new ArrowheadPrecision());
        statistics.add(new ArrowheadPrecisionCommonEdges());
        statistics.add(new ArrowheadRecall());
//        statistics.add(new MathewsCorrAdj());
//        statistics.add(new MathewsCorrArrow());
//        statistics.add(new F1Adj());
//        statistics.add(new F1Arrow());
//        statistics.add(new SHD());
//        statistics.add(new ElapsedTime());

        statistics.setWeight("AP", 1.0);
        statistics.setWeight("AR", 1.0);
        statistics.setWeight("AHPC", 1.0);
//        statistics.setWeight("AHR", 1.0);
//        statistics.setWeight("SHD", 1.0);

        Algorithms algorithms = new Algorithms();

//        algorithms.add(new Fges(new SemBicScore()));
//        algorithms.add(new PcAll(new FisherZ()));
//        algorithms.add(new R3(new FAS(new FisherZ())));

//        algorithms.add(new Fci(new FisherZ()));
//        algorithms.add(new Rfci(new FisherZ()));
//        algorithms.add(new FciMax(new FisherZ()));
//        algorithms.add(new CFCI(new FisherZ()));
        algorithms.add(new Gfci(new FisherZ(), new SemBicScore()));
//        algorithms.add(new Gfci(new SemBicTest(), new SemBicScore()));
//        algorithms.add(new GfciNg(new FisherZ(), new SemBicScore()));
//        algorithms.add(new GfciNg(new SemBicTest(), new SemBicScore()));


        Simulations simulations = new Simulations();

        if (type == ExampleCompareSimulationContinuousPag.Type.LinearGaussian) {
            simulations.add(new SemSimulation(new RandomForward()));
        } else if (type == ExampleCompareSimulationContinuousPag.Type.LinearNongaussian) {
            simulations.add(new LinearFisherModel(new RandomForward()));
            parameters.set("errorsNormal", false);
        }

        Comparison comparison = new Comparison();

        comparison.setShowAlgorithmIndices(true);
        comparison.setShowSimulationIndices(true);
        comparison.setSortByUtility(true);
        comparison.setShowUtilities(true);
//        comparison.setParallelized(true);

        comparison.setSaveGraphs(true);
        comparison.setSavePags(true);
        comparison.setSavePatterns(true);
//        comparison.setSaveTrueDags(true);

        comparison.setComparisonGraph(Comparison.ComparisonGraph.PAG_of_the_true_DAG);

        comparison.compareFromSimulations("comparison.continuous.pag", simulations, "comparison_continuous_pag_" + sampleSize, algorithms, statistics, parameters);
    }
}




