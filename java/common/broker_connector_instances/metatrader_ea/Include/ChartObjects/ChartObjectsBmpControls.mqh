//+------------------------------------------------------------------+
//|                                      ChartObjectsBmpControls.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//| All objects with "bmp" pictures.                                 |
//+------------------------------------------------------------------+
#include "ChartObject.mqh"
//+------------------------------------------------------------------+
//| Class CChartObjectBitmap.                                        |
//| Purpose: Class of the "Bitmap" object of chart.                  |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectBitmap : public CChartObject
  {
public:
                     CChartObjectBitmap(void);
                    ~CChartObjectBitmap(void);
   //--- methods of access to properties of the object
   string            BmpFile(void) const;
   bool              BmpFile(const string name) const;
   int               X_Offset(void) const;
   bool              X_Offset(const int X) const;
   int               Y_Offset(void) const;
   bool              Y_Offset(const int Y) const;
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const datetime time,const double price);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_BITMAP); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectBitmap::CChartObjectBitmap(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectBitmap::~CChartObjectBitmap(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Bitmapp"                                          |
//+------------------------------------------------------------------+
bool CChartObjectBitmap::Create(long chart_id,const string name,const int window,const datetime time,const double price)
  {
   if(!ObjectCreate(chart_id,name,OBJ_BITMAP,window,time,price))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get name of bmp-file                                             |
//+------------------------------------------------------------------+
string CChartObjectBitmap::BmpFile(void) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_BMPFILE));
  }
//+------------------------------------------------------------------+
//| Set name of bmp-file                                             |
//+------------------------------------------------------------------+
bool CChartObjectBitmap::BmpFile(const string name) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_BMPFILE,name));
  }
//+------------------------------------------------------------------+
//| Get the XOffset property                                         |
//+------------------------------------------------------------------+
int CChartObjectBitmap::X_Offset(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XOFFSET));
  }
//+------------------------------------------------------------------+
//| Set the XOffset property                                         |
//+------------------------------------------------------------------+
bool CChartObjectBitmap::X_Offset(const int X) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_XOFFSET,X));
  }
//+------------------------------------------------------------------+
//| Get the YOffset property                                         |
//+------------------------------------------------------------------+
int CChartObjectBitmap::Y_Offset(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YOFFSET));
  }
//+------------------------------------------------------------------+
//| Set the YOffset property                                         |
//+------------------------------------------------------------------+
bool CChartObjectBitmap::Y_Offset(const int Y) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_YOFFSET,Y));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectBitmap::Save(const int file_handle)
  {
   int    len;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObject::Save(file_handle))
      return(false);
//--- write value of the "name of bmp-file" property
   str=ObjectGetString(m_chart_id,m_name,OBJPROP_BMPFILE);
   len=StringLen(str);
   if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
      return(false);
   if(len!=0 && FileWriteString(file_handle,str,len)!=len)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading parameters of object from file                           |
//+------------------------------------------------------------------+
bool CChartObjectBitmap::Load(const int file_handle)
  {
   int    len;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObject::Load(file_handle))
      return(false);
//--- read value of the "name of bmp-file" property
   len=FileReadInteger(file_handle,INT_VALUE);
   str=(len!=0) ? FileReadString(file_handle,len) : "";
   if(!ObjectSetString(m_chart_id,m_name,OBJPROP_BMPFILE,str))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Class CChartObjectBmpLabel.                                      |
