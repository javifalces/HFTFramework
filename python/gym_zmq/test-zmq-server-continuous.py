import tensorflow as tf
import onnx
import datetime
import time
from typing import Tuple

import gymnasium

import numpy as np
from gymnasium.core import ObsType

from gym_zmq.envs import ZmqEnv
from gym_zmq.zmq_env_manager import ZmqEnvManager
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from utils.list_utils import list_value

if __name__ == '__main__':
    import tensorflow as tf
    import onnx

    state_columns = 82
    action_columns = 7
    port = 3000
    # min value value
    low = list_value(
        value=float('-inf'), size=state_columns
    )  # [-1.0, -1.0, -1.0, -1.0]
    high = list_value(value=float('inf'), size=state_columns)  # [1.0, 1.0, 1.0, 1.0]
    observation_space = gymnasium.spaces.Box(
        np.array(low), np.array(high), dtype=np.float32
    )

    # action_space = gymnasium.spaces.Discrete(action_columns)
    low = list_value(
        value=float('-inf'), size=action_columns
    )  # [-1.0, -1.0, -1.0, -1.0]
    high = list_value(value=float('inf'), size=action_columns)  # [1.0, 1.0, 1.0, 1.0]
    action_space = gymnasium.spaces.Box(np.array(low), np.array(high), dtype=np.float32)
    number_of_actions = action_columns
    env_config = {
        'observation_space': observation_space,
        'action_space': action_space,
        'port': port,
    }

    env = ZmqEnv(env_config=env_config)

    obs_space = env.observation_space
    action_space = env.action_space
    print("The observation space: {}".format(obs_space))
    print("The action space: {}".format(action_space))
    policy_network, optimizer, loss_fn = None, None, None

    episode_rewards = []
    episode_lengths = []

    num_episodes = 3
    discount_factor = 0.99
    # number_of_actions = action_space.n
    # Train the agent using the REINFORCE algorithm

    # random_action_probs = np.ones(number_of_actions) / number_of_actions
    # if np.sum(random_action_probs) != 1:
    #     random_action_probs = random_action_probs / np.sum(random_action_probs)

    state, info = env.reset()  # send the start signal
    action_columns = info['action']
    for episode in range(num_episodes):
        # Reset the environment and get the initial state

        episode_reward = 0
        episode_length = 0
        # Keep track of the states, actions, and rewards for each step in the episode
        states = []
        actions = []
        rewards = []
        # Run the episode
        last_episode_sent_time = datetime.datetime.utcnow()
        while True:
            # Get the action probabilities from the policy network
            # random

            action = []
            try:
                action = action_space.sample()
            except Exception as e:
                print(rf"action_space.sample() failed with {e}")

            # action = [action]
            # Take the chosen action and observe the next state and reward
            send_time = datetime.datetime.utcnow()
            time_diff = send_time - last_episode_sent_time
            print(
                rf"{episode_length} step action {action} in {time_diff.total_seconds()} seconds"
            )
            last_episode_sent_time = send_time

            next_state, reward, done, truncated, _ = env.step(action)

            # Store the current state, action, and reward
            states.append(state)
            actions.append(action)
            rewards.append(reward)

            # Update the current state and episode reward
            state = next_state
            episode_reward += reward
            episode_length += 1

            # End the episode if the environment is done
            if done:
                print('Episode {} done'.format(episode))
                break
        # Calculate the discounted rewards for each step in the episode
        discounted_rewards = np.zeros_like(rewards)
        running_total = 0
        for i in reversed(range(len(rewards))):
            running_total = running_total * discount_factor + rewards[i]
            discounted_rewards[i] = running_total

        # Normalize the discounted rewards
        discounted_rewards -= np.mean(discounted_rewards)
        discounted_rewards /= np.std(discounted_rewards)
        # Convert the lists of states, actions, and discounted rewards to tensors
        states = tf.convert_to_tensor(states)
        actions = tf.convert_to_tensor(actions)
        discounted_rewards = tf.convert_to_tensor(discounted_rewards)

        # Train the policy network using the REINFORCE algorithm
        # with tf.GradientTape() as tape:
        #     # Get the action probabilities from the policy network
        #     action_probs = policy_network(states)
        #     # Calculate the loss
        #     loss = tf.cast(tf.math.log(tf.gather(action_probs, actions, axis=1, batch_dims=1)), tf.float64)
        #
        #     loss = loss * discounted_rewards
        #     loss = -tf.reduce_sum(loss)

        # Calculate the gradients and update the policy network
        # grads = tape.gradient(loss, policy_network.trainable_variables)
        # optimizer.apply_gradients(zip(grads, policy_network.trainable_variables))

        # Store the episode reward and length
        episode_rewards.append(episode_reward)
        episode_lengths.append(episode_length)

        # policy_network.save('keras_test/')
        # keras policy network to onnx model

        # onnx_model = keras2onnx.convert_keras(policy_network, policy_network.name, target_opset=8)
        # onnx.save_model(onnx_model, "keras_test/keras_test.onnx")
        # wait until next reset
        time.sleep(10)
        # java is waiting for reset signal
        state, action_columns = env.reset()  # restart the backtest process
