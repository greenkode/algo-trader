import com.greenkode.trader.broker.SimulatedExecutionHandler
import com.greenkode.trader.data.HistoricalCsvDailyDataHandler
import com.greenkode.trader.domain.EventTypeEnum
import com.greenkode.trader.domain.Symbol
import com.greenkode.trader.event.Event
import com.greenkode.trader.portfolio.NaivePortfolio
import com.greenkode.trader.portfolio.RiskManager
import com.greenkode.trader.strategy.BuyAndHoldStrategy
import java.math.BigDecimal
import java.util.*

fun main() {

    val events: Queue<Event> = LinkedList()
    val dataHandler = HistoricalCsvDailyDataHandler(events, DIRECTORY, TOP_CRYPTOS)
    val strategy = BuyAndHoldStrategy(dataHandler, events)
    val riskManager = RiskManager()
    val portfolio = NaivePortfolio(dataHandler, events, riskManager, null, BigDecimal.valueOf(100000))
    val broker = SimulatedExecutionHandler(events)

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
}

val DIRECTORY = "/Volumes/Seagate Expansion Drive/binance/data/1d"
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