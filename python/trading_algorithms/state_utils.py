from database.tick_db import TickDB

DEFAULT_TA_INDICATORS_PERIODS = [3, 5, 7, 9, 11, 13, 15, 17, 21]
DEFAULT_TA_INDICATORS_PERIODS_BINARY = [9]
DEFAULT_MARKET_HORIZON_SAVE = 15
REMOVE_PRIVATE_STATES = False
DISABLE_LAST_CLOSE = False
MARKET_MIDPRICE_RELATIVE = True
PRIVATE_COLUMNS = ['inventory', 'unrealized', 'realized']
INDIVIDUAL_COLUMNS = ['minutesToFinish']


class StateType:
    market_state = "MARKET_STATE"
    ta_state = "TA_STATE"
    ta_state_fx = "TA_STATE_FX"


class StateUtils:
    '''
    TAKE CARE dont mess up the order with java!!
    '''

    @staticmethod
    def ga_ta_state_columns(
            periods: list = DEFAULT_TA_INDICATORS_PERIODS,
            market_horizon_save: int = DEFAULT_MARKET_HORIZON_SAVE,
            binary_outputs: bool = False,
    ) -> list:
        single_state_columns = [
            "hour_of_the_day_utc",
            "minutes_from_start",
            "volume_from_start",
        ]
        ta_prefixes = [
            "microprice_",
            "vpin_",
            "rsi_",
            "sma_",
            "ema_",
            "max_",
            "min_",
            "volume_rsi_",
            "volume_sma_",
            "volume_ema_",
            "volume_max_",
            "volume_min_",
        ]
        candles_market_ta_prefixes = [
            "signed_transaction_volume_",
            "signed_transaction_",
            "microprice_",
            "vpin_",
        ]
        market_ta_prefixes = [
            "bid_price_",
            "ask_price_",
            "bid_qty_",
            "ask_qty_",
            "spread_",
            "imbalance_",
            "microprice_",
        ]

        if binary_outputs:
            # single_state_columns = []
            ta_prefixes = [
                "microprice_ema_",
                "vpin_ema_",
                "rsi_",
                "sma_",
                "ema_",
                "max_",
                "min_",
            ]
            single_state_columns = ["hour_of_the_day_utc"]

        states = []
        # candles
        ta_periods = periods
        for ta_prefix in ta_prefixes:
            for period in ta_periods:
                column_name = f"{ta_prefix}{int(period)}"
                states.append(column_name)

        for ta_prefix in candles_market_ta_prefixes:
            for period in range(int(max(ta_periods))):
                column_name = f"{ta_prefix}{period}"
                states.append(column_name)

        # market
        if market_horizon_save > 0:
            for market_prefix in market_ta_prefixes:
                for market_horizon in range(int(market_horizon_save)):
                    column_name = f"{market_prefix}{int(market_horizon)}"
                    states.append(column_name)

        return states + single_state_columns

    @staticmethod
    def ga_ta_state_columns_fx(
            periods: list = DEFAULT_TA_INDICATORS_PERIODS,
            market_horizon_save: int = DEFAULT_MARKET_HORIZON_SAVE,
            binary_outputs: bool = False,
    ) -> list:
        single_state_columns = [
            "hour_of_the_day_utc",
            "minutes_from_start",
            # "volume_from_start",
        ]
        ta_prefixes = [
            # "microprice_",
            # "vpin_",
            "rsi_",
            "sma_",
            "ema_",
            "max_",
            "min_",
            # "volume_rsi_",
            # "volume_sma_",
            # "volume_ema_",
            # "volume_max_",
            # "volume_min_",
        ]
        candles_market_ta_prefixes = [
            # "signed_transaction_volume_",
            # "signed_transaction_",
            # "microprice_",
            # "vpin_",
        ]
        market_ta_prefixes = [
            "bid_price_",
            "ask_price_",
            # "bid_qty_",
            # "ask_qty_",
            "spread_",
            # "imbalance_",
            # "microprice_",
        ]

        if binary_outputs:
            # single_state_columns = []
            ta_prefixes = [
                # "microprice_ema",
                # "vpin_ema",
                "rsi_",
                "sma_",
                "ema_",
                "max_",
                "min_",
            ]

        states = []
        # candles
        ta_periods = periods
        for ta_prefix in ta_prefixes:
            for period in ta_periods:
                column_name = f"{ta_prefix}{int(period)}"
                states.append(column_name)

        for ta_prefix in candles_market_ta_prefixes:
            for period in range(int(max(ta_periods))):
                column_name = f"{ta_prefix}{period}"
                states.append(column_name)

        # market
        if market_horizon_save > 0:
            for market_prefix in market_ta_prefixes:
                for market_horizon in range(int(market_horizon_save)):
                    column_name = f"{market_prefix}{int(market_horizon)}"
                    states.append(column_name)

        return states + single_state_columns

    @staticmethod
    def get_default_state_columns(
            state_type: StateType,
            is_binary: bool,
            horizonTicksMarketState: int,
            periods: list,
            instrument_pk: str = None,
    ):
        if state_type == StateType.market_state:
            return StateUtils._get_market_state_columns()

        if state_type == StateType.ta_state:
            # is_binary = False
            # if "binaryStateOutputs" in self.parameters.keys():
            #     is_binary = self.parameters["binaryStateOutputs"] > 0

            # horizonTicksMarketState = DEFAULT_MARKET_HORIZON_SAVE
            # if "horizonTicksMarketState" in self.parameters.keys():
            #     horizonTicksMarketState = self.parameters["horizonTicksMarketState"]

            # periods = DEFAULT_TA_INDICATORS_PERIODS
            # if is_binary:
            #     periods = DEFAULT_TA_INDICATORS_PERIODS_BINARY
            # if "periodsTAStates" in self.parameters.keys():
            #     periods = self.parameters["periodsTAStates"]

            is_fx = instrument_pk is not None and TickDB().is_fx_instrument(
                instrument_pk=instrument_pk
            )
            return StateUtils._get_ta_state_columns(
                binary_outputs=is_binary,
                market_horizon_save=horizonTicksMarketState,
                periods=periods,
                is_fx=is_fx,
            )

    @staticmethod
    def _get_multimarket_state_columns(
            multimarket_instruments: list, multimarket_periods: list
    ) -> list:

        # multimarket_instruments = self.parameters['otherInstrumentsStates']
        # multimarket_periods = self.parameters['otherInstrumentsMsPeriods']
        multimarket_states = []
        if multimarket_instruments is not None and len(multimarket_instruments) > 0:
            pattern_base = ['zscore_mid']
            for instrument in multimarket_instruments:
                for pattern in pattern_base:
                    for period in multimarket_periods:
                        multimarket_states.append(rf'{pattern}_{instrument}_{period}')
        return multimarket_states

    @staticmethod
    def _get_ta_state_columns(
            binary_outputs: bool = False,
            market_horizon_save: int = DEFAULT_MARKET_HORIZON_SAVE,
            periods: list = DEFAULT_TA_INDICATORS_PERIODS,
            multimarket_instruments: list = [],
            multimarket_periods: list = [],
            is_fx: bool = False,
    ):
        if is_fx:
            ta_states = StateUtils.ga_ta_state_columns_fx(
                binary_outputs=binary_outputs,
                market_horizon_save=market_horizon_save,
                periods=periods,
            )
        else:
            ta_states = StateUtils.ga_ta_state_columns(
                binary_outputs=binary_outputs,
                market_horizon_save=market_horizon_save,
                periods=periods,
            )

        # adding multimarket parameters
        multimarket_states = StateUtils._get_multimarket_state_columns(
            multimarket_instruments=multimarket_instruments,
            multimarket_periods=multimarket_periods,
        )
        return ta_states + multimarket_states

    @staticmethod
    def get_individual_state_columns():
        return INDIVIDUAL_COLUMNS

    @staticmethod
    def get_private_state_columns(private_horizon_ticks: int):
        private_states = []
        for private_column in PRIVATE_COLUMNS:
            for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                private_states.append(rf'{private_column}_{private_state_horizon}')
        return private_states

    @staticmethod
    def _get_market_state_columns(
            private_horizon_ticks: int,
            market_horizon_ticks: int,
            candle_horizon: int,
            multimarket_instruments: list = [],
            multimarket_periods: list = [],
    ):

        private_states = []
        individual_states = StateUtils.get_individual_state_columns()
        market__depth_states = []
        candle_states = []
        market__trade_states = []
        # private_horizon_ticks = self.parameters['horizonTicksPrivateState']
        # market_horizon_ticks = self.parameters['horizonTicksMarketState']
        # candle_horizon = self.parameters['horizonCandlesState']
        #     "otherInstrumentsStates": [],
        #     "otherInstrumentsMsPeriods": []

        if not REMOVE_PRIVATE_STATES:
            private_states = StateUtils.get_private_state_columns(private_horizon_ticks)

        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_bid_price_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_ask_price_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_bid_qty_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_ask_qty_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_spread_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_midprice_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_imbalance_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_microprice_%d' % market_state_horizon)

        if not DISABLE_LAST_CLOSE:
            for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
                market__trade_states.append(
                    'market_last_close_price_%d' % market_state_horizon
                )
            for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
                market__trade_states.append(
                    'market_last_close_qty_%d' % market_state_horizon
                )

        if not MARKET_MIDPRICE_RELATIVE:
            for candle_state_horizon in range(candle_horizon - 1, -1, -1):
                candle_states.append('candle_open_%d' % candle_state_horizon)
        for candle_state_horizon in range(candle_horizon - 1, -1, -1):
            candle_states.append('candle_high_%d' % candle_state_horizon)
        for candle_state_horizon in range(candle_horizon - 1, -1, -1):
            candle_states.append('candle_low_%d' % candle_state_horizon)
        for candle_state_horizon in range(candle_horizon - 1, -1, -1):
            candle_states.append('candle_close_%d' % candle_state_horizon)

        candle_states.append('candle_ma')
        candle_states.append('candle_std')
        candle_states.append('candle_max')
        candle_states.append('candle_min')

        # adding multimarket parameters
        multimarket_states = StateUtils._get_multimarket_state_columns(
            multimarket_instruments=multimarket_instruments,
            multimarket_periods=multimarket_periods,
        )
        columns_states = (
                private_states
                + individual_states
                + market__depth_states
                + market__trade_states
                + candle_states
                + multimarket_states
        )
        return columns_states
