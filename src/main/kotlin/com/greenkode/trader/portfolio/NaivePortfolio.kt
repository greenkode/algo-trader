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
    private val dataHandler: DataHandler, private val events: Queue<Event>, private val riskManager: RiskManager,
    private var startDate: LocalDateTime?, private val initialCapital: Double
) : Portfolio() {

    private val logger by LoggerDelegate()

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
            DateTimeColumn.create(DATA_COLUMN_TIMESTAMP),
            DoubleColumn.create(EQUITY_CURVE_CASH),
            DoubleColumn.create(EQUITY_CURVE_COMMISSION),
            DoubleColumn.create(EQUITY_CURVE_TOTAL)
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
            commission = 0.0,
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
                commission = 0.0,
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
            total = currentHoldings.cash
        )

        symbols.forEach { symbol ->
            val marketValue = currentPositions.positions[symbol]!! * getLatestClose(symbol)
            dh.positions[symbol] = marketValue
            dh.total += marketValue
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
        currentPositions.timestamp = fillEvent.timeIndex
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {
        val closePrice = getLatestClose(fillEvent.symbol)
        val cost = fillEvent.quantity * fillEvent.orderDirection.value * closePrice
        currentHoldings.positions[fillEvent.symbol] = currentHoldings.positions[fillEvent.symbol]!! + cost
        currentHoldings = Holdings(
            timestamp = fillEvent.timeIndex,
            positions = currentHoldings.positions,
            commission = currentHoldings.commission + fillEvent.calculateCommission(),
            cash = currentHoldings.cash - (cost + fillEvent.calculateCommission()),
            total = currentHoldings.total - (cost + fillEvent.calculateCommission())
        )

        logger.info(
            "${fillEvent.timeIndex} - Order: Symbol=${fillEvent.symbol}, Type=${fillEvent.orderType}, " +
                    "Direction=${fillEvent.orderDirection}, Quantity=${fillEvent.quantity}, Price=${closePrice}, " +
                    "Commission=${fillEvent.calculateCommission()}, Fill Cost=${fillEvent.fillCost}"
        )
    }

    private fun generateNaiveOrder(signalEvent: SignalEvent): OrderEvent? {

        val closePrice = getLatestClose(signalEvent.symbol)
        val marketQuantity = (riskManager.sizePosition(signalEvent) * currentHoldings.total) / closePrice

        val currentQuantity = currentPositions.positions[signalEvent.symbol]!!
        val orderType = OrderType.MKT

        var order: OrderEvent? = null
        if (signalEvent.direction == OrderDirection.BUY) order =
            createOrder(signalEvent, orderType, marketQuantity, closePrice)

        if (signalEvent.direction == OrderDirection.EXIT) {
            if (currentQuantity > Double.ZERO)
                order = createOrder(signalEvent, orderType, currentQuantity, closePrice)
            else if (currentQuantity < Double.ZERO)
                order = createOrder(signalEvent, orderType, currentQuantity, closePrice)
        }
        return order
    }

    private fun getLatestClose(symbol: Symbol): Double {
        val close = dataHandler.getLatestBars(symbol)
        if (!close.isEmpty)
            return close.first().getDouble(DATA_COLUMN_CLOSE)
        return 0.0
    }

    private fun createOrder(
        signalEvent: SignalEvent,
        orderType: OrderType,
        currentQuantity: Double,
        price: Double
    ): OrderEvent {
        return OrderEvent(
            symbol = signalEvent.symbol,
            orderType = orderType,
            quantity = currentQuantity,
            direction = signalEvent.direction,
            timestamp = signalEvent.timestamp,
            price = price
        )
    }

    fun createEquityCurve(): Table {

        allHoldings.forEach { holding ->
            equityCurve.dateTimeColumn(DATA_COLUMN_TIMESTAMP).append(holding.timestamp)
            equityCurve.doubleColumn(EQUITY_CURVE_CASH).append(holding.cash)
            equityCurve.doubleColumn(EQUITY_CURVE_COMMISSION).append(holding.commission)
            equityCurve.doubleColumn(EQUITY_CURVE_TOTAL).append(holding.total)
        }
        val returns = equityCurve.doubleColumn(EQUITY_CURVE_TOTAL).pctChange().setName(EQUITY_CURVE_RETURNS)
        equityCurve.addColumns(returns)
        equityCurve.addColumns(returns.add(1).cumProd().setName(EQUITY_CURVE_CURVE))

        logger.debug(equityCurve.print())

        return equityCurve
    }

    fun printSummaryStats() {

        val totalReturn = equityCurve.last().getDouble(EQUITY_CURVE_TOTAL)
        val returns = equityCurve.doubleColumn(EQUITY_CURVE_RETURNS)
        val pnl = equityCurve.doubleColumn(EQUITY_CURVE_CURVE)

        val sharpeRatio = createSharpeRatio(returns, 365)
        val drawDowns = createDrawdowns(pnl)

        logger.info(
            "Total Return=${((totalReturn - 1.0) * 100.0)}\n" +
                    "Sharpe Ratio=${sharpeRatio}\n" +
                    "Max DrawDown=${(drawDowns.first * 100.0)}\n" +
                    "DrawDown Duration=${drawDowns.second}"
        )
    }
}