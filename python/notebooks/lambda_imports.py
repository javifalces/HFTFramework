import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)
import matplotlib.pyplot as plt
plt.ioff()#disable interactive plot
import datetime
import os
# import sys
import numpy as np
import pandas as pd
from pandas.tseries import offsets
import seaborn as sns
sns.set_theme()
from backtest.iterations_period_time import IterationsPeriodTime
import pylab
import statsmodels as sm
from backtest.avellaneda_stoikov import AvellanedaStoikov
from backtest.avellaneda_q import AvellanedaQ
from backtest.avellaneda_dqn import AvellanedaDQN
from backtest.sma_cross import SMACross
from backtest.stat_arb import StatArb
from backtest.linear_constant_spread import LinearConstantSpread
from backtest.constant_spread import ConstantSpread

from backtest.rsi_dqn import RsiDQN
from backtest.rsi import RSI
import database.candle_generation
from backtest.algorithm import *
from backtest.algorithm_enum import *
from backtest.backtest_launcher import *
from backtest.input_configuration import *
from database.tick_db import TickDB
import copy
import mplfinance as mpf
import mlfinlab
from backtest.pnl_utils import get_drawdown, get_max_drawdowns, get_sortino, get_asymetric_dampened_reward, \
    get_pnl_to_map, get_sharpe, get_max_drawdown
from configuration import LAMBDA_INPUT_PATH, LAMBDA_OUTPUT_PATH, ZEROMQ_JAR_PATH, BACKTEST_JAR_PATH
import dill
from backtest.style import Style

from database.tick_db import *
# import plotly.graph_objects as go
from notebooks.email_util import EmailConnector
plt.rcParams['figure.figsize'] = [12, 8]
plt.rcParams['figure.dpi'] = 100 # 200 e.g. is really fine, but slower
from backtest.score_enum import ScoreEnum
# pd.options.plotting.backend ='matplotlib'# "plotly"
pd.set_option('display.max_rows', 50)
pd.set_option('display.max_columns',50)

# pd.set_option('display.width', 1000)
def plot_with_style():
    sns.set_style('darkgrid')
    plt.style.use('ggplot')
    # set up standard plotting style with monospace fonts (needed for nice comparision of statistics on legend)
    # requires sudo apt-get install ttf-dejavu
    # pd.options.display.mpl_style = "default"
    # use monospace so that columns are aligned inside legends
    pylab.rcParams["font.family"] = "monospace"
    if "DejaVu Sans Mono" not in pylab.rcParams["font.monospace"]:
        pylab.rcParams["font.monospace"] += ["DejaVu Sans Mono"]

def send_email(recipient, subject, body, html=None, file_append=[]):
    EmailConnector().send_email(recipient=recipient,subject=subject,body=body,html=html,file_append=file_append)

def save_notebook_session(session_name:str):
    print('notebook session file save as  %s' % session_name)
    dill.dump_session(session_name)

def load_notebook_session(session_name:str):
    if os.path.isfile(session_name):
        print('notebook session file found %s'%session_name)
        dill.load_session(session_name)

def plot_with_dark_style():

    # # plot_with_style()
    # # import jtplot module in notebook
    # from jupyterthemes import jtplot
    #
    # # choose which theme to inherit plotting style from
    # # onedork | grade3 | oceans16 | chesterish | monokai | solarizedl | solarizedd
    # jtplot.style(theme='onedork')
    #
    # # set "context" (paper, notebook, talk, poster)
    # # scale font-size of ticklabels, legend, etc.
    # # remove spines from x and y axes and make grid dashed
    # jtplot.style(context='talk', fscale=1.4, spines=False, gridlines='--')
    #
    # # turn on X- and Y-axis tick marks (default=False)
    # # turn off the axis grid lines (default=True)
    # # and set the default figure size
    # jtplot.style(ticks=True, grid=False, figsize=(6, 4.5))
    #
    # # reset default matplotlib rcParams
    # jtplot.reset()
    from jupyterthemes import jtplot

    # currently installed theme will be used to
    # set plot style if no arguments provided
    jtplot.style()

# plot_with_style()
# plot_with_dark_style()
plt.ion()  # plt show not required
