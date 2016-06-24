package edu.cmu.tetrad.algcomparison.continuous.pattern;

import edu.cmu.tetrad.algcomparison.Algorithm;
import edu.cmu.tetrad.data.CovarianceMatrixOnTheFly;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.DagToPag;
import edu.cmu.tetrad.search.Fgs;
import edu.cmu.tetrad.search.Fgs2;
import edu.cmu.tetrad.search.SemBicScore;

import java.util.Map;

/**
 * Created by jdramsey on 6/4/16.
 */
public class ContinuousFgs2 implements Algorithm {
    public Graph search(DataSet dataSet, Map<String, Number> parameters) {
        SemBicScore score = new SemBicScore(new CovarianceMatrixOnTheFly(dataSet));
        score.setPenaltyDiscount(parameters.get("penaltyDiscount").doubleValue());
        Fgs2 fgs = new Fgs2(score);
//        fgs.setDepth(parameters.get("fgsDepth").intValue());
        return fgs.search();
    }

    public Graph getComparisonGraph(Graph dag) {
        return new DagToPag(dag).convert();
    }

    public String getDescription() {
        return "FGS2 using the SEM BIC score";
    }


    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }
}
