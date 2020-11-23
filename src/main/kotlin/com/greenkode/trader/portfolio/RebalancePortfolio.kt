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
    private val riskManager: RiskManager,
    private var startDate: LocalDateTime?,
    private val initialCapital: Double,
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
            fillEvent.timestamp,
            fillEvent.quantity * fillEvent.orderDirection.value
        )
    }

    private fun updateHoldingsFromFill(fillEvent: FillEvent) {
        val closePrice = getLatestClose(fillEvent.symbol)
        val cost = fillEvent.quantity * fillEvent.orderDirection.value * closePrice

        holdingsContainer.updateHoldings(fillEvent.timestamp, cost, positionsContainer.getCurrentPositions())

        logger.info(
            "${fillEvent.timestamp} - Order: Symbol=${fillEvent.symbol}, Type=${fillEvent.orderType}, " +
                    "Direction=${fillEvent.orderDirection}, Quantity=${fillEvent.quantity}, Price=${closePrice}, " +
                    "Commission=${fillEvent.calculateCommission()}, Fill Cost=${fillEvent.fillCost}"
        )
    }

    private fun generateNaiveOrder(signalEvent: SignalEvent): OrderEvent? {

        val closePrice = getLatestClose(signalEvent.symbol)
        val marketQuantity = (riskManager.sizePosition(signalEvent) * holdingsContainer.getCurrentTotal()) / closePrice

        val currentQuantity = positionsContainer.getCurrentQuantity()
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
}