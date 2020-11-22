package com.greenkode.trader.data

import com.greenkode.trader.domain.Symbol
import tech.tablesaw.api.Table
import java.time.LocalDateTime

interface DataHandler {

    val symbols: List<Symbol>

    fun continueBacktest(): Boolean

    fun updateBars()
    fun getLatestBars(symbol: Symbol, window: Int = 1): Table
    fun getEarliestStartDate(): LocalDateTime
}

