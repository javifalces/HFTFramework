# -*- coding: utf-8 -*-

from __future__ import print_function

# __all__ = ['download', 'darwinex', 'parse_ticker_csv', 'pdr_override']

from ftplib import FTP as _FTP
from io import BytesIO as _BytesIO
from sys import modules as _modules
from time import sleep

import pandas as _pd
from pandas.core.base import PandasObject


class DarwinexTicksConnection:
    """
    Object to connect with Darwinex Ticks Data Servers.
    """

    def __init__(
        self,
        dwx_ftp_user='<insert your Darwinex username>',
        dwx_ftp_pass='<insert your Darwinex password>',
        dwx_ftp_hostname='<insert Darwinex Tick Data FTP host>',
        dwx_ftp_port=21,
    ):

        if dwx_ftp_hostname[:6] == 'ftp://':
            dwx_ftp_hostname = dwx_ftp_hostname[6:]

        # # Dictionary DB to hold dictionary objects in FX/Hour format
        # self._asset_db = {}

        self._ftpObj = _FTP(dwx_ftp_hostname)
        self._ftpObj.login(dwx_ftp_user, dwx_ftp_pass)
        self._virtual_dl = None
        self.available_assets = self._dir('')
        self._widgets_available = True
        self.num_retries = 3
        self.await_time = 10
        print('Connected Darwinex Ticks Data Server')

    def close(self):
        try:
            msg = self._ftpObj.quit()
        except:
            msg = self._ftpObj.close()
        return msg

    def list_of_files(self, asset='EURUSD'):
        """Return a dataframe with the files on server for a asset.
        :param asset: str, a Darwinex asset
        :return: pandas.core.frame.DataFrame with asset files at Darwinex
        servers
        """

        _dir = _pd.DataFrame(self._ftpObj.nlst(asset)[2:])
        _dir['file'] = _dir[0]
        _dir[['asset', 'pos', 'date', 'hour']] = _dir[0].str.split('_', expand=True)
        _dir.drop(0, axis=1, inplace=True)
        _dir.hour = _dir.hour.str[:2]
        _dir.index = _pd.to_datetime(_dir.date + ' ' + _dir.hour)
        _dir.index.name = 'time'
        return _dir

    def _dir(self, folder=''):
        return self._ftpObj.nlst(folder)[2:]

    @property
    def assets(self):
        return self._dir('')

    def _get_file(self, _file):
        self._virtual_dl = _BytesIO()
        self._ftpObj.retrbinary("RETR {}".format(_file), self._virtual_dl.write)
        self._virtual_dl.seek(0)

    @staticmethod
    def _parser(_data):
        return _pd.to_datetime(_data, unit='ms', utc=True)

    def _get_ticks(
        self,
        asset='EURUSD',
        start=None,
        end=None,
        cond=None,
        verbose=False,
        side='both',
        separated=False,
        fill=True,
        darwinex_time=False,
    ):

        if asset not in self.available_assets:
            raise KeyError('Asset {} not available'.format(asset))

        if cond is None:
            _files_df = self.list_of_files(asset).loc[start:end]
        else:
            _files_df = self.list_of_files(asset).loc[cond]

        if verbose is True:
            print("\n[INFO] Retrieving data from Darwinex Tick Data " "Server..")

        side = side.upper()
        posits = ['ASK', 'BID']
        max_bar = _files_df.shape[0]
        if side in posits:
            posits = [side]
            max_bar /= 2

        data = {}

        if _isnotebook():
            if 'ipywidgets' in _modules:
                from ipywidgets import FloatProgress as _FloatProgress
                from IPython.display import display as _display

                progressbar = _FloatProgress(min=0, max=max_bar)
                if max_bar > 1:
                    _display(progressbar)
            elif self._widgets_available:
                print(
                    'You must install ipywidgets module to display progress bar '
                    'for notebooks.  Use "pip install ipywidgets"'
                )
                self._widgets_available = False
        else:
            self._widgets_available = False

        right_download = 0
        wrong_download = 0

        for posit in posits:
            _files = _files_df[_files_df.pos == posit]['file'].values
            _files = ['{}/{}'.format(asset, f) for f in _files]
            data_rec = []
            #             print(_files)
            for _file in _files:
                pos = 'Ask' if 'ASK' in _file else 'Bid'
                try:
                    data_pro = None
                    for retry in range(self.num_retries):
                        self._get_file(_file)
                        # Construct DataFrame
                        data_pro = [
                            _pd.read_table(
                                self._virtual_dl,
                                compression='gzip',
                                sep=',',
                                header=None,
                                lineterminator='\n',
                                names=['Time', pos, pos + '_size'],
                                index_col='Time',
                                parse_dates=[0],
                                date_parser=self._parser,
                            )
                        ]
                        if len(data_pro) > 0:
                            right_download += 1
                            break
                        else:
                            sleep(self.await_time)
                            if verbose:
                                print('File missing, retrying.')

                    data_rec += data_pro

                    if verbose is True:
                        print('Downloaded file {}'.format(_file))

                # Case: if file not found
                except Exception as ex:
                    _exstr = "\nException Type {0}. Args:\n{1!r}"
                    _msg = _exstr.format(type(ex).__name__, ex.args)
                    print(_msg)
                    wrong_download += 1

                if self._widgets_available:
                    progressbar.value += 1
                else:
                    try:
                        print('*', end="", flush=True),
                    except TypeError:
                        print('*', end=""),

            data[posit] = _pd.concat(
                data_rec, sort=True, axis=0, verify_integrity=False
            )

        if len(posits) == 2:
            if not separated:
                data = _pd.concat([data[posit] for posit in posits], axis=1)
                if fill:
                    data = data.ffill()

        else:
            data = data[posits[0]]

        print('Process completed. {} files downloaded'.format(right_download))
        if wrong_download > 0:
            print('{} files could not be downloaded.'.format(wrong_download))

        if darwinex_time:
            data.index = _index_utc_to_mt_time(data.index)
        return data

    def ticks_from_darwinex(
        self,
        assets,
        start=None,
        end=None,
        cond=None,
        verbose=False,
        side='both',
        separated=False,
        fill=True,
        darwinex_time=False,
    ):
        """

        :param assets: str with asset or list str assets to download data
        :param start: str datetime to start ticks data, if cond is not None
        start is ignored
        :param end: str datetime to end ticks data, if cond is not None
        end is ignored
        :param cond: str valid datetime value, eg '2017-12-24 12'. '2018-08'
        :param verbose: str display information of the process
        :param side: str 'ask', 'bid' or 'both'
        :param separated: True to return a dict with ask and bid separated,
        just available for one asset.
        :param fill: boolean, if True fill side gaps when both side are
        return. False, return NaN when one side don't change at this moment.
        :param darwinex_time: boolean, False (default) use the UTC time zone,
        the same that the FTP files uses. If True the Darwinex Metatrader
        timezone is used, GMT+2 but with the New York DST. If True, start and
        end must be passed, cond is not accepted.
        :return: pandas.core.frame.DataFrame with ticks data for assets and
        conditions asked, or a dict of dataframe if separated is True and
        only one asset is asked.


        """

        if darwinex_time:
            if cond:
                raise ValueError(
                    'With Darwinex time, start and end params must'
                    ' be used, not cond.'
                )
            elif start:
                start = _dw_time_to_utc(start)
                end = _dw_time_to_utc(end)

        if isinstance(assets, list):
            data_dict = {}
            for asset in assets:
                print('\n' + asset)
                data_dict[asset] = self._get_ticks(
                    asset,
                    start=start,
                    end=end,
                    cond=cond,
                    verbose=verbose,
                    side=side,
                    separated=False,
                    fill=fill,
                    darwinex_time=darwinex_time,
                )
            data = _pd.concat(data_dict, keys=data_dict.keys())
        else:
            data = self._get_ticks(
                assets,
                start=start,
                end=end,
                cond=cond,
                verbose=verbose,
                side=side,
                separated=separated,
                fill=fill,
                darwinex_time=darwinex_time,
            )
        return data


