package com.lambda.investing.algorithmic_trading.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmObserver;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.silentsoft.net.rest.RESTfulAPI;
import org.silentsoft.pushbullet.api.Device;
import org.silentsoft.pushbullet.api.Push;
import org.silentsoft.pushbullet.api.PushbulletAPI;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TelegramBot is an implementation of AlgorithmObserver that sends updates about algorithm execution to a Telegram chat.
 * It listens for execution reports and sends messages to a specified Telegram chat when trades are executed.
 * The bot uses the telegrambots library to interact with the Telegram Bot API.
 * <p>
 * To use this bot, create an instance of TelegramBot with the algorithm, bot token, and chat ID, and register it as an observer to your algorithm.
 * The bot will start sending messages to the Telegram chat when trades are executed.
 * <p>
 * Example usage:
 * <pre>{@code
 * String botToken = "your-telegram-bot-token";
 * String botUsername = "your-bot-username";
 * String chatId = "your-chat-id";
 * TelegramBot telegramBot = new TelegramBot(algorithm, botToken, botUsername, chatId);
 * algorithm.register(telegramBot);
 * }</pre>
 */
public class PushbulletAlgorithmObserver implements AlgorithmObserver {

    protected Logger logger = LogManager.getLogger(PushbulletAlgorithmObserver.class);
    private final String pushbulletToken;

    private final Algorithm algorithm;

    /**
     * Constructor that accepts botToken and botUsername only. The chatId must be set later using setChatId() method.
     *
     * @param algorithm The algorithm to observe
     * @param botToken  The Telegram bot token
     */
    public PushbulletAlgorithmObserver(Algorithm algorithm, String botToken) {
        this.pushbulletToken = botToken;
        this.algorithm = algorithm;
        try {
            List<Device> devices = PushbulletAPI.getDevices(this.pushbulletToken);
        } catch (Exception e) {
            logger.error("Error validating Pushbullet token: {}", e.getMessage());
        }
    }


    @Override
    public void onUpdateDepth(String algorithmInfo, Depth depth) {

    }

    @Override
    public void onUpdatePnlSnapshot(String algorithmInfo, PnlSnapshot pnlSnapshot) {

    }

    @Override
    public void onUpdatePortfolioSnapshot(String algorithmInfo, PortfolioSnapshot portfolioSnapshot) {

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
    public void onCustomColumns(long timestamp, String algorithmInfo, String instrumentPk, String key, Double value) {

    }


    @Override
    public void onExecutionReportUpdate(String algorithmInfo, ExecutionReport executionReport) {
        boolean isTrade = ExecutionReport.isTradeStatus(executionReport);
        if (isTrade) {
            String message = Configuration.formatLog("{} -> {} {} {}@{}",
                    executionReport.getAlgorithmInfo(),
                    executionReport.getVerb(),
                    executionReport.getInstrument(),
                    executionReport.getQuantity(),
                    executionReport.getPrice());
            try {
                Push push = sendMessage("Trade", message);
                logger.info("Pushbullet message sent: {}", push);
            } catch (Exception e) {
                logger.error("Error sending Pushbullet message: {}", e.getMessage());
            }

        }
    }

    public Push sendMessage(String title, String message) throws Exception {
        return PushbulletAPI.sendNote(
                pushbulletToken,
                PushbulletAPI.TargetType.device_iden, // deviceIden
                null, // email
                title,
                message
        );
    }


}
