package com.greenkode.trader.domain

enum class OrderAction(val value: Long) {

    BUY(1),
    SELL(-1),
    NOTHING(0)
}