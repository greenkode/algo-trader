package com.greenkode.trader.portfolio.ojalgo

import org.ojalgo.OjAlgoUtils
import org.ojalgo.finance.data.DataSource
import org.ojalgo.finance.data.fetcher.YahooSession
import org.ojalgo.netio.BasicLogger
import org.ojalgo.random.SampleSet
import org.ojalgo.random.process.GeometricBrownianMotion
import org.ojalgo.series.BasicSeries.NaturallySequenced
import org.ojalgo.series.primitive.CoordinatedSet
import org.ojalgo.type.CalendarDateUnit
import java.time.LocalDate


object FinancialData {
    @JvmStatic
    fun main(args: Array<String>) {

        BasicLogger.debug()
        BasicLogger.debug(FinancialData::class.java)
        BasicLogger.debug(OjAlgoUtils.getTitle())
        BasicLogger.debug(OjAlgoUtils.getDate())
        BasicLogger.debug()

        val sourceMSFT: DataSource = DataSource.newAlphaVantage("MSFT", CalendarDateUnit.DAY, "demo")
        val sourceAAPL: DataSource = DataSource.newIEXTrading("AAPL")

        val yahooSession = YahooSession()
        val sourceIBM: DataSource = DataSource.newYahoo(yahooSession, "IBM", CalendarDateUnit.DAY)
        val sourceORCL: DataSource = DataSource.newYahoo(yahooSession, "ORCL", CalendarDateUnit.DAY)

        val seriesIBM: NaturallySequenced<LocalDate, Double> = sourceIBM.getLocalDateSeries(CalendarDateUnit.MONTH)
        val seriesORCL: NaturallySequenced<LocalDate, Double> = sourceORCL.getLocalDateSeries(CalendarDateUnit.MONTH)

        val seriesMSFT: NaturallySequenced<LocalDate, Double> = sourceMSFT.getLocalDateSeries(CalendarDateUnit.MONTH)
        val seriesAAPL: NaturallySequenced<LocalDate, Double> = sourceAAPL.getLocalDateSeries(CalendarDateUnit.MONTH)

        BasicLogger.debug("Range for {} is from {} to {}", seriesMSFT.name, seriesMSFT.firstKey(), seriesMSFT.lastKey())
        BasicLogger.debug("Range for {} is from {} to {}", seriesAAPL.name, seriesAAPL.firstKey(), seriesAAPL.lastKey())
        BasicLogger.debug("Range for {} is from {} to {}", seriesIBM.name, seriesIBM.firstKey(), seriesIBM.lastKey())
        BasicLogger.debug("Range for {} is from {} to {}", seriesORCL.name, seriesORCL.firstKey(), seriesORCL.lastKey())

        val coordinationSet = CoordinatedSet.from(seriesMSFT, seriesAAPL, seriesIBM, seriesORCL)

        BasicLogger.debug()
        BasicLogger.debug("Common range is from {} to {}", coordinationSet.firstKey, coordinationSet.lastKey)

        val primitiveMSFT = coordinationSet.getSeries(0)
        val primitiveAAPL = coordinationSet.getSeries(1)
        val primitiveIBM = coordinationSet.getSeries(2)
        val primitiveORCL = coordinationSet.getSeries(3)

        val sampleSetMSFT = SampleSet.wrap(primitiveMSFT.log().differences())
        val sampleSetIBM = SampleSet.wrap(primitiveIBM.log().differences())

        BasicLogger.debug()
        BasicLogger.debug("Sample statistics (logarithmic differences on monthly data)")
        BasicLogger.debug("MSFT:  {}", sampleSetMSFT)
        BasicLogger.debug("IBM: {}", sampleSetIBM)
        BasicLogger.debug("Correlation: {}", sampleSetIBM.getCorrelation(sampleSetMSFT))

        val monthlyProcAAPL = GeometricBrownianMotion.estimate(primitiveAAPL, 1.0)
        val monthlyProcORCL = GeometricBrownianMotion.estimate(primitiveORCL, 1.0)
        monthlyProcAAPL.value = 1.0 // To normalize the current value

        monthlyProcORCL.value = 1.0

        val yearsPerMonth = CalendarDateUnit.YEAR.convert(CalendarDateUnit.MONTH)
        val annualProcAAPL = GeometricBrownianMotion.estimate(primitiveAAPL, yearsPerMonth)
        val annualProcORCL = GeometricBrownianMotion.estimate(primitiveORCL, yearsPerMonth)
        annualProcAAPL.value = 1.0 // To normalize the current value

        annualProcORCL.value = 1.0

        BasicLogger.debug()
        BasicLogger.debug("    Apple    Monthly proc    Annual proc    (6 months from now)")
        BasicLogger.debug(
            "Expected: {}    {}",
            monthlyProcAAPL.getDistribution(6.0).expected,
            annualProcAAPL.getDistribution(0.5).expected
        )
        BasicLogger.debug(
            "StdDev:   {}    {}", monthlyProcAAPL.getDistribution(6.0).standardDeviation,
            annualProcAAPL.getDistribution(0.5).standardDeviation
        )
        BasicLogger.debug(
            "Var:      {}    {}",
            monthlyProcAAPL.getDistribution(6.0).variance,
            annualProcAAPL.getDistribution(0.5).variance
        )

        BasicLogger.debug()
        BasicLogger.debug("    Apple    Oracle    (1 year from now)")
        BasicLogger.debug("Current:  {}    {}", annualProcAAPL.value, annualProcORCL.value)
        BasicLogger.debug("Expected: {}    {}", annualProcAAPL.expected, annualProcORCL.expected)
        BasicLogger.debug("StdDev:   {}    {}", annualProcAAPL.standardDeviation, annualProcORCL.standardDeviation)
        BasicLogger.debug("Var:      {}    {}", annualProcAAPL.variance, annualProcORCL.variance)

        val simulationResults = annualProcORCL.simulate(1000, 12, yearsPerMonth)
        BasicLogger.debug()
        BasicLogger.debug("Simulate future Oracle: 1000 scenarios, take 12 incremental steps of size 'yearsPerMonth' (1/12)")
        BasicLogger.debug("Simulated sample set: {}", simulationResults.getSampleSet(11))
        BasicLogger.debug("Simulated scenario: {}", simulationResults.getScenario(999))

        val coordinated: DataSource.Coordinated = DataSource.coordinated(CalendarDateUnit.DAY)
        coordinated.addAlphaVantage("MSFT", "demo")
        coordinated.addIEXTrading("AAPL")
        coordinated.addYahoo("IBM")
        coordinated.addYahoo("ORCL")
        val coordinationSet2: CoordinatedSet<LocalDate> = coordinated.get()
    }
}