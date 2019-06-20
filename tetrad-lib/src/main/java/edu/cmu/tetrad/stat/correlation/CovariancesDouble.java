/*
 * Copyright (C) 2016 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.cmu.tetrad.stat.correlation;

import edu.cmu.tetrad.util.TetradMatrix;

/**
 * Compute covariance on the fly. Warning! This class will overwrite the values
 * in the input data.
 *
 * Jan 27, 2016 4:37:44 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public class CovariancesDouble implements Covariances {

    private final double[][] doubleData;

    private final int numOfRows;

    private final int numOfCols;
    private final double[][] covariances;

    public CovariancesDouble(double[][] data, boolean biasCorrected) {
        doubleData = new double[data.length][data[0].length];

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                this.doubleData[i][j] = data[i][j];
            }
        }

        this.numOfRows = data.length;
        this.numOfCols = data[0].length;
        this.covariances = compute(biasCorrected);
    }

    public CovariancesDouble(float[][] data, boolean biasCorrected) {
        doubleData = new double[data.length][data[0].length];

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                this.doubleData[i][j] = data[i][j];
            }
        }

        this.numOfRows = data.length;
        this.numOfCols = data[0].length;
        this.covariances = compute(biasCorrected);
    }

    public CovariancesDouble(double[][] matrix, int sampleSize) {
        this.covariances = matrix;
        this.numOfCols = matrix.length;
        this.numOfRows = sampleSize;
        this.doubleData = null;
    }

    public double[] computeLowerTriangle(boolean biasCorrected) {
        double[] covarianceMatrix = new double[(numOfCols * (numOfCols + 1)) / 2];

        computeMeans();

        int index = 0;
        for (int col = 0; col < numOfCols; col++) {
            for (int col2 = 0; col2 < col; col2++) {
                double variance = 0;
                for (int row = 0; row < numOfRows; row++) {
                    variance += ((doubleData[row][col]) * (doubleData[row][col2]) - variance) / (row + 1);
                }
                covarianceMatrix[index++] = biasCorrected ? variance * ((double) numOfRows / (double) (numOfRows - 1)) : variance;
            }
            double variance = 0;
            for (int row = 0; row < numOfRows; row++) {
                variance += ((doubleData[row][col]) * (doubleData[row][col]) - variance) / (row + 1);
            }
            covarianceMatrix[index++] = biasCorrected ? variance * ((double) numOfRows / (double) (numOfRows - 1)) : variance;
        }

        return covarianceMatrix;
    }

    public double[][] compute(boolean biasCorrected) {
        double[][] covarianceMatrix = new double[numOfCols][numOfCols];

        computeMeans();

        for (int col = 0; col < numOfCols; col++) {
            for (int col2 = 0; col2 < col; col2++) {
                double cov = 0;
                for (int row = 0; row < numOfRows; row++) {
                    cov += ((doubleData[row][col]) * (doubleData[row][col2]) - cov) / (row + 1);
                }
                cov = biasCorrected ? cov * ((double) numOfRows / (double) (numOfRows - 1)) : cov;
                covarianceMatrix[col][col2] = cov;
                covarianceMatrix[col2][col] = cov;
            }
            double variance = 0;
            for (int row = 0; row < numOfRows; row++) {
                variance += ((doubleData[row][col]) * (doubleData[row][col]) - variance) / (row + 1);
            }
            covarianceMatrix[col][col] = biasCorrected ? variance * ((double) numOfRows / (double) (numOfRows - 1)) : variance;
        }

        return covarianceMatrix;
    }

    private void computeMeans() {
        for (int col = 0; col < numOfCols; col++) {
            double mean = 0;
            for (int row = 0; row < numOfRows; row++) {
                mean += doubleData[row][col];
            }
            mean /= numOfRows;
            for (int row = 0; row < numOfRows; row++) {
                doubleData[row][col] -= mean;
            }
        }
    }

    @Override
    public double covariance(int i, int j) {
        return covariances[i][j];
    }

    @Override
    public int size() {
        return numOfCols;
    }

    @Override
    public void setCovariance(int i, int j, double v) {
        covariances[i][j] = v;
        covariances[j][i] = v;
    }

    @Override
    public double[][] getMatrix() {
        int[] rows = new int[size()];
        for (int i = 0; i < rows.length; i++) rows[i] = i;
        return getSubMatrix(rows, rows);
    }

    @Override
    public double[][] getSubMatrix(int[] rows, int[] cols) {
        double[][] submatrix = new double[rows.length][cols.length];

        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                submatrix[i][j] = covariances[rows[i]][cols[j]];
            }
        }

        return submatrix;
    }
}
