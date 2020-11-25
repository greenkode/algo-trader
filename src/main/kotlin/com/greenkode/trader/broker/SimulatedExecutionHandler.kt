package com.greenkode.trader.broker

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import java.util.*


class SimulatedExecutionHandler(private val events: Queue<Event>) : ExecutionHandler {

    override fun executeOrder(event: Event) {
        if (event.type == EventTypeEnum.ORDER) {
            val order = event as OrderEvent
            val fillEvent = FillEvent(
                order.symbol,
                order.timestamp,
                "Binance",
                order.orderType,
                order.quantity,
                order.action,
                order.quantity * order.price
            )
            events.offer(fillEvent)
        }
    }
}