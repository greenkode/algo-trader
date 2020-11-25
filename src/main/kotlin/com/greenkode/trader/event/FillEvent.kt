package com.greenkode.trader.event

import com.greenkode.trader.domain.*
import java.time.LocalDateTime

class FillEvent(
    val symbol: Symbol, val timestamp: LocalDateTime, val exchange: String, val orderType: OrderType,
    val quantity: Double, val orderAction: OrderAction, val fillCost: Double
) : Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.FILL

    fun calculateCommission(): Double {
        return fillCost * 0.001
    }
}