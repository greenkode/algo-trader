package com.greenkode.trader.domain
enum class OrderDirection(val value: Long) {
    BUY(1),
    SELL(-1),
    EXIT(0)
}