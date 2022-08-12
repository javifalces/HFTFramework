//+------------------------------------------------------------------+
//|                                                      Volumes.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Indicator.mqh"
//+------------------------------------------------------------------+
//| Class CiAD.                                                      |
//| Purpose: Class of the "Accumulation/Distribution" indicator.     |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiAD : public CIndicator
  {
protected:
   ENUM_APPLIED_VOLUME m_applied;        // applied volume

public:
                     CiAD(void);
                    ~CiAD(void);
   //--- methods of access to protected data
   ENUM_APPLIED_VOLUME Applied(void) const { return(m_applied); }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_AD); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiAD::CiAD(void) : m_applied(WRONG_VALUE)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiAD::~CiAD(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "Accumulation/Distribution" indicator                 |
//+------------------------------------------------------------------+
bool CiAD::Create(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iAD(symbol,period,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,applied))
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
bool CiAD::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(ENUM_APPLIED_VOLUME)params[0].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with special parameters                 |
//+------------------------------------------------------------------+
bool CiAD::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="AD";
      m_status="("+symbol+","+PeriodDescription()+","+VolumeDescription(applied)+") H="+IntegerToString(m_handle);
      //--- save settings
      m_applied=applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Accumulation/Distribution"                  |
//+------------------------------------------------------------------+
double CiAD::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiMFI.                                                     |
//| Purpose: Class of the "Money Flow Index" indicator.              |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiMFI : public CIndicator
  {
protected:
   int               m_ma_period;
   ENUM_APPLIED_VOLUME m_applied;

public:
                     CiMFI(void);
                    ~CiMFI(void);
   //--- methods of access to protected data
   int               MaPeriod(void)        const { return(m_ma_period); }
   ENUM_APPLIED_VOLUME Applied(void)       const { return(m_applied);   }
   //--- method of creation
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,
                            const int ma_period,const ENUM_APPLIED_VOLUME applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_MFI); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                                const int ma_period,const ENUM_APPLIED_VOLUME applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiMFI::CiMFI(void) : m_ma_period(-1),
                     m_applied(WRONG_VALUE)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiMFI::~CiMFI(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "Money Flow Index" indicator                          |
//+------------------------------------------------------------------+
bool CiMFI::Create(const string symbol,const ENUM_TIMEFRAMES period,
                   const int ma_period,const ENUM_APPLIED_VOLUME applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iMFI(symbol,period,ma_period,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,ma_period,applied))
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
bool CiMFI::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(int)params[0].integer_value,(ENUM_APPLIED_VOLUME)params[1].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with special parameters                 |
//+------------------------------------------------------------------+
bool CiMFI::Initialize(const string symbol,const ENUM_TIMEFRAMES period,
                       const int ma_period,const ENUM_APPLIED_VOLUME applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="MFI";
      m_status="("+symbol+","+PeriodDescription()+","+
               IntegerToString(ma_period)+","+VolumeDescription(applied)+","+") H="+IntegerToString(m_handle);
      //--- save settings
      m_ma_period=ma_period;
      m_applied  =applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Money Flow Index"                           |
//+------------------------------------------------------------------+
double CiMFI::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiOBV.                                                     |
//| Purpose: Class of the "On Balance Volume" indicator.             |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiOBV : public CIndicator
  {
protected:
   ENUM_APPLIED_VOLUME m_applied;

public:
                     CiOBV(void);
                    ~CiOBV(void);
   //--- methods of access to protected data
   ENUM_APPLIED_VOLUME Applied(void) const { return(m_applied); }
   //--- method create
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const  { return(IND_OBV); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiOBV::CiOBV(void) : m_applied(WRONG_VALUE)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiOBV::~CiOBV(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "On Balance Volume" indicator                         |
//+------------------------------------------------------------------+
bool CiOBV::Create(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iOBV(symbol,period,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,applied))
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
bool CiOBV::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(ENUM_APPLIED_VOLUME)params[0].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with special parameters                 |
//+------------------------------------------------------------------+
bool CiOBV::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="OBV";
      m_status="("+symbol+","+PeriodDescription()+","+VolumeDescription(applied)+","+") H="+IntegerToString(m_handle);
      //--- save settings
      m_applied=applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "On Balance Volume"                          |
//+------------------------------------------------------------------+
double CiOBV::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
//| Class CiVolumes.                                                 |
//| Purpose: Class of the "Volumes" indicator.                       |
//|          Derives from class CIndicator.                          |
//+------------------------------------------------------------------+
class CiVolumes : public CIndicator
  {
protected:
   ENUM_APPLIED_VOLUME m_applied;

public:
                     CiVolumes(void);
                    ~CiVolumes(void);
   //--- methods of access to protected data
   ENUM_APPLIED_VOLUME Applied(void) const { return(m_applied); }
   //--- method create
   bool              Create(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied);
   //--- methods of access to indicator data
   double            Main(const int index) const;
   //--- method of identifying
   virtual int       Type(void) const { return(IND_VOLUMES); }

protected:
   //--- methods of tuning
   virtual bool      Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[]);
   bool              Initialize(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CiVolumes::CiVolumes(void) : m_applied(WRONG_VALUE)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CiVolumes::~CiVolumes(void)
  {
  }
//+------------------------------------------------------------------+
//| Create the "Volumes" indicator                                   |
//+------------------------------------------------------------------+
bool CiVolumes::Create(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied)
  {
//--- check history
   if(!SetSymbolPeriod(symbol,period))
      return(false);
//--- create
   m_handle=iVolumes(symbol,period,applied);
//--- check result
   if(m_handle==INVALID_HANDLE)
      return(false);
//--- indicator successfully created
   if(!Initialize(symbol,period,applied))
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
bool CiVolumes::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const int num_params,const MqlParam &params[])
  {
   return(Initialize(symbol,period,(ENUM_APPLIED_VOLUME)params[0].integer_value));
  }
//+------------------------------------------------------------------+
//| Initialize the indicator with special parameters                 |
//+------------------------------------------------------------------+
bool CiVolumes::Initialize(const string symbol,const ENUM_TIMEFRAMES period,const ENUM_APPLIED_VOLUME applied)
  {
   if(CreateBuffers(symbol,period,1))
     {
      //--- string of status of drawing
      m_name  ="Volumes";
      m_status="("+symbol+","+PeriodDescription()+","+VolumeDescription(applied)+","+") H="+IntegerToString(m_handle);
      //--- save settings
      m_applied=applied;
      //--- create buffers
      ((CIndicatorBuffer*)At(0)).Name("MAIN_LINE");
      //--- ok
      return(true);
     }
//--- error
   return(false);
  }
//+------------------------------------------------------------------+
//| Access to buffer of "Volumes"                                    |
//+------------------------------------------------------------------+
double CiVolumes::Main(const int index) const
  {
   CIndicatorBuffer *buffer=At(0);
//--- check
   if(buffer==NULL)
      return(EMPTY_VALUE);
//---
   return(buffer.At(index));
  }
//+------------------------------------------------------------------+
