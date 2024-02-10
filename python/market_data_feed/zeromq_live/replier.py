from time import sleep

from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import (
    ZeroMqPublisher,
    ZeroMqMarketDataListener,
    ZeroMqReplierConnector,
    ZeroMqReplier,
)

if __name__ == "__main__":

    class CustomReplier(ZeroMqReplier):
        def reply(self, request: str) -> str:
            return "custom reply " + request

    custom_replier = CustomReplier()
    zeromq_configuration = ZeroMqConfiguration(url='localhost', port=8787, topic='hola')
    zeromq_replier_connector = ZeroMqReplierConnector(
        zeromq_configuration=zeromq_configuration, replier=custom_replier
    )
    zeromq_replier_connector.start_listening()
