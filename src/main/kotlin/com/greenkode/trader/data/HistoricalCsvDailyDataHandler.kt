package com.greenkode.trader.data

import com.greenkode.trader.domain.DATA_COLUMN_TIMESTAMP
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.MarketEvent
import com.greenkode.trader.logger.LoggerDelegate
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.Table
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.TemporalUnit
import java.util.*

class HistoricalCsvDailyDataHandler(
    private val events: Queue<Event>,
    private val directory: String,
    override val symbols: List<Symbol>,
    private val startDateTime: LocalDateTime?
) : DataHandler {

    val logger by LoggerDelegate()

    val symbolData = mutableMapOf<Symbol, Table>()
    var continueBacktest = true
    var currentDate: LocalDateTime
    var lastDate: LocalDateTime = LocalDateTime.MIN


    init {
        loadCsvFiles()
        currentDate = getEarliestStartDate()
    }

    private fun loadCsvFiles() {
        val symbolNames = symbols.map { it.name }
        File(directory).walk().filter {
            !it.isDirectory && it.name.split('-')[1] in symbolNames
        }.forEach { file ->
            val symbol = Symbol(file.name.split('-')[1])
            val table = Table.read().csv(file)
            val column = DateTimeColumn.create(
                DATA_COLUMN_TIMESTAMP,
                table.dateColumn(DATA_COLUMN_TIMESTAMP).atStartOfDay().asList()
            )
            table.replaceColumn(DATA_COLUMN_TIMESTAMP, column)
            symbolData[symbol] = table
        }
    }

    override fun continueBacktest(): Boolean {
        return continueBacktest
    }

    override fun updateBars(amount: Long, temporalUnit: TemporalUnit) {
        if (currentDate.isEqual(lastDate)) {
            continueBacktest = false
            logger.info("Last date executed: $currentDate")
        } else {
            events.add(MarketEvent())
            currentDate = currentDate.plus(amount, temporalUnit)
            if(currentDate.isAfter(lastDate))
                currentDate = lastDate
        }
    }

    override fun getLatestBars(symbol: Symbol, window: Int): Table {
        val table = symbolData[symbol]!!
        return table.where(
            table.dateTimeColumn(DATA_COLUMN_TIMESTAMP)
                .isBetweenIncluding(currentDate.minusDays(window.toLong() - 1), currentDate)
        )
    }

    override fun getEarliestStartDate(): LocalDateTime {

        return if(startDateTime!== null) startDateTime
        else {
            var earliestDate = LocalDateTime.MAX
            symbolData.forEach { (_, table) ->
                val date = table.first().getDateTime(DATA_COLUMN_TIMESTAMP)
                earliestDate = if (date < earliestDate) date else earliestDate
                val lDate = table.last().getDateTime(DATA_COLUMN_TIMESTAMP)
                lastDate = if (lDate > lastDate) lDate else lastDate
            }
            earliestDate
        }
    }
}