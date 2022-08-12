//+------------------------------------------------------------------+
//|                                                        Trend.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Indicator.mqh"
//+------------------------------------------------------------------+
//| Class CiADX.                                                     |
//| Purpose: Class of the "Average Directional Index" indicator.     |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiADX : public CIndicator
  {
protected:
   int               m_ma_period;

public:
                     CiADX(void);
                    ~CiADX(void);
   //--- methods of access to protected data
   int               MaPeriod(void) const { return(m_ma_period); }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   double            Plus(const int index) const;
   double            Minus(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_ADX); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiADX::CiADX(void) : m_ma_period(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiADX::~CiADX(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "Average Directional Index" indicator                 |
//+------------------------------------------------------------------+
bool CiADX::Create(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iADX(symbol,period,ma_period);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiADX::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiADX::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period)
  {
   if(CreateBuffers(symbol,period,3))
     {
      //--- string of status of drawing
      m_name  ="ADX";
      m_status="("+symbol+","+PeriodDescription()+","+IntegerToString(ma_period)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(1)).Name("PLUS_LINE");
      ((CIndicatorBuffer*)At(2)).Name("MINUS_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to Main buffer of "Average Directional Index"             |
//+------------------------------------------------------------------+
double CiADX::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Plus buffer of "Average Directional Index"             |
//+------------------------------------------------------------------+
double CiADX::Plus(const int index) const
  {
   CIndicatorBuffer *buffer=At(1);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Minus buffer of "Average Directional Index"            |
//+------------------------------------------------------------------+
double CiADX::Minus(const int index) const
  {
   CIndicatorBuffer *buffer=At(2);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiADXWilder.                                               |
//| Purpose: Class of the "Average Directional Index                 |
//|          by Welles Wilder" indicator.                            |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiADXWilder : public CIndicator
  {
protected:
   int               m_ma_period;

public:
                     CiADXWilder(void);
                    ~CiADXWilder(void);
   //--- methods of access to protected data
   int               MaPeriod(void) const { return(m_ma_period); }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   double            Plus(const int index) const;
   double            Minus(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_ADXW); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiADXWilder::CiADXWilder(void) : m_ma_period(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiADXWilder::~CiADXWilder(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Average Directional Index by Welles Wilder"    |
//+------------------------------------------------------------------+
bool CiADXWilder::Create(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iADXWilder(symbol,period,ma_period);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiADXWilder::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiADXWilder::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int ma_period)
  {
   if(CreateBuffers(symbol,period,3))
     {
      //--- string of status of drawing
      m_name  ="ADXWilder";
      m_status="("+symbol+","+PeriodDescription()+","+IntegerToString(ma_period)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(1)).Name("PLUS_LINE");
      ((CIndicatorBuffer*)At(2)).Name("MINUS_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to Main buffer of "Average Directional Index              |
//|                           by Welles Wilder"                      |
//+------------------------------------------------------------------+
double CiADXWilder::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Plus buffer of "Average Directional Index              |
//|                           by Welles Wilder"                      |
//+------------------------------------------------------------------+
double CiADXWilder::Plus(const int index) const
  {
   CIndicatorBuffer *buffer=At(1);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Minus buffer of "Average Directional Index             |
//|                            by Welles Wilder"                     |
//+------------------------------------------------------------------+
double CiADXWilder::Minus(const int index) const
  {
   CIndicatorBuffer *buffer=At(2);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiBands.                                                   |
//| Purpose: Class of the "Bollinger Bands" indicator.               |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiBands : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ma_shift;
   double            m_deviation;
   int               m_applied;

public:
                     CiBands(void);
                    ~CiBands(void);
   //--- methods of access to protected data
   int               MaPeriod(void)         const { return(m_ma_period); }
   int               MaShift(void)          const { return(m_ma_shift);  }
   double            Deviation(void)        const { return(m_deviation); }
   int               Applied(void)          const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ma_shift,
                            const double deviation,const int applied);
   //--- methods of access to indicator data
   double            Base(const int index) const;
   double            Upper(const int index) const;
   double            Lower(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_BANDS); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ma_shift,
                                const double deviation,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiBands::CiBands(void) : m_ma_period(-1),
                         m_ma_shift(-1),
                         m_deviation(EMPTY_VALUE),
                         m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiBands::~CiBands(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Bollinger Bands"                               |
//+------------------------------------------------------------------+
bool CiBands::Create(const string symbol,const ENUM_TIMEFRAMES period,
                     const int ma_period,const int ma_shift,
                     const double deviation,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iBands(symbol,period,ma_period,ma_shift,deviation,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ma_shift,deviation,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiBands::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,
          params[2].double_value,(int)params[3].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiBands::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                         const int ma_period,const int ma_shift,
                         const double deviation,const int applied)
  {
   if(CreateBuffers(symbol,period,3))
     {
      //--- string of status of drawing
      m_name  ="Bands";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ma_shift)+","+
               DoubleToString(deviation)+","+PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_ma_shift =ma_shift;
      m_deviation=deviation;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("BASE_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ma_shift);
      ((CIndicatorBuffer*)At(1)).Name("UPPER_BAND");
      ((CIndicatorBuffer*)At(1)).Offset(ma_shift);
      ((CIndicatorBuffer*)At(2)).Name("LOWER_BAND");
      ((CIndicatorBuffer*)At(2)).Offset(ma_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to Base buffer of "Bollinger Bands"                       |
//+------------------------------------------------------------------+
double CiBands::Base(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Upper buffer of "Bollinger Bands"                      |
//+------------------------------------------------------------------+
double CiBands::Upper(const int index) const
  {
   CIndicatorBuffer *buffer=At(1);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Lower buffer of "Bollinger Bands"                      |
//+------------------------------------------------------------------+
double CiBands::Lower(const int index) const
  {
   CIndicatorBuffer *buffer=At(2);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiEnvelopes.                                               |
//| Purpose: Class of the "Envelopes" indicator.                     |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiEnvelopes : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ma_shift;
   ENUM_MA_METHOD    m_ma_method;
   int               m_applied;
   double            m_deviation;

public:
                     CiEnvelopes(void);
                    ~CiEnvelopes(void);
   //--- methods of access to protected data
   int               MaPeriod(void)         const { return(m_ma_period);   }
   int               MaShift(void)          const { return(m_ma_shift);    }
   ENUM_MA_METHOD    MaMethod(void)         const { return(m_ma_method);   }
   int               Applied(void)          const { return(m_applied);     }
   double            Deviation(void)        const { return(m_deviation);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ma_shift,const ENUM_MA_METHOD ma_method,
                            const int applied,const double deviation);
   //--- methods of access to indicator data
   double            Upper(const int index) const;
   double            Lower(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_ENVELOPES); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ma_shift,const ENUM_MA_METHOD ma_method,
                                const int applied,const double deviation);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiEnvelopes::CiEnvelopes(void) : m_ma_period(-1),
                                 m_ma_shift(-1),
                                 m_ma_method(WRONG_VALUE),
                                 m_applied(-1),
                                 m_deviation(EMPTY_VALUE)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiEnvelopes::~CiEnvelopes(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Envelopes"                                     |
//+------------------------------------------------------------------+
bool CiEnvelopes::Create(const string symbol,const ENUM_TIMEFRAMES period,
                         const int ma_period,const int ma_shift,const ENUM_MA_METHOD ma_method,
                         const int applied,const double deviation)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iEnvelopes(symbol,period,ma_period,ma_shift,ma_method,applied,deviation);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ma_shift,ma_method,applied,deviation))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiEnvelopes::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,
          (ENUM_MA_METHOD)params[2].integer_value,(int)params[3].integer_value,
          (int)params[4].double_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiEnvelopes::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                             const int ma_period,const int ma_shift,const ENUM_MA_METHOD ma_method,
                             const int applied,const double deviation)
  {
   if(CreateBuffers(symbol,period,2))
     {
      //--- string of status of drawing
      m_name="Envelopes";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ma_shift)+","+
               MethodDescription(ma_method)+","+PriceDescription(applied)+","+
               DoubleToString(deviation)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_ma_shift =ma_shift;
      m_ma_method=ma_method;
      m_applied  =applied;
      m_deviation=deviation;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("UPPER_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ma_shift);
      ((CIndicatorBuffer*)At(1)).Name("LOWER_LINE");
      ((CIndicatorBuffer*)At(1)).Offset(ma_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to Upper buffer of "Envelopes"                            |
//+------------------------------------------------------------------+
double CiEnvelopes::Upper(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to Lower buffer of "Envelopes"                            |
//+------------------------------------------------------------------+
double CiEnvelopes::Lower(const int index) const
  {
   CIndicatorBuffer *buffer=At(1);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiIchimoku.                                                |
//| Purpose: Class of the "Ichimoku Kinko Hyo" indicator.            |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiIchimoku : public CIndicator
  {
protected:
   int               m_tenkan_sen;
   int               m_kijun_sen;
   int               m_senkou_span_b;

public:
                     CiIchimoku(void);
                    ~CiIchimoku(void);
   //--- methods of access to protected data
   int               TenkanSenPeriod(void)        const { return(m_tenkan_sen);    }
   int               KijunSenPeriod(void)         const { return(m_kijun_sen);     }
   int               SenkouSpanBPeriod(void)      const { return(m_senkou_span_b); }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int tenkan_sen,const int kijun_sen,const int senkou_span_b);
   //--- methods of access to indicator data
   double            TenkanSen(const int index) const;
   double            KijunSen(const int index) const;
   double            SenkouSpanA(const int index) const;
   double            SenkouSpanB(const int index) const;
   double            ChinkouSpan(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_ICHIMOKU); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int tenkan_sen,const int kijun_sen,const int senkou_span_b);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiIchimoku::CiIchimoku(void) : m_tenkan_sen(-1),
                               m_kijun_sen(-1),
                               m_senkou_span_b(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiIchimoku::~CiIchimoku(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Ichimoku Kinko Hyo"                            |
//+------------------------------------------------------------------+
bool CiIchimoku::Create(const string symbol,const ENUM_TIMEFRAMES period,
                        const int tenkan_sen,const int kijun_sen,const int senkou_span_b)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iIchimoku(symbol,period,tenkan_sen,kijun_sen,senkou_span_b);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,tenkan_sen,kijun_sen,senkou_span_b))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiIchimoku::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,(int)params[2].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiIchimoku::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                            const int tenkan_sen,const int kijun_sen,const int senkou_span_b)
  {
   if(CreateBuffers(symbol,period,5))
     {
      //--- string of status of drawing
      m_name  ="Ichimoku";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(tenkan_sen)+","+IntegerToString(kijun_sen)+","+
               IntegerToString(senkou_span_b)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_tenkan_sen   =tenkan_sen;
      m_kijun_sen    =kijun_sen;
      m_senkou_span_b=senkou_span_b;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("TENKANSEN_LINE");
      ((CIndicatorBuffer*)At(1)).Name("KIJUNSEN_LINE");
      ((CIndicatorBuffer*)At(2)).Name("SENKOUSPANA_LINE");
      ((CIndicatorBuffer*)At(2)).Offset(kijun_sen);
      ((CIndicatorBuffer*)At(3)).Name("SENKOUSPANB_LINE");
      ((CIndicatorBuffer*)At(3)).Offset(kijun_sen);
      ((CIndicatorBuffer*)At(4)).Name("CHIKOUSPAN_LINE");
      ((CIndicatorBuffer*)At(4)).Offset(-kijun_sen);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to TenkanSen buffer of "Ichimoku Kinko Hyo"               |
//+------------------------------------------------------------------+
double CiIchimoku::TenkanSen(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to KijunSen buffer of "Ichimoku Kinko Hyo"                |
//+------------------------------------------------------------------+
double CiIchimoku::KijunSen(const int index) const
  {
   CIndicatorBuffer *buffer=At(1);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to SenkouSpanA buffer of "Ichimoku Kinko Hyo"             |
//+------------------------------------------------------------------+
double CiIchimoku::SenkouSpanA(const int index) const
  {
   CIndicatorBuffer *buffer=At(2);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to SenkouSpanB buffer of "Ichimoku Kinko Hyo"             |
//+------------------------------------------------------------------+
double CiIchimoku::SenkouSpanB(const int index) const
  {
   CIndicatorBuffer *buffer=At(3);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Access to ChikouSpan buffer of "Ichimoku Kinko Hyo"              |
//+------------------------------------------------------------------+
double CiIchimoku::ChinkouSpan(const int index) const
  {
   CIndicatorBuffer *buffer=At(4);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiMA.                                                      |
//| Purpose: Class of the "Moving Average" indicator.                |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiMA : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ma_shift;
   ENUM_MA_METHOD    m_ma_method;
   int               m_applied;

public:
                     CiMA(void);
                    ~CiMA(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period); }
   int               MaShift(void)         const { return(m_ma_shift);  }
   ENUM_MA_METHOD    MaMethod(void)        const { return(m_ma_method); }
   int               Applied(void)         const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ma_shift,
                            const ENUM_MA_METHOD ma_method,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_MA); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ma_shift,
                                const ENUM_MA_METHOD ma_method,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiMA::CiMA(void) : m_ma_period(-1),
                   m_ma_shift(-1),
                   m_ma_method(WRONG_VALUE),
                   m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiMA::~CiMA(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Moving Average"                                |
//+------------------------------------------------------------------+
bool CiMA::Create(const string symbol,const ENUM_TIMEFRAMES period,
                  const int ma_period,const int ma_shift,
                  const ENUM_MA_METHOD ma_method,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iMA(symbol,period,ma_period,ma_shift,ma_method,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ma_shift,ma_method,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,
          (ENUM_MA_METHOD)params[2].integer_value,(int)params[3].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                      const int ma_period,const int ma_shift,
                      const ENUM_MA_METHOD ma_method,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="MA";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ma_shift)+","+
               MethodDescription(ma_method)+","+PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_ma_shift =ma_shift;
      m_ma_method=ma_method;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ma_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Moving Average"                             |
//+------------------------------------------------------------------+
double CiMA::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiSAR.                                                     |
//| Purpose: Class of the "Parabolic Stop And Reverse System"        |
//|          indicator.                                              |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiSAR : public CIndicator
  {
protected:
   double            m_step;
   double            m_maximum;

public:
                     CiSAR(void);
                    ~CiSAR(void);
   //--- methods of access to protected data
   double            SarStep(void)         const { return(m_step);    }
   double            Maximum(void)         const { return(m_maximum); }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,const double step,const double maximum);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_SAR); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,const double step,const double maximum);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiSAR::CiSAR(void) : m_step(EMPTY_VALUE),
                     m_maximum(EMPTY_VALUE)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiSAR::~CiSAR(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Parabolic Stop And Reverse System"             |
//+------------------------------------------------------------------+
bool CiSAR::Create(const string symbol,const ENUM_TIMEFRAMES period,const double step,const double maximum)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iSAR(symbol,period,step,maximum);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,step,maximum))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiSAR::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,params[0].double_value,params[1].double_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiSAR::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const double step,const double maximum)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="SAR";
      m_status="("+symbol+","+PeriodDescription()+","+
               DoubleToString(step,4)+","+DoubleToString(maximum,4)+","+") H="+IntegerToString(m_handle);
      //--- save settings
      m_step   =step;
      m_maximum=maximum;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Parabolic Stop And Reverse System"          |
//+------------------------------------------------------------------+
double CiSAR::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiStdDev.                                                  |
//| Purpose: Class indicator "Standard Deviation".                   |
//|              Derives from class CIndicator.                      |
//+------------------------------------------------------------------+
class CiStdDev : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ma_shift;
   ENUM_MA_METHOD    m_ma_method;
   int               m_applied;

public:
                     CiStdDev(void);
                    ~CiStdDev(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period); }
   int               MaShift(void)         const { return(m_ma_shift);  }
   ENUM_MA_METHOD    MaMethod(void)        const { return(m_ma_method); }
   int               Applied(void)         const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ma_shift,
                            const ENUM_MA_METHOD ma_method,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_STDDEV); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ma_shift,
                                const ENUM_MA_METHOD ma_method,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiStdDev::CiStdDev(void) : m_ma_period(-1),
                           m_ma_shift(-1),
                           m_ma_method(WRONG_VALUE),
                           m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiStdDev::~CiStdDev(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Standard Deviation"                            |
//+------------------------------------------------------------------+
bool CiStdDev::Create(const string symbol,const ENUM_TIMEFRAMES period,
                      const int ma_period,const int ma_shift,
                      const ENUM_MA_METHOD ma_method,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iStdDev(symbol,period,ma_period,ma_shift,ma_method,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ma_shift,ma_method,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiStdDev::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,
          (ENUM_MA_METHOD)params[2].integer_value,(int)params[3].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiStdDev::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                          const int ma_period,const int ma_shift,
                          const ENUM_MA_METHOD ma_method,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="StdDev";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ma_shift)+","+
               MethodDescription(ma_method)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_ma_shift =ma_shift;
      m_ma_method=ma_method;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ma_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Standard Deviation"                         |
//+------------------------------------------------------------------+
double CiStdDev::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiDEMA.                                                    |
//| Purpose: Class indicator "Double Exponential Moving Average".    |
//|              Derives from class CIndicator.                      |
//+------------------------------------------------------------------+
class CiDEMA : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ind_shift;
   int               m_applied;

public:
                     CiDEMA(void);
                    ~CiDEMA(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period); }
   int               IndShift(void)        const { return(m_ind_shift); }
   int               Applied(void)         const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ind_shift,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_DEMA); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ind_shift,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiDEMA::CiDEMA(void) : m_ma_period(-1),
                       m_ind_shift(-1),
                       m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiDEMA::~CiDEMA(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Double Exponential Moving Average"             |
//+------------------------------------------------------------------+
bool CiDEMA::Create(const string symbol,const ENUM_TIMEFRAMES period,
                    const int ma_period,const int ind_shift,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iDEMA(symbol,period,ma_period,ind_shift,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ind_shift,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiDEMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,(int)params[2].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiDEMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                        const int ma_period,const int ind_shift,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="DEMA";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ind_shift)+","+
               PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_ind_shift=ind_shift;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ind_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Double Exponential Moving Average"          |
//+------------------------------------------------------------------+
double CiDEMA::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiTEMA.                                                    |
//| Purpose: Class indicator "Triple Exponential Moving Average".    |
//|              Derives from class CIndicator.                      |
//+------------------------------------------------------------------+
class CiTEMA : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ind_shift;
   int               m_applied;

public:
                     CiTEMA(void);
                    ~CiTEMA(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period); }
   int               IndShift(void)        const { return(m_ind_shift); }
   int               Applied(void)         const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ma_shift,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_TEMA); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ma_shift,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiTEMA::CiTEMA(void) : m_ma_period(-1),
                       m_ind_shift(-1),
                       m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiTEMA::~CiTEMA(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Triple Exponential Moving Average"             |
//+------------------------------------------------------------------+
bool CiTEMA::Create(const string symbol,const ENUM_TIMEFRAMES period,
                    const int ma_period,const int ind_shift,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iTEMA(symbol,period,ma_period,ind_shift,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ind_shift,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiTEMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,(int)params[2].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiTEMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                        const int ma_period,const int ind_shift,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="TEMA";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ind_shift)+","+
               PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_ind_shift=ind_shift;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ind_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Triple Exponential Moving Average"          |
//+------------------------------------------------------------------+
double CiTEMA::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiFrAMA.                                                   |
//| Purpose: Class indicator "Fractal Adaptive Moving Average".      |
//|              Derives from class CIndicator.                      |
//+------------------------------------------------------------------+
class CiFrAMA : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_ind_shift;
   int               m_applied;

public:
                     CiFrAMA(void);
                    ~CiFrAMA(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period); }
   int               IndShift(void)        const { return(m_ind_shift); }
   int               Applied(void)         const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int ind_shift,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_FRAMA); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int ind_shift,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiFrAMA::CiFrAMA(void) : m_ma_period(-1),
                         m_ind_shift(-1),
                         m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiFrAMA::~CiFrAMA(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Fractal Adaptive Moving Average"               |
//+------------------------------------------------------------------+
bool CiFrAMA::Create(const string symbol,const ENUM_TIMEFRAMES period,
                     const int ma_period,const int ind_shift,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iFrAMA(symbol,period,ma_period,ind_shift,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,ind_shift,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiFrAMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,(int)params[2].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiFrAMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                         const int ma_period,const int ind_shift,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="FrAMA";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(ind_shift)+","+
               PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ind_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Fractal Adaptive Moving Average"            |
//+------------------------------------------------------------------+
double CiFrAMA::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiAMA.                                                     |
//| Purpose: Class indicator "Adaptive Moving Average".              |
//|              Derives from class CIndicator.                      |
//+------------------------------------------------------------------+
class CiAMA : public CIndicator
  {
protected:
   int               m_ma_period;
   int               m_fast_ema_period;
   int               m_slow_ema_period;
   int               m_ind_shift;
   int               m_applied;

public:
                     CiAMA(void);
                    ~CiAMA(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period);       }
   int               FastEmaPeriod(void)   const { return(m_fast_ema_period); }
   int               SlowEmaPeriod(void)   const { return(m_slow_ema_period); }
   int               IndShift(void)        const { return(m_ind_shift);       }
   int               Applied(void)         const { return(m_applied);         }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const int fast_ema_period,const int slow_ema_period,
                            const int ind_shift,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_AMA); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const int fast_ema_period,const int slow_ema_period,
                                const int ind_shift,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiAMA::CiAMA(void) : m_ma_period(-1),
                     m_fast_ema_period(-1),
                     m_slow_ema_period(-1),
                     m_ind_shift(-1),
                     m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiAMA::~CiAMA(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Adaptive Moving Average"                       |
//+------------------------------------------------------------------+
bool CiAMA::Create(const string symbol,const ENUM_TIMEFRAMES period,
                   const int ma_period,const int fast_ema_period,const int slow_ema_period,
                   const int ind_shift,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iAMA(symbol,period,ma_period,fast_ema_period,slow_ema_period,ind_shift,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,fast_ema_period,slow_ema_period,ind_shift,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiAMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,
          (int)params[2].integer_value,(int)params[3].integer_value,
          (int)params[4].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiAMA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                       const int ma_period,const int fast_ema_period,const int slow_ema_period,
                       const int ind_shift,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="AMA";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+IntegerToString(fast_ema_period)+","+IntegerToString(slow_ema_period)+","+
               IntegerToString(ind_shift)+","+PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period      =ma_period;
      m_fast_ema_period=fast_ema_period;
      m_slow_ema_period=slow_ema_period;
      m_ind_shift      =ind_shift;
      m_applied        =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ind_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Adaptive Moving Average"                    |
//+------------------------------------------------------------------+
double CiAMA::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiVIDyA.                                                   |
//| Purpose: Class indicator "Variable Index DYnamic Average".       |
//|              Derives from class CIndicator.                      |
//+------------------------------------------------------------------+
class CiVIDyA : public CIndicator
  {
protected:
   int               m_cmo_period;
   int               m_ema_period;
   int               m_ind_shift;
   int               m_applied;

public:
                     CiVIDyA(void);
                    ~CiVIDyA(void);
   //--- methods of access to protected data
   int               CmoPeriod(void)       const { return(m_cmo_period); }
   int               EmaPeriod(void)       const { return(m_ema_period); }
   int               IndShift(void)        const { return(m_ind_shift);  }
   int               Applied(void)         const { return(m_applied);    }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int cmo_period,const int ema_period,
                            const int ind_shift,const int applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_VIDYA); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int cmo_period,const int ema_period,
                                const int ind_shift,const int applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiVIDyA::CiVIDyA(void) : m_cmo_period(-1),
                         m_ema_period(-1),
                         m_ind_shift(-1),
                         m_applied(-1)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiVIDyA::~CiVIDyA(void)
  {
  }
//+------------------------------------------------------------------+
//| Create indicator "Variable Index DYnamic Average"                |
//+------------------------------------------------------------------+
bool CiVIDyA::Create(const string symbol,const ENUM_TIMEFRAMES period,
                     const int cmo_period,const int ema_period,
                     const int ind_shift,const int applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iVIDyA(symbol,period,cmo_period,ema_period,ind_shift,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,cmo_period,ema_period,ind_shift,applied))
     {
      //--- initialization failed
      IndicatorRelease(m_handle);
      m_handle=INVALID_HANDLE;
      return(false);
     }
//--- ok
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with universal parameters               |
//+------------------------------------------------------------------+
bool CiVIDyA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
//---
   return(Initialize(symbol,period,(int)params[0].integer_value,(int)params[1].integer_value,
          (int)params[2].integer_value,(int)params[3].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize indicator with the special parameters                 |
//+------------------------------------------------------------------+
bool CiVIDyA::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                         const int cmo_period,const int ema_period,
                         const int ind_shift,const int applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="VIDyA";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(cmo_period)+","+IntegerToString(ema_period)+","+IntegerToString(ind_shift)+","+
               PriceDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_cmo_period=cmo_period;
      m_ema_period=ema_period;
      m_ind_shift =ind_shift;
      m_applied   =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      ((CIndicatorBuffer*)At(0)).Offset(ind_shift);
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Variable Index DYnamic Average"             |
//+------------------------------------------------------------------+
double CiVIDyA::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
