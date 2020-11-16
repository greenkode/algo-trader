package com.greenkode.trader.portfolio

import com.greenkode.trader.analysis.createDrawdowns
import com.greenkode.trader.analysis.createSharpeRatio
import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.event.SignalEvent
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class NaivePortfolio(
    val dataHandler: DataHandler, val events: Queue<Event>, val riskManager: RiskManager,
    var startDate: LocalDateTime?, val initialCapital: BigDecimal
) : Portfolio() {

    private val currentPosition: Position
    private val allPositions: MutableList<Position>
    private var currentHolding: Holding
    private val allHoldings: MutableList<Holding>
    private val symbols = dataHandler.symbols

    private val equityCurve: Table

    init {

        if (startDate == null)
            startDate = dataHandler.getEarliestStartDate()

        currentPosition = constructCurrentPositions()
        allPositions = constructAllPositions()
        allHoldings = constructAllHoldings()
        currentHolding = constructCurrentHoldings()

        val columns = listOf(
            DateTimeColumn.create("timestamp"),
            DoubleColumn.create("returns"),
            DoubleColumn.create("equity_curve"),
            DoubleColumn.create("total")
        )

        equityCurve = Table.create("Equity Curve", columns)
    }

    private fun constructCurrentPositions(): Position {
        val positions = initializePositionsForSymbols()
        return Position(positions = positions)
    }

    private fun constructCurrentHoldings(): Holding {
        return Holding(
            cash = initialCapital,
            commission = 0.0001,
            total = initialCapital,
            positions = initializePositionsForSymbols()
        )
    }

    private fun constructAllPositions(): MutableList<Position> {
        return mutableListOf(Position(positions = initializePositionsForSymbols(), timestamp = startDate))
    }

    private fun constructAllHoldings(): MutableList<Holding> {
        return mutableListOf(
            Holding(
                cash = initialCapital,
                commission = 0.0001,
                total = initialCapital,
                positions = initializePositionsForSymbols(),
                timestamp = startDate
            )
        )
    }

    override fun updateTimeIndex(event: Event) {
        val bars = symbols.associateBy({ it }, { dataHandler.getLatestBars(it, 1) })
        val timestamp = bars[symbols[0]]?.first()?.getDateTime(DATA_COLUMN_TIMESTAMP)!!
        val dp = Position(
            positions = initializePositionsForSymbols(),
            timestamp = timestamp
        )

        symbols.forEach { symbol ->
            currentPosition.positions[symbol]?.let { dp.positions[symbol] = it }
        }
        allPositions.add(dp)

        val dh = Holding(
            positions = initializePositionsForSymbols(),
            timestamp = timestamp,
            cash = currentHolding.cash,
            commission = currentHolding.commission,
            total = currentHolding.total
        )

        symbols.forEach { symbol ->
            val marketValue =
                currentPosition.positions[symbol]?.multiply(
                    BigDecimal.valueOf(
                        bars[symbols[0]]?.first()?.getDouble(DATA_COLUMN_CLOSE)!!
                    )
                ) ?: BigDecimal.ZERO
            dh.positions[symbol] = marketValue
            dh.addToTotal(marketValue)
        }

        allHoldings.add(dh)
    }

    private fun initializePositionsForSymbols() = symbols.associateBy({ it }, { BigDecimal.ZERO }).toMutableMap()

    override fun updateFill(event: Event) {
        if (event.type == EventTypeEnum.FILL) {
            updatePositionsFromFill(event as FillEvent)
            updateHoldingsFromFill(event)
        }
    }

    override fun updateSignal(event: Event) {
        if (event.type == EventTypeEnum.SIGNAL) {
            val orderEvent = generateNaiveOrder(event as SignalEvent)
            if(orderEvent != null)
                events.offer(orderEvent)
        }
    }

    private fun updatePositionsFromFill(fillEvent: FillEvent) {
        val currPosition = currentPosition.positions[fillEvent.symbol]
        currentPosition.positions[fillEvent.symbol] =
            currPosition?.add(fillEvent.quantity.multiply(BigDecimal.valueOf(fillEvent.orderDirection.value)))!!
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {

        val fillCost = dataHandler.getLatestBars(fillEvent.symbol).first().getDouble("close")
        val cost = fillEvent.quantity.multiply(BigDecimal.valueOf(fillEvent.orderDirection.value * fillCost))
        currentHolding = Holding(
            timestamp = currentHolding.timestamp,
            positions = currentHolding.positions,
            cash = currentHolding.cash.minus(cost.plus(BigDecimal.valueOf(fillEvent.calculateCommission()))),
            commission = currentHolding.commission + fillEvent.calculateCommission(),
            total = currentHolding.total.min(cost.plus(BigDecimal.valueOf(fillEvent.calculateCommission())))
        )
        currentHolding.positions[fillEvent.symbol] = currentHolding.positions[fillEvent.symbol]?.plus(cost)!!
    }

    private fun generateNaiveOrder(signalEvent: SignalEvent): OrderEvent? {

        val marketQuantity = riskManager.sizePosition(signalEvent).multiply(BigDecimal.valueOf(signalEvent.strength))
        val currentQuantity = currentPosition.positions[signalEvent.symbol]!!
        val orderType = OrderType.MKT

        var order: OrderEvent? = null
        if (currentQuantity.toDouble() == 0.0) {
            order = OrderEvent(
                symbol = signalEvent.symbol,
                orderType = orderType,
                quantity = marketQuantity,
                direction = signalEvent.direction,
                timestamp = signalEvent.timestamp
            )
        }

        if (signalEvent.direction == OrderDirection.EXIT) {
            if (currentQuantity > BigDecimal.ZERO)
                order = OrderEvent(
                    symbol = signalEvent.symbol,
                    orderType = orderType,
                    quantity = currentQuantity.abs(),
                    direction = OrderDirection.SELL,
                    timestamp = signalEvent.timestamp
                )
            else if (currentQuantity < BigDecimal.ZERO)
                order = OrderEvent(
                    symbol = signalEvent.symbol,
                    orderType = orderType,
                    quantity = currentQuantity.abs(),
                    direction = OrderDirection.BUY,
                    timestamp = signalEvent.timestamp
                )
        }
        return order
    }

    fun createEquityCurve(): Table {

        equityCurve.doubleColumn("total").pctChange()
        equityCurve.doubleColumn("equity_curve").cumProd()

        return equityCurve
    }

    fun printSummaryStats(): List<String> {

        val totalReturn = equityCurve.last().getDouble("total")
        val returns = equityCurve.doubleColumn("returns")
        val pnl = equityCurve.doubleColumn("equity_curve")

        val sharpeRatio = createSharpeRatio(returns, 365)
        val drawdowns = createDrawdowns(pnl)

        return listOf<String>(
            "Total Return=${((totalReturn - 1.0) * 100.0)}",
            "Sharpe Ratio=${sharpeRatio}",
            "Max Drawdown=${(drawdowns.first * 100.0)}",
            "Drawdown Duration=${drawdowns.second}"
        )
    }
}