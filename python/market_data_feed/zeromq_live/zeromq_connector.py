from market_data_feed.zeromq_live.zeromq_market_data import (
    ZeroMqMarketData,
    TypeMessage,
)
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
import zmq
import zmq.asyncio
import json
from threading import Thread


class ZeroMqListener:
    def notify(self, topic: str, message: str):
        pass


class ZeroMqReplier:
    def reply(self, request: str) -> str:
        print(rf"override this method! -> return empty")
        return ""


class ZeroMqSubConnector:
    listeners = []

    # def __del__(self):
    #     self.context.destroy()
    def __init__(
            self, zeromq_configuration: ZeroMqConfiguration, thread_number: int = 4
    ):
        self.zeromq_configuration = zeromq_configuration
        self.listeners = []
        self.thread_number = thread_number
        self.context = ZeroMqConfiguration.get_context()
        # self.poller = zmq.Poller()

    def start_listening(self):

        self.socket = self.context.socket(zmq.SUB)
        # if self.zeromq_configuration.topic!='':
        #     #filter to topics
        topic = self.zeromq_configuration.topic
        if topic is None:
            topic = ''
        self.socket.setsockopt_string(zmq.SUBSCRIBE, topic)

        url = self.zeromq_configuration.get_address()
        print(rf"listening ZeroMqPubConnector topic:{topic} -> {url}")
        self.socket.connect(url)

        # self.poller.register(self.socket, zmq.POLLIN)

        should_continue = True
        while should_continue:
            # socks = dict(self.poller.poll())

            # if self.socket in socks and socks[self.socket] == zmq.POLLIN:
            message = self.socket.recv_multipart()
            topic = message[0].decode('utf-8')
            json_message = message[1].decode('utf-8')

            self.notify(topic, json_message)

    # async def start_listening(self):
    #     self.context  = zmq.asyncio.Context(io_threads=self.thread_number)
    #     self.socket = self.context.socket(zmq.SUB)
    #     # if self.zeromq_configuration.topic!='':
    #     #     #filter to topics
    #     topic=self.zeromq_configuration.topic
    #     if topic is None:
    #         topic=''
    #     self.socket.setsockopt_string(zmq.SUBSCRIBE, topic)
    #
    #
    #     url=self.zeromq_configuration.get_address()
    #     print(rf"listening ZeroMqPubConnector topic:{topic} -> {url}")
    #     self.socket.connect(url)
    #
    #     # self.poller.register(self.socket, zmq.POLLIN)
    #
    #     should_continue = True
    #     while should_continue:
    #         message= await self.socket.recv_multipart()
    #         topic = message[0].decode('utf-8')
    #         json_message=message[1].decode('utf-8')
    #         self.notify(topic,json_message)

    def subscribe(self, listener: ZeroMqListener):
        self.listeners.append(listener)

    def notify(self, topic: str, message: str):
        for listener in self.listeners:
            listener.notify(topic, message)


class ZeroMqReplierConnector:
    listeners = []

    def __del__(self):
        self.context.destroy()

    def __init__(
            self, zeromq_configuration: ZeroMqConfiguration, replier: ZeroMqReplier
    ):
        self.zeromq_configuration = zeromq_configuration
        self.replier = replier
        self.context = ZeroMqConfiguration.get_context()
        # self.poller = zmq.Poller()

    def start_listening(self):
        self.socket = self.context.socket(zmq.REP)

        url = self.zeromq_configuration.get_server_address()
        print(rf"listening sync ZeroMqReplierConnector -> {url}")
        self.socket.bind(url)
        should_continue = True
        while should_continue:
            # socks = dict(self.poller.poll())
            # if self.socket in socks and socks[self.socket] == zmq.POLLIN:
            request = self.socket.recv_string()
            reply = self.replier.reply(request)
            self.socket.send_string(reply)

    def add_replier(self, replier: ZeroMqReplier):
        self.replier = replier


class ZeroMqMarketDataListener(ZeroMqListener):
    def __init__(self, zeromq_configuration: ZeroMqConfiguration):
        self.zeromq_configuration = zeromq_configuration
        self.zeromq_connector = ZeroMqSubConnector(self.zeromq_configuration)
        self.zeromq_connector.subscribe(self)

    def start(self):
        self.zeromq_connector.start_listening()

    def notify(self, topic: str, message: str):
        try:
            zeromq_market_data = ZeroMqMarketData(topic, message)
        except Exception as e:
            print(
                rf"ZeroMqMarketDataListener.zeromq_market_data exception {e} on  topic:{topic} message:{message}-> skip it"
            )
            return
        if zeromq_market_data.type_message == TypeMessage.trade:
            trade_message = zeromq_market_data.get_trade()
            print(rf"trade detected  {trade_message.time}")
        if zeromq_market_data.type_message == TypeMessage.depth:
            depth_message = zeromq_market_data.get_depth()
            print(rf"depth detected  {depth_message.time}")
        # TODO something else with this data


