package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import java.math.BigDecimal

abstract class Portfolio {

    abstract fun updateTimeIndex(event: Event)

    abstract fun updateFill(event: Event)

    abstract fun updateSignal(event: Event)

    abstract fun getCurrentPositions(): Positions

    abstract fun getHistoricalPositions(): List<Positions>
}