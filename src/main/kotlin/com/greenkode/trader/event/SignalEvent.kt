package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.Symbol
import java.time.LocalDateTime


class SignalEvent(val symbol: Symbol, val timestamp: LocalDateTime, val direction: OrderDirection, val strength: Double) :
    Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.SIGNAL
}