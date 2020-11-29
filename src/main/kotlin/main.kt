import com.greenkode.trader.analysis.Performance
import com.greenkode.trader.broker.SimulatedExecutionHandler
import com.greenkode.trader.data.HistoricalCsvDailyDataHandler
import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import com.greenkode.trader.event.OrderEvent
import com.greenkode.trader.exception.InsufficientFundsException
import com.greenkode.trader.portfolio.HoldingsContainer
import com.greenkode.trader.portfolio.PositionsContainer
import com.greenkode.trader.portfolio.ReBalancePortfolio
import com.greenkode.trader.portfolio.RiskManager
import com.greenkode.trader.strategy.MomentumRebalanceStrategy
import java.math.BigDecimal
import java.util.*

fun main() {

    val events: Queue<Event> = LinkedList()
    val riskManager = RiskManager()
    val dataHandler = HistoricalCsvDailyDataHandler(events, DIRECTORY, TOP_CRYPTOS, null)

    val positionsContainer = PositionsContainer(TOP_CRYPTOS)
    val holdingsContainer = HoldingsContainer(BigDecimal.valueOf(10000.0), TOP_CRYPTOS)
    val portfolio = ReBalancePortfolio(dataHandler, events, null, positionsContainer, holdingsContainer)

    val strategy = MomentumRebalanceStrategy(dataHandler, events, riskManager, portfolio)
    val broker = SimulatedExecutionHandler(events, holdingsContainer)
    val performance = Performance()

    while (dataHandler.continueBacktest()) {

        dataHandler.updateBars()

        while (!events.isEmpty()) {
            val event = events.poll()
            when (event.type) {
                EventTypeEnum.MARKET -> {
                    strategy.calculateSignals(event)
                    portfolio.updateTimeIndex(event)
                }
                EventTypeEnum.SIGNAL -> portfolio.updateSignal(event)
                EventTypeEnum.ORDER -> broker.executeOrder(event)
                EventTypeEnum.FILL -> portfolio.updateFill(event)
            }
        }
    }

    performance.createEquityCurve(portfolio.getHistoricalHoldings())
    performance.printSummaryStats()
}

const val DIRECTORY = "/Volumes/Seagate Expansion Drive/binance/data/1d"
val TOP_CRYPTOS = listOf(
    "BTCUSDT",
    "ETHUSDT",
    "XRPUSDT",
    "BCHUSDT",
    "LINKUSDT",
    "BNBUSDT",
    "LTCUSDT",
    "ADAUSDT",
    "EOSUSDT",
    "XMRUSDT",
    "TRXUSDT",
    "XLMUSDT",
    "XTZUSDT",
    "NEOUSDT",
    "ATOMUSDT"
).map { Symbol(it) }