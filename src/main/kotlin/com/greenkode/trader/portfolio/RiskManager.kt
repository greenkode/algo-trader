package com.greenkode.trader.portfolio

import com.greenkode.trader.event.SignalEvent
import java.math.BigDecimal

class RiskManager {
    fun sizePosition(signal: SignalEvent): BigDecimal {
        return BigDecimal.valueOf(0.2)
    }

}
