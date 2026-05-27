package com.lambda.investing.algorithmic_trading.observer.push;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.AlgorithmParameters;
import com.lambda.investing.algorithmic_trading.AlgorithmProviderImpl;
import com.lambda.investing.algorithmic_trading.observer.push.pushbullet.PushbulletAlgorithmObserver;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.threadly.concurrent.collections.ConcurrentArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.lambda.investing.Configuration.PUSHBULLET_TOKEN;


/**
 * PushService is an abstract base class for push notification observers.
 * It implements {@link AlgorithmObserver} and centralizes common logic such as:
 * <ul>
 *   <li>Sending trade execution notifications</li>
 *   <li>Tracking the latest portfolio snapshot</li>
 *   <li>Handling incoming text commands (stop, start, portfolio status)</li>
 * </ul>
 *
 * <p>Concrete implementations only need to provide a {@link #sendMessage(String, String)} method
 * that delivers the message through the specific push channel (Pushbullet, Telegram, etc.).
 *
 * <p>Incoming messages from the push channel should be forwarded to
 * {@link #handleIncomingMessage(String, String)} so that command routing is applied consistently
 * across all implementations.
 *
 * <p>Example of a minimal implementation:
 * <pre>{@code
 * public class MyPushObserver extends PushService {
 *
 *     public MyPushObserver(Algorithm algorithm) {
 *         super(algorithm);
 *     }
 *
 *     @Override
 *     public void sendMessage(String title, String message) throws Exception {
 *         // deliver via custom channel
 *     }
 * }
 * }</pre>
 */
public abstract class PushService implements AlgorithmObserver {


    protected static final Logger logger = LogManager.getLogger(PushService.class);
    private static List<PushService> activePushServices = new ConcurrentArrayList<>();
    private static AlgorithmProviderImpl provider;

    public static void createPushServices(Algorithm algorithm) {
        if (!PUSHBULLET_TOKEN.isBlank()) {
            try {
                PushbulletAlgorithmObserver pushbulletAlgorithmObserver = new PushbulletAlgorithmObserver(algorithm, PUSHBULLET_TOKEN);
                algorithm.register(pushbulletAlgorithmObserver);
                activePushServices.add(pushbulletAlgorithmObserver);
                provider = AlgorithmProviderImpl.getInstanceOrCreate(algorithm);
                logger.info("PushbulletAlgorithmObserver registered successfully");
            } catch (Exception e) {
                logger.error("error registering createPushServices PushbulletAlgorithmObserver ", e);
            }
        }
    }

    public static void sendPushMessage(String topic, String message) {
        for (PushService pushService : activePushServices) {
            try {
                pushService.sendMessage(topic, message);
            } catch (Exception e) {
                logger.error("Error sending push message: {}", e.getMessage());
            }
        }
    }
    // -----------------------------------------------------------------------
    // Built-in command sets – subclasses may override to extend or replace
    // -----------------------------------------------------------------------

    protected static final List<String> STOP_COMMANDS = Arrays.asList(
            "stopalgo", "stop_algo", "stoptrading"
    );

    protected static final List<String> START_COMMANDS = Arrays.asList(
            "startalgo", "start_algo", "starttrading"
    );

    protected static final List<String> PORTFOLIO_COMMANDS = Arrays.asList(
            "portfolio", "positions", "pnl", "status"
    );

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------


    protected final Algorithm algorithm;
    protected PortfolioSnapshot latestPortfolioSnapshot;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a PushService bound to the given algorithm.
     *
     * @param algorithm the algorithm this observer is attached to
     */
    protected PushService(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    // -----------------------------------------------------------------------
    // Abstract API – must be implemented by every push channel
    // -----------------------------------------------------------------------

    /**
     * Sends a push notification through the concrete channel.
     *
     * @param title   short title / subject of the notification
     * @param message body / content of the notification
     * @throws Exception if the underlying transport fails
     */
    public abstract void sendMessage(String title, String message) throws Exception;

    // -----------------------------------------------------------------------
    // Incoming-message command routing (shared across all implementations)
    // -----------------------------------------------------------------------

    /**
     * Routes an incoming push message to the appropriate action.
     * Concrete subclasses should call this method from their channel-specific
     * message callback so that stop / start / portfolio commands are handled
     * consistently regardless of the push provider.
     *
     * @param title the title of the incoming message (may be empty)
     * @param body  the body of the incoming message
     */
    protected void handleIncomingMessage(String title, String body) {
        logger.info("Received push message: [{}] {}", title, body);

        String combined = (title + " " + body).toLowerCase().trim();

        // Stop command
        if (STOP_COMMANDS.stream().anyMatch(cmd -> combined.equalsIgnoreCase(cmd))) {
            logger.info("Received stop command via push – stopping algorithm");
            System.out.println(Configuration.formatLog("Received stop command via push – stopping algorithm"));
            provider.stopAlgo();
            try {
                sendMessage(algorithm.getAlgorithmInfo() + " stopped",
                        "Received stop command via push – stopping algorithm");
            } catch (Exception e) {
                logger.error("Error sending push confirmation after stop: {}", e.getMessage());
            }
            return;
        }

        // Start command
        if (START_COMMANDS.stream().anyMatch(cmd -> combined.equalsIgnoreCase(cmd))) {
            logger.info("Received start command via push – starting algorithm");
            System.out.println(Configuration.formatLog("Received start command via push – starting algorithm"));
            provider.startAlgo();
            try {
                sendMessage(algorithm.getAlgorithmInfo() + " started",
                        "Received start command via push – starting algorithm");
            } catch (Exception e) {
                logger.error("Error sending push confirmation after start: {}", e.getMessage());
            }
            return;
        }

        // Portfolio / status command
        if (PORTFOLIO_COMMANDS.stream().anyMatch(cmd -> combined.equalsIgnoreCase(cmd))) {
            logger.info("Received portfolio command via push – sending snapshot");
            System.out.println(Configuration.formatLog("Received portfolio command via push – sending snapshot"));
            try {
                String portfolioMessage = latestPortfolioSnapshot != null
                        ? latestPortfolioSnapshot.toString()
                        : "No portfolio snapshot available yet";
                sendMessage(algorithm.getAlgorithmInfo() + " Portfolio", portfolioMessage);
            } catch (Exception e) {
                logger.error("Error sending push portfolio snapshot: {}", e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // AlgorithmObserver – default implementations
    // -----------------------------------------------------------------------

    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {
    }

    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {
    }

    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {
        this.latestPortfolioSnapshot = portfolioSnapshot;
    }

    @Override
    public void onUpdateTrade(String algorithmInfo, Trade trade) {
    }

    @Override
    public void onUpdateParams(String algorithmInfo, Map<String, Object> newParams) {
    }

    @Override
    public void onUpdateMessage(String algorithmInfo, String name, String body) {
    }

    @Override
    public void onOrderRequest(String algorithmInfo, OrderRequest orderRequest) {
    }

    @Override
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk,
                                String key, Double value) {
    }

    /**
     * Sends a push notification when a trade is executed.
     * Subclasses may override this to customise the notification format.
     */
    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {
        if (!ExecutionReport.isTradeStatus(executionReport)) {
            return;
        }
        String message = Configuration.formatLog("{} -> {} {} {}@{}",
                executionReport.getAlgorithmInfo(),
                executionReport.getVerb(),
                executionReport.getInstrument(),
                executionReport.getQuantity(),
                executionReport.getPrice());
        try {
            sendMessage("Trade", message);
            logger.info("Push trade notification sent: {}", message);
        } catch (Exception e) {
            logger.error("Error sending push trade notification: {}", e.getMessage());
        }
    }
}
