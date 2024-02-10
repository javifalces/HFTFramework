from time import sleep

from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import (
    ZeroMqPublisher,
    ZeroMqReplier,
    ZeroMqRequester,
)

if __name__ == "__main__":
    counter = 0
    zeromq_configuration_pub = ZeroMqConfiguration(url='localhost', port=8787)
    zeromq_requester = ZeroMqRequester(zeromq_configuration=zeromq_configuration_pub)
    while True:
        reply = zeromq_requester.send_request(message=rf"ola {counter}")
        sleep(3)
        counter += 1
