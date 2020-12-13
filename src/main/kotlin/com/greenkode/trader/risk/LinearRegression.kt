package com.greenkode.trader.risk

/**
 * The `LinearRegression` class performs a simple linear regression
 * on an set of *n* data points (*y<sub>i</sub>*, *x<sub>i</sub>*).
 * That is, it fits a straight line *y* =  +  *x*,
 * (where *y* is the response variable, *x* is the predictor variable,
 *  is the *y-intercept*, and  is the *slope*)
 * that minimizes the sum of squared residuals of the linear regression model.
 * It also computes associated statistics, including the coefficient of
 * determination *R*<sup>2</sup> and the standard deviation of the
 * estimates for the slope and *y*-intercept.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
class LinearRegression(x: DoubleArray, y: DoubleArray) {
    private val intercept: Double
    private val slope: Double
    private val r2: Double
    private val svar0: Double
    private val svar1: Double

    /**
     * Returns the *y*-intercept  of the best of the best-fit line *y* =  +  *x*.
     *
     * @return the *y*-intercept  of the best-fit line *y =  +  x*
     */
    fun intercept(): Double {
        return intercept
    }

    /**
     * Returns the slope  of the best of the best-fit line *y* =  +  *x*.
     *
     * @return the slope  of the best-fit line *y* =  +  *x*
     */
    fun slope(): Double {
        return slope
    }

    /**
     * Returns the coefficient of determination *R*<sup>2</sup>.
     *
     * @return the coefficient of determination *R*<sup>2</sup>,
     * which is a real number between 0 and 1
     */
    fun R2(): Double {
        return r2
    }

    /**
     * Returns the standard error of the estimate for the intercept.
     *
     * @return the standard error of the estimate for the intercept
     */
    fun interceptStdErr(): Double {
        return Math.sqrt(svar0)
    }

    /**
     * Returns the standard error of the estimate for the slope.
     *
     * @return the standard error of the estimate for the slope
     */
    fun slopeStdErr(): Double {
        return Math.sqrt(svar1)
    }

    /**
     * Returns the expected response `y` given the value of the predictor
     * variable `x`.
     *
     * @param x the value of the predictor variable
     * @return the expected response `y` given the value of the predictor
     * variable `x`
     */
    fun predict(x: Double): Double {
        return slope * x + intercept
    }

    /**
     * Returns a string representation of the simple linear regression model.
     *
     * @return a string representation of the simple linear regression model,
     * including the best-fit line and the coefficient of determination
     * *R*<sup>2</sup>
     */
    override fun toString(): String {
        return String.format("%.2f n + %.2f", slope(), intercept()) +
                "  (R^2 = " + String.format("%.3f", R2()) + ")"
    }

    /**
     * Performs a linear regression on the data points `(y[i], x[i])`.
     *
     * @param x the values of the predictor variable
     * @param y the corresponding values of the response variable
     * @throws IllegalArgumentException if the lengths of the two arrays are not equal
     */
    init {
        require(x.size == y.size) { "array lengths are not equal" }
        val n = x.size

        // first pass
        var sumx = 0.0
        var sumy = 0.0
        var sumx2 = 0.0
        for (i in 0 until n) {
            sumx += x[i]
            sumx2 += x[i] * x[i]
            sumy += y[i]
        }
        val xbar = sumx / n
        val ybar = sumy / n

        // second pass: compute summary statistics
        var xxbar = 0.0
        var yybar = 0.0
        var xybar = 0.0
        for (i in 0 until n) {
            xxbar += (x[i] - xbar) * (x[i] - xbar)
            yybar += (y[i] - ybar) * (y[i] - ybar)
            xybar += (x[i] - xbar) * (y[i] - ybar)
        }
        slope = xybar / xxbar
        intercept = ybar - slope * xbar

        // more statistical analysis
        var rss = 0.0 // residual sum of squares
        var ssr = 0.0 // regression sum of squares
        for (i in 0 until n) {
            val fit = slope * x[i] + intercept
            rss += (fit - y[i]) * (fit - y[i])
            ssr += (fit - ybar) * (fit - ybar)
        }
        val degreesOfFreedom = n - 2
        r2 = ssr / yybar
        val svar = rss / degreesOfFreedom
        svar1 = svar / xxbar
        svar0 = svar / n + xbar * xbar * svar1
    }
}