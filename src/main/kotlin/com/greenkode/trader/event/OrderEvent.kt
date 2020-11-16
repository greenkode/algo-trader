package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime


class OrderEvent(
    val symbol: Symbol,
    val orderType: OrderType,
    val quantity: BigDecimal,
    val direction: OrderDirection,
    val timestamp: LocalDateTime
) : Event {

    override val type: EventTypeEnum
        get() = EventTypeEnum.ORDER

    override fun toString(): String {
        return "${timestamp.toLocalDate()} - Order: Symbol=$symbol, Type=$orderType, Quantity=$quantity, Direction=$direction"
    }
}