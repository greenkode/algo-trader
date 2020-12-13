package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum

class RebalanceEvent ( val signals: List<SignalEvent>): Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.REBALANCE
}