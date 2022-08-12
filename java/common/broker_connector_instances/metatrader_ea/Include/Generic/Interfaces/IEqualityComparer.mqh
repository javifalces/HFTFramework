//+------------------------------------------------------------------+
//|                                            IEqualityComparer.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Interface IEqualityComparer<T>.                                  |
//| Usage: Defines methods to support the comparison of values for   |
//|        equality.                                                 |
//+------------------------------------------------------------------+
template<typename T>
interface IEqualityComparer
  {
//--- determines whether the specified values are equal
   bool      Equals(T x,T y);
//--- returns a hash code for the specified object
   int       HashCode(T value);
  };
//+------------------------------------------------------------------+
