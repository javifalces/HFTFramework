import json

from candle_publisher.candle_listener import CandleListener
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import (
    ZeroMqSubConnector,
    ZeroMqListener,
)


class CandleSubscriber(ZeroMqListener):
    def __init__(self, zeromq_configuration: ZeroMqConfiguration):
        self.zeromq_configuration = zeromq_configuration
        self.listeners = []

        self.zeromq_candle_sub = ZeroMqSubConnector(
            zeromq_configuration=self.zeromq_configuration
        )
        self.zeromq_candle_sub.subscribe(self)

    def subscribe(self, candle_listener: CandleListener):
        self.listeners.append(candle_listener)

    def notify(self, topic: str, message: str):
        topic_instrument = topic.split('.')[0]
        topic_name = topic.split('.')[-1]
        candle_dict = json.loads(message)

        instrument = candle_dict['instrument']
        if topic_instrument.upper() != instrument.upper():
            print(
                rf"WARNING: topic_instrument {topic_instrument} !=dict instrument {instrument}"
            )
        self.notify_candle(candle_dict)

    def notify_candle(self, candle_dict: dict):
        for candle_listener in self.listeners:
            candle_listener.on_candle(candle_dict)

    def start(self):
        self.zeromq_candle_sub.start_listening()
