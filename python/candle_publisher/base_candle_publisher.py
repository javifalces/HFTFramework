import asyncio
import datetime

from configuration import get_logger
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import (
    ZeroMqSubConnector,
    ZeroMqListener,
    ZeroMqPublisher,
    ZeroMqReplierConnector,
    ZeroMqReplier,
)
from market_data_feed.zeromq_live.zeromq_market_data import (
    ZeroMqMarketData,
    TypeMessage,
)
import json


class BaseCandlePublisher(ZeroMqListener, ZeroMqReplier):
    MAX_SIZE_QUEUE = 500
    REQUEST_INSTRUMENT_KEY = 'instrument'
    REQUEST_CANDLES_KEY = 'candles'

    def __init__(
            self,
            zeromq_configuration_subscriber: ZeroMqConfiguration,
            zeromq_configuration_publisher: ZeroMqConfiguration,
            zeromq_configuration_replier: ZeroMqConfiguration,
    ):
        self.zeromq_configuration_subscriber = zeromq_configuration_subscriber
        self.zeromq_configuration_publisher = zeromq_configuration_publisher
        self.zeromq_configuration_replier = zeromq_configuration_replier
        self.logger = get_logger('candle_publisher')

        self.zeromq_market_data_sub = ZeroMqSubConnector(
            zeromq_configuration=self.zeromq_configuration_subscriber
        )
        self.zeromq_market_data_sub.subscribe(self)

        self.candle_publisher = ZeroMqPublisher(
            zeromq_configuration=self.zeromq_configuration_publisher
        )
        print(
            rf"publishing candles -> {self.zeromq_configuration_publisher.get_server_address()}"
        )
        self.trades_received = 0
        self.depths_received = 0
        self.candles_published = 0
        self.topic_name = ''
        self.historical_queue_dict = {}
        self.last_price = {}

        self.candle_replier = ZeroMqReplierConnector(
            zeromq_configuration=self.zeromq_configuration_replier, replier=self
        )
        print(
            rf"replying candles -> {self.zeromq_configuration_replier.get_server_address()}"
        )

    def start(self):
        self.trades_received = 0
        self.depths_received = 0

        # self.zeromq_market_data_sub.start_listening_sync()

        # self.reply_task=asyncio.run(self.candle_replier.start_listening())
        import threading

        self.reply_task = threading.Thread(
            target=self.candle_replier.start_listening, name='replierThread'
        )
        self.reply_task.start()

        self.sub_task = self.zeromq_market_data_sub.start_listening()
        # self.sub_task=threading.Thread(target=self.zeromq_market_data_sub.start_listening,name='publisherThread')

        self.reply_task.join()

    def request_correct(self, json_dict: dict) -> bool:
        keys_received = list(json_dict.keys())
        if BaseCandlePublisher.REQUEST_INSTRUMENT_KEY not in keys_received:
            return False
        if BaseCandlePublisher.REQUEST_CANDLES_KEY not in keys_received:
            return False

        return True

    def reply(self, request: str) -> str:
        json_dict = json.loads(request)
        if not self.request_correct(json_dict):
            return rf"KO : wrong format {request}"

        instrument_pk = json_dict[BaseCandlePublisher.REQUEST_INSTRUMENT_KEY]

        candles_requested = int(json_dict[BaseCandlePublisher.REQUEST_CANDLES_KEY])
        candles = self.get_last(
            instrument_pk=instrument_pk, n_elements=candles_requested
        )
        print(
            rf"BaseCandlePublisher.reply {instrument_pk} {candles_requested} candles -> {len(candles)}  "
        )
        output_json = json.dumps(candles)
        return output_json

    def notify(self, topic: str, message: str):
        try:
            zeromq_market_data = ZeroMqMarketData(topic, message)
        except Exception as e:
            print(
                rf"BaseCandlePublisher.zeromq_market_data exception {e} on  topic:{topic} message:{message}-> skip it"
            )
            return
        if zeromq_market_data.type_message == TypeMessage.trade:
            self.trades_received += 1

        if zeromq_market_data.type_message == TypeMessage.depth:
            self.depths_received += 1

        if zeromq_market_data.type_message == TypeMessage.info:
            # not process it
            return

        self.process_market_data(zeromq_market_data)

    def process_market_data(self, zeromq_market_data: ZeroMqMarketData):
        raise NotImplementedError

    def _publish_candle(self, publish_dict: dict):
        instrument_pk = publish_dict[BaseCandlePublisher.REQUEST_INSTRUMENT_KEY]
        message = json.dumps(publish_dict)
        topic_public = rf"{instrument_pk}.{self.topic_name}"
        print(rf"{instrument_pk} publish candle {message}")
        self.candle_publisher.publish(message=message, topic=topic_public)

        if instrument_pk not in self.historical_queue_dict.keys():
            self.historical_queue_dict[instrument_pk] = []
        historical_queue = self.historical_queue_dict[instrument_pk]
        historical_queue.append(publish_dict)

        if len(historical_queue) >= BaseCandlePublisher.MAX_SIZE_QUEUE:
            del historical_queue[0]
        self.candles_published += 1

    def get_mid_price(self, zeromq_market_data: ZeroMqMarketData):
        # only in
        if zeromq_market_data.type_message == TypeMessage.depth:
            depth_message = zeromq_market_data.get_depth()
            self.last_price[
                zeromq_market_data.instrument
            ] = depth_message.get_midprice()
            return self.last_price[zeromq_market_data.instrument]
        else:
            # we are not interested in trade data
            return None

    def get_last_trade_price(self, zeromq_market_data: ZeroMqMarketData):
        if zeromq_market_data.type_message == TypeMessage.trade:
            trade_message = zeromq_market_data.get_trade()
            self.last_price[zeromq_market_data.instrument] = trade_message.price
            return self.last_price[zeromq_market_data.instrument]
        else:
            return None

    def _get_price(self, zeromq_market_data: ZeroMqMarketData):
        return self.get_mid_price(zeromq_market_data=zeromq_market_data)

    def get_last(self, instrument_pk, n_elements: int):
        if instrument_pk not in self.historical_queue_dict.keys():
            print(rf"WARNING: {instrument_pk} not in self.historical_queue_dict ")
            return []

        if n_elements > BaseCandlePublisher.MAX_SIZE_QUEUE:
            n_elements = BaseCandlePublisher.MAX_SIZE_QUEUE
        historical_queue = self.historical_queue_dict[instrument_pk]
        output = historical_queue[-n_elements:]
        if len(output) < n_elements:
            print(
                rf"WARNING: requested instrument_pk:{instrument_pk} n_elements:{n_elements} but total len is {len(historical_queue)} -> return {len(output)}"
            )
        return historical_queue[-n_elements:]
