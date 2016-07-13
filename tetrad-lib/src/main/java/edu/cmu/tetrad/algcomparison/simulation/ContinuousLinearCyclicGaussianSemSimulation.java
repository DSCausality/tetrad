package edu.cmu.tetrad.algcomparison.simulation;

import edu.cmu.tetrad.algcomparison.DataType;
import edu.cmu.tetrad.algcomparison.Parameters;
import edu.cmu.tetrad.algcomparison.Simulation;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.sem.SemIm;
import edu.cmu.tetrad.sem.SemImInitializationParams;
import edu.cmu.tetrad.sem.SemPm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdramsey on 6/4/16.
 */
public class ContinuousLinearCyclicGaussianSemSimulation implements Simulation {
    private List<DataSet> dataSets;
    private List<Graph> graphs;

    public ContinuousLinearCyclicGaussianSemSimulation(Parameters parameters) {
        dataSets = new ArrayList<>();
        graphs = new ArrayList<>();

        for (int i = 0; i < parameters.getInt("numRuns"); i++) {
            Graph graph = GraphUtils.cyclicGraph2(parameters.getInt("numMeasures"),
                    parameters.getInt("numEdges"));
            SemPm pm = new SemPm(graph);
            SemImInitializationParams params = new SemImInitializationParams();
            params.setCoefRange(.2, .9);
            params.setCoefSymmetric(true);
            SemIm im = new SemIm(pm, params);
            dataSets.add(im.simulateData(parameters.getInt("sampleSize"), false));
            graphs.add(graph);
        }
    }

    public DataSet getDataSet(int index) {
        return dataSets.get(index);
    }

    public Graph getTrueGraph(int index) {
        return graphs.get(index);
    }

    public String getDescription() {
        return "Linear, Gaussian SEM simulation";
    }

    @Override
    public int getNumDataSets() {
        return dataSets.size();
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }
}
