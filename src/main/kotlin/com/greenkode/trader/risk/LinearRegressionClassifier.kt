package com.greenkode.trader.risk

class LinearRegressionClassifier(private val xData: List<Double>, private val yData: List<Double>) {

    init {
        predictValue(xData.last())
    }

    fun getXMean(): Double {
        return xData.sum() / xData.size
    }

    fun getYMean(): Double {
        return yData.sum() / xData.size
    }

    fun getLineSlope(xMean: Double, yMean: Double, x1: Double, y1: Double): Double {
        return (x1 - xMean) * (y1 - yMean) / (x1 - xMean) * (x1 - xMean)
    }

    fun getYIntercept(xMean: Double, yMean: Double, slope: Double): Double {
        return yMean - (slope * xMean)
    }

    fun predictValue(input: Double): Double {

        val xMean = getXMean()
        val yMean = getYMean()

        val slope = getLineSlope(xMean, yMean, xData[0], yData[0])
        val yIntercept = getYIntercept(xMean, yMean, slope)

        return (slope * input) + yIntercept
    }
}