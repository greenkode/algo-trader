package com.greenkode.trader.portfolio

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.*
import com.greenkode.trader.logger.LoggerDelegate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.*

class ReBalancePortfolio(
    private val dataHandler: DataHandler,
    private val events: Queue<Event>,
    private var startDate: LocalDateTime?,
    private val positionsContainer: PositionsContainer,
    commissions: BigDecimal
) : Portfolio() {

    private val logger by LoggerDelegate()
    private val symbols = dataHandler.symbols
    private val orderCreator = OrderCreator(positionsContainer, commissions)


    init {
        if (startDate == null)
            startDate = dataHandler.getEarliestStartDate()
    }

    override fun updateTimeIndex(event: Event) {
        val bars = symbols.associateBy({ it }, { dataHandler.getLatestBars(it, 1) })
        val timestamp = bars[symbols[0]]?.first()?.getDateTime(DATA_COLUMN_TIMESTAMP)!!
        val closePrices = mutableMapOf<Symbol, BigDecimal>()
        symbols.forEach { symbol -> closePrices[symbol] = getLatestClose(symbol) }
        positionsContainer.newRecord(timestamp, closePrices)
    }


    override fun updateFill(event: Event) {
        if (event.type == EventTypeEnum.FILL) {
            updatePositionsFromFill(event as FillEvent)
        }
    }

    override fun updateSignal(event: Event) {
        if (event.type == EventTypeEnum.SIGNAL) {
            val orderEvent = orderCreator.generateNaiveOrder(event as SignalEvent, getLatestClose(event.symbol))
            if (orderEvent.action != OrderAction.NOTHING)
                events.offer(orderEvent)
        } else if (event.type == EventTypeEnum.REBALANCE) {
            val orders = mutableListOf<OrderEvent>()
            (event as RebalanceEvent).signals.forEach {
                orders.add(orderCreator.generateNaiveOrder(it, getLatestClose(it.symbol)))
            }
            events.addAll(orders.sortedByDescending { it.action })
        }
    }

    override fun getCurrentPositions(): Positions {
        return positionsContainer.getCurrentPositions()
    }

    private fun updatePositionsFromFill(fillEvent: FillEvent) {
        positionsContainer.addPosition(
            Position(
                fillEvent.symbol,
                fillEvent.quantity,
                fillEvent.price,
                fillEvent.calculateCommission()
            )
        )
        val mc = MathContext(8, RoundingMode.HALF_UP)
        logger.info(
            "${fillEvent.timestamp} - Order: Symbol=${fillEvent.symbol}, Type=${fillEvent.orderType}, " +
                    "Direction=${fillEvent.orderAction}, " +
                    "Quantity=${fillEvent.quantity.round(mc)}, " +
                    "Price=${fillEvent.price.round(mc)}, " +
                    "Commission=${fillEvent.calculateCommission().round(mc)}, " +
                    "Fill Cost=${(fillEvent.price * fillEvent.quantity).round(mc)}"
        )
    }

    private fun getLatestClose(symbol: Symbol): BigDecimal {
        val close = dataHandler.getLatestBars(symbol)
        if (!close.isEmpty)
            return BigDecimal.valueOf(close.first().getDouble(DATA_COLUMN_CLOSE))
        return BigDecimal.ZERO
    }

    override fun getHistoricalPositions(): List<Positions> {
        return positionsContainer.getHistoricalPositions()
    }
}