//+------------------------------------------------------------------+
//|                                          IEqualityComparable.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Interface IEqualityComparable<T>.                                |
//| Usage: Defines a generalized method to create a type-specific    |
//| method for determining equality of instances.                    |
//+------------------------------------------------------------------+
template<typename T>
interface IEqualityComparable
  {
//--- method for determining equality
   bool              Equals(T value);
//--- method to calculate hash code   
   int               HashCode(void);
  };
//+------------------------------------------------------------------+
