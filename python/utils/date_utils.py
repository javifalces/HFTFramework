import datetime

DATE_FORMAT = '%Y-%m-%d'
FILE_DATE_FORMAT = '%Y%m%d'


def date_to_string(date: datetime.datetime, format=FILE_DATE_FORMAT) -> str:
    return date.strftime(format)


def string_to_date(date_str: str, format=FILE_DATE_FORMAT) -> datetime.datetime:
    return datetime.datetime.strptime(date_str, format)
