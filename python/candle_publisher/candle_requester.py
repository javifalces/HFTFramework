import json

from candle_publisher.base_candle_publisher import BaseCandlePublisher
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import (
    ZeroMqSubConnector,
    ZeroMqRequester,
)


class CandleRequester:
    def __init__(self, zeromq_configuration: ZeroMqConfiguration):
        self.zeromq_configuration = zeromq_configuration
        self.listeners = []

        self.zeromq_candle_request = ZeroMqRequester(
            zeromq_configuration=self.zeromq_configuration
        )

    def request(self, instrument_pk: str, historical: int) -> list:
        message_dict = {}
        message_dict[BaseCandlePublisher.REQUEST_INSTRUMENT_KEY] = instrument_pk
        message_dict[BaseCandlePublisher.REQUEST_CANDLES_KEY] = historical
        print(
            rf"CandleRequester.request {instrument_pk} {historical} candles to {self.zeromq_configuration.get_address()}"
        )
        message = json.dumps(message_dict)

        list_candle_dicts_str = self.zeromq_candle_request.send_request(message=message)
        list = json.loads(list_candle_dicts_str)
        return list


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
    zeromq_configuration = ZeroMqConfiguration(url='localhost', port=6601)
    candle_publisher = CandleRequester(zeromq_configuration=zeromq_configuration)
    list_candle_dicts = candle_publisher.request(
        instrument_pk='ethusdt_binance', historical=3
    )
    print(rf"return {len(list_candle_dicts)} -> {json.loads(list_candle_dicts)}")
