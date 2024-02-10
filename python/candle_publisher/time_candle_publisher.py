import datetime

from candle_publisher.base_candle_publisher import BaseCandlePublisher
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


class TimeCandlePublisher(BaseCandlePublisher):
    class InstrumentCandle:
        def __str__(self) -> str:
            return rf"InstrumentCandle {self.instrument_pk} treshold:{self.seconds_threshold} last_candle_time:{self.last_candle_time} open_price:{self.open_price} close_price:{self.close_price} high_price:{self.high_price} low_price:{self.low_price} ticks:{self.ticks}"

        def __init__(self, instrument_pk: str, seconds_threshold: int):
            self.last_candle_time = None
            self.open_price = None
            self.close_price = None
            self.high_price = None
            self.low_price = None
            self.seconds_threshold = seconds_threshold
            self.minutes_threshold = self.seconds_threshold / 60.0
            self.hours_threshold = self.minutes_threshold / 60.0

            self.last_minute_saved = None
            self.instrument_pk = instrument_pk
            self.ticks = 0

        def reset_new_candle(self, current_price: float):
            # update open price for next
            self.open_price = current_price
            self.low_price = None
            self.high_price = None
            self.ticks = 0

        def is_new_candle(self, date_to_check: datetime.datetime):
            return self.is_new_candle_elapsed(date_to_check=date_to_check)

        def get_candle_dict(self, date_to_send: datetime.datetime) -> dict:
            if self.ticks == 0:
                print(
                    rf"WARNING: publishing candles {self.instrument_pk} without market data updates -> all with previous close_price {self.open_price}"
                )
                self.low_price = self.open_price
                self.high_price = self.open_price
                self.close_price = self.open_price

            publish_dict = {
                "instrument": self.instrument_pk,
                "timestamp": date_to_send.timestamp() * 1e3,  # need to have 13 digits
                "open": self.open_price,
                "close": self.close_price,
                "high": self.high_price,
                "low": self.low_price,
                "ticks": self.ticks,
            }
            return publish_dict

        def _is_new_minute_candle_change(self, date_to_check: datetime.datetime):
            '''
            Is new on change of minute second ~0 to start time candles on clean change of minute
            Parameters
            ----------
            date_to_check

            Returns
            -------

            '''
            if self.last_minute_saved is None:
                self.last_minute_saved = date_to_check.minute
                return False
            current_minute = date_to_check.minute
            if current_minute != self.last_minute_saved:
                self.last_minute_saved = date_to_check.minute
                return True
            return False

        def _change_custom_minute(self, date_to_check: datetime.datetime) -> bool:
            '''
            Check if change of minute is multiple
            Parameters
            ----------
            date_to_check

            Returns
            -------

            '''
            if self.hours_threshold < 1 < self.minutes_threshold:
                # is minute threshold multiple of minutes threshold
                is_minute_multiple_threshold = (
                        date_to_check.minute % self.minutes_threshold == 0
                )
                if not is_minute_multiple_threshold:
                    return False
                else:
                    return True
            return True

        def is_new_candle_elapsed(self, date_to_check: datetime.datetime) -> bool:
            '''
            Is new when x seconds elapsed .... change on time we start it!
            Parameters
            ----------
            date_to_check

            Returns
            -------

            '''
            if self.last_candle_time is None:
                if self._is_new_minute_candle_change(date_to_check):
                    if not self._change_custom_minute(date_to_check):
                        self.last_candle_time = None  # avoid desynchronization
                        return False

                    self.last_candle_time = date_to_check
                    print(
                        rf"starting candles is_new_candle_elapsed {self.last_candle_time}"
                    )
                    return True
                else:
                    return False
            seconds_elapsed = (date_to_check - self.last_candle_time).seconds
            if seconds_elapsed > self.seconds_threshold:
                # set seconds to zero to avoid change in the middle of minutes
                date_to_check = date_to_check.replace(second=0, microsecond=0)
                self.last_candle_time = date_to_check

                return True
            return False

        def update_prices(self, current_price: float):
            self.ticks += 1

            # updated always
            self.close_price = current_price

            # updated initially
            if self.open_price is None:
                self.open_price = current_price

            if self.high_price is None or current_price > self.high_price:
                self.high_price = current_price
            if self.low_price is None or current_price < self.low_price:
                self.low_price = current_price

    def __init__(
            self,
            zeromq_configuration_subscriber: ZeroMqConfiguration,
            zeromq_configuration_publisher: ZeroMqConfiguration,
            zeromq_configuration_replier: ZeroMqConfiguration,
            resolution='MIN',
            num_units=1,
    ):
        '''

        Parameters
        ----------
        zeromq_configuration_subscriber
        zeromq_configuration_publisher
        :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
        :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
        '''
        from mlfinlab.data_structures.time_data_structures import TimeBars

        super().__init__(
            zeromq_configuration_subscriber,
            zeromq_configuration_publisher,
            zeromq_configuration_replier,
        )
        self.topic_name = rf"timecandle_{num_units}{resolution}"
        self.time_bar = TimeBars(resolution=resolution, num_units=num_units)
        self.seconds_threshold = self.time_bar.threshold
        self.instrument_candles = {}
        self.last_price = {}

    def get_instrument_candle(self, instrument_pk) -> InstrumentCandle:
        if instrument_pk not in self.instrument_candles.keys():
            print(
                f"creating new instrument candle {instrument_pk} seconds_treshold:{self.seconds_threshold}"
            )
            self.instrument_candles[
                instrument_pk
            ] = TimeCandlePublisher.InstrumentCandle(
                instrument_pk=instrument_pk, seconds_threshold=self.seconds_threshold
            )
        return self.instrument_candles[instrument_pk]

    def process_market_data(self, zeromq_market_data: ZeroMqMarketData):
        '''
        Here we process market data and to candles and publish them if needed
        Parameters
        ----------
        zeromq_market_data

        Returns
        -------

        '''
        current_price = self._get_price(zeromq_market_data)
        if current_price is None:
            return

        instrument = zeromq_market_data.instrument
        instrument_candle = self.get_instrument_candle(instrument)
        instrument_candle.update_prices(current_price=current_price)

        # every market data we check all candles finished!
        self.last_price[instrument] = current_price
        self._check_candles_finish(time=zeromq_market_data.time)

    def _check_candles_finish(self, time: datetime.datetime):
        '''
        Check if candles are finished and publish them
        Parameters
        ----------
        time

        Returns
        -------

        '''
        for instrument_candle_it in self.instrument_candles.values():
            if instrument_candle_it.is_new_candle(date_to_check=time):
                publish_dict = instrument_candle_it.get_candle_dict(date_to_send=time)
                print(
                    rf"{datetime.datetime.now()} publish {self.topic_name} ->{publish_dict} "
                )
                self._publish_candle(publish_dict=publish_dict)
                instrument_pk = instrument_candle_it.instrument_pk
                if self.last_price[instrument_pk] is None:
                    print(
                        rf"last_price is None for {instrument_pk} -> set skip it publish"
                    )
                    continue

                last_price = self.last_price[instrument_pk]
                instrument_candle_it.reset_new_candle(current_price=last_price)


