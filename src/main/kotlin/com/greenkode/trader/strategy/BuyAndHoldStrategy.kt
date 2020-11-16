package com.greenkode.trader.strategy

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.DATA_COLUMN_TIMESTAMP
import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.OrderDirection
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.SignalEvent
import java.util.*

class BuyAndHoldStrategy(val dataHandler: DataHandler, val events: Queue<Event>) : Strategy() {

    var bought = mutableMapOf<Symbol, Boolean>()

    init {
        calculateInitialBought()
    }

    private fun calculateInitialBought() {
        dataHandler.symbols.forEach { symbol ->
            bought[symbol] = false
        }
    }

    override fun calculateSignals(event: Event) {
        if (event.type != EventTypeEnum.MARKET) return
        dataHandler.symbols.forEach { symbol ->
            val bars = dataHandler.getLatestBars(symbol, 1)
            if (!bars.isEmpty) {
                events.add(
                    SignalEvent(
                        symbol,
                        bars.dateTimeColumn(DATA_COLUMN_TIMESTAMP).last(),
                        OrderDirection.BUY,
                        0.2
                    )
                )
                bought[symbol] = true
            }
        }
    }
}
