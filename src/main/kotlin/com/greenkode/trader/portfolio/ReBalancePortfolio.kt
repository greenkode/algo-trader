package com.greenkode.trader.portfolio

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.SignalEvent
import com.greenkode.trader.logger.LoggerDelegate
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
        }
    }

    override fun getHistoricalHoldings(): List<Holdings> {
        return holdingsContainer.getHoldingsHistory()
    }

    override fun getCurrentHoldings(): Map<Symbol, Double> {
        return holdingsContainer.getCurrentHoldings().holdings
    }

    override fun getCurrentPositions(): Map<Symbol, Double> {
        return positionsContainer.getCurrentPositions().positions
    }

    private fun updatePositionsFromFill(fillEvent: FillEvent) {
        positionsContainer.updateQuantity(
            fillEvent.symbol,
            fillEvent.quantity * fillEvent.orderAction.value
        )
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {
        val closePrice = getLatestClose(fillEvent.symbol)
        val cost = fillEvent.quantity * fillEvent.orderAction.value * closePrice

        holdingsContainer.updateHoldings(fillEvent.symbol, cost, fillEvent.calculateCommission())

        logger.info(
            "${fillEvent.timestamp} - Order: Symbol=${fillEvent.symbol}, Type=${fillEvent.orderType}, " +
                    "Direction=${fillEvent.orderAction}, Quantity=${fillEvent.quantity}, Price=${closePrice}, " +
                    "Commission=${fillEvent.calculateCommission()}, Fill Cost=${fillEvent.fillCost}"
        )
    }

    private fun getLatestClose(symbol: Symbol): Double {
        val close = dataHandler.getLatestBars(symbol)
        if (!close.isEmpty)
            return close.first().getDouble(DATA_COLUMN_CLOSE)
        return 0.0
    }
}