# TOOLS


def _isnotebook():
    try:
        shell = get_ipython().__class__.__name__
        if shell == 'ZMQInteractiveShell':
            return True  # Jupyter notebook or qtconsole
        elif shell == 'TerminalInteractiveShell':
            return False  # Terminal running IPython
        else:
            return False  # Other type (?)
    except NameError:
        return False


def _dw_time_to_utc(times):
    times = (
        (_pd.to_datetime(times) - _pd.Timedelta('07:00:00'))
        .tz_localize('America/New_York')
        .tz_convert('UTC')
    )
    return times.strftime('%Y-%m-%d %H')


def to_darwinex_time(data):
    if data.index.is_all_dates:
        data.index = (
            ((data.index.tz_convert('America/New_York')) + _pd.Timedelta('07:00:00'))
            .tz_localize(None)
            .tz_localize('Etc/GMT+2')
        )
    else:
        raise KeyError('Dataframe index must be dates')
    return data


def _index_utc_to_mt_time(serie):
    if serie.is_all_dates:
        serie = (
            ((serie.tz_convert('America/New_York')) + _pd.Timedelta('07:00:00'))
            .tz_localize(None)
            .tz_localize('Etc/GMT+2')
        )
    else:
        raise KeyError('Dataframe index must be dates')
    return serie


