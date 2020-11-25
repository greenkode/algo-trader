package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderAction
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime


class OrderEvent(
    val symbol: Symbol,
    val orderType: OrderType,
    val quantity: Double,
    val action: OrderAction,
    val timestamp: LocalDateTime,
    val price: Double
) : Event {

    override val type: EventTypeEnum
        get() = EventTypeEnum.ORDER
}