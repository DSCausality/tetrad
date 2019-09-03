package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataWriter;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.*;
import edu.pitt.dbmi.data.reader.Delimiter;
import edu.pitt.dbmi.data.reader.tabular.ContinuousTabularDatasetFileReader;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

/**
 * @author Madelyn Glymour 7/21/18
 */
public class DemixerNongaussian {

    // The following fields are fixed.

    // The given data, N rows, numVars vars.
    private final DataSet data;

    // The data as an N x numVars matrix.
    private final TetradMatrix X;

    // Sample size
    private final int N;

    // The number of variables.
    private final int numVars;

    // The number of components. This must be specified up front.
    private final int numComponents;

    // Learning rate, by default 0.001.
    private final double learningRate;

    // The following fields are all updated.

    // S matrices - N x numVars, numComponents of them. This is updated.
    private final TetradMatrix[] S;

    // Diagonal kurtosis matrices, numComponents of them. This is updated.
    private final TetradMatrix[] K;

    // Likelihood of each record, per component. This is updated but is rewritten every time and is only
    // made a field to save memory allocation.
    private final TetradMatrix likelihoods;

    // Posterior probability of each record, per component. This is updated.
    private final TetradMatrix posteriorProbs;

    // Biases. This is updated.
    private TetradMatrix[] bias;

    // Unmixing matrices. These are updated.
    private TetradMatrix[] W;

    private DemixerNongaussian(DataSet data, int numComponents, double learningRate) {

        // Fixed.
        this.data = data;
        this.X = data.getDoubleData();
        this.N = X.rows();
        this.numVars = X.columns();
        this.numComponents = numComponents;
        this.learningRate = learningRate;

        // Updated.
        this.bias = new TetradMatrix[this.numComponents];
        this.likelihoods = new TetradMatrix(N, this.numComponents);
        this.posteriorProbs = new TetradMatrix(N, this.numComponents);
        this.W = new TetradMatrix[this.numComponents];
        this.K = new TetradMatrix[this.numComponents];
        this.S = new TetradMatrix[this.numComponents];
    }

    private MixtureModelNongaussian demix() {
        FastIca ica = new FastIca(X.transpose(), numVars);
        ica.setMaxIterations(1000);
        ica.setAlgorithmType(FastIca.PARALLEL);
        ica.setFunction(FastIca.EXP);
        ica.setAlpha(1.1);
        ica.setTolerance(1-6);
        FastIca.IcaResult result = ica.findComponents();

        TetradMatrix _W = result.getW().transpose();

        for (int k = 0; k < numComponents; k++) {
//            W[k] = _W;
            W[k] = addNoise(_W, 1);
        }

        for (int k = 0; k < numComponents; k++) {
            TetradMatrix _bias = new TetradMatrix(1, numVars);

            for (int i = 0; i < numVars; i++) {
                _bias.set(0, i, 0.5);//RandomUtil.getInstance().nextNormal(0, 0.01));
            }

            bias[k] = _bias;
        }

        adaptSourceMatrices();

        initializeKurtosisMatrices();

        double _l = Double.NaN;
        double l = calculateLikelihoods();

        while (true) {

            printLikelihood();

            if (Double.isNaN(l) || l - 0.01 <= _l) {
                break;
            }

            _l = l;

            l = calculatePosteriors();
            maximization();
        }

        return new MixtureModelNongaussian(data, posteriorProbs, invertWMatrices(W), bias);
    }

    private void printLikelihood() {
        double l = 0;

        for (int n = 0; n < N; n++) {
            for (int k = 0; k < numComponents; k++) {
                final double L = likelihoods.get(n, k);

                if (L != 0) {
                    l += L;
                }
            }
        }

        System.out.println("L from likelihood array = " + l);
    }

    private void initializeKurtosisMatrices() {
        for (int k = 0; k < numComponents; k++) {
            if (K[k] == null) K[k] = new TetradMatrix(numVars, numVars);

            for (int v = 0; v < numVars; v++) {
                double sechSum = 0;
                double _sum = 0;
                double tanhSum = 0;

                for (int n = 0; n < N; n++) {
                    sechSum += 1.0 / pow(cosh(S[k].get(n, v)), 2.0);
                    _sum += pow(S[k].get(n, v), 2.0);
                    tanhSum += tanh(S[k].get(n, v)) * S[k].get(n, v);
                }

                sechSum /= N;
                _sum /= N;
                tanhSum /= N;

                double kurtosis = signum(sechSum * _sum - tanhSum);

                K[k].set(v, v, kurtosis);
            }
        }
    }

