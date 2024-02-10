class IterationsPeriodTime:
    OFF = -1
    HALF_HOUR = -10
    HOUR = -11
    TWO_HOURS = -12
    THREE_HOURS = -13
    FOUR_HOURS = -14
    FIVE_HOURS = -15
    SIX_HOURS = -16
    SEVEN_HOURS = -17
    EIGHT_HOURS = -18
    DAILY = -24
    END_OF_SESSION = -25

    @staticmethod
    def is_a_period(period: int) -> bool:
        output = False
        for period_time in IterationsPeriodTime.__dict__.values():
            if period_time == period:
                output = True
                break
        return output
