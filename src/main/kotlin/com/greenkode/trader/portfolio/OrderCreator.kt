package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.OrderAction
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.domain.ZERO
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.event.SignalEvent
import kotlin.math.abs

class OrderCreator(
    private val holdingsContainer: HoldingsContainer,
    private val positionsContainer: PositionsContainer
) {

    fun generateNaiveOrder(signal: SignalEvent, closePrice: Double): OrderEvent {

        val marketQuantity =
            ((signal.strength * holdingsContainer.getCurrentTotal()) / closePrice + positionsContainer.getQuantityForSymbol(
                signal.symbol
            )) * (1 - 0.001)

        val currentQuantity = positionsContainer.getQuantityForSymbol(signal.symbol)

        val orderType = OrderType.MKT

        var order = OrderEvent(signal.symbol, OrderType.MKT, 0.0, OrderAction.NOTHING, signal.timestamp, 0.0)

        when {
            signal.direction == OrderDirection.LONG && currentQuantity == Double.ZERO -> order =
                createOrder(signal, orderType, marketQuantity, OrderAction.BUY, closePrice)

            signal.direction == OrderDirection.SHORT && currentQuantity == Double.ZERO -> order =
                createOrder(signal, orderType, marketQuantity, OrderAction.SELL, closePrice)

            signal.direction == OrderDirection.LONG && currentQuantity - marketQuantity > 0 -> order =
                createOrder(signal, orderType, abs(currentQuantity - marketQuantity), OrderAction.SELL, closePrice)

            signal.direction == OrderDirection.LONG && currentQuantity - marketQuantity < 0 -> order =
                createOrder(signal, orderType, abs(currentQuantity - marketQuantity), OrderAction.BUY, closePrice)

            signal.direction == OrderDirection.EXIT && currentQuantity > Double.ZERO -> order =
                createOrder(signal, orderType, abs(currentQuantity), OrderAction.SELL, closePrice)

            signal.direction == OrderDirection.EXIT && currentQuantity < Double.ZERO -> order =
                createOrder(signal, orderType, abs(currentQuantity), OrderAction.BUY, closePrice)
        }

        return order
    }

    private fun createOrder(
        signalEvent: SignalEvent,
        orderType: OrderType,
        currentQuantity: Double,
        action: OrderAction,
        price: Double
    ): OrderEvent {
        return OrderEvent(
            symbol = signalEvent.symbol,
            orderType = orderType,
            quantity = currentQuantity,
            timestamp = signalEvent.timestamp,
            action = action,
            price = price
        )
    }
}