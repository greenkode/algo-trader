package com.greenkode.trader.portfolio

import com.greenkode.trader.domain.DATA_COLUMN_TIMESTAMP
import com.greenkode.trader.domain.Symbol
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.Table
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.LocalDateTime

internal class HoldingsContainerTest {

    private lateinit var container: HoldingsContainer

    private val BTCUSDT = "BTCUSDT"
    private val ETHUSDT = "ETHUSDT"
    private val symbols = listOf(Symbol(BTCUSDT), Symbol(ETHUSDT))

    private val COMMISSION = BigDecimal.valueOf(0.001)

    @BeforeEach
    fun setup() {
        container = HoldingsContainer(BigDecimal.valueOf(10000), symbols)
    }

    @Test
    fun `Test Portfolio Properly Initialized`() {
        assertThat(container.getCurrentTotal()).isEqualTo(BigDecimal.valueOf(10000))
        assertThat(container.getCurrentHoldings().getTotal()).isEqualTo(BigDecimal.valueOf(10000))
        assertThat(container.getCurrentHoldings().getCash()).isEqualTo(BigDecimal.valueOf(10000))
        assertThat(container.getCommissions()).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `Test Buying An Asset`() {

        var dateTime = LocalDate.now().atStartOfDay()

        container.newRecord(
            dateTime,
            Positions(dateTime, createPositions(dateTime, container.getCurrentHoldings().holdings)),
            initializeTable(dateTime)
        )

        // Buy bitcoin 6000
        var buy = BigDecimal.valueOf(6000.0)
        var commission = buy * COMMISSION
        container.updateHoldings(Symbol(BTCUSDT), buy, commission)
        assertThat(container.getCurrentHoldings().getCash()).isEqualByComparingTo(BigDecimal.valueOf(4000.0))
        assertThat(container.getHoldingForSymbol(Symbol(BTCUSDT))).isEqualByComparingTo(BigDecimal.valueOf(5994.0))
        assertThat(container.getCommissions()).isEqualByComparingTo(BigDecimal.valueOf(6.0))
        assertThat(container.getCurrentTotal()).isEqualByComparingTo(BigDecimal.valueOf(9994.0))

        // Buy ethereum 4000
        buy = BigDecimal.valueOf(4000.0)
        commission = buy * COMMISSION
        container.updateHoldings(Symbol(ETHUSDT), buy, commission)
        assertThat(container.getCurrentHoldings().getCash()).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(container.getHoldingForSymbol(Symbol(ETHUSDT))).isEqualByComparingTo(BigDecimal.valueOf(3996.0))

        // Commission = 6 + 4
        assertThat(container.getCommissions()).isEqualByComparingTo(BigDecimal.valueOf(10.0))
        assertThat(container.getCurrentTotal()).isEqualByComparingTo(BigDecimal.valueOf(9990.0))

        dateTime = dateTime.plusDays(1)
        val positions = createPositions(dateTime, container.getCurrentHoldings().holdings)
        container.newRecord(dateTime, Positions(dateTime, positions), initializeTable(dateTime))
        assertThat(container.getCommissions()).isEqualByComparingTo(BigDecimal.valueOf(0.0))
        assertThat(container.getCurrentTotal().round(MathContext(5))).isEqualByComparingTo(BigDecimal.valueOf(9990))
        assertThat(container.getCurrentHoldings().getCash()).isEqualByComparingTo(BigDecimal.valueOf(0.0))

        // Sell 10 Ethereum
        val sell = BigDecimal.valueOf(400)
        commission = sell * COMMISSION
        container.updateHoldings(Symbol(BTCUSDT), -sell, commission)
        assertThat(container.getCurrentTotal().round(MathContext(5))).isEqualTo(BigDecimal.valueOf(9989.6))
        assertThat(container.getCurrentHoldings().getCash()).isEqualTo(BigDecimal.valueOf(399.6))

    }

    private fun createPositions(
        dateTime: LocalDateTime,
        share: Map<Symbol, BigDecimal>
    ): MutableMap<Symbol, BigDecimal> {

        val tables = initializeTable(dateTime)
        val result = mutableMapOf<Symbol, BigDecimal>()
        tables.forEach { (symbol, table) ->
            result[symbol] = share.getOrElse(symbol) { BigDecimal.ZERO } / BigDecimal.valueOf(
                table.where(table.dateTimeColumn(DATA_COLUMN_TIMESTAMP).isEqualTo(dateTime)).doubleColumn("close")
                    .first()
            )
        }
        return result
    }

    private fun initializeTable(timestamp: LocalDateTime): Map<Symbol, Table> {

        val start = LocalDate.now().atStartOfDay()
        val dates = mutableListOf<LocalDateTime>()
        for (i in 0..4) dates.add(start.plusDays(i.toLong()))
        val dtColumn = DateTimeColumn.create("timestamp", dates)
        val btcTable =
            Table.create(dtColumn, DoubleColumn.create("close", arrayOf(1000.0, 1100.0, 1050.0, 1100.0, 1000.0)))
        val ethTable = Table.create(dtColumn, DoubleColumn.create("close", arrayOf(200.0, 210.0, 200.0, 205.0, 210.0)))

        return mapOf(
            Symbol(BTCUSDT) to btcTable.where(btcTable.dateTimeColumn(DATA_COLUMN_TIMESTAMP).isEqualTo(timestamp)),
            Symbol(ETHUSDT) to ethTable.where(ethTable.dateTimeColumn(DATA_COLUMN_TIMESTAMP).isEqualTo(timestamp))
        )
    }
}