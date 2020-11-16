package com.greenkode.trader.broker

import com.greenkode.trader.event.Event

interface ExecutionHandler {
    fun executeOrder(event: Event)
}