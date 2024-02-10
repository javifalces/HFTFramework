import datetime

DATE_FORMAT = '%Y-%m-%d'
FILE_DATE_FORMAT = '%Y%m%d'


def date_to_string(date: datetime.datetime, format=FILE_DATE_FORMAT) -> str:
    return date.strftime(format)


def string_to_date(date_str: str, format=FILE_DATE_FORMAT) -> datetime.datetime:
    return datetime.datetime.strptime(date_str, format)


def get_business_date_offset(reference_date, business_days, hour):
    from pandas.tseries.offsets import BDay

    date = reference_date - BDay(business_days)
    return datetime.datetime.fromtimestamp(
        date.timestamp(), tz=datetime.timezone.utc
    ).replace(hour=hour, minute=0, second=0, microsecond=0)
