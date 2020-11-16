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

fun volatility(numbers: List<Double>) {
    val prices: DoubleColumn = DoubleColumn.create("prices", numbers).pctChange()
    val table = Table.create(prices).summarize(prices, AggregateFunctions.stdDev).apply()
    print(table)
}
