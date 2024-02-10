# import unittest
#
# from trading_algorithms.reinforcement_learning.rl_algorithm import ReinforcementLearningActionType, BaseModelType
# from zeromq_trading.gym_agent_launcher import GymAgentLauncher
# import os
#
#
# class GymAgentLauncherTest(unittest.TestCase):
#
#     def test_gym_agent_launcher_zero_state(self):
#         test_dir = os.path.abspath(
#             os.path.join(
#                 os.path.dirname(os.path.dirname(os.path.realpath(__file__))),
#             )
#         )
#         model_base_path = test_dir + os.sep + rf"data" + os.sep + rf"rl_gym_model"
#         model_path = model_base_path + os.sep + rf"agent_model.zip"
#         normalizer_model_path = model_base_path + os.sep + rf"normalizer_model.pkl"
#
#         states_number = 82
#         actions_number = 18
#         gym_agent = GymAgentLauncher(rl_host="localhost", rl_port=5555, model_path=model_path,
#                                      normalizer_model_path=normalizer_model_path, base_model_str=BaseModelType.DQN,
#                                      # action_adaptor_path="action_adaptor_path",
#                                      reinforcement_learning_action_type=ReinforcementLearningActionType.discrete)
#         try:
#             gym_agent._load()
#         except Exception as e:
#             print(rf"Exception {e}")
#         gym_agent.model.exploration_rate = 0.0
#         self.assertIsNotNone(gym_agent.model)
#         zeros_input = [0] * states_number
#         action, state = gym_agent.get_action_state(zeros_input)
#         self.assertIsNotNone(action)
#         # self.assertIsNotNone(state)
