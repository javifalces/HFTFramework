package com.lambda.investing.algorithmic_trading.observer.pushbullet;

/**
 * Interface for listening to incoming Pushbullet messages.
 * Implementations of this interface will be notified when new messages are received.
 */
public interface PushbulletMessageListener {

    /**
     * Called when a new Pushbullet message is received.
     *
     * @param title The title of the Pushbullet message
     * @param body  The body content of the Pushbullet message
     */
    void onPushbulletMessage(String title, String body);
}

