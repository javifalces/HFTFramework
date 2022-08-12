//+------------------------------------------------------------------+
//|                                                     DateTime.mqh |
//|                   Copyright 2009-2015, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Structure CDateTime.                                             |
//| Purpose: Working with dates and time.                            |
//|         Extends the MqlDateTime structure.                       |
//+------------------------------------------------------------------+
struct CDateTime : public MqlDateTime
  {
   //--- additional information
   string            MonthName(const int num) const;
   string            ShortMonthName(const int num) const;
   string            DayName(const int num) const;
   string            ShortDayName(const int num) const;
   string            MonthName(void)               const { return(MonthName(mon));            }
   string            ShortMonthName(void)          const { return(ShortMonthName(mon));       }
   string            DayName(void)                 const { return(DayName(day_of_week));      }
   string            ShortDayName(void)            const { return(ShortDayName(day_of_week)); }
   int               DaysInMonth(void) const;
   //--- data access
   datetime          DateTime(void)                      { return(StructToTime(this));        }
   void              DateTime(const datetime value)      { TimeToStruct(value,this);          }
   void              DateTime(const MqlDateTime& value)  { this=value;                        }
   void              Date(const datetime value);
   void              Date(const MqlDateTime &value);
   void              Time(const datetime value);
   void              Time(const MqlDateTime &value);
   //--- settings
   void              Sec(const int value);
   void              Min(const int value);
   void              Hour(const int value);
   void              Day(const int value);
   void              Mon(const int value);
   void              Year(const int value);
   //--- increments
   void              SecDec(int delta=1);
   void              SecInc(int delta=1);
   void              MinDec(int delta=1);
   void              MinInc(int delta=1);
   void              HourDec(int delta=1);
   void              HourInc(int delta=1);
   void              DayDec(int delta=1);
   void              DayInc(int delta=1);
   void              MonDec(int delta=1);
   void              MonInc(int delta=1);
   void              YearDec(int delta=1);
   void              YearInc(int delta=1);
   //--- check
   void              DayCheck(void);
  };
//+------------------------------------------------------------------+
//| Gets month name                                                  |
//+------------------------------------------------------------------+
string CDateTime::MonthName(const int num) const
  {
   switch(num)
     {
      case  1: return("January");
      case  2: return("February");
      case  3: return("March");
      case  4: return("April");
      case  5: return("May");
      case  6: return("June");
      case  7: return("July");
      case  8: return("August");
      case  9: return("September");
      case 10: return("October");
      case 11: return("November");
      case 12: return("December");
     }
//---
   return("Bad month");
  }
//+------------------------------------------------------------------+
//| Gets short name of month                                         |
//+------------------------------------------------------------------+
string CDateTime::ShortMonthName(const int num) const
  {
   switch(num)
     {
      case  1: return("jan");
      case  2: return("feb");
      case  3: return("mar");
      case  4: return("apr");
      case  5: return("may");
      case  6: return("jun");
      case  7: return("jul");
      case  8: return("aug");
      case  9: return("sep");
      case 10: return("oct");
      case 11: return("nov");
      case 12: return("dec");
     }
//---
   return("Bad month");
  }
//+------------------------------------------------------------------+
//| Gets name of week day                                            |
//+------------------------------------------------------------------+
string CDateTime::DayName(const int num) const
  {
   switch(num)
     {
      case 0: return("Sunday");
      case 1: return("Monday");
      case 2: return("Tuesday");
      case 3: return("Wednesday");
      case 4: return("Thursday");
      case 5: return("Friday");
      case 6: return("Saturday");
     }
//---
   return("Bad day of week");
  }
//+------------------------------------------------------------------+
//| Gets short name of week day                                      |
//+------------------------------------------------------------------+
string CDateTime::ShortDayName(const int num) const
  {
   switch(num)
     {
      case 0: return("Su");
      case 1: return("Mo");
      case 2: return("Tu");
      case 3: return("We");
      case 4: return("Th");
      case 5: return("Fr");
      case 6: return("Sa");
     }
//---
   return("Bad day of week");
  }
