package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.CovarianceMatrixOnTheFly;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.regression.RegressionCovariance;
import edu.cmu.tetrad.regression.RegressionResult;
import edu.cmu.tetrad.util.DepthChoiceGenerator;

import java.util.*;

import static java.lang.Math.abs;

public class Ida {

    private final DataSet dataSet;
    private final Graph pattern;
    private final RegressionCovariance regression;

    public Ida(DataSet dataSet) {
        this.dataSet = dataSet;
        final CovarianceMatrixOnTheFly covariances = new CovarianceMatrixOnTheFly(dataSet);
        Fges fges = new Fges(new SemBicScore(covariances));
        this.pattern = fges.search();
        regression = new RegressionCovariance(covariances);
    }

    public double getMinimumEffect(Node x, Node y) {
        if (x == y) return 0.0;

        List<Node> parents = pattern.getParents(x);

        List<Node> siblings = pattern.getAdjacentNodes(x);
        siblings.removeAll(parents);
        siblings.remove(x);

        DepthChoiceGenerator gen = new DepthChoiceGenerator(siblings.size(), siblings.size());
        int[] choice;

        double minEffect = Double.POSITIVE_INFINITY;
        double minBeta = Double.POSITIVE_INFINITY;

        while ((choice = gen.next()) != null) {
            List<Node> sibbled = GraphUtils.asList(choice, siblings);
            sibbled.remove(y);

            List<Node> regressors = new ArrayList<>();
            regressors.add(x);
            regressors.addAll(sibbled);

            RegressionResult result = regression.regress(y, regressors);

            double beta;

            if (regressors.contains(y)) {
                beta = 0.0;
            } else {
                beta = result.isZeroInterceptAssumed() ? result.getCoef()[0] : result.getCoef()[1];
            }

            if (abs(beta) <= minEffect) {
                minBeta = beta;
                minEffect = abs(beta);
            }
        }

        return minBeta;
    }

    public Map<Node, Double> calculateEffectsOnY(Node y) {
        Map<Node, Double> effects = new HashMap<>();

        for (Node x : dataSet.getVariables()) {
            effects.put(x, getMinimumEffect(x, y));
        }

        return effects;
    }

    public NodeEffects getSortedEffects(Node y) {
        List<Node> nodes = new ArrayList<>();

        Map<Node, Double> allEffects = calculateEffectsOnY(y);

        for (Node node : allEffects.keySet()) {
            nodes.add(node);
        }

        Collections.sort(nodes, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                final Double d1 = allEffects.get(o1);
                final Double d2 = allEffects.get(o2);
                return -Double.compare(abs(d1), abs(d2));
            }
        });

        List<Double> effects = new ArrayList<>();

        for (Node node : nodes) {
            effects.add(allEffects.get(node));
        }

        return new NodeEffects(nodes, effects);
    }

    public class NodeEffects {
        private List<Node> nodes;
        private List<Double> effects;

        public NodeEffects(List<Node> nodes, List<Double> effects) {
            this.setNodes(nodes);
            this.setEffects(effects);
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public void setNodes(List<Node> nodes) {
            this.nodes = nodes;
        }

        public List<Double> getEffects() {
            return effects;
        }

        public void setEffects(List<Double> effects) {
            this.effects = effects;
        }
    }
}
