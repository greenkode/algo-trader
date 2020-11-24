package com.greenkode.trader.strategy

import com.greenkode.trader.data.DataHandler
import com.greenkode.trader.domain.*
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.SignalEvent
import org.ta4j.core.BarSeries
import org.ta4j.core.BaseBarSeriesBuilder
import tech.tablesaw.api.Row
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class MultipleEmaStrategy(val dataHandler: DataHandler, val events: Queue<Event>) : Strategy() {
    var bought = mutableMapOf<Symbol, Boolean>()
    lateinit var currentDate: LocalDateTime

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

        val series = getBars(20)
        
    }

    private fun sendSignal(symbol: Symbol, quantity: Double) {
        events.add(SignalEvent(symbol, currentDate, OrderDirection.LONG, 0.2))
    }

    private fun getBars(window: Int): BarSeries {

        val series = BaseBarSeriesBuilder().withName("BTCUSDT").build()
        val table = dataHandler.getLatestBars(Symbol("BTCUSDT"), window = window)
        currentDate = table.dateTimeColumn(DATA_COLUMN_TIMESTAMP).last()
        if (!table.isEmpty && table.count() == window) {
            for (row: Row in table) {
                series.addBar(
                    ZonedDateTime.of(row.getDateTime(DATA_COLUMN_TIMESTAMP), ZoneId.systemDefault()),
                    row.getDouble(DATA_COLUMN_OPEN),
                    row.getDouble(DATA_COLUMN_HIGH),
                    row.getDouble(DATA_COLUMN_LOW),
                    row.getDouble(DATA_COLUMN_CLOSE),
                    row.getDouble(DATA_COLUMN_VOLUME)
                )
            }
        }

        return series
    }
}