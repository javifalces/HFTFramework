import pandas as pd
import numpy as np
from joblib import Memory
from numba import njit

import collections

from backtest.score_enum import ScoreEnum, get_score_enum_csv_column

ZERO_VALUE_DEFAULT = 0.00000001


@njit
def __calculate_open_returns_numba__(trades_input_reversed, best_bid, best_ask):
    position_column = 2
    quantity_column = 0
    price_column = 1

    last_side_position = 0
    open_returns = np.zeros(trades_input_reversed.shape[0])
    total_position = trades_input_reversed[0][position_column]
    total_position_sign = np.sign(total_position)
    exit_price = 0
    if total_position > 0:
        exit_price = best_bid
    elif total_position < 0:
        exit_price = best_ask
    else:
        return open_returns
    for index in range(trades_input_reversed.shape[0]):
        if total_position == 0:
            break
        row = trades_input_reversed[index][:]
        quantity = row[quantity_column]
        position = row[position_column]
        side = np.sign(row[quantity_column])
        side_position = np.sign(row[position_column])
        entry_price = row[price_column]
        if side == total_position_sign:
            if np.abs(quantity) <= np.abs(total_position):
                open_return = quantity * (exit_price - entry_price)
                total_position = total_position - quantity
                open_returns[index] = open_return
            elif np.abs(quantity) > np.abs(total_position):
                open_return = total_position * (exit_price - entry_price)
                total_position = 0
                open_returns[index] = open_return

    return open_returns


@njit
def __calculate_close_returns_numba__(trades_input):
    '''

    :param trades_input: trades
    :return: closed returns , open bids or open asks dictionaries
    '''
    position_column = 2
    quantity_column = 0
    price_column = 1

    last_side_position = 0
    last_row = None

    buy_dict = dict()
    sell_dict = dict()

    closed_returns = np.zeros(trades_input.shape[0])

    for index in range(trades_input.shape[0]):
        row = trades_input[index][:]
        position = row[position_column]
        side = np.sign(row[quantity_column])
        side_position = np.sign(row[position_column])
        # To add directly to quantities dict
        quantity_to_save_dict = abs(row[quantity_column])
        if last_row is None:
            # first iteration
            pass
        else:
            if side == last_side_position:
                pass
            else:
                position_abs = np.abs(last_row[position_column])
                quantity_abs = np.abs(row[quantity_column])
                close_quantity = position_abs
                if close_quantity > quantity_abs:
                    close_quantity = quantity_abs
                # close_quantity = np.min([position_abs,quantity_abs ])  # close the previous position
                remaining_position = close_quantity
                quantity_to_save_dict = abs(row[quantity_column])
                closed_return = 0
                iterations = 0
                while remaining_position != 0:
                    # while remaining_position > 1e-6 or remaining_position < -1e-6:
                    # if iterations > 2000:
                    #     print(
                    #         'Error calculating pnl iterations infinite to calculate closed returns '
                    #     )
                    #     break
                    if side < 0 and len(buy_dict) != 0:
                        # search the entry point in buys - loiwest price first
                        # entry_price = sorted(list(buy_dict.keys()), reverse=False)[0]
                        # fifo
                        entry_price = list(buy_dict.keys())[0]

                        entry_quantity = buy_dict[entry_price]
                        entry_quantity = min(entry_quantity, abs(remaining_position))

                        quantity_to_save_dict -= entry_quantity
                        remaining_position -= entry_quantity
                        exit_price = row[price_column]
                        closed_return += entry_quantity * (exit_price - entry_price)

                        entry_remain_position = buy_dict[entry_price] - entry_quantity
                        if entry_remain_position == 0:
                            del buy_dict[entry_price]
                        else:
                            buy_dict[entry_price] = entry_remain_position
                    elif side < 0 and len(buy_dict) == 0:
                        break

                    elif side > 0 and len(sell_dict) != 0:
                        # search the entry point in sells , highest price first
                        # entry_price = sorted(list(sell_dict.keys()), reverse=True)[0]

                        # fifo
                        entry_price = list(sell_dict.keys())[0]

                        entry_quantity = sell_dict[entry_price]
                        entry_quantity = min(entry_quantity, abs(remaining_position))

                        quantity_to_save_dict -= entry_quantity
                        remaining_position -= entry_quantity
                        closed_return += -entry_quantity * (
                            row[price_column] - entry_price
                        )

                        entry_remain_position = sell_dict[entry_price] - entry_quantity
                        if entry_remain_position == 0:
                            del sell_dict[entry_price]
                        else:
                            sell_dict[entry_price] = entry_remain_position

                    elif side > 0 and len(sell_dict) == 0:
                        break
                    iterations += 1

                closed_returns[index] = closed_return

        if quantity_to_save_dict > 0:
            if side > 0:
                if row[price_column] not in list(buy_dict.keys()):
                    buy_dict[
                        row[price_column]
                    ] = quantity_to_save_dict  # row['quantity']
                else:
                    buy_dict[
                        row[price_column]
                    ] = +quantity_to_save_dict  # row['quantity']
                # buy_dict = (sorted(buy_dict.items()))
            else:
                if row[price_column] not in list(sell_dict.keys()):
                    sell_dict[
                        row[price_column]
                    ] = quantity_to_save_dict  # row['quantity']
                else:
                    sell_dict[
                        row[price_column]
                    ] = +quantity_to_save_dict  # row['quantity']
                # sell_dict = (reversed(sorted(sell_dict.items())))
        last_side_position = side_position
        last_side = side
        last_row = row
        last_index = row

    return closed_returns, buy_dict, sell_dict


