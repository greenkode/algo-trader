package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime

class FillEvent(
    val symbol: Symbol, val timeIndex: LocalDateTime, val exchange: String,
    val quantity: BigDecimal, val orderDirection: OrderDirection, val fillCost: BigDecimal
) : Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.FILL

    fun calculateCommission(): Double {
        return quantity.multiply(fillCost).toDouble()
    }
}