package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.OrderType
import com.greenkode.trader.domain.Symbol
import java.math.BigDecimal
import java.time.LocalDateTime

interface Event {
    val type: EventTypeEnum
}