//+------------------------------------------------------------------+
//| Gets number of days in month                                     |
//+------------------------------------------------------------------+
int CDateTime::DaysInMonth(void) const
  {
   int leap_year;
//---
   switch(mon)
     {
      case  1:
      case  3:
      case  5:
      case  7:
      case  8:
      case 10:
      case 12:
         return(31);
      case  2:
         leap_year=year;
         if(year%100==0)
            leap_year/=100;
         return((leap_year%4==0)? 29 : 28);
      case  4:
      case  6:
      case  9:
      case 11:
         return(30);
     }
//---
   return(0);
  }
//+------------------------------------------------------------------+
//| Sets date                                                        |
//+------------------------------------------------------------------+
void CDateTime::Date(const datetime value)
  {
   MqlDateTime dt;
//--- convert to structure
   TimeToStruct(value,dt);
//--- set
   Date(dt);
  }
//+------------------------------------------------------------------+
//| Sets date                                                        |
//+------------------------------------------------------------------+
void CDateTime::Date(const MqlDateTime &value)
  {
   day =value.day;
   mon =value.mon;
   year=value.year;
//--- check if day is correct
   DayCheck();
  }
//+------------------------------------------------------------------+
//| Sets time                                                        |
//+------------------------------------------------------------------+
void CDateTime::Time(const datetime value)
  {
   MqlDateTime dt;
//--- convert to structure
   TimeToStruct(value,dt);
//--- set
   Time(dt);
  }
//+------------------------------------------------------------------+
//| Sets time                                                        |
//+------------------------------------------------------------------+
void CDateTime::Time(const MqlDateTime &value)
  {
   hour=value.hour;
   min =value.min;
   sec =value.sec;
  }
//+------------------------------------------------------------------+
//| Sets seconds                                                     |
//+------------------------------------------------------------------+
void CDateTime::Sec(const int value)
  {
//--- check and set
   if(value>=0 && value<60)
      sec=value;
  }
//+------------------------------------------------------------------+
//| Sets minutes                                                     |
//+------------------------------------------------------------------+
void CDateTime::Min(const int value)
  {
//--- check and set
   if(value>=0 && value<60)
      min=value;
  }
//+------------------------------------------------------------------+
//| Sets hours                                                       |
//+------------------------------------------------------------------+
void CDateTime::Hour(const int value)
  {
//--- check and set
   if(value>=0 && value<24)
      hour=value;
  }
//+------------------------------------------------------------------+
//| Sets day of month                                                |
//+------------------------------------------------------------------+
void CDateTime::Day(const int value)
  {
//--- check and set
   if(value>0 && value<=DaysInMonth())
     {
      day=value;
      //--- check if day is correct
      DayCheck();
     }
  }
//+------------------------------------------------------------------+
//| Sets month                                                       |
//+------------------------------------------------------------------+
void CDateTime::Mon(const int value)
  {
//--- check and set
   if(value>0 && value<=12)
     {
      mon=value;
      //--- check if day is correct
      DayCheck();
     }
  }
//+------------------------------------------------------------------+
//| Sets year                                                        |
//+------------------------------------------------------------------+
void CDateTime::Year(const int value)
  {
//--- check and set
   if(value>=1970)
     {
      year=value;
      //--- check if day is correct
      DayCheck();
     }
  }
//+------------------------------------------------------------------+
//| Subtracts specified number of seconds                            |
//+------------------------------------------------------------------+
void CDateTime::SecDec(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      SecInc(-delta);
      return;
     }
//--- check if subtract from upper number positions
   if(delta>60)
     {
      MinDec(delta/60);
      delta%=60;
     }
   sec-=delta;
   if(sec<0)
     {
      sec+=60;
      MinDec();
     }
  }
//+------------------------------------------------------------------+
//| Adds specified number of seconds                                 |
//+------------------------------------------------------------------+
void CDateTime::SecInc(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      SecDec(-delta);
      return;
     }
//--- check if add to upper number positions
   if(delta>60)
     {
      MinInc(delta/60);
      delta%=60;
     }
   sec+=delta;
   if(sec>=60)
     {
      sec-=60;
      MinInc();
     }
  }
//+------------------------------------------------------------------+
//| Subtracts specified number of minutes                            |
//+------------------------------------------------------------------+
void CDateTime::MinDec(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      MinInc(-delta);
      return;
     }
//--- check if subtract from upper number positions
   if(delta>60)
     {
      HourDec(delta/60);
      delta%=60;
     }
   min-=delta;
   if(min<0)
     {
      min+=60;
      HourDec();
     }
  }
