//+------------------------------------------------------------------+
//|                                                 TrailingNone.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Expert\ExpertTrailing.mqh>
// wizard description start
//+------------------------------------------------------------------+
//| Description of the class                                         |
//| Title=Trailing Stop not used                                     |
//| Type=Trailing                                                    |
//| Name=None                                                        |
//| Class=CTrailingNone                                              |
//| Page=                                                            |
//+------------------------------------------------------------------+
// wizard description end
//+------------------------------------------------------------------+
//| Class CTrailingNone.                                             |
//| Appointment: Class no traling stops.                             |
//|              Derives from class CExpertTrailing.                 |
//+------------------------------------------------------------------+
class CTrailingNone : public CExpertTrailing
  {
public:
                     CTrailingNone(void);
                    ~CTrailingNone(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CTrailingNone::CTrailingNone(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CTrailingNone::~CTrailingNone(void)
  {
  }
//+------------------------------------------------------------------+
