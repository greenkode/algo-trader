package com.greenkode.trader.domain
enum class OrderDirection(val value: Long) {
    LONG(1),
    SELL(-1),
    SHORT(0)
}