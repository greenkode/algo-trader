package com.greenkode.trader.domain

const val DATA_COLUMN_TIMESTAMP = "timestamp"
const val DATA_COLUMN_OPEN = "open"
const val DATA_COLUMN_HIGH = "high"
const val DATA_COLUMN_LOW = "low"
const val DATA_COLUMN_CLOSE = "close"
const val DATA_COLUMN_VOLUME = "volume"

const val EQUITY_CURVE_CASH = "cash"
const val EQUITY_CURVE_COMMISSION = "commission"
const val EQUITY_CURVE_TOTAL = "total"
const val EQUITY_CURVE_RETURNS = "returns"
const val EQUITY_CURVE_CURVE = "equity_curve"


val Double.Companion.ZERO: Double
    get() {
        return 0.0
    }