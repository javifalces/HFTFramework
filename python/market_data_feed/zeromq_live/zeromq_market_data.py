import datetime
import json


class TypeMessage:
    depth = 'depth'
    trade = 'trade'
    info = 'info'
    unknown = 'unknown'


class TradeMessage:
    def __init__(self, time: datetime.datetime, price: float, quantity: float):
        self.time = time
        self.price = price
        self.quantity = quantity


class DepthMessage:
    def __init__(
            self,
            time: datetime.datetime,
            bid_prices: list,
            ask_prices: list,
            bid_quantities: list,
            ask_quantities: list,
    ):
        self.time = time
        self.bid_prices = bid_prices
        self.ask_prices = ask_prices
        self.bid_quantities = bid_quantities
        self.ask_quantities = ask_quantities

    def get_midprice(self):
        return (self.bid_prices[0] + self.ask_prices[0]) / 2.0


class ZeroMqMarketData:
    '''

    topic:
    ethusdt_binance.depth

    onDepth
    {"instrument":"ethusdt_binance","timestamp":1668949928761,"bidsQuantities":[0.1986,0.0117,0.325,1.1795,0.3],"asksQuantities":[52.2893,2.8954,6.3824,0.1278,0.09],"bids":[1170.55,1170.51,1170.47,1170.46,1170.45],"asks":[1170.56,1170.6,1170.61,1170.63,1170.7],"levels":0,"askLevels":0,"bidLevels":0,"timeToNextUpdateMs":-9223372036854775808}

    onTrade
    {"id":"7bfad670-421c-4d53-b54a-edabb56865f8","instrument":"btcusdt_binance","timestamp":1668949928757,"quantity":0.00157,"price":16543.18,"timeToNextUpdateMs":-9223372036854775808}

    '''

    # com.lambda.investing.market_data_connector.metatrader.MetatraderMarketDataPublisher#onUpdate
    def __init__(self, topic: str, json_message: str):
        self.topic = topic
        self.json_message = json_message

        self.dict_message = json.loads(self.json_message)
        self.type_message = self._get_type_message()
        if (
                self.type_message == TypeMessage.info
                or self.type_message == TypeMessage.unknown
        ):
            # info message return
            return
        self.topic_instrument = self.topic.split('.')[0]
        self.instrument = self.dict_message['instrument']
        if self.topic_instrument.upper() != self.instrument.upper():
            print(
                rf"WARNING: self.topic_instrument {self.topic_instrument} !=self.instrument {self.instrument}"
            )
        self.timestamp = (int)(self.dict_message['timestamp'])
        self.time = datetime.datetime.fromtimestamp(
            self.timestamp / 1e3, tz=datetime.timezone.utc
        )

    def _get_type_message(self) -> str:
        type_str = self.topic.split('.')[-1].upper()
        if type_str == 'DEPTH':
            return TypeMessage.depth
        elif type_str == 'TRADE':
            return TypeMessage.trade
        elif type_str == 'INFO':
            return TypeMessage.info
        else:
            return TypeMessage.unknown

    def get_depth(self) -> DepthMessage:
        # bid_prices=[]
        # ask_prices=[]
        # bid_quantities=[]
        # ask_quantities=[]

        bid_quantities = self.dict_message["bidsQuantities"]
        bid_prices = self.dict_message["bids"]
        ask_quantities = self.dict_message["asksQuantities"]
        ask_prices = self.dict_message["asks"]

        depth_message = DepthMessage(
            time=self.time,
            bid_prices=bid_prices,
            ask_prices=ask_prices,
            ask_quantities=ask_quantities,
            bid_quantities=bid_quantities,
        )
        return depth_message

    def get_trade(self) -> TradeMessage:
        quantity = self.dict_message['quantity']
        price = self.dict_message['price']
        trade_message = TradeMessage(time=self.time, price=price, quantity=quantity)
        return trade_message
