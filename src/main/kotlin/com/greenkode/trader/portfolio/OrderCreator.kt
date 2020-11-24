package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.domain.ZERO
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.event.SignalEvent

class OrderCreator(
    private val holdingsContainer: HoldingsContainer,
    private val positionsContainer: PositionsContainer
) {

    fun generateNaiveOrder(signal: SignalEvent, closePrice: Double): OrderEvent? {

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