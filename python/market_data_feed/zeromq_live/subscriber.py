from time import sleep

from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import (
    ZeroMqPublisher,
    ZeroMqMarketDataListener,
)

if __name__ == "__main__":
    zeromq_configuration = ZeroMqConfiguration(url='localhost', port=8787, topic='hola')
    zeromq_marketdata_listener = ZeroMqMarketDataListener(
        zeromq_configuration=zeromq_configuration
    )
    zeromq_marketdata_listener.start()