    /*
     * Inline updating of the mixing matrix A, updated every time a new posterior is determined
     */
    private void adaptMixingMatrices() {
        for (int k = 0; k < numComponents; k++) {
            TetradMatrix M = TetradMatrix.identity(numVars).minus(K[k].times(MatrixUtils.tanh(S[k]).transpose().times(S[k])))
                    .minus(S[k].transpose().times((S[k])));
            TetradMatrix delta = new TetradMatrix(numVars, numVars);

            for (int n = 0; n < N; n++) {
                double p = posteriorProbs.get(n, k);
                delta = delta.plus(M.times(W[k]).scalarMult(p * learningRate / (N * numComponents)));
            }

//            final double det = delta.det();

//            if (abs(det) > 0) {
            W[k] = W[k].plus(delta);
//            }
        }
    }

    private void adaptBiasVectors() {
        for (int k = 0; k < numComponents; k++) {
            TetradMatrix _W = W[k];
            TetradMatrix sourceVector = S[k];
            TetradMatrix _bias = new TetradMatrix(1, numVars);

            for (int n = 0; n < N; n++) {
                double prob = posteriorProbs.get(n, k);

//                double sum = 0;
//                double det = Math.log(Math.abs(_W.det()));

                for (int i = 0; i < numVars; i++) {
                    TetradMatrix bMatrix = _W.row(i);
                    _bias = _bias.plus((bMatrix.scalarMult(exp(Math.tanh(sourceVector.get(n, i)))).plus(bMatrix.scalarMult(sourceVector.get(n, i)))));

//                    double l = -.5 * Math.log(2.0 * Math.PI) - (K[k].get(i, i) * log(cosh(S[k].get(n, i))))
//                            - (pow(S[k].get(n, i), 2.0) / 2.0);
//
//                    if (K[k].get(i, i) > 0) {
//                        l = l - Math.log(0.7413);
//                    }
//
//                    sum += l;
                }

//                double L = (sum - det) / N;
//                double L = likelihoods.get(n, k);

                _bias = _bias.scalarMult(prob);
            }

            bias[k] = bias[k].plus(_bias);
        }
    }

    private void maximization() {
        adaptMixingMatrices();
        adaptBiasVectors();
        adaptSourceMatrices();
    }

    private double calculateLikelihoods() {
        for (int k = 0; k < numComponents; k++) {
            double det = Math.log(Math.abs(W[k].det()));

            for (int n = 0; n < N; n++) {
                double sum = 0;

                for (int c = 0; c < numVars; c++) {
                    double l = -.5 * Math.log(2.0 * Math.PI) - (K[k].get(c, c) * log(cosh(S[k].get(n, c))))
                            - (pow(S[k].get(n, c), 2.0) / 2.0);

                    if (K[k].get(c, c) > 0) {
                        l = l - Math.log(0.7413);
                    }

                    sum += l;
                }

                double L = (sum - det) / N;

                likelihoods.set(n, k, L);
            }
        }

        double likelihood = 0.0;

        for (int k = 0; k < numComponents; k++) {
            for (int n = 0; n < N; n++) {
                likelihood += likelihoods.get(n, k);
            }
        }

        return likelihood;
    }

    // Returns the likelihood just for component k.
    private double calculatePosteriors() {

        for (int n = 0; n < N; n++) {
            for (int k = 0; k < numComponents; k++) {
                double prob = exp(likelihoods.get(n, k));
                posteriorProbs.set(n, k, prob);
            }

            normalize(posteriorProbs, n);
        }

        for (int n = 0; n < N; n++) {
            for (int k = 0; k < numComponents; k++) {
                double prob = log(posteriorProbs.get(n, k));
                posteriorProbs.set(n, k, prob);
            }

            normalize(posteriorProbs, n);
        }

        double likelihood = 0.0;

        for (int k = 0; k < numComponents; k++) {
            for (int n = 0; n < N; n++) {
                likelihood += likelihoods.get(n, k);
            }
        }

        System.out.println("Posterior probs = " + posteriorProbs);
//
        return likelihood;
    }


