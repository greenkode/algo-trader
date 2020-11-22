package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum

interface Event {
    val type: EventTypeEnum
}