if __name__ == "__main__":
    '''
    192.168.1.70

    binance.marketdata.port=6600
    binance.tradeengine.port=6601
    coinbase.marketdata.port=6610
    coinbase.tradeengine.port=6611
    kraken.marketdata.port=6620
    kraken.tradeengine.port=6621
    [PersistorParquetMarketDataConnector_day] PersistorMarketDataConnector - 0 files moved
    [Statistics] Statistics - ******** PersistorMarketDataConnector ********
    [Statistics] Statistics - 	etheur_coinbase.trade:	203
    [Statistics] Statistics - 	btceur_coinbase.depth:	643258
    [Statistics] Statistics - 	ethbtc_coinbase.depth:	23162
    [Statistics] Statistics - 	btceur_coinbase.trade:	231
    [Statistics] Statistics - 	etheur_coinbase.depth:	129737
    [Statistics] Statistics - 	btcusdt_coinbase.trade:	190
    [Statistics] Statistics - 	ethusdt_coinbase.depth:	179892
    [Statistics] Statistics - 	btcusdt_coinbase.depth:	908961
    [Statistics] Statistics - 	ethusdt_coinbase.trade:	439
    [Statistics] Statistics - 	ethbtc_coinbase.trade:	55
    [Statistics] Statistics - ****************

    '''
    zeromq_configuration_sub = ZeroMqConfiguration(
        url='192.168.1.70', port=6600, topic=''
    )
    zeromq_configuration_pub = ZeroMqConfiguration(url='*', port=7700)
    zeromq_configuration_replier = ZeroMqConfiguration(url='*', port=7701)
    candle_publisher = TimeCandlePublisher(
        zeromq_configuration_subscriber=zeromq_configuration_sub,
        zeromq_configuration_publisher=zeromq_configuration_pub,
        zeromq_configuration_replier=zeromq_configuration_replier,
        num_units=15,
        resolution="S",
    )
    candle_publisher.start()