def spread(data, ask='Ask', bid='Bid', pip=None):
    """
    Return the spread between ask and bid.
    :param data: pandas dataframe with Ask and Bid columns
    :param ask: str, name of the Ask column
    :param bid: str, name of the Bid column
    :param pip: float, if  pip is passed the spread is in pip units.
    :return: pandas serie with the spread
    """
    if not all([_ in data.columns for _ in [ask, bid]]):
        raise KeyError('Parameters ask and bid must be column names')
    _spreads = data.Ask.values - data.Bid.values
    if pip is not None:
        _spreads = _spreads / pip
    return _pd.Series(_spreads, index=data.index)


def to_mtcsv(data, path=None, decimals=5):
    """
    Save a downloaded ticks dataframe as a csv with a optimized format to be
    imported by MetaTrader.
    :param data: pandas dataframe with Ask and Bid columns
    :param path: file path to save
    :param decimals: number of decimals to save
    :return: str if path is None
    """
    csv = data[['Ask', 'Bid']].to_csv(
        path, header=False, float_format='%.{}f'.format(decimals)
    )
    return csv


def price(
    data,
    method='midpoint',
    ask='Ask',
    bid='Bid',
    ask_size='Ask_size',
    bid_size='Bid_size',
):
    """
    Return the midpoint or the weighted price of ask and bid
    :param data: pandas dataframe with Ask and Bid columns
    :param method: str, 'midpoint', or 'weighted' to use the sizes
    :param ask:  str, name of the columns with the ask
    :param bid: str, name of the columns with the bid
    :param ask_size: str, name of the columns with the ask size
    :param bid_size: str, name of the columns with the bid size
    :return:  pandas serie with price
    """
    if method == 'midpoint':
        _price = (data[ask].values + data[bid].values) / 2
    elif method == 'weighted':
        _price = (
            data[ask].values * data[ask_size].values
            + data[bid].values * data[bid_size].values
        ) / (data[ask_size] + data[bid_size])
    else:
        raise KeyError('Valid param method must be passed')
    return _pd.Series(_price, index=data.index)


PandasObject.spread = spread
PandasObject.to_mtcsv = to_mtcsv
PandasObject.price = price
