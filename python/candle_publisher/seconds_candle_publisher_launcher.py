from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from candle_publisher.time_candle_publisher import TimeCandlePublisher

if __name__ == '__main__':
    # nohup bash seconds_candle_publisher_launcher.sh &>/dev/null &
    import sys

    # launcher = AlgoTradingZeroMqLauncher(
    #     algorithm_settings_path=LAMBDA_INPUT_PATH + os.sep + 'parameters_rsi_dqn_eurusd.json')
    # launcher.run()
    # import time
    # time.sleep(50)
    # launcher.kill()
    sub_host = '192.168.1.70'
    sub_port = 6600
    pub_port = 7700
    replier_port = 7701

    if len(sys.argv) < 3:
        print(rf"ERROR we need at least host and port arguments!!")
        sys.exit()

    sub_host = sys.argv[1]
    sub_port = int(sys.argv[2])

    if len(sys.argv) == 5:
        pub_port = int(sys.argv[3])
        replier_port = int(sys.argv[4])

    num_units = 5
    resolution = "S"

    zeromq_configuration_sub = ZeroMqConfiguration(
        url=sub_host, port=sub_port, topic=''
    )
    zeromq_configuration_pub = ZeroMqConfiguration(url='*', port=pub_port)
    zeromq_configuration_replier = ZeroMqConfiguration(url='*', port=replier_port)
    candle_publisher = TimeCandlePublisher(
        zeromq_configuration_subscriber=zeromq_configuration_sub,
        zeromq_configuration_publisher=zeromq_configuration_pub,
        zeromq_configuration_replier=zeromq_configuration_replier,
        num_units=num_units,
        resolution=resolution,
    )
    candle_publisher.start()
