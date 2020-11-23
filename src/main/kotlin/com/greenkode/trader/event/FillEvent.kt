package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime

class FillEvent(
    val symbol: Symbol, val timestamp: LocalDateTime, val exchange: String, val orderType: OrderType,
    val quantity: Double, val orderDirection: OrderDirection, val fillCost: Double
) : Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.FILL

    fun calculateCommission(): Double {
        return fillCost * 0.001
    }
}