//+------------------------------------------------------------------+
//| Adds specified number of minutes                                 |
//+------------------------------------------------------------------+
void CDateTime::MinInc(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      MinDec(-delta);
      return;
     }
//--- check if add to upper number positions
   if(delta>60)
     {
      HourInc(delta/60);
      delta%=60;
     }
   min+=delta;
   if(min>=60)
     {
      min-=60;
      HourInc();
     }
  }
//+------------------------------------------------------------------+
//| Subtracts specified number of hours                              |
//+------------------------------------------------------------------+
void CDateTime::HourDec(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      HourInc(-delta);
      return;
     }
//--- check if subtract from upper number positions
   if(delta>24)
     {
      DayDec(delta/24);
      delta%=24;
     }
   hour-=delta;
   if(hour<0)
     {
      hour+=24;
      DayDec();
     }
  }
//+------------------------------------------------------------------+
//| Adds specified number of hours                                   |
//+------------------------------------------------------------------+
void CDateTime::HourInc(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      HourDec(-delta);
      return;
     }
//--- check if add to upper number positions
   if(delta>24)
     {
      DayInc(delta/24);
      delta%=24;
     }
   hour+=delta;
   if(hour>=24)
     {
      hour-=24;
      DayInc();
     }
  }
//+------------------------------------------------------------------+
//| Subtracts specified number of days                               |
//+------------------------------------------------------------------+
void CDateTime::DayDec(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      DayInc(-delta);
      return;
     }
//--- uncertain condition, as the number of days in month can differ
   while(day<=delta)
     {
      delta-=day;
      MonDec();
      day=DaysInMonth();
     }
   day-=delta;
//--- check if day is correct
   DayCheck();
  }
//+------------------------------------------------------------------+
//| Adds specified number of days                                    |
//+------------------------------------------------------------------+
void CDateTime::DayInc(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      DayDec(-delta);
      return;
     }
//--- uncertain condition, as the number of days in month can differ
   while(DaysInMonth()-day<delta)
     {
      delta-=DaysInMonth()-day+1;
      MonInc();
      day=1;
     }
   day+=delta;
//--- check if day is correct
   DayCheck();
  }
//+------------------------------------------------------------------+
//| Subtracts specified number of months                             |
//+------------------------------------------------------------------+
void CDateTime::MonDec(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      MonInc(-delta);
      return;
     }
//--- check if subtract from upper number positions
   if(delta>12)
     {
      YearDec(delta/12);
      delta%=12;
     }
   mon-=delta;
   if(mon<=0)
     {
      mon+=12;
      YearDec();
     }
//--- check if day is correct
   DayCheck();
  }
//+------------------------------------------------------------------+
//| Adds specified number of months                                  |
//+------------------------------------------------------------------+
void CDateTime::MonInc(int delta)
  {
//--- if increment is 0 - exit
   if(delta==0)
      return;
//--- if increment is negative - inverse the operation
   if(delta<0)
     {
      MonDec(-delta);
      return;
     }
//--- check if add to upper number positions
   if(delta>12)
     {
      YearInc(delta/12);
      delta%=12;
     }
   mon+=delta;
   if(mon>12)
     {
      mon-=12;
      YearInc();
     }
//--- check if day is correct
   DayCheck();
  }
//+------------------------------------------------------------------+
//| Subtracts specified number of years                              |
//+------------------------------------------------------------------+
void CDateTime::YearDec(int delta)
  {
//--- if increment is 0 - exit
   if(delta!=0)
     {
      year-=delta;
      //--- check if day is correct
      DayCheck();
     }
  }
//+------------------------------------------------------------------+
//| Adds specified number of years                                   |
//+------------------------------------------------------------------+
void CDateTime::YearInc(int delta)
  {
//--- if increment is 0 - exit
   if(delta!=0)
     {
      year+=delta;
      //--- check if day is correct
      DayCheck();
     }
  }
//+------------------------------------------------------------------+
//| Checks if day number is correct                                  |
//+------------------------------------------------------------------+
void CDateTime::DayCheck(void)
  {
   if(day>DaysInMonth())
      day=DaysInMonth();
//--- this is required to get day of week and day of year
   TimeToStruct(StructToTime(this),this);
  }
//+------------------------------------------------------------------+
