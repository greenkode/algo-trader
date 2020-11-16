package com.greenkode.trader.strategy

import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.DATA_COLUMN_TIMESTAMP
import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.SignalEvent
import tech.tablesaw.aggregate.AggregateFunctions
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import java.time.LocalDateTime
import java.util.*

abstract class Strategy {
    abstract fun calculateSignals(event: Event)
}