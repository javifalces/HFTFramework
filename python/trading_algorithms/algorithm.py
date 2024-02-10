import os
import datetime
import numpy as np
import pandas as pd
from matplotlib import dates
import copy
from trading_algorithms.algorithm_enum import AlgorithmEnum
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
    MultiThreadConfiguration,
)
from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.parameter_tuning.ga_parameter_tuning import GAParameterTuning
from backtest.pnl_utils import get_drawdown, get_backtest_df_date_indexed
from configuration import LAMBDA_OUTPUT_PATH, SHARPE_BACKTEST_FREQ
import math


class AlgorithmParameters:
    ui = 'ui'
    quantity = "quantity"
    first_hour = "firstHour"
    last_hour = "lastHour"
    seed = "seed"


class Algorithm:
    MAXTICKS_PLOT = 30000
    NAME = ''
    PLOT_ROLLING_WINDOW_TICKS = 25
    FORMAT_SAVE_NUMBERS = '%.18e'

    DELAY_MS = 65
    FEES_COMMISSIONS_INCLUDED = True
    MULTITHREAD_CONFIGURATION = MultiThreadConfiguration.multithread

    @staticmethod
    def set_defaults_parameters(parameters: dict, DEFAULT_PARAMETERS: dict) -> dict:
        for default_key, default_value in DEFAULT_PARAMETERS.items():
            parameters.setdefault(default_key, default_value)
        return parameters

    def __init__(self, algorithm_info: str, parameters: dict) -> None:
        super().__init__()
        # self.NAME=''
        self.algorithm_info = algorithm_info
        self.parameters = copy.copy(parameters)

    def __reduce__(self):
        return (self.__class__, (self.algorithm_info, self.parameters))

    def get_json_param(self) -> dict:
        # import json
        output_dict = {}
        output_dict['algorithmName'] = self.algorithm_info
        output_dict['parameters'] = self.parameters
        return output_dict

    def _is_memory_file(self, file: str, algorithm_name: str):
        return algorithm_name in file and (
                'qmatrix' in file or 'memoryReplay' in file or 'input_ml' in file
        )

    def _is_model_file(self, file: str, algorithm_name: str):
        return algorithm_name in file and (
                file.endswith('.model') or file.endswith('.onnx')
        )

    def clean_experience(self, output_path):
        # clean it
        for file in os.listdir(output_path):
            if self._is_memory_file(file, algorithm_name=self.algorithm_info):
                print('clean experience qmatrix/memoryReplay/input_ml %s' % file)
                os.remove(output_path + os.sep + file)

    def clean_model(self, output_path):
        # clean it
        for file in os.listdir(output_path):
            if self._is_model_file(file, algorithm_name=self.algorithm_info):
                print('clean ml model %s' % file)
                os.remove(output_path + os.sep + file)

    def get_test_name(self, name: str, algorithm_number: int = None):
        prefix = self.algorithm_info
        if not prefix.startswith(name):
            prefix = '%s_%s' % (name, self.algorithm_info)
        if algorithm_number is None:
            algorithm_name = prefix
        else:
            algorithm_name = '%s_%d' % (prefix, algorithm_number)
        return algorithm_name

    def clean_permutation_cache(self, output_path):
        # clean it
        for file in os.listdir(output_path):
            if self.algorithm_info in file and 'permutationStates' in file:
                print('clean permutationStates cache %s' % file)
                os.remove(output_path + os.sep + file)

    def set_parameters(self, parameters: dict):
        parameters_key_set_it = []
        for key_param in self.parameters.keys():
            if key_param in parameters.keys():
                self.parameters[key_param] = parameters[key_param]
                parameters_key_set_it.append(key_param)

        parameters_key_input = set(parameters.keys())
        # if len(parameters_key_set_it) < len(parameters_key_input):
        #     not_set = list(set(parameters_key_input) - set(parameters_key_set_it))
        #     not_set_str = ','.join(not_set)
        #     print(f'WARNING some parameters not set! {not_set_str}')

        if AlgorithmParameters.seed in parameters.keys():
            self.parameters[AlgorithmParameters.seed] = parameters[
                AlgorithmParameters.seed
            ]

    def set_training_seed(self, parameters, iteration: int, algorithm_number: int):
        if AlgorithmParameters.seed in self.parameters.keys():
            self.parameters[AlgorithmParameters.seed] = (
                    self.parameters[AlgorithmParameters.seed]
                    + iteration * 1000
                    + algorithm_number
            )

    def get_iteration_number_filename(self, filename) -> int:
        try:
            splitted = filename.split('_')
            last_item = splitted[-1]
            number = last_item[: last_item.find(".")]
            output = int(number)
            return output
        except:
            return 0

    def parameter_tuning(
            self,
            algorithm_enum: AlgorithmEnum,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            parameters_base: dict,
            parameters_min: dict,
            parameters_max: dict,
            max_simultaneous: int,
            generations: int,
            ga_configuration: GAConfiguration,
            clean_initial_generation_experience: bool = True,
    ) -> (dict, pd.DataFrame):

        backtest_configuration = BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            delay_order_ms=self.DELAY_MS,
            multithread_configuration=self.MULTITHREAD_CONFIGURATION,
            fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
        )

        # exponential reducing sigma
        generations_exp_decay = []
        exp_multiplier = ga_configuration.exp_multiplier
        sigmas = [ga_configuration.sigma]
        for generation in range(1, generations):
            sigma_expected = ga_configuration.sigma * math.exp(
                -exp_multiplier * generation
            )
            generations_exp_decay.append(ga_configuration.sigma - sigma_expected)
            sigmas.append(ga_configuration.sigma - generations_exp_decay[-1])
        sigmas_str = [str(i) for i in sigmas]
        print(rf"Sigmas parameter_tuning -> {sigmas_str}")

        ga_configuration.decay = generations_exp_decay
        # %

        ga_parameter_tuning = GAParameterTuning(
            ga_configuration=ga_configuration,
            algorithm=algorithm_enum,
            parameters_base=copy.copy(parameters_base),
            parameters_min=copy.copy(parameters_min),
            parameters_max=copy.copy(parameters_max),
            max_simultaneous=max_simultaneous,
            initial_param_dict_list=[],
        )

        best_param_dict = {}
        for generation in range(generations):
            # clean between generations exp
            if clean_initial_generation_experience:
                print(
                    'cleaning outputs/experience on generation %d in path %s'
                    % (generation, LAMBDA_OUTPUT_PATH)
                )
                self.clean_experience(output_path=LAMBDA_OUTPUT_PATH)
                self.clean_permutation_cache(output_path=LAMBDA_OUTPUT_PATH)

            ga_parameter_tuning.run_generation(
                backtest_configuration=backtest_configuration,
                check_unique_population=True,
            )
            best_param_dict = ga_parameter_tuning.get_best_param_dict()

            best_score = ga_parameter_tuning.get_best_score()
        print(
            '%s best score %.4f\nbest_param_dict %s'
            % (algorithm_enum, best_score, best_param_dict)
        )
        return (best_param_dict, ga_parameter_tuning.population_df_out)

    def get_sharpe(self, trade_df: pd.DataFrame, column: str = 'returns') -> float:
        if len(trade_df) == 0:
            return 0.0
        # group by time
        trade_df = get_backtest_df_date_indexed(backtest_df=trade_df)
        returns = (
            trade_df[column]
            .groupby(pd.Grouper(freq=SHARPE_BACKTEST_FREQ))
            .sum()
            .reset_index(drop=True)
            .fillna(0.0)
        )
        return returns.mean() / returns.std()

    def get_trade_df(self, raw_trade_pnl_df: pd.DataFrame):
        columns_rn = {
            'date': 'time',
            'historicalTotalPnl': 'total_pnl',
            'historicalRealizedPnl': 'close_pnl',
            'historicalUnrealizedPnl': 'open_pnl',
            'netPosition': 'position',
            'clientOrderId': 'client_order_id',
        }
        if raw_trade_pnl_df is None or len(raw_trade_pnl_df) == 0:
            print(rf"WARNING: raw_trade_pnl_df on get_trade_df is empty -> return None")
            return None

        trade_pnl_df = raw_trade_pnl_df.rename(columns=columns_rn)
        trade_pnl_df['time'] = pd.to_datetime(
            trade_pnl_df['timestamp'] * 1000000
        )  # + pd.DateOffset(hours=1)

        # trade_pnl_df['returns'] = trade_pnl_df['total_pnl'].pct_change().fillna(0)
        # trade_pnl_df['close_returns']=trade_pnl_df['close_pnl'].pct_change().fillna(0)
        # trade_pnl_df['open_returns'] = trade_pnl_df['open_pnl'].pct_change().fillna(0)

        trade_pnl_df['returns'] = trade_pnl_df['total_pnl'].diff().fillna(0)
        trade_pnl_df['close_returns'] = trade_pnl_df['close_pnl'].diff().fillna(0)
        trade_pnl_df['open_returns'] = trade_pnl_df['open_pnl'].diff().fillna(0)

        trade_pnl_df['open_pnl'] = trade_pnl_df['total_pnl']

        trade_pnl_df['returns'] = trade_pnl_df['returns'].replace(
            [np.inf, -np.inf], np.nan
        )
        trade_pnl_df['close_returns'] = trade_pnl_df['close_returns'].replace(
            [np.inf, -np.inf], np.nan
        )
        trade_pnl_df['open_returns'] = trade_pnl_df['open_returns'].replace(
            [np.inf, -np.inf], np.nan
        )
        trade_pnl_df.ffill(inplace=True)
        before_len = len(trade_pnl_df)
        trade_pnl_df.dropna(axis=0, inplace=True)  # all the rest!
        if len(trade_pnl_df) == 0 and before_len > 0:
            print(rf"WARNING: trade pnl size =0 and before dropna {before_len}!!")

        trade_pnl_df['sharpe'] = self.get_sharpe(
            trade_df=trade_pnl_df, column='returns'
        )
        trade_pnl_df['drawdown'] = get_drawdown(trade_pnl_df['close_pnl'])

        return trade_pnl_df

    def plot_params_base(
            self,
            fig,
            axs,
            last_index_plotted,
            color,
            color_mean,
            bid_color,
            ask_color,
            lw,
            alpha,
            raw_trade_pnl_df: pd.DataFrame,
    ):
        from matplotlib import dates
        import matplotlib.pyplot as plt

        df = self.get_trade_df(raw_trade_pnl_df=raw_trade_pnl_df)
        df.set_index('time', inplace=True)

        index = last_index_plotted + 1
        ax = axs[index]
        ax.plot(df['ask'], color=ask_color, lw=lw, alpha=alpha)
        ax.plot(df['bid'], color=bid_color, lw=lw, alpha=alpha)
        ax.grid(axis='y', ls='--', alpha=0.7)
        ax.set_ylabel('price')
        ax.legend(['ask', 'bid'])

        index += 1
        ax = axs[index]
        # ax.bar(x=df.index,height=df['ask_qty'], color=ask_color, lw=lw, alpha=alpha)
        # ax.bar(x=df.index,height=df['bid_qty'], color=bid_color, lw=lw, alpha=alpha)
        ax.plot(df['ask_qty'], color=ask_color, lw=lw, alpha=alpha)
        ax.plot(df['bid_qty'], color=bid_color, lw=lw, alpha=alpha)

        ax.grid(axis='y', ls='--', alpha=0.7)
        ax.set_ylabel('quantity')
        ax.legend(['ask', 'bid'])

        index += 1
        ax = axs[index]
        ax.plot(df['imbalance'], color=color, lw=lw, alpha=alpha)
        ax.plot(
            df['imbalance'].rolling(window=self.PLOT_ROLLING_WINDOW_TICKS).mean(),
            color=color_mean,
            lw=lw - 0.1,
            alpha=alpha,
        )
        ax.grid(axis='y', ls='--', alpha=0.7)
        ax.set_ylabel('imbalance')

        index += 1
        ax = axs[index]
        ax.plot(df['reward'], color=color, lw=lw, alpha=alpha)
        ax.plot(
            df['reward'].rolling(window=self.PLOT_ROLLING_WINDOW_TICKS).mean(),
            color=color_mean,
            lw=lw - 0.1,
            alpha=alpha,
        )
        ax.set_ylabel('reward')
        ax.grid(axis='y', ls='--', alpha=0.7)
        ax.set_title('mean reward=%.2f' % df['reward'].mean())

        #### xaxis
        # date_locator=dates.MinuteLocator()
        date_locator = dates.HourLocator()
        date_locator.MAXTICKS = Algorithm.MAXTICKS_PLOT

        plt.gca().xaxis.set_major_locator(date_locator)
        plt.gca().xaxis.set_major_formatter(dates.DateFormatter('%H:%M'))

        minor_locator = dates.MinuteLocator(byminute=[0, 15, 30, 45], interval=1)
        minor_locator.MAXTICKS = Algorithm.MAXTICKS_PLOT
        plt.gca().xaxis.set_minor_locator(minor_locator)
        plt.xticks(rotation=60)

        return fig

    def plot_params(
            self, raw_trade_pnl_df: pd.DataFrame, figsize=None, title: str = None
    ):
        import seaborn as sns

        sns.set_theme()
        import matplotlib.pyplot as plt

        try:
            if figsize is None:
                figsize = (20, 12)

            df = self.get_trade_df(raw_trade_pnl_df=raw_trade_pnl_df)
            df.set_index('time', inplace=True)

            print('plotting params from %s to %s' % (df.index[0], df.index[-1]))

            skew_change = True
            windows_change = True

            if len(df['skew'].fillna(0).diff().fillna(0).unique()) == 0:
                skew_change = False

            # if len(df['windows_tick'].fillna(0).diff().fillna(0).unique()) == 0:
            #     windows_change = False

            plt.close()
            subplot_origin = 510
            nrows = 5
            if skew_change:
                subplot_origin += 100
                nrows += 1

            if windows_change:
                subplot_origin += 100
                nrows += 1

            subplot_origin += 1

            index = 0
            fig, axs = plt.subplots(nrows=nrows, ncols=1, figsize=figsize, sharex=True)
            color = 'black'
            color_mean = 'gray'
            bid_color = 'green'
            ask_color = 'red'

            window = self.PLOT_ROLLING_WINDOW_TICKS
            alpha = 0.9
            lw = 0.5

            ax = axs[index]
            ax.plot(df['riskAversion'], color=color, lw=lw, alpha=alpha)
            ax.plot(
                df['riskAversion'].rolling(window=window).mean(),
                color=color_mean,
                lw=lw - 0.1,
                alpha=alpha,
            )

            ax.set_ylabel('riskAversion')
            ax.grid(axis='y', ls='--', alpha=0.7)

            if title is not None:
                ax.set_title(title)

            if windows_change:
                index += 1
                ax = axs[index]
                ax.plot(df['midpricePeriodSeconds'], color=color, lw=lw, alpha=alpha)
                ax.plot(
                    df['midpricePeriodSeconds'].rolling(window=window).mean(),
                    color=color_mean,
                    lw=lw - 0.1,
                    alpha=alpha,
                )

                ax.set_ylabel('midpricePeriodSeconds')
                ax.grid(axis='y', ls='--', alpha=0.7)

            if skew_change:
                index += 1
                ax = axs[index]
                ax.plot(df['skew'], color=color, lw=lw, alpha=alpha)
                ax.set_ylabel('skew')
                ax.grid(axis='y', ls='--', alpha=0.7)

            fig = self.plot_params_base(
                fig,
                axs=axs,
                last_index_plotted=index,
                color=color,
                color_mean=color_mean,
                bid_color=bid_color,
                ask_color=ask_color,
                lw=lw,
                alpha=alpha,
                raw_trade_pnl_df=raw_trade_pnl_df,
            )
            # plt.show()
            return fig
        except Exception as e:
            print('Some error plotting params %s' % e)
        return None

    def plot_trade_results(
            self,
            raw_trade_pnl_df: pd.DataFrame,
            figsize=None,
            title: str = None,
            plot_open: bool = True,
    ) -> tuple:
        import warnings
        import matplotlib.pyplot as plt
        import seaborn as sns

        sns.set_theme()

        trade_pnl_df = self.get_trade_df(raw_trade_pnl_df=raw_trade_pnl_df)
        from matplotlib import MatplotlibDeprecationWarning
        import pandas as pd

        warnings.filterwarnings("ignore", category=MatplotlibDeprecationWarning)
        warnings.filterwarnings("ignore", category=Warning)
        # prepare dataframe to plot

        # plt.style.use('seaborn-white')
        # plt.style.use('seaborn-whitegrid')
        if trade_pnl_df is None:
            print('No trades to plot!')
            return (None, trade_pnl_df)

        if len(trade_pnl_df) == 0:
            print('No trades to plot!')
            return (None, trade_pnl_df)
        print(
            'plotting trade_results from %s to %s'
            % (trade_pnl_df['time'].iloc[0], trade_pnl_df['time'].iloc[-1])
        )
        trade_pnl_df = trade_pnl_df.set_index('time')
        # prepare blank plot
        plt.close()
        if figsize is None:
            figsize = (20, 12)

        date_locator = dates.HourLocator()
        date_locator.MAXTICKS = Algorithm.MAXTICKS_PLOT

        minor_locator = dates.MinuteLocator(byminute=[0, 15, 30, 45], interval=1)
        minor_locator.MAXTICKS = Algorithm.MAXTICKS_PLOT
        color = 'black'
        color_close = 'black'
        color_open = 'lightgray'

        color_mean = 'lightgray'
        bid_color = 'green'
        ask_color = 'red'

        window = self.PLOT_ROLLING_WINDOW_TICKS
        alpha = 0.9
        lw = 0.5
        # prepare blank plot

        index = 0
        fig, axs = plt.subplots(nrows=4, ncols=1, figsize=figsize)

        # pnl
        try:
            ax = axs[index]
            legend_values = ['close_pnl']
            ax.plot(trade_pnl_df['close_pnl'], color=color_close, lw=lw, alpha=alpha)
            if plot_open:
                ax.plot(trade_pnl_df['open_pnl'], color=color_open, lw=lw, alpha=alpha)
                legend_values.append('open_pnl')

            ax.legend(legend_values)
            ax.set_ylabel('pnl (€)')
            ax.grid(axis='y', ls='--', alpha=0.7)

            if title is None:
                ax.set_title(self.algorithm_info)
            else:
                ax.set_title(title)
            index += 1

        except Exception as e:
            print(rf"error plotting pnl {e}")

        # drawdown
        try:
            ax = axs[index]
            from backtest.pnl_utils import get_drawdown

            dd_open = get_drawdown(trade_pnl_df['open_pnl'])
            dd_close = get_drawdown(trade_pnl_df['close_pnl'])
            dd_close.index = pd.to_datetime(dd_close.index)
            dd_open.index = pd.to_datetime(dd_open.index)
            legend_values = ['drawdown_close_pnl']
            dd_close.plot.area(ax=ax, color=color_close, lw=lw, alpha=alpha)
            # ax.plot(dd_close,kind='area', color=color_close, lw=lw, alpha=alpha)

            if plot_open:
                dd_open.plot.area(ax=ax, color=color_open, lw=lw, alpha=alpha)
                # ax.area(dd_open,kind='area', color=color_open, lw=lw, alpha=alpha)
                legend_values.append('drawdown_open_pnl')

            ax.legend(legend_values)
            ax.set_ylabel('drawdown (€)')
            plt.gca().xaxis.set_major_locator(date_locator)
            plt.gca().xaxis.set_major_formatter(dates.DateFormatter('%H:%M'))
            plt.gca().xaxis.set_minor_locator(minor_locator)

            index += 1
        except Exception as e:
            print(rf"error plotting dd {e}")

        # position sizing
        try:
            ax = axs[index]
            ax.plot(trade_pnl_df['position'], color=color, lw=lw, alpha=alpha)
            ax.set_ylabel('position')
            # quantity_mean = trade_pnl_df['quantity']*(trade_pnl_df['position'].max())
            # quantity_mean.plot(color='k', figsize=figsize)
            # plt.legend(['position','quantity_scaled'])

            plt.gca().xaxis.set_major_locator(date_locator)
            plt.gca().xaxis.set_major_formatter(dates.DateFormatter('%H:%M'))
            plt.gca().xaxis.set_minor_locator(minor_locator)

            ax.set_xlabel('time')
            index += 1
        except Exception as e:
            print(rf"error plotting position {e}")

        plt.gca().xaxis.set_major_locator(date_locator)
        plt.gca().xaxis.set_major_formatter(dates.DateFormatter('%H:%M'))
        plt.gca().xaxis.set_minor_locator(minor_locator)

        # returns hist
        ax = plt.subplot(427)
        # axs = fig.subplots(nrows=4, ncols=2)
        index_col = 0
        try:
            # ax = axs[index][index_col]
            legend_values = ['close_returns']
            sns.histplot(
                ax=ax,
                data=trade_pnl_df,
                x="close_returns",
                bins=30,
                kde=True,
                color=color_close,
            )
            # trade_pnl_df['close_returns'].plot(
            #     kind='hist', ax=ax, stacked=True, color=color_close, lw=lw, alpha=alpha
            # )

            # ax.plot(trade_pnl_df['close_returns'], color=color_close, lw=lw, alpha=alpha)
            if plot_open:
                sns.histplot(
                    ax=ax,
                    data=trade_pnl_df,
                    x="returns",
                    bins=30,
                    kde=True,
                    color=color_open,
                )
                # trade_pnl_df['returns'].plot(
                #     kind='hist', ax=ax, stacked=True, color=color_open, lw=lw, alpha=alpha
                # )

                # ax.plot(trade_pnl_df['returns'],color=color_open, lw=lw, alpha=alpha)

                legend_values.append('total_returns')

            ax.legend(legend_values)
            ax.set_title('returns - hist')
            ax.set_ylabel('returns pct')

            # ax.tight_layout()
            # index+=1
            index_col += 1

        except Exception as e:
            print(rf"error plotting returns {e}")

        ax = plt.subplot(428)
        # add some metrics : sharpe max dd etc
        try:
            # ax = axs[index][index_col]
            column_sharpe = 'returns'

            sharpe = trade_pnl_df['sharpe'].iloc[-1]
            realized_sharpe = self.get_sharpe(
                trade_df=trade_pnl_df.reset_index(), column='close_returns'
            )

            if trade_pnl_df['sharpe'][:-1].sum() != 0:
                ax.plot(trade_pnl_df['sharpe'][:-1], color=color, lw=lw, alpha=alpha)
                # trade_pnl_df['sharpe'][:-1].plot(figsize=figsize)
                ax.set_title('rolling sharpe')

            # max_dd_close,max_dd_open,time_max_dd_close,time_max_dd_open
            from backtest.pnl_utils import get_max_drawdowns

            # MDD_start, MDD_end, time_difference, drawdown, UW_dt, UW_duration
            (
                open_time_dd,
                close_time_dd,
                td,
                open_dd,
                UW_dt,
                UW_duration,
            ) = get_max_drawdowns(trade_pnl_df['open_pnl'])
            (
                open_time_dd,
                close_time_dd,
                td,
                close_dd,
                UW_dt,
                UW_duration,
            ) = get_max_drawdowns(trade_pnl_df['close_pnl'])
            try:
                duration_mins_open = int(open_time_dd.seconds / 60)
            except:
                duration_mins_open = 0
            try:
                duration_mins_close = int(close_time_dd.seconds / 60)
            except:
                duration_mins_close = 0

            textstr = '\n'.join(
                (
                    'trades =%d ' % (len(trade_pnl_df)),
                    'open sharpe=%.4f   close_sharpe=%.4f' % (sharpe, realized_sharpe),
                    'open max_drawdown pct=%.5f duration_mins=%d'
                    % (open_dd / 100, duration_mins_open),
                    'close max_drawdown pct=%.5f duration_mins=%d'
                    % (close_dd / 100, duration_mins_close),
                )
            )
            props = dict(boxstyle='round', facecolor='wheat', alpha=0.5)
            ax.text(
                0.05,
                0.95,
                textstr,
                transform=ax.transAxes,
                fontsize=16,
                verticalalignment='top',
                bbox=props,
            )
        except Exception as e:
            print(rf"error plotting stats text box {e}")

        # plt.show()  #this is going to create a new image!

        return (plt.gcf(), trade_pnl_df)

    def get_parameters(self) -> dict:
        parameters = copy.copy(self.parameters)
        return parameters

    def test(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            algorithm_number: int = 0,
            clean_experience: bool = False,
    ) -> dict:
        backtest_configuration = BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            delay_order_ms=self.DELAY_MS,
            multithread_configuration=self.MULTITHREAD_CONFIGURATION,
            fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
        )
        parameters = self.get_parameters()

        algorithm_name = self.get_test_name(
            name=self.NAME, algorithm_number=algorithm_number
        )

        algorithm_configurationQ = AlgorithmConfiguration(
            algorithm_name=algorithm_name, parameters=parameters
        )
        input_configuration = InputConfiguration(
            backtest_configuration=backtest_configuration,
            algorithm_configuration=algorithm_configurationQ,
        )

        backtest_launcher = BacktestLauncher(
            input_configuration=input_configuration,
            id=algorithm_name,
            jar_path=JAR_PATH,
        )

        if clean_experience:
            self.clean_experience(output_path=backtest_launcher.output_path)

        backtest_controller = BacktestLauncherController(
            backtest_launchers=[backtest_launcher], max_simultaneous=1
        )
        output_dict = {}
        try:
            output_dict = backtest_controller.run()
            if self.algorithm_info is not algorithm_name:
                output_dict[self.algorithm_info] = output_dict[algorithm_name]
                del output_dict[algorithm_name]

        except Exception as e:
            print(rf"error getting test -> return empty  {e}")

        return output_dict
