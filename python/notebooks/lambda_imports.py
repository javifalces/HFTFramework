import warnings

warnings.filterwarnings("ignore", category=DeprecationWarning)
import matplotlib.pyplot as plt

plt.ioff()  # disable interactive plot
# import sys
import seaborn as sns
import pandas as pd
import numpy as np
import time
import datetime
import tqdm
import os
import math
import dill
import joblib
import pickle


sns.set_theme()
import pylab


from database.tick_db import *

from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.parameter_tuning.ga_parameter_tuning import GAParameterTuning

# import plotly.graph_objects as go
from notebooks.email_util import EmailConnector
from configuration import *

from trading_algorithms.iterations_period_time import IterationsPeriodTime
from trading_algorithms.score_enum import ScoreEnum
from trading_algorithms.state_utils import *
from trading_algorithms.candles_type import *
from trading_algorithms.algorithm_enum import *
from utils.pandas_utils.dataframe_utils import *
from utils.date_utils import *
from backtest.pnl_utils import *

from trading_algorithms.algorithm import Algorithm
from trading_algorithms.dqn_algorithm import DQNAlgorithm

from trading_algorithms.market_making.avellaneda_stoikov import AvellanedaStoikov
from trading_algorithms.market_making.alpha_avellaneda_stoikov import (
    AlphaAvellanedaStoikov,
)
from trading_algorithms.market_making.rl4j_alpha_avellaneda_stoikov import (
    Rl4jAlphaAvellanedaStoikov,
)

from trading_algorithms.market_making.constant_spread import ConstantSpread
from trading_algorithms.market_making.linear_constant_spread import LinearConstantSpread
from trading_algorithms.market_making.alpha_constant_spread import AlphaConstantSpread




plt.rcParams['figure.figsize'] = [12, 8]
plt.rcParams['figure.dpi'] = 100  # 200 e.g. is really fine, but slower
# pd.options.plotting.backend ='matplotlib'# "plotly"
pd.set_option('display.max_rows', 50)
pd.set_option('display.max_columns', 50)

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
    EmailConnector().send_email(
        recipient=recipient,
        subject=subject,
        body=body,
        html=html,
        file_append=file_append,
    )


def save_notebook_session(session_name: str):
    print('notebook session file save as  %s' % session_name)
    dill.dump_session(session_name)


def load_notebook_session(session_name: str):
    if os.path.isfile(session_name):
        print('notebook session file found %s' % session_name)
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

# from utils.tensorflow_utils import *