def __calculate_closed_returns_trades_numba__(trades_input: pd.DataFrame) -> tuple:
    trades_output = trades_input.copy()
    # close returns
    numba_closed_returns = 0
    bid_dict = {}
    ask_dict = {}
    if trades_input is not None and len(trades_input) > 0:
        matrix_input = trades_input.values[:, 2:].astype('float64')
        numba_closed_returns, bid_dict, ask_dict = __calculate_close_returns_numba__(
            trades_input=matrix_input
        )
    trades_output['close_returns'] = numba_closed_returns
    return trades_output, bid_dict, ask_dict


def __calculate_closed_returns_trades__(trades_input: pd.DataFrame) -> tuple:
    '''

    :param trades_input: dataframe with trades hapen
    :return: closed returns , dictionry pendig bids , dictionary pending asks
    '''
    # change to numba
    # return self.calculate_returns_trades_pandas(trades_input=trades_input)
    return __calculate_closed_returns_trades_numba__(trades_input=trades_input)





def get_asymetric_dampened_reward(trade_df: pd.DataFrame):
    return trade_df['asymmetric_dampened_pnl'].iloc[-1]







def get_max_drawdowns(backtest_returns):
    equity_curve = backtest_returns.cumsum()
    i = np.argmax(
        np.maximum.accumulate(equity_curve.values) - equity_curve.values
    )  # end of the period
    if i == 0:
        MDD_start, MDD_end, MDD_duration, drawdown, UW_dt, UW_duration = (
            0,
            0,
            0,
            0,
            0,
            0,
        )
        return MDD_start, MDD_end, MDD_duration, drawdown, UW_dt, UW_duration
    j = np.argmax(equity_curve.values[:i])  # start of period

    drawdown = 100 * ((equity_curve[i] - equity_curve[j]) / equity_curve[j])

    DT = equity_curve.index.values

    start_dt = pd.to_datetime(str(DT[j]))
    MDD_start = start_dt.strftime("%Y-%m-%d")

    end_dt = pd.to_datetime(str(DT[i]))
    MDD_end = end_dt.strftime("%Y-%m-%d")

    NOW = pd.to_datetime(str(DT[-1]))
    NOW = NOW.strftime("%Y-%m-%d")

    MDD_duration = np.busday_count(MDD_start, MDD_end)
    time_difference = end_dt - start_dt
    try:
        UW_dt = (
            equity_curve[i:]
            .loc[equity_curve[i:].values >= equity_curve[j]]
            .index.values[0]
        )
        UW_dt = pd.to_datetime(str(UW_dt))
        UW_dt = UW_dt.strftime("%Y-%m-%d")
        UW_duration = np.busday_count(MDD_end, UW_dt)
    except:
        UW_dt = "0000-00-00"
        UW_duration = np.busday_count(MDD_end, NOW)

    return MDD_start, MDD_end, time_difference, drawdown, UW_dt, UW_duration


def get_drawdown(equity_curve:pd.Series)->pd.Series:
    data_ser_df = equity_curve  # backtest_returns.cumsum()
    # return data_ser_df.expanding(1).max() - data_ser_df
    highest_value = data_ser_df.cummax()
    return highest_value - data_ser_df

