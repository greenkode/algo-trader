package com.greenkode.trader.portfolio

import com.greenkode.trader.event.SignalEvent
import java.math.BigDecimal
import kotlin.math.min

class RiskManager {
    fun sizePosition(signal: SignalEvent): BigDecimal {
        return BigDecimal.valueOf(min(0.2, signal.strength))
    }
}
