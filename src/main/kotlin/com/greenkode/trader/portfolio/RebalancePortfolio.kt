package com.greenkode.trader.portfolio

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.event.SignalEvent
import com.greenkode.trader.logger.LoggerDelegate
import java.time.LocalDateTime
import java.util.*

class RebalancePortfolio(
    private val dataHandler: DataHandler,
    private val events: Queue<Event>,
    private var startDate: LocalDateTime?,
    private val positionsContainer: PositionsContainer,
    private val holdingsContainer: HoldingsContainer
) : Portfolio() {

    private val logger by LoggerDelegate()
    private val symbols = dataHandler.symbols


    init {
        if (startDate == null)
            startDate = dataHandler.getEarliestStartDate()
    }

    override fun updateTimeIndex(event: Event) {
        val bars = symbols.associateBy({ it }, { dataHandler.getLatestBars(it, 1) })
        val timestamp = bars[symbols[0]]?.first()?.getDateTime(DATA_COLUMN_TIMESTAMP)!!
        positionsContainer.newRecord(timestamp)
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
            val orderEvent = generateNaiveOrder(event as SignalEvent)
            if (orderEvent != null)
                events.offer(orderEvent)
        }
    }

    override fun getHoldings(): List<Holdings> {
        return holdingsContainer.getHoldingsHistory()
    }

    private fun updatePositionsFromFill(fillEvent: FillEvent) {
        positionsContainer.updateQuantity(
            fillEvent.symbol,
            fillEvent.quantity * fillEvent.orderDirection.value - fillEvent.calculateCommission()
        )
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {
        val closePrice = getLatestClose(fillEvent.symbol)
        val cost = fillEvent.quantity * fillEvent.orderDirection.value * closePrice

        holdingsContainer.updateHoldings(fillEvent.symbol, cost, fillEvent.calculateCommission())

        logger.info(
            "${fillEvent.timestamp} - Order: Symbol=${fillEvent.symbol}, Type=${fillEvent.orderType}, " +
                    "Direction=${fillEvent.orderDirection}, Quantity=${fillEvent.quantity}, Price=${closePrice}, " +
                    "Commission=${fillEvent.calculateCommission()}, Fill Cost=${fillEvent.fillCost}"
        )
    }

    private fun generateNaiveOrder(signal: SignalEvent): OrderEvent? {

        val closePrice = getLatestClose(signal.symbol)
        val marketQuantity = (signal.strength * holdingsContainer.getCurrentTotal()) / closePrice

        val currentQuantity = positionsContainer.getCurrentQuantity(signal.symbol)
        val orderType = OrderType.MKT

        var order: OrderEvent? = null
        if (signal.direction == OrderDirection.LONG) order =
            createOrder(signal, orderType, marketQuantity, closePrice)

        if (signal.direction == OrderDirection.SHORT) {
            if (currentQuantity > Double.ZERO)
                order = createOrder(signal, orderType, currentQuantity, closePrice)
            else if (currentQuantity < Double.ZERO)
                order = createOrder(signal, orderType, currentQuantity, closePrice)
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
}