package com.greenkode.trader.broker

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderAction
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.exception.InsufficientFundsException
import com.greenkode.trader.portfolio.HoldingsContainer
import java.math.BigDecimal
import java.util.*


class SimulatedExecutionHandler(private val events: Queue<Event>, private val holdingsContainer: HoldingsContainer) :
    ExecutionHandler {

    override fun executeOrder(event: Event) {
        if (event.type == EventTypeEnum.ORDER) {
            val order = event as OrderEvent

            val quantity = order.quantity
            val executionValue = order.quantity * order.price * BigDecimal.valueOf(1 - 0.001)

            if (order.action == OrderAction.BUY && executionValue > holdingsContainer.getCurrentCash()) {
                throw InsufficientFundsException(
                    holdingsContainer.getCurrentCash(), order.price,
                    "Insufficient funds for transaction ${order.symbol}=${order.quantity * order.price} with " +
                            "available cash ${holdingsContainer.getCurrentCash()}"
                )
            }

            val fillEvent = FillEvent(
                order.symbol,
                order.timestamp,
                "Binance",
                order.orderType,
                quantity,
                order.action,
                order.quantity * order.price
            )
            events.offer(fillEvent)
        }
    }
}