def get_sortino(equity_curve: pd.Series, rfr=0, target=0) -> float:
    # rfr = 0
    # target = 0
    df = equity_curve.diff().to_frame('Returns')
    df['downside_returns'] = 0
    df.loc[df['Returns'] < target, 'downside_returns'] = df['Returns'] ** 2
    expected_return = df['Returns'].mean()
    down_stdev = np.sqrt(df['downside_returns'].mean())
    if down_stdev == 0:
        down_stdev = 0.000001

    sortino_ratio = (expected_return - rfr) / down_stdev
    return sortino_ratio


def get_sharpe(equity_curve: pd.Series, rfr=0) -> float:
    df = equity_curve.diff().to_frame('Returns')
    expected_return = df['Returns'].mean()
    down_stdev = df['Returns'].std()
    sharpe = (expected_return - rfr) / down_stdev
    return sharpe

def get_falces_marin_ratio(equity_curve: pd.Series, number_trades: int) -> float:
    '''
    Falces Marin ratio

    But the main idea is something like (number_of_trades * profit)/(underwater_area*complexity)

    :param equity_curve:
    :return:
    '''
    if number_trades == 0:
        return -9999.99
    returns = equity_curve.pct_change(periods=1).replace([0.0, np.nan, np.inf, -np.inf], np.nan).dropna()
    max_dd_pct = get_max_drawdown_pct(equity_curve)#to avoid start at zero

    if max_dd_pct == 0:
        max_dd_pct = ZERO_VALUE_DEFAULT

    med_return = returns.median()  # avoid outliers

    return (med_return / max_dd_pct) * number_trades


def get_ulcer_index(equity_curve: pd.Series):
    returns = equity_curve.diff()
    max_dd = get_max_drawdown(equity_curve=equity_curve)
    if max_dd == 0:
        max_dd = ZERO_VALUE_DEFAULT

    return returns.mean() / max_dd


def get_pnl_to_map(equity_curve: pd.Series, position: pd.Series, ) -> float:
    '''
    Pnl to mean absolute position
    simultaneously considers both the profitability and the incurred inventory risk of the MM strategy up to the time t.
    PnlMap = W/Map

    W = pnl
    Map = 1/M*Sum(inventory)
    :param equity_curve:
    :param position:
    :return:
    '''
    # based on MM With Signals Through DRL
    return (equity_curve / position.abs()).iloc[-1]


def get_max_drawdown(equity_curve: pd.Series) -> float:
    dd = get_drawdown(equity_curve)
    return dd.max()


def get_max_drawdown_pct(equity_curve: pd.Series) -> float:
    dd = get_drawdown(equity_curve)
    try:
        max_dd_position = dd.argmax()
        if max_dd_position == 0:
            return 0.0
        max_pnl = max(equity_curve.iloc[:max_dd_position])
        min_pnl = equity_curve.iloc[max_dd_position]
        max_dd = dd.iloc[max_dd_position]  # max_pnl-min_pnl
        assert max_dd == max_pnl - min_pnl
        if max_pnl == 0.0:
            return 1.0
        return (max_dd) / max_pnl
    except Exception as e:
        print(rf"error calculating get_max_drawdown_pct {str(e)}")
        return 1.0

def get_score(backtest_df: pd.DataFrame, score_enum: ScoreEnum,
              equity_column_score: ScoreEnum = ScoreEnum.total_pnl) -> float:
    equity_curve = backtest_df[get_score_enum_csv_column(equity_column_score)]
    trades = len(backtest_df)
    score = 0
    if score_enum == ScoreEnum.falcma_ratio:
        score = get_falces_marin_ratio(equity_curve=equity_curve, number_trades=trades)
    if score_enum == ScoreEnum.sharpe:
        score = get_sharpe(equity_curve=equity_curve)
    if score_enum == ScoreEnum.max_dd:
        score = -get_max_drawdown(equity_curve=equity_curve)
    if score_enum == ScoreEnum.sortino:
        score = get_sortino(equity_curve=equity_curve)

    if score_enum == ScoreEnum.total_pnl:
        score = equity_curve.iloc[-1]
    if score_enum == ScoreEnum.realized_pnl:
        score = backtest_df[get_score_enum_csv_column(ScoreEnum.realized_pnl)]

    return score


if __name__ == '__main__':
    # equity_curve_test = pd.Series([100., 110., 90., 80., 95., 100.01, 120., 140.1, 170.2])
    equity_curve_test = pd.Series([100., 110., 120., 150.])
    num_trades = 3
    max_dd = get_max_drawdown(equity_curve_test)
    sharpe = get_sharpe(equity_curve=equity_curve_test)
    falcma = get_falces_marin_ratio(equity_curve=equity_curve_test, number_trades=num_trades)