//| Purpose: Class of the "Bitmap label" object of chart.            |
//|          Derives from class CChartObject.                        |
//+------------------------------------------------------------------+
class CChartObjectBmpLabel : public CChartObject
  {
public:
                     CChartObjectBmpLabel(void);
                    ~CChartObjectBmpLabel(void);
   //--- methods of access to properties of the object
   int               X_Distance(void) const;
   bool              X_Distance(const int X) const;
   int               Y_Distance(void) const;
   bool              Y_Distance(const int Y) const;
   int               X_Size(void) const;
   int               Y_Size(void) const;
   ENUM_BASE_CORNER  Corner(void) const;
   bool              Corner(const ENUM_BASE_CORNER corner) const;
   string            BmpFileOn(void) const;
   bool              BmpFileOn(const string name) const;
   string            BmpFileOff(void) const;
   bool              BmpFileOff(const string name) const;
   bool              State(void) const;
   bool              State(const bool state) const;
   int               X_Offset(void) const;
   bool              X_Offset(const int X) const;
   int               Y_Offset(void) const;
   bool              Y_Offset(const int Y) const;
   //--- change of time/price coordinates is blocked
   bool              Time(const datetime time) const { return(false); }
   bool              Price(const double price) const { return(false); }
   //--- method of creating the object
   bool              Create(long chart_id,const string name,const int window,const int X,const int Y);
   //--- method of identifying the object
   virtual int       Type(void) const { return(OBJ_BITMAP_LABEL); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CChartObjectBmpLabel::CChartObjectBmpLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CChartObjectBmpLabel::~CChartObjectBmpLabel(void)
  {
  }
//+------------------------------------------------------------------+
//| Create object "Bitmap label"                                     |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::Create(long chart_id,const string name,const int window,const int X,const int Y)
  {
   if(!ObjectCreate(chart_id,name,OBJ_BITMAP_LABEL,window,0,0.0))
      return(false);
   if(!Attach(chart_id,name,window,1))
      return(false);
   if(!X_Distance(X) || !Y_Distance(Y))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Get the X-distance property                                      |
//+------------------------------------------------------------------+
int CChartObjectBmpLabel::X_Distance(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE));
  }
//+------------------------------------------------------------------+
//| Set the X-distance property                                      |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::X_Distance(const int X) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE,X));
  }
//+------------------------------------------------------------------+
//| Get the Y-distance property                                      |
//+------------------------------------------------------------------+
int CChartObjectBmpLabel::Y_Distance(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE));
  }
//+------------------------------------------------------------------+
//| Set the Y-distance property                                      |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::Y_Distance(const int Y) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE,Y));
  }
//+------------------------------------------------------------------+
//| Get the X-size                                                   |
//+------------------------------------------------------------------+
int CChartObjectBmpLabel::X_Size(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XSIZE));
  }
//+------------------------------------------------------------------+
//| Get the Y-size                                                   |
//+------------------------------------------------------------------+
int CChartObjectBmpLabel::Y_Size(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YSIZE));
  }
//+------------------------------------------------------------------+
//| Get the Corner property                                          |
//+------------------------------------------------------------------+
ENUM_BASE_CORNER CChartObjectBmpLabel::Corner(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(WRONG_VALUE);
//--- result
   return((ENUM_BASE_CORNER)ObjectGetInteger(m_chart_id,m_name,OBJPROP_CORNER));
  }
//+------------------------------------------------------------------+
//| Set the Corner property                                          |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::Corner(const ENUM_BASE_CORNER corner) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_CORNER,corner));
  }
//+------------------------------------------------------------------+
//| Get filename of the "bmp-ON" property                            |
//+------------------------------------------------------------------+
string CChartObjectBmpLabel::BmpFileOn(void) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_BMPFILE,0));
  }
//+------------------------------------------------------------------+
//| Set filename for the "bmp-ON" property                           |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::BmpFileOn(const string name) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_BMPFILE,0,name));
  }
//+------------------------------------------------------------------+
//| Get filename of the "bmp-OFF" property                           |
//+------------------------------------------------------------------+
string CChartObjectBmpLabel::BmpFileOff(void) const
  {
//--- check
   if(m_chart_id==-1)
      return("");
//--- result
   return(ObjectGetString(m_chart_id,m_name,OBJPROP_BMPFILE,1));
  }
//+------------------------------------------------------------------+
//| Set filename for the "bmp-OFF" property                          |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::BmpFileOff(const string name) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetString(m_chart_id,m_name,OBJPROP_BMPFILE,1,name));
  }
