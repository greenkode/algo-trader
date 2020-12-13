package com.greenkode.trader.portfolio

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.*
import com.greenkode.trader.logger.LoggerDelegate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class ReBalancePortfolio(
    private val dataHandler: DataHandler,
    private val events: Queue<Event>,
    private var startDate: LocalDateTime?,
    private val positionsContainer: PositionsContainer,
    private val holdingsContainer: HoldingsContainer
) : Portfolio() {

    private val logger by LoggerDelegate()
    private val symbols = dataHandler.symbols
    private val orderCreator = OrderCreator(holdingsContainer, positionsContainer)


    init {
        if (startDate == null)
            startDate = dataHandler.getEarliestStartDate()
    }

    override fun updateTimeIndex(event: Event) {
        val bars = symbols.associateBy({ it }, { dataHandler.getLatestBars(it, 1) })
        val timestamp = bars[symbols[0]]?.first()?.getDateTime(DATA_COLUMN_TIMESTAMP)!!
        positionsContainer.newRecord(timestamp, positionsContainer.getCurrentPositions())
        holdingsContainer.newRecord(timestamp, positionsContainer.getCurrentPositions(), bars)
    }


    override fun updateFill(event: Event) {
        if (event.type == EventTypeEnum.FILL) {
            updatePositionsFromFill(event as FillEvent)
            updateHoldingsFromFill(event)
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

    override fun getHistoricalHoldings(): List<Holdings> {
        return holdingsContainer.getHoldingsHistory()
    }

    override fun getCurrentHoldings(): Map<Symbol, BigDecimal> {
        return holdingsContainer.getCurrentHoldings().holdings
    }

    override fun getCurrentPositions(): Map<Symbol, BigDecimal> {
        return positionsContainer.getCurrentPositions().positions
    }

    private fun updatePositionsFromFill(fillEvent: FillEvent) {
        positionsContainer.updateQuantity(
            fillEvent.symbol,
            fillEvent.quantity * BigDecimal.valueOf(fillEvent.orderAction.value)
        )
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {
        val closePrice = getLatestClose(fillEvent.symbol)
        val cost = fillEvent.quantity * BigDecimal.valueOf(fillEvent.orderAction.value) * closePrice

        holdingsContainer.updateHoldings(fillEvent.symbol, cost, fillEvent.calculateCommission())

        logger.info(
            "${fillEvent.timestamp} - Order: Symbol=${fillEvent.symbol}, Type=${fillEvent.orderType}, " +
                    "Direction=${fillEvent.orderAction}, Quantity=${fillEvent.quantity.toDouble()}, Price=${closePrice}, " +
                    "Commission=${
                        fillEvent.calculateCommission().toDouble()
                    }, Fill Cost=${fillEvent.fillCost.toDouble()}"
        )
    }

    private fun getLatestClose(symbol: Symbol): BigDecimal {
        val close = dataHandler.getLatestBars(symbol)
        if (!close.isEmpty)
            return BigDecimal.valueOf(close.first().getDouble(DATA_COLUMN_CLOSE))
        return BigDecimal.ZERO
    }
}