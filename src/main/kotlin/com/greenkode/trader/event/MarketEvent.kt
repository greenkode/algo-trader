package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum

class MarketEvent : Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.MARKET
}