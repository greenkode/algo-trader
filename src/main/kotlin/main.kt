import com.greenkode.trader.analysis.Performance
import com.greenkode.trader.broker.SimulatedExecutionHandler
import com.greenkode.trader.data.HistoricalCsvDailyDataHandler
import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import com.greenkode.trader.portfolio.HoldingsContainer
import com.greenkode.trader.portfolio.PositionsContainer
import com.greenkode.trader.portfolio.RebalancePortfolio
import com.greenkode.trader.portfolio.RiskManager
import com.greenkode.trader.strategy.MomentumRebalanceStrategy
import java.util.*

fun main() {

    val events: Queue<Event> = LinkedList()
    val riskManager = RiskManager()
    val dataHandler = HistoricalCsvDailyDataHandler(events, DIRECTORY, TOP_CRYPTOS)
    val strategy = MomentumRebalanceStrategy(dataHandler, events, riskManager)
    val portfolio =
        RebalancePortfolio(
            dataHandler, events, null, PositionsContainer(TOP_CRYPTOS), HoldingsContainer(10000.0, TOP_CRYPTOS)
        )
    val broker = SimulatedExecutionHandler(events)
    val performance = Performance()

    while (dataHandler.continueBacktest()) {

        dataHandler.updateBars()

        while (!events.isEmpty()) {
            val event = events.poll()
            if (event.type == EventTypeEnum.MARKET) {
                strategy.calculateSignals(event)
                portfolio.updateTimeIndex(event)
            } else if (event.type == EventTypeEnum.SIGNAL)
                portfolio.updateSignal(event)
            else if (event.type == EventTypeEnum.ORDER)
                broker.executeOrder(event)
            else if (event.type == EventTypeEnum.FILL)
                portfolio.updateFill(event)
        }
    }

    performance.createEquityCurve(portfolio.getHoldings())
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