//+------------------------------------------------------------------+
//|                                                    SignalITF.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertSignal.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Signals of intraday time filter                            |
//| Type=SignalAdvanced                                              |
//| Name=IntradayTimeFilter                                          |
//| ShortName=ITF                                                    |
//| Class=CSignalITF                                                 |
//| Page=signal_time_filter                                          |
//| Parameter=GoodHourOfDay,int,-1,Good hour                         |
//| Parameter=BadHoursOfDay,int,0,Bad hours (bit-map)                |
//| Parameter=GoodDayOfWeek,int,-1,Good day of week                  |
//| Parameter=BadDaysOfWeek,int,0,Bad days of week (bit-map)         |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CSignalITF.                                                |
//| Appointment: Class trading signals time filter.                  |
//|              Derives from class CExpertSignal.                   |
//+------------------------------------------------------------------+
class CSignalITF : public CExpertSignal
  {
protected:
   //--- input parameters
   int               m_good_minute_of_hour;
   long              m_bad_minutes_of_hour;
   int               m_good_hour_of_day;
   int               m_bad_hours_of_day;
   int               m_good_day_of_week;
   int               m_bad_days_of_week;

public:
                     CSignalITF(void);
                    ~CSignalITF(void);
   //--- methods initialize protected data
   void              GoodMinuteOfHour(int value)  { m_good_minute_of_hour=value; }
   void              BadMinutesOfHour(long value) { m_bad_minutes_of_hour=value; }
   void              GoodHourOfDay(int value)     { m_good_hour_of_day=value;    }
   void              BadHoursOfDay(int value)     { m_bad_hours_of_day=value;    }
   void              GoodDayOfWeek(int value)     { m_good_day_of_week=value;    }
   void              BadDaysOfWeek(int value)     { m_bad_days_of_week=value;    }
   //--- methods of checking conditions of entering the market
   virtual double    Direction(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CSignalITF::CSignalITF(void) : m_good_minute_of_hour(-1),
                               m_bad_minutes_of_hour(0),
                               m_good_hour_of_day(-1),
                               m_bad_hours_of_day(0),
                               m_good_day_of_week(-1),
                               m_bad_days_of_week(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CSignalITF::~CSignalITF(void)
  {
  }
//+------------------------------------------------------------------+
//| Check conditions for time filter.                                |
//+------------------------------------------------------------------+
double CSignalITF::Direction(void)
  {
   MqlDateTime s_time;
//---
   TimeCurrent(s_time);
//--- check days conditions
   if(!((m_good_day_of_week==-1 || m_good_day_of_week==s_time.day_of_week) && 
      !(m_bad_days_of_week&(1<<s_time.day_of_week))))
      return(EMPTY_VALUE);
//--- check hours conditions
   if(!((m_good_hour_of_day==-1 || m_good_hour_of_day==s_time.hour) && 
      !(m_bad_hours_of_day&(1<<s_time.hour))))
      return(EMPTY_VALUE);
//--- check minutes conditions
   if(!((m_good_minute_of_hour==-1 || m_good_minute_of_hour==s_time.min) && 
      !(m_bad_minutes_of_hour&(1<<s_time.min))))
      return(EMPTY_VALUE);
//--- condition OK
   return(0.0);
  }
//+------------------------------------------------------------------+
