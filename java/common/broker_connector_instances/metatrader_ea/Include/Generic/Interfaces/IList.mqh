//+------------------------------------------------------------------+
//|                                                        IList.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include "ICollection.mqh"
//+------------------------------------------------------------------+
//| Interface IList<T>.                                              |
//| Usage: Represents a collection of objects that can be            |
//|        individually accessed by index.                           |
//+------------------------------------------------------------------+
template<typename T>
interface IList: public ICollection<T>
  {
//--- method of access to the data
   bool              TryGetValue(const int index,T &value);
   bool              TrySetValue(const int index,T value);
//--- methods of filling the array
   bool              Insert(const int index,T item);
//--- methods for searching index   
   int               IndexOf(T item);
   int               LastIndexOf(T item);
//--- methods of cleaning and deleting   
   bool              RemoveAt(const int index);
  };
//+------------------------------------------------------------------+
