//+------------------------------------------------------------------+
//|                                           ChartObjectsArrows.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All arrows.                                                      |
//+------------------------------------------------------------------+
#include "ChartObject.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectArrow.                                         |
//| Purpose: Class of the "Arrow" object of chart.                   |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectArrow : public CChartObject
  {
public:
                     CChartObjectArrow(void);
                    ~CChartObjectArrow(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price,const char code);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_ARROW); }
   //--- methods of access to properties of the object
   char              ArrowCode(void) const;
   virtual bool      ArrowCode(const char code) const;
   ENUM_ARROW_ANCHOR Anchor(void) const;
   virtual bool      Anchor(const ENUM_ARROW_ANCHOR anchor) const;
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrow::CChartObjectArrow(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrow::~CChartObjectArrow(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Arrow"                                            |
//+------------------------------------------------------------------+
bool CChartObjectArrow::Create(long chart_id,const string name,const int window,const datetime time,const double price,const char code)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
   if(!ArrowCode(code))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get code of "arrow" symbol                                       |
//+------------------------------------------------------------------+
char CChartObjectArrow::ArrowCode(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((char)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ARROWCODE));
  }
//+------------------------------------------------------------------+
//| Set code of "arrow" symbol                                       |
//+------------------------------------------------------------------+
bool CChartObjectArrow::ArrowCode(const char code) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_ARROWCODE,code));
  }
//+------------------------------------------------------------------+
//| Get anchor type                                                  |
//+------------------------------------------------------------------+
ENUM_ARROW_ANCHOR CChartObjectArrow::Anchor(void) const
  {
//--- result
   return((ENUM_ARROW_ANCHOR)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ANCHOR));
  }
//+------------------------------------------------------------------+
//| Set anchor type                                                  |
//+------------------------------------------------------------------+
bool CChartObjectArrow::Anchor(const ENUM_ARROW_ANCHOR anchor) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_ANCHOR,anchor));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectArrow::Save(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- writing
   if(!CObject::Save(file_handle))
      return(false);
//--- write code of "arrow" symbol
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ARROWCODE),CHAR_VALUE)!=sizeof(char))
      return(false);
//--- write anchor type
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_ANCHOR),INT_VALUE)!=sizeof(int))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectArrow::Load(const int file_handle)
  {
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- reading
   if(!CObject::Load(file_handle))
      return(false);
//--- read code of "arrow" symbol
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_ARROWCODE,FileReadInteger(file_handle,CHAR_VALUE)))
      return(false);
//--- read anchor type
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_ANCHOR,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowThumbUp.                                  |
//| Purpose: Class of the "Thumbs Up" object of chart.               |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowThumbUp : public CChartObjectArrow
  {
public:
                     CChartObjectArrowThumbUp(void);
                    ~CChartObjectArrowThumbUp(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_THUMB_UP); }
   //--- change of arrow code is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowThumbUp::CChartObjectArrowThumbUp(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowThumbUp::~CChartObjectArrowThumbUp(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Thumbs Up"                                        |
//+------------------------------------------------------------------+
bool CChartObjectArrowThumbUp::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_THUMB_UP,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowThumbDown.                                |
//| Purpose: Class of the "Thumbs Down" object of chart.             |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowThumbDown : public CChartObjectArrow
  {
public:
                     CChartObjectArrowThumbDown(void);
                    ~CChartObjectArrowThumbDown(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_THUMB_DOWN); }
   //--- change of arrow code is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowThumbDown::CChartObjectArrowThumbDown(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowThumbDown::~CChartObjectArrowThumbDown(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "ThumbsDown"                                       |
//+------------------------------------------------------------------+
bool CChartObjectArrowThumbDown::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_THUMB_DOWN,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowUp.                                       |
//| Purpose: Class of the "Arrow Up" object of chart.                |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowUp : public CChartObjectArrow
  {
public:
                     CChartObjectArrowUp(void);
                    ~CChartObjectArrowUp(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_UP); }
   //--- change of arrow code is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowUp::CChartObjectArrowUp(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowUp::~CChartObjectArrowUp(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Arrow Up"                                         |
//+------------------------------------------------------------------+
bool CChartObjectArrowUp::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_UP,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowDown.                                     |
//| Purpose: Class of the "Arrow Down" object of chart.              |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowDown : public CChartObjectArrow
  {
public:
                     CChartObjectArrowDown(void);
                    ~CChartObjectArrowDown(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_DOWN); }
   //--- change of arrow code is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowDown::CChartObjectArrowDown(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowDown::~CChartObjectArrowDown(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Arrow Down"                                       |
//+------------------------------------------------------------------+
bool CChartObjectArrowDown::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_DOWN,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowStop.                                     |
//| Purpose: Class of the "Stop Sign" object of chart.               |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowStop : public CChartObjectArrow
  {
public:
                     CChartObjectArrowStop(void);
                    ~CChartObjectArrowStop(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_STOP); }
   //--- change of arrow code is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowStop::CChartObjectArrowStop(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowStop::~CChartObjectArrowStop(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Stop Sign"                                        |
//+------------------------------------------------------------------+
bool CChartObjectArrowStop::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_STOP,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowCheck.                                    |
//| Purpose: Class of the "Check Sign" object of chart.              |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowCheck : public CChartObjectArrow
  {
public:
                     CChartObjectArrowCheck(void);
                    ~CChartObjectArrowCheck(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_CHECK); }
   //--- change of arrow code is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowCheck::CChartObjectArrowCheck(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowCheck::~CChartObjectArrowCheck(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Check Sign"                                       |
//+------------------------------------------------------------------+
bool CChartObjectArrowCheck::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_CHECK,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowLeftPrice.                                |
//| Purpose: Class of the "Left Price Label" object of chart.        |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowLeftPrice : public CChartObjectArrow
  {
public:
                     CChartObjectArrowLeftPrice(void);
                    ~CChartObjectArrowLeftPrice(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_LEFT_PRICE); }
   //--- change of arrow code and anchor point is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
   virtual bool      Anchor(const ENUM_ARROW_ANCHOR anchor) const override { return(false); }
   ENUM_ARROW_ANCHOR Anchor(void) const { return(CChartObjectArrow::Anchor()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowLeftPrice::CChartObjectArrowLeftPrice(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowLeftPrice::~CChartObjectArrowLeftPrice(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Left Price Label"                                 |
//+------------------------------------------------------------------+
bool CChartObjectArrowLeftPrice::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_LEFT_PRICE,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectArrowRightPrice.                               |
//| Purpose: Class of the "Right Price Label" object of chart.       |
//|          Derives from class CChartObjectArrow.                   |
//+------------------------------------------------------------------+
class CChartObjectArrowRightPrice : public CChartObjectArrow
  {
public:
                     CChartObjectArrowRightPrice(void);
                    ~CChartObjectArrowRightPrice(void);
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const override { return(OBJ_ARROW_RIGHT_PRICE); }
   //--- change of arrow code and anchor point is blocked
   virtual bool      ArrowCode(const char code) const override { return(false); }
   char              ArrowCode(void) const { return(CChartObjectArrow::ArrowCode()); }
   virtual bool      Anchor(const ENUM_ARROW_ANCHOR anchor) const override { return(false); }
   ENUM_ARROW_ANCHOR Anchor(void) const { return(CChartObjectArrow::Anchor()); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectArrowRightPrice::CChartObjectArrowRightPrice(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectArrowRightPrice::~CChartObjectArrowRightPrice(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Right Price Label"                                |
//+------------------------------------------------------------------+
bool CChartObjectArrowRightPrice::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_ARROW_RIGHT_PRICE,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
