# https://stackoverflow.com/questions/48990688/pathos-parallel-processing-options-could-someone-explain-the-differences
from concurrent.futures._base import Executor, wait, FIRST_COMPLETED, ALL_COMPLETED

from pathos.pools import ProcessPool as ProcessPool  # multiprocess backend

# from pathos.pools import ThreadPool as ProcessPool  # multiprocess backend
from pathos.parallel import ParallelPool, stats
from pathos.parallel import ParallelPythonPool as ParallelPool

import numpy as np

def get_os():
    import platform

    system = platform.system().lower()
    return system

def process_jobs_(jobs):
    # Run jobs sequentially, for debugging
    out = []
    for job in jobs:
        out_ = expand_call(job)
        out.append(out_)
    return out




def join_df(df0, dataframe_input):
    if len(df0) == 0:
        df0 = dataframe_input
    else:
        i_clean = dataframe_input.replace([np.inf, -np.inf], np.nan)
        i_clean = i_clean.fillna(0)
        i_clean = dataframe_input[
            dataframe_input.columns[(i_clean.sum().astype(int) != 0) == True]
        ]
        df0[i_clean.columns] = dataframe_input[i_clean.columns]
    return df0

def expand_call(kargs):
    # Expand the arguments of a callback function, kargs['func']
    func = kargs["func"]
    del kargs["func"]
    out = func(**kargs)
    return out

def linear_parts(num_atoms, num_threads):
    # partition of atoms with a single loop
    parts = np.linspace(0, num_atoms, min(num_threads, num_atoms) + 1)
    parts = np.ceil(parts).astype(int)
    return parts


def nested_parts(num_atoms, num_threads, upper_triang=False):
    # partition of atoms with an inner loop
    parts, numThreads_ = [0], min(num_threads, num_atoms)
    for num in range(numThreads_):
        part = 1 + 4 * (
                parts[-1] ** 2 + parts[-1] + num_atoms * (num_atoms + 1.0) / numThreads_
        )
        part = (-1 + part**0.5) / 2.0
        parts.append(part)
    parts = np.round(parts).astype(int)
    if upper_triang:  # the first rows are heaviest
        parts = np.cumsum(np.diff(parts)[::-1])
        parts = np.append(np.array([0]), parts)
    return parts


def process_jobs_joblib(jobs, task=None, num_threads=24):
    from joblib import Parallel, delayed

    # joblib
    # outputs = Parallel(n_jobs=numThreads, prefer="threads")(delayed(expandCall)(i) for i in jobs)
    outputs = Parallel(n_jobs=num_threads, prefer="threads")(
        delayed(expand_call)(i) for i in jobs
    )
    out = [x for x in outputs if x is not None]
    return out


def process_jobs_pathos(jobs, task=None, num_threads=24, pool_class=ProcessPool):
    # Run in parallel.
    # jobs must contain a 'func' callback, for expandCall
    if task is None:
        task = jobs[0]["func"].__name__
    # pathos
    pool = pool_class(processes=num_threads, maxtasksperchild=1000)
    return _process_job(pool, jobs)


def _process_job(pool_instance, jobs):
    # logger.debug("process job in pool: %s" % pool_instance)
    if isinstance(pool_instance, ParallelPool):
        outputs = pool_instance.map(expand_call, jobs)
    else:
        outputs = pool_instance.imap(expand_call, jobs)

    out = [x for x in outputs if x is not None]
    # pool.restart()
    pool_instance.close()

    pool_instance.join()  # this is needed to prevent memory leaks

    pool_instance.clear()

    return out