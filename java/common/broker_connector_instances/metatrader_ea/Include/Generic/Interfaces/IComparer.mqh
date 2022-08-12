//+------------------------------------------------------------------+
//|                                                    IComparer.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Interface IComparer<T>.                                          |
//| Usage: Defines a method that a type implements to compare two    |
//|        values.                                                   |
//+------------------------------------------------------------------+
template<typename T>
interface IComparer
  {
//--- compares two values and returns a value indicating whether one is less than, equal to, or greater than the other
   int       Compare(T x,T y);
  };
//+------------------------------------------------------------------+
