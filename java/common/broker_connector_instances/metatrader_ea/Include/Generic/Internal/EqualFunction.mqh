//+------------------------------------------------------------------+
//|                                                EqualFunction.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\IEqualityComparable.mqh>
//+------------------------------------------------------------------+
//| Indicates whether x object is equal y object of the same type.   |
//+------------------------------------------------------------------+
template<typename T>
bool Equals(T x,T y)
  {
//--- try to convert to equality comparable object  
   IEqualityComparable<T>*equtable=dynamic_cast<IEqualityComparable<T>*>(x);
   if(equtable)
     {
      //--- use specied equality compare method
      return equtable.Equals(y);
     }
   else
     {
      //--- use default equality comparer operator
      return(x==y);
     }
  }
//+------------------------------------------------------------------+
