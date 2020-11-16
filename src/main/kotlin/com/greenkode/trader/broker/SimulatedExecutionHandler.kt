package com.greenkode.trader.broker

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.FillEvent
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.logger.LoggerDelegate
import java.math.BigDecimal
import java.util.*


class SimulatedExecutionHandler(private val events: Queue<Event>) : ExecutionHandler {

    val logger by LoggerDelegate()

    override fun executeOrder(event: Event) {
        if (event.type == EventTypeEnum.ORDER) {
            val order = event as OrderEvent
            val fillEvent = FillEvent(
                order.symbol,
                order.timestamp,
                "Binance",
                order.quantity,
                order.direction,
                BigDecimal.valueOf(0.1)
            )
            logger.info("$order")
            events.offer(fillEvent)
        }
    }
}