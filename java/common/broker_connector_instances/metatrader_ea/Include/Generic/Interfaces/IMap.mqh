//+------------------------------------------------------------------+
//|                                                         IMap.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include "ICollection.mqh"
template<typename TKey,typename TValue>
class CKeyValuePair;
//+------------------------------------------------------------------+
//| Interface IMap<TKey,TValue>.                                     |
//| Usage: Represents a generic collection of key/value pairs.       |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
interface IMap: public ICollection<CKeyValuePair<TKey,TValue>*>
  {
//--- methods of filling data 
   bool      Add(TKey key,TValue value);
//--- methods of access to protected data
   bool      Contains(TKey key,TValue value);
   bool      Remove(TKey key);
//--- method of access to the data
   bool      TryGetValue(TKey key,TValue &value);
   bool      TrySetValue(TKey key,TValue value);
//--- methods of copy data from collection   
   int       CopyTo(TKey &dst_keys[],TValue &dst_values[],const int dst_start=0);
  };
//+------------------------------------------------------------------+
