package com.greenkode.trader.broker

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import java.math.BigDecimal
import java.util.*


class SimulatedExecutionHandler(private val events: Queue<Event>, private val commissionsPercentage: BigDecimal) :
    ExecutionHandler {

    override fun executeOrder(event: Event) {
        if (event.type == EventTypeEnum.ORDER) {
            val order = event as OrderEvent

            val fillEvent = FillEvent(
                order.symbol,
                order.timestamp,
                "Binance",
                order.orderType,
                order.quantity * BigDecimal.valueOf(order.action.value),
                order.price,
                order.action,
                commissionsPercentage
            )
            events.offer(fillEvent)
        }
    }
}