package com.lambda.investing.algorithmic_trading.observer.push.pushbullet;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.observer.push.PushService;
import org.silentsoft.pushbullet.api.Device;
import org.silentsoft.pushbullet.api.Push;
import org.silentsoft.pushbullet.api.PushbulletAPI;
import java.util.List;

/**
 * PushbulletAlgorithmObserver is a {@link PushService} implementation that sends algorithm
 * notifications through the Pushbullet API and listens for incoming Pushbullet messages to
 * execute commands (stop, start, portfolio status).
 *
 * <p>It delegates all command routing and observer logic to the parent {@link PushService}.
 * Only Pushbullet-specific transport details live here.
 *
 * <p>Example usage:
 * <pre>{@code
 * String token = "your-pushbullet-token";
 * PushbulletAlgorithmObserver observer = new PushbulletAlgorithmObserver(algorithm, token);
 * algorithm.register(observer);
 * }</pre>
 */
public class PushbulletAlgorithmObserver extends PushService implements PushbulletMessageListener {

    private final String pushbulletToken;

    /**
     * Creates a PushbulletAlgorithmObserver.
     *
     * @param algorithm      the algorithm to observe
     * @param pushbulletToken the Pushbullet API access token
     */
    public PushbulletAlgorithmObserver(Algorithm algorithm, String pushbulletToken) {
        super(algorithm);
        this.pushbulletToken = pushbulletToken;

        try {
            List<Device> devices = PushbulletAPI.getDevices(this.pushbulletToken);
            logger.info("Pushbullet token validated – {} device(s) found", devices.size());
        } catch (Exception e) {
            logger.error("Error validating Pushbullet token: {}", e.getMessage());
        }

        PushbulletMessageReader messageReader = new PushbulletMessageReader(algorithm, pushbulletToken, 5);
        messageReader.registerListener(this);
        messageReader.start();
    }

    // -----------------------------------------------------------------------
    // PushService contract
    // -----------------------------------------------------------------------

    /**
     * Sends a Pushbullet note to all devices associated with the token.
     *
     * @param title   the notification title
     * @param message the notification body
     * @throws Exception if the API call fails
     */
    @Override
    public void sendMessage(String title, String message) throws Exception {
        Push push = PushbulletAPI.sendNote(
                pushbulletToken,
                PushbulletAPI.TargetType.device_iden,
                null,  // email – not used when targeting all devices
                title,
                message
        );
        logger.info("Pushbullet message sent: {}", push);
    }

    // -----------------------------------------------------------------------
    // PushbulletMessageListener – forwards to PushService command router
    // -----------------------------------------------------------------------

    @Override
    public void onPushbulletMessage(String title, String body) {
        handleIncomingMessage(title, body);
    }


}
