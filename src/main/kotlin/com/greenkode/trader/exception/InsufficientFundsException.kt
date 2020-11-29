package com.greenkode.trader.exception

import java.math.BigDecimal

class InsufficientFundsException(val cash: BigDecimal, val price: BigDecimal, override val message: String ) : Exception()