//+------------------------------------------------------------------+
//| Get the State property                                           |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::State(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return((bool)ObjectGetInteger(m_chart_id,m_name,OBJPROP_STATE));
  }
//+------------------------------------------------------------------+
//| Set the State property                                           |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::State(const bool state) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_STATE,state));
  }
//+------------------------------------------------------------------+
//| Get the XOffset property                                         |
//+------------------------------------------------------------------+
int CChartObjectBmpLabel::X_Offset(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XOFFSET));
  }
//+------------------------------------------------------------------+
//| Set the XOffset property                                         |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::X_Offset(const int X) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_XOFFSET,X));
  }
//+------------------------------------------------------------------+
//| Get the YOffset property                                         |
//+------------------------------------------------------------------+
int CChartObjectBmpLabel::Y_Offset(void) const
  {
//--- check
   if(m_chart_id==-1)
      return(0);
//--- result
   return((int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YOFFSET));
  }
//+------------------------------------------------------------------+
//| Set the YOffset property                                         |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::Y_Offset(const int Y) const
  {
//--- check
   if(m_chart_id==-1)
      return(false);
//--- result
   return(ObjectSetInteger(m_chart_id,m_name,OBJPROP_YOFFSET,Y));
  }
//+------------------------------------------------------------------+
//| Writing parameters of object to file                             |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::Save(const int file_handle)
  {
   int    len;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- write
   if(!CChartObject::Save(file_handle))
      return(false);
//--- write value of the "X-distance" property 
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Y-distance" property
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "Corner" property 
   if(FileWriteInteger(file_handle,(int)ObjectGetInteger(m_chart_id,m_name,OBJPROP_CORNER),INT_VALUE)!=sizeof(int))
      return(false);
//--- write value of the "filename bmp-ON" property 
   str=ObjectGetString(m_chart_id,m_name,OBJPROP_BMPFILE,0);
   len=StringLen(str);
   if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
      return(false);
   if(len!=0 && FileWriteString(file_handle,str,len)!=len)
      return(false);
//--- write value of the "filename bmp-OFF" property
   str=ObjectGetString(m_chart_id,m_name,OBJPROP_BMPFILE,1);
   len=StringLen(str);
   if(FileWriteInteger(file_handle,len,INT_VALUE)!=INT_VALUE)
      return(false);
   if(len!=0 && FileWriteString(file_handle,str,len)!=len)
      return(false);
//--- write state
   if(FileWriteLong(file_handle,ObjectGetInteger(m_chart_id,m_name,OBJPROP_STATE))!=sizeof(long))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading object parameters from file                              |
//+------------------------------------------------------------------+
bool CChartObjectBmpLabel::Load(const int file_handle)
  {
   int    len;
   string str;
//--- check
   if(file_handle==INVALID_HANDLE || m_chart_id==-1)
      return(false);
//--- read
   if(!CChartObject::Load(file_handle))
      return(false);
//--- read value of the "X-distance" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_XDISTANCE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "Y-distance" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_YDISTANCE,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of "Corner" property
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_CORNER,FileReadInteger(file_handle,INT_VALUE)))
      return(false);
//--- read value of the "filename bmp-ON" property
   len=FileReadInteger(file_handle,INT_VALUE);
   str=(len!=0) ? FileReadString(file_handle,len) : "";
   if(!ObjectSetString(m_chart_id,m_name,OBJPROP_BMPFILE,0,str))
      return(false);
//--- read value of the "filename bmp-OFF" property
   len=FileReadInteger(file_handle,INT_VALUE);
   str=(len!=0) ? FileReadString(file_handle,len) : "";
   if(!ObjectSetString(m_chart_id,m_name,OBJPROP_BMPFILE,1,str))
      return(false);
//--- read state
   if(!ObjectSetInteger(m_chart_id,m_name,OBJPROP_STATE,FileReadLong(file_handle)))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
