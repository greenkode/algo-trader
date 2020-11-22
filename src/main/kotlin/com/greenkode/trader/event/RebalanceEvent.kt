package com.greenkode.trader.event

import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.Symbol
import tech.tablesaw.api.Table

class RebalanceEvent(val rankingTable: Map<Symbol, Double>, val series: Table) : Event {
    override val type: EventTypeEnum
        get() = EventTypeEnum.REBALANCE
}