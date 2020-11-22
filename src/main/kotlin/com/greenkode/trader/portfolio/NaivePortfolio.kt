package com.greenkode.trader.portfolio

import com.greenkode.trader.analysis.createDrawdowns
import com.greenkode.trader.analysis.createSharpeRatio
import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.event.SignalEvent
import com.greenkode.trader.logger.LoggerDelegate
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import java.time.LocalDateTime
import java.util.*

val Double.Companion.ZERO: Double
    get() {
        return 0.0
    }

class NaivePortfolio(
    val dataHandler: DataHandler, val events: Queue<Event>, val riskManager: RiskManager,
    var startDate: LocalDateTime?, val initialCapital: Double
) : Portfolio() {

    val logger by LoggerDelegate()

    private val currentPositions: Positions
    private val allPositions: MutableList<Positions>
    private var currentHoldings: Holdings
    private val allHoldings: MutableList<Holdings>
    private val symbols = dataHandler.symbols

    private val equityCurve: Table

    init {

        if (startDate == null)
            startDate = dataHandler.getEarliestStartDate()

        currentPositions = constructCurrentPositions()
        allPositions = constructAllPositions()
        allHoldings = constructAllHoldings()
        currentHoldings = constructCurrentHoldings()

        val columns = listOf(
            DateTimeColumn.create("timestamp"),
            DoubleColumn.create("cash"),
            DoubleColumn.create("commission"),
            DoubleColumn.create("total")
        )

        equityCurve = Table.create("Equity Curve", columns)
    }

    private fun constructCurrentPositions(): Positions {
        val positions = initializePositionsForSymbols()
        return Positions(positions = positions)
    }

    private fun constructCurrentHoldings(): Holdings {
        return Holdings(
            cash = initialCapital,
            commission = 0.001,
            total = initialCapital,
            positions = initializePositionsForSymbols()
        )
    }

    private fun constructAllPositions(): MutableList<Positions> {
        return mutableListOf(Positions(positions = initializePositionsForSymbols(), timestamp = startDate))
    }

    private fun constructAllHoldings(): MutableList<Holdings> {
        return mutableListOf(
            Holdings(
                cash = initialCapital,
                commission = 0.001,
                total = initialCapital,
                positions = initializePositionsForSymbols(),
                timestamp = startDate
            )
        )
    }

    override fun updateTimeIndex(event: Event) {
        val bars = symbols.associateBy({ it }, { dataHandler.getLatestBars(it, 1) })
        val timestamp = bars[symbols[0]]?.first()?.getDateTime(DATA_COLUMN_TIMESTAMP)!!
        val dp = Positions(
            positions = initializePositionsForSymbols(),
            timestamp = timestamp
        )

        symbols.forEach { symbol ->
            currentPositions.positions[symbol]?.let { dp.positions[symbol] = it }
        }
        allPositions.add(dp)

        val dh = Holdings(
            positions = initializePositionsForSymbols(),
            timestamp = timestamp,
            cash = currentHoldings.cash,
            commission = currentHoldings.commission,
            total = currentHoldings.total
        )

        symbols.forEach { symbol ->
            val marketValue =
                currentPositions.positions[symbol]!! * bars[symbols[0]]?.first()?.getDouble(DATA_COLUMN_CLOSE)!!
            dh.positions[symbol] = marketValue
            dh.addToTotal(marketValue)
        }

        allHoldings.add(dh)
    }

    private fun initializePositionsForSymbols() = symbols.associateBy({ it }, { Double.ZERO }).toMutableMap()

    override fun updateFill(event: Event) {
        if (event.type == EventTypeEnum.FILL) {
            updatePositionsFromFill(event as FillEvent)
            updateHoldingsFromFill(event)
        }
    }

    override fun updateSignal(event: Event) {
        if (event.type == EventTypeEnum.SIGNAL) {
            val orderEvent = generateNaiveOrder(event as SignalEvent)
            if (orderEvent != null)
                events.offer(orderEvent)
        }
    }

    private fun updatePositionsFromFill(fillEvent: FillEvent) {
        val currPosition = currentPositions.positions[fillEvent.symbol]
        currentPositions.positions[fillEvent.symbol] =
            currPosition!! + fillEvent.quantity * fillEvent.orderDirection.value
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {

        val cost = fillEvent.quantity / (fillEvent.orderDirection.value * getLatestClose(fillEvent.symbol))
        currentHoldings = Holdings(
            timestamp = currentHoldings.timestamp,
            positions = currentHoldings.positions,
            cash = currentHoldings.cash.minus(cost + fillEvent.calculateCommission()),
            commission = currentHoldings.commission + fillEvent.calculateCommission(),
            total = currentHoldings.total - (cost + fillEvent.calculateCommission())
        )
        currentHoldings.positions[fillEvent.symbol] = currentHoldings.positions[fillEvent.symbol]?.plus(cost)!!
    }

    private fun generateNaiveOrder(signalEvent: SignalEvent): OrderEvent? {

        val fillCost = getLatestClose(signalEvent.symbol)
        val marketQuantity = (riskManager.sizePosition(signalEvent) * currentHoldings.total) / fillCost

        val currentQuantity = currentPositions.positions[signalEvent.symbol]!!
        val orderType = OrderType.MKT

        var order: OrderEvent? = null
        if (signalEvent.direction == OrderDirection.BUY)
            order = createOrder(signalEvent, orderType, marketQuantity)

        if (signalEvent.direction == OrderDirection.EXIT) {
            if (currentQuantity > Double.ZERO)
                order = createOrder(signalEvent, orderType, currentQuantity)
            else if (currentQuantity < Double.ZERO)
                order = createOrder(signalEvent, orderType, currentQuantity)
        }
        return order
    }

    private fun getLatestClose(symbol: Symbol): Double {
        val close = dataHandler.getLatestBars(symbol).first()
        return close.getDouble(DATA_COLUMN_CLOSE)
    }

    private fun createOrder(signalEvent: SignalEvent, orderType: OrderType, currentQuantity: Double): OrderEvent {
        return OrderEvent(
            symbol = signalEvent.symbol,
            orderType = orderType,
            quantity = currentQuantity,
            direction = signalEvent.direction,
            timestamp = signalEvent.timestamp
        )
    }

    fun createEquityCurve(): Table {

        allHoldings.forEach { holding ->
            equityCurve.dateTimeColumn(DATA_COLUMN_TIMESTAMP).append(holding.timestamp)
            equityCurve.doubleColumn("cash").append(holding.cash)
            equityCurve.doubleColumn("commission").append(holding.commission)
            equityCurve.doubleColumn("total").append(holding.total)
        }
        val returns = equityCurve.doubleColumn("total").pctChange().setName("returns")
        equityCurve.addColumns(returns)
        equityCurve.addColumns(returns.add(1).cumProd().setName("equity_curve"))

        logger.debug(equityCurve.print())

        return equityCurve
    }

    fun printSummaryStats() {

        val totalReturn = equityCurve.last().getDouble("total")
        val returns = equityCurve.doubleColumn("returns")
        val pnl = equityCurve.doubleColumn("equity_curve")

        val sharpeRatio = createSharpeRatio(returns, 365)
        val drawdowns = createDrawdowns(pnl)

        logger.info(
            "Total Return=${((totalReturn - 1.0) * 100.0)}\n" +
                    "Sharpe Ratio=${sharpeRatio}\n" +
                    "Max Drawdown=${(drawdowns.first * 100.0)}\n" +
                    "Drawdown Duration=${drawdowns.second}"
        )
    }
}