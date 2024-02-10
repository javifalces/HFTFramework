from trading_algorithms.algorithm_enum import AlgorithmEnum, get_algorithm
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
)
import json
from backtest.parameter_tuning.ga_configuration import GAConfiguration

from backtest.pnl_utils import *
from trading_algorithms.score_enum import ScoreEnum, get_score_enum_csv_column

import random
import copy
from backtest.parameter_tuning.ga_optimization_utils import (
    random_param_dict,
    crossover_param_dict,
)


class GAParameterTuning:
    INCREASE_POPULATION = True

    def __init__(
            self,
            ga_configuration: GAConfiguration,
            algorithm: AlgorithmEnum,
            parameters_base: dict,
            parameters_min: dict,
            parameters_max: dict,
            max_simultaneous: int,
            initial_param_dict_list: list = [],
    ):

        self.counter_algorithms = 0
        self.max_simultaneous = max_simultaneous
        self.initial_param_dict_list = initial_param_dict_list
        self.ga_configuration = ga_configuration
        self.algorithm = algorithm
        self.parameters_base_final = copy.copy(parameters_base)
        self.parameters_base = parameters_base

        # remove keys cant be tuned
        self.ignored_keys = []
        self.ignored_dict = {}
        for parameter in self.parameters_base.keys():
            if not isinstance(self.parameters_base[parameter], int) and not isinstance(
                    self.parameters_base[parameter], float
            ):
                self.ignored_keys.append(parameter)
        ignored_keys_str = ','.join(self.ignored_keys)
        print(f'ignoring params to optimize {ignored_keys_str}')
        for ignored in self.ignored_keys:
            self.ignored_dict[ignored] = self.parameters_base[ignored]
            del self.parameters_base[ignored]

        # set from base
        self.parameters_min = copy.copy(parameters_base)
        for key in parameters_min.keys():
            self.parameters_min[key] = parameters_min[key]

        # set from base
        self.parameters_max = copy.copy(parameters_base)
        for key in parameters_max.keys():
            self.parameters_max[key] = parameters_max[key]

        self.max_ser = pd.Series(self.parameters_max)
        self.min_ser = pd.Series(self.parameters_min)
        self.scale_ser = self.max_ser - self.min_ser
        self.sigma = self.ga_configuration.sigma
        self.decay = self.ga_configuration.decay

        self.population_df_all_iterations = {}
        self.generation = 0
        self.population_df = pd.DataFrame(
            columns=['param_dict', 'score', 'pnl', 'sharpe', 'trades']
        )
        self.population_df_out = pd.DataFrame(
            columns=['param_dict', 'score', 'pnl', 'sharpe', 'trades', 'generation']
        )

    def _random_param_dict(
            self, current_eval_ser=None, sigma_eval: float = None
    ) -> dict:
        # randomizer between min and max

        # remove not analized keys
        if sigma_eval is None:
            sigma_eval = self.sigma

        ignored_dict = {}
        if current_eval_ser is not None:
            for ignored in self.ignored_keys:
                if ignored in current_eval_ser.keys():
                    ignored_dict[ignored] = current_eval_ser[ignored]
                    del current_eval_ser[ignored]

        output_dict = random_param_dict(
            sigma=sigma_eval,
            scale_ser=self.scale_ser,
            min_ser=self.min_ser,
            max_ser=self.max_ser,
            current_eval_ser=current_eval_ser,
        )

        # add not analized keys
        if len(ignored_dict) > 0:
            for ignored_dict_keys in ignored_dict.keys():
                output_dict[ignored_dict_keys] = ignored_dict[ignored_dict_keys]
        return output_dict

    def _create_random_population(
            self, population_size: int, sigma_eval: float
    ) -> dict:
        output = {}
        id = 0

        for pop in range(population_size):
            algorithm_class = get_algorithm(self.algorithm)
            random_parameters = self._random_param_dict(sigma_eval=sigma_eval)
            new_algorithm = algorithm_class(
                algorithm_info=str(id), parameters=random_parameters
            )
            output[id] = new_algorithm
            id += 1
        return output

    def _mutate_param_dict(self, parameter_dict: dict):
        current_eval_ser = pd.Series(parameter_dict)
        return self._random_param_dict(current_eval_ser=current_eval_ser)

    def _get_weights(self):
        weights = copy.copy(self.population_df['score'])
        weights += abs(min(weights))  # avoid neg weights
        return weights

    def _get_parents_dict_crossover(self) -> list:
        '''get better parents in function of score'''
        assert len(self.population_df) > 0
        weights = self._get_weights()
        try:
            sample = self.population_df.sample(weights=weights, n=2)
        except:
            sample = self.population_df.sample(n=2)
        output_parents = sample['param_dict'].to_list()
        return copy.copy(output_parents)

    def _crossover(self) -> dict:
        parents_to_cross = self._get_parents_dict_crossover()

        # remove not analized keys
        ignored_dict_0 = {}
        ignored_dict_1 = {}
        for ignored in self.ignored_keys:

            if ignored in parents_to_cross[0].keys():
                ignored_dict_0[ignored] = parents_to_cross[0][ignored]
                del parents_to_cross[0][ignored]

            if ignored in parents_to_cross[1].keys():
                ignored_dict_1[ignored] = parents_to_cross[1][ignored]
                del parents_to_cross[1][ignored]
        param_dict = parents_to_cross[0]
        while param_dict == parents_to_cross[0] or param_dict == parents_to_cross[1]:
            param_dict = crossover_param_dict(parents_to_cross[0], parents_to_cross[1])
        assert param_dict != parents_to_cross[0]
        assert param_dict != parents_to_cross[1]

        # add not analized keys
        if len(ignored_dict_1) > 0:
            for ignored_dict_keys in ignored_dict_1.keys():
                param_dict[ignored_dict_keys] = ignored_dict_1[ignored_dict_keys]

        return param_dict

    def _get_param_dict_mutate(self) -> dict:
        '''get better probabilty in function of score'''
        assert len(self.population_df) > 0
        # another version could be to mutate randomly population not better performance
        # sample = self.population_df.sample(n=1)
        weights = self._get_weights()
        try:
            sample = self.population_df.sample(weights=weights, n=1)
        except:
            sample = self.population_df.sample(n=1)
        output = sample['param_dict'].to_list()[0]
        return copy.copy(output)

    def _mutate(self) -> dict:

        param_to_mutate = self._get_param_dict_mutate()
        output = param_to_mutate
        while param_to_mutate == output:
            output = self._mutate_param_dict(parameter_dict=param_to_mutate)
        return output

    def _elite(self, elite_population: int) -> list:
        assert len(self.population_df) > 0
        population_sorted = self.population_df.sort_values(by='score', ascending=False)
        output = population_sorted['param_dict'].to_list()[: int(elite_population)]
        # if int(elite_population)>1:
        #     output = [
        #         dict(t) for t in {tuple(d.items()) for d in output}
        #     ]  # removing duplicate dicts
        scores = population_sorted['score'].to_list()[: int(elite_population)]
        # removing duplicate dicts

        for param_dict in output:
            score = scores[output.index(param_dict)]
            print('[%.8f score ] ELITE -> %s' % (score, param_dict))
        return output

    def _generate_initial_population(self, check_unique_population: bool) -> list:

        param_dicts = []
        initialized_with_precreated_population = (
                self.generation == 0 and len(self.initial_param_dict_list) > 0
        )
        if initialized_with_precreated_population:
            print('loading initial population')
            param_dicts = [param_dict for param_dict in self.initial_param_dict_list]
            while len(param_dicts) < self.ga_configuration.population:
                random_param = self._random_param_dict()
                if random_param in param_dicts:
                    continue
                print('adding more random param dict to initial population')
                param_dicts.append(self._random_param_dict())

            while len(param_dicts) > self.ga_configuration.population:
                print('removing last param dicts until reach population')
                param_dicts.remove(param_dicts[-1])
        else:
            print(
                'generate all %d population randomly' % self.ga_configuration.population
            )
            max_retries = self.ga_configuration.population * 4
            retries = 0
            sigma_eval = self.sigma
            while len(param_dicts) < (self.ga_configuration.population):
                if sigma_eval == 0:
                    sigma_eval = self.sigma
                if retries > max_retries:
                    print('cant fill all population different!! -> less population')
                    break

                param_dict = self._random_param_dict(sigma_eval=sigma_eval)
                if check_unique_population:
                    if param_dict not in param_dicts:
                        param_dicts.append(param_dict)
                    else:
                        retries += 1
                else:
                    param_dicts.append(param_dict)

        for param_dict in param_dicts:
            print(
                'initial [%d] sigma=%.2f'
                % (self.counter_algorithms + param_dicts.index(param_dict), self.sigma)
                + json.dumps(param_dict, indent=4)
            )

        return param_dicts

    def _generate_next_gen(self, check_unique_population: bool) -> list:

        param_dicts = []  # new offpsring params
        if not self.INCREASE_POPULATION:
            # ELITE population
            elite_population = int(
                np.ceil(
                    self.ga_configuration.population
                    * self.ga_configuration.elite_population_percent
                )
            )
            # counter_elite = elite_population
            # rest_population = self.ga_configuration.population - elite_population
            param_dicts = self._elite(elite_population=elite_population)
            print('best %d elite params passing to next generation' % len(param_dicts))

        retries = 0
        max_retries = 15
        crossover_count = 0
        mutation_count = 0
        elite_population = len(param_dicts)
        while len(param_dicts) < self.ga_configuration.population:
            # fill the rest of population/new population

            number = random.random()  # if number<crossover_prob=> crossover
            if retries > max_retries:
                print('cant fill all population different!! -> less population')
                break
            is_crossover = (
                    number < self.ga_configuration.crossover_prob
                    and len(self.population_df) > 1
            )
            # CROSSOVER
            if is_crossover:
                param_dict = self._crossover()
                if check_unique_population and param_dict in param_dicts:
                    retries += 1
                    continue
                else:
                    crossover_count += 1
            # MUTATION
            else:
                param_dict = self._mutate()
                if check_unique_population and param_dict in param_dicts:
                    retries += 1
                    continue
                else:
                    mutation_count += 1
            ## add new pop
            param_dicts.append(param_dict)
        if not self.INCREASE_POPULATION:
            print(
                "generation %d with new %d mutations and %d crossover"
                % (self.generation, mutation_count, crossover_count)
            )
        else:
            print(
                "generation %d with new %d mutations  %d crossover  %d elite"
                % (self.generation, mutation_count, crossover_count, elite_population)
            )

        for param_dict in param_dicts:
            print(
                '[%d]gen=%d sigma=%.2f'
                % (
                    self.counter_algorithms + param_dicts.index(param_dict),
                    self.generation,
                    self.sigma,
                )
                + json.dumps(param_dict, indent=4)
            )
        return param_dicts

    def run_generation(
            self,
            backtest_configuration: BacktestConfiguration,
            check_unique_population: bool = True,
    ) -> list:
        is_empty_population = (
                len(self.population_df) == 0 or self.population_df['score'].sum() == 0
        )
        if is_empty_population:
            param_dicts = self._generate_initial_population(check_unique_population)
        else:
            param_dicts = self._generate_next_gen(check_unique_population)
        try:
            assert len(param_dicts) == self.ga_configuration.population
        except Exception as e:
            print(
                rf"something was wrong len(param_dicts) {len(param_dicts)}!={self.ga_configuration.population} self.ga_configuration.population"
            )
        self._run_backtests(
            backtest_configuration=backtest_configuration, param_dicts=param_dicts
        )

    def _run_backtests(
            self, backtest_configuration: BacktestConfiguration, param_dicts: list
    ):

        algorithms = []
        for param_dict in param_dicts:
            algorithm_info = '%s_ga_%d' % (self.algorithm, self.counter_algorithms)
            self.counter_algorithms += 1

            algorithm_class = get_algorithm(self.algorithm)
            if algorithm_class is None:
                print(
                    "WARNING: need to add algorithm to backtest.algorithm_enum.get_algorithm() "
                )
                print(
                    "WARNING: need to add algorithm to backtest.algorithm_enum.get_algorithm() "
                )
                print(
                    "WARNING: need to add algorithm to backtest.algorithm_enum.get_algorithm() "
                )
            # restore the base params cant be optimized
            parameters = copy.copy(self.parameters_base_final)
            for param_dict_key in param_dict.keys():
                parameters[param_dict_key] = param_dict[param_dict_key]

            new_algorithm = algorithm_class(
                algorithm_info=algorithm_info, parameters=parameters
            )
            algorithms.append(new_algorithm)
        assert len(algorithms) == len(param_dicts)

        backtest_launchers = []
        for algorithm in algorithms:
            algorithm_configuration = AlgorithmConfiguration(
                algorithm_name=algorithm.algorithm_info, parameters=algorithm.parameters
            )
            input_configuration = InputConfiguration(
                backtest_configuration=backtest_configuration,
                algorithm_configuration=algorithm_configuration,
            )
            backtest_launcher = BacktestLauncher(
                input_configuration=input_configuration,
                id=algorithm.algorithm_info,
                jar_path=JAR_PATH,
            )
            backtest_launchers.append(backtest_launcher)

        backtest_controller = BacktestLauncherController(
            backtest_launchers=backtest_launchers,
            max_simultaneous=self.max_simultaneous,
        )
        output_dict = backtest_controller.run()

        # get all outputs
        param_dicts = []
        scores = []
        pnls = []
        sharpes = []
        tradess = []
        ulcers = []
        sortinos = []
        dds = []
        generations = []
        for algorithm in algorithms:
            algorithm_info = algorithm.algorithm_info
            param_dict = algorithm.parameters
            if algorithm_info not in output_dict or output_dict[algorithm_info] is None:
                score = 0.0
                pnl = 0.0
                trades = 0
                sharpe = -999
                ulcer = -999
                sortino = -999
                dd = 999
            else:
                backtest_df = output_dict[algorithm_info]
                backtest_df = get_backtest_df_date_indexed(backtest_df=backtest_df)

                equity_curve = backtest_df[
                    get_score_enum_csv_column(ScoreEnum.total_pnl)
                ]

                # group by time
                from configuration import SHARPE_BACKTEST_FREQ

                equity_curve = (
                    equity_curve.groupby(pd.Grouper(freq=SHARPE_BACKTEST_FREQ))
                    .last()
                    .fillna(method='ffill')
                )
                try:
                    sharpe = get_sharpe(equity_curve)
                    dd = get_max_drawdown(equity_curve)
                    ulcer = get_ulcer_index(equity_curve)
                    sortino = get_sortino(equity_curve)
                    trades = len(output_dict[algorithm_info])
                    pnl = output_dict[algorithm_info][
                        get_score_enum_csv_column(ScoreEnum.total_pnl)
                    ].iloc[-1]

                    score = get_score(
                        backtest_df=output_dict[algorithm_info],
                        score_enum=self.ga_configuration.score_column,
                        equity_column_score=self.ga_configuration.equity_column_score,
                    )
                except Exception as e:
                    print(
                        rf"Error getting scores on {algorithm_info} -> score=0  {str(e)}"
                    )
                    score = 0.0
                    pnl = 0.0
                    trades = 0
                    sharpe = -999
                    ulcer = -999
                    sortino = -999
                    dd = 999

                # if self.ga_configuration.score_column == ScoreEnum.sharpe:
                #     score = sharpe
                # elif self.ga_configuration.score_column == ScoreEnum.ulcer:
                #     score = ulcer
                # elif self.ga_configuration.score_column == ScoreEnum.max_dd:
                #     score = -dd  # highest drawdown -> worst system
                # elif self.ga_configuration.score_column == ScoreEnum.sortino:
                #     score = sortino
                # elif  self.ga_configuration.score_column == ScoreEnum.falcma_ratio:
                #     score=get_falcma_ratio(equity_curve=equity_curve,number_trades=trades)
                # else:
                #     score = output_dict[algorithm_info][
                #         get_score_enum_csv_column(self.ga_configuration.score_column)
                #     ].iloc[-1]

            param_dicts.append(param_dict)
            scores.append(score)
            pnls.append(pnl)
            dds.append(dd)
            sortinos.append(sortino)
            ulcers.append(ulcer)
            sharpes.append(sharpe)
            tradess.append(trades)
            generations.append(self.generation)

        new_generation_df = pd.DataFrame(
            columns=[
                'param_dict',
                'score',
                'pnl',
                'sharpe',
                'trades',
                'sortino',
                'ulcer',
                'max_dd',
            ]
        )

        new_generation_df['generation'] = generations
        new_generation_df['param_dict'] = param_dicts
        new_generation_df['score'] = scores
        new_generation_df['pnl'] = pnls
        new_generation_df['sharpe'] = sharpes
        new_generation_df['ulcer'] = ulcers
        new_generation_df['sortino'] = sortinos
        new_generation_df['max_dd'] = dds
        new_generation_df['trades'] = tradess

        # filter for the next generation
        self.population_df = new_generation_df

        self.population_df_out = self.population_df_out.append(
            new_generation_df, ignore_index=True
        )

        if self.INCREASE_POPULATION:
            self.population_df = self.population_df_out

        if isinstance(self.decay, float):
            self.sigma -= self.decay

        if isinstance(self.decay, list):
            index_to_take = self.generation
            if index_to_take >= len(self.decay):
                index_to_take = len(self.decay) - 1
            self.sigma = self.ga_configuration.sigma - self.decay[index_to_take]
            self.sigma = max(self.sigma, 0.01)

        self.generation += 1

    def get_param_dict(self, in_param_dict: dict):
        out = copy.copy(in_param_dict)
        for ignored in self.ignored_dict.keys():
            out[ignored] = self.ignored_dict[ignored]
        return out

    def get_best_param_dict(self):
        output = self.population_df_out.sort_values(by='score', ascending=False).iloc[
            0
        ]['param_dict']
        return self.get_param_dict(output)

    def get_best_score(self):
        return self.population_df_out.sort_values(by='score', ascending=False).iloc[0][
            'score'
        ]
