//+------------------------------------------------------------------+
//|                                              CompareFunction.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\IComparable.mqh>
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const bool x,const bool y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const char x,const char y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const uchar x,const uchar y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const short x,const short y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const ushort x,const ushort y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const color x,const color y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const int x,const int y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const uint x,const uint y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const datetime x,const datetime y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const long x,const long y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const ulong x,const ulong y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const float x,const float y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const double x,const double y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
int Compare(const string x,const string y)
  {
   if(x>y)
      return(1);
   else if(x<y)
      return(-1);
   else
      return(0);
  }
//+------------------------------------------------------------------+
//| Compares two objects and returns a value indicating whether one  |
//| is less than, equal to, or greater than the other.               |
//+------------------------------------------------------------------+
template<typename T>
int Compare(T x,T y)
  {
//--- try to convert to comparable object  
   IComparable<T>*comparable=dynamic_cast<IComparable<T>*>(x);
   if(comparable)
     {
      //--- use specied compare method
      return comparable.Compare(y);
     }
   else
     {
      //--- unknown compare function     
      return(0);
     }
  }
//+------------------------------------------------------------------+
