package com.greenkode.trader.strategy

import com.greenkode.trader.event.Event

abstract class Strategy {
    abstract fun calculateSignals(event: Event)
}