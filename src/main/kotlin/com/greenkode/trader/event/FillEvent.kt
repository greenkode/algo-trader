package com.greenkode.trader.event

import com.greenkode.trader.domain.*
import java.math.BigDecimal
import java.time.LocalDateTime

class FillEvent(
    val symbol: Symbol, val timestamp: LocalDateTime, val exchange: String, val orderType: OrderType,
    val quantity: BigDecimal, val orderAction: OrderAction, val fillCost: BigDecimal
) : Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.FILL

    fun calculateCommission(): BigDecimal {
        return fillCost * BigDecimal.valueOf(0.001)
    }
}