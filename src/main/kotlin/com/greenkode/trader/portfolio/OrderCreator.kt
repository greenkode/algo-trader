package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.OrderAction
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.event.SignalEvent
import java.math.BigDecimal

class OrderCreator(private val positionsContainer: PositionsContainer, private val commissions: BigDecimal) {

    fun generateNaiveOrder(signal: SignalEvent, closePrice: BigDecimal): OrderEvent {

        val marketQuantity =
            ((signal.strength * positionsContainer.getCurrentValue()) / closePrice) * (BigDecimal.valueOf(1) - commissions)

        val currentQuantity = positionsContainer.getQuantityForSymbol(signal.symbol)

        val orderType = OrderType.MKT

        var order = OrderEvent(
            signal.symbol,
            OrderType.MKT,
            BigDecimal.ZERO,
            OrderAction.NOTHING,
            signal.timestamp,
            BigDecimal.ZERO
        )

        when {
            signal.direction == OrderDirection.LONG && currentQuantity == BigDecimal.ZERO -> order =
                createOrder(signal, orderType, marketQuantity, OrderAction.BUY, closePrice)

            signal.direction == OrderDirection.SHORT && currentQuantity == BigDecimal.ZERO -> order =
                createOrder(signal, orderType, marketQuantity, OrderAction.SELL, closePrice)

            signal.direction == OrderDirection.LONG && currentQuantity - marketQuantity > BigDecimal.ZERO -> order =
                createOrder(signal, orderType, (currentQuantity - marketQuantity).abs(), OrderAction.SELL, closePrice)

            signal.direction == OrderDirection.LONG && currentQuantity - marketQuantity < BigDecimal.ZERO -> order =
                createOrder(signal, orderType, (currentQuantity - marketQuantity).abs(), OrderAction.BUY, closePrice)

            signal.direction == OrderDirection.EXIT && currentQuantity > BigDecimal.ZERO -> order =
                createOrder(signal, orderType, (currentQuantity).abs(), OrderAction.SELL, closePrice)

            signal.direction == OrderDirection.EXIT && currentQuantity < BigDecimal.ZERO -> order =
                createOrder(signal, orderType, (currentQuantity).abs(), OrderAction.BUY, closePrice)
        }

        return order
    }

    private fun createOrder(
        signalEvent: SignalEvent,
        orderType: OrderType,
        currentQuantity: BigDecimal,
        action: OrderAction,
        price: BigDecimal
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