class ZeroMqPublisher:
    # def __del__(self):
    #     self.context.destroy()

    def __init__(self, zeromq_configuration: ZeroMqConfiguration):
        self.zeromq_configuration = zeromq_configuration
        self.context = ZeroMqConfiguration.get_context()
        self.socket = self.context.socket(zmq.PUB)
        self.socket.bind(addr=self.zeromq_configuration.get_server_address())

    def publish(self, message: str, topic: str = None):
        if topic is not None:
            # self.socket.send_string("%s %s" % (topic, message))
            self.socket.send_multipart([topic.encode("utf8"), message.encode("utf8")])
        else:
            self.socket.send_string(message)


class ZeroMqRequester:
    SEND_MAX_RETRIES = 5

    # def __del__(self):
    #     self.context.destroy()

    def __init__(self, zeromq_configuration: ZeroMqConfiguration):
        self.zeromq_configuration = zeromq_configuration
        self.context = ZeroMqConfiguration.get_context()
        self._connect()
        self.last_reply = ""

    def disconnect(self, timeout_seconds: int = -1):
        if timeout_seconds > 0:
            thread = Thread(
                target=self.socket.disconnect(self.zeromq_configuration.get_address())
            )
            thread.start()
            thread.join(timeout=timeout_seconds)
        else:
            self.socket.disconnect(self.zeromq_configuration.get_address())

        self.socket.close()

    def _connect(self):
        self.socket = self.context.socket(zmq.REQ)

        # self.socket.setsockopt(zmq.HWM, 0)
        self.socket.connect(addr=self.zeromq_configuration.get_address())

    def send_request(
            self, message: str, send_request_retries: int = 1, receive_timeout_ms: int = -1
    ) -> str:
        '''

        Parameters
        ----------
        close_process
        message
        request_retries : int number of retries to send the message if no reply is received
        request_timeout_ms: int timeout in ms to wait for a reply or retry a request

        Returns reply of the request
        -------

        '''
        if send_request_retries == 1 and receive_timeout_ms < 0:
            reply = self._send_request(message)
        else:
            reply = self._send_lazy_pirate(
                message, send_request_retries, receive_timeout_ms
            )

        return reply

    def _send_request(self, message: str):
        try:
            self.socket.send_string(message)
            reply = self.socket.recv_string()
            return reply
        except Exception as e:
            print(rf"WARNING: Error sending request {message} {e}")

    def _send_lazy_pirate(
            self, message: str, send_request_retries: int, receive_timeout_ms: int
    ) -> str:
        '''
        https://learning-0mq-with-pyzmq.readthedocs.io/en/latest/pyzmq/patterns/lazy_pirate.html

        Parameters
        ----------
        message
        request_retries
        request_timeout_ms

        Returns
        -------

        '''
        import sys

        # synchronize this method to avoid sending multiple request at the same time
        sent = False
        counter_retries = 0
        while not sent:
            try:
                self.socket.send_string(message)
                sent = True
            except Exception as e:
                print(
                    rf"WARNING: Error sending message {message} {counter_retries + 1}/{send_request_retries + 1}-> \
                    {e}"
                )
                counter_retries += 1
                if counter_retries > send_request_retries:
                    print(
                        rf"WARNING: Error requesting {message}: {counter_retries}/{send_request_retries + 1} -> \
                        return KO"
                    )
                    return "KO : request(send) message"

        if (self.socket.poll(receive_timeout_ms) & zmq.POLLIN) != 0:
            reply = self.socket.recv_string()
            # everything was fine
            self.last_reply = reply
            return reply
        return f"KO : request(receive) timeout {receive_timeout_ms / 60.0 / 1000.0} minutes"


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
    zeromq_configuration = ZeroMqConfiguration(
        url='192.168.1.70', port=6600, topic='btcusdt_binance'
    )
    zeromq_marketdata_listener = ZeroMqMarketDataListener(
        zeromq_configuration=zeromq_configuration
    )
    zeromq_marketdata_listener.start()

    # zeromq_configuration_pub = ZeroMqConfiguration(url='localhost',port=8787)
    # zeromq_publisher = ZeroMqPublisher(zeromq_configuration=zeromq_configuration_pub)
    # zeromq_publisher.publish("hola","ola")