    // Needs bias and W.
    private void adaptSourceMatrices() {
        for (int k = 0; k < numComponents; k++) {
            S[k] = X.minus(repmat(bias[k].getRow(0), N)).times(W[k].transpose());
        }
    }

    public static void main(String... args) {

        DataSet dataSet = loadData("/Users/user/Downloads/mixfile1.csv");
//        DataSet dataSet = loadData("/Users/user/Box Sync/data/Sachs/data.logged.txt");

        System.out.println(dataSet.getVariableNames());
        List<Node> vars = new ArrayList<>();
        vars.add(dataSet.getVariable(0));
        vars.add(dataSet.getVariable(1));
        dataSet = dataSet.subsetColumns(vars);

        long startTime = System.currentTimeMillis();

        DemixerNongaussian pedro = new DemixerNongaussian(dataSet, 2, 0.001);
        MixtureModelNongaussian model = pedro.demix();

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("\n================ WHAT I LEARNED ===================");

//        for (int k = 0; k < model.getMixingMatrices().length; k++) {
//            System.out.println("W = " + model.getMixingMatrices()[0]);
//        }

        DataSet[] datasets = model.getDemixedData();

        System.out.println("\n\nDatasets:");

        int count = 1;

        for (DataSet _dataSet : datasets) {
            System.out.println("#" + count++ + ": rows = " + _dataSet.getNumRows() + " cols = " + _dataSet.getNumColumns());
        }

        System.out.println();

        System.out.println(model.getLabeledData());

        System.out.println("Elapsed: " + (elapsed / 1000));

        try {
            DataWriter.writeRectangularData(model.getLabeledData(), new FileWriter(new File("/Users/user/Downloads/labeled.txt")), '\t');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TetradMatrix addNoise(TetradMatrix w, double v) {
        w = new TetradMatrix(w);

        for (int r = 0; r < w.rows(); r++) {
            for (int c = 0; c < w.columns(); c++) {
                w.set(r, c, w.get(r, c) + RandomUtil.getInstance().nextNormal(0, v));
            }
        }

        return w;
    }


    private TetradMatrix addNoise(TetradMatrix X) {
        TetradMatrix cov = X.times(X.transpose()).scalarMult(1.0 / X.columns());

        SingularValueDecomposition s = new SingularValueDecomposition(cov.getRealMatrix());
        TetradMatrix D = new TetradMatrix(s.getS());
        TetradMatrix U = new TetradMatrix(s.getU());

        for (int i = 0; i < D.rows(); i++) {
            D.set(i, i, 1.0 / Math.sqrt(D.get(i, i)));
        }

        TetradMatrix K = D.times(U.transpose());
//        K = K.scalarMult(-1); // This SVD gives -U from R's SVD.
//        K = K.getPart(0, numComponents - 1, 0, p - 1);

//        w = new TetradMatrix(w);
//
//        for (int r = 0; r < w.rows(); r++) {
//            for (int c = 0; c < w.columns(); c++) {
//                w.set(r, c, w.get(r, c) + RandomUtil.getInstance().nextNormal(0, v));
//            }
//        }

        return K;
    }

    private TetradMatrix repmat(TetradVector _bias, int rows) {
        TetradMatrix rep = X.like();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < _bias.size(); c++) {
                rep.set(r, c, _bias.get(c));
            }
        }

        return rep;
    }


    private TetradMatrix[] invertWMatrices(TetradMatrix[] m) {
        TetradMatrix[] n = new TetradMatrix[m.length];

        for (int k = 0; k < m.length; k++) {
            n[k] = m[k].inverse();
        }

        return n;
    }


    private void normalize(TetradMatrix m, int row) {
        double _sum = 0.0;

        for (int k = 0; k < m.columns(); k++) {
            _sum += m.get(row, k);
        }

        for (int k = 0; k < m.columns(); k++) {
            m.set(row, k, m.get(row, k) / _sum);
        }
    }


    private static DataSet loadData(String path) {
        try {
            ContinuousTabularDatasetFileReader dataReader = new ContinuousTabularDatasetFileReader(
                    new File(path).toPath(), Delimiter.COMMA);
            return (DataSet) DataConvertUtils.toDataModel(dataReader.readInData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}