//+------------------------------------------------------------------+
//|                                                         ISet.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include "ICollection.mqh"
//+------------------------------------------------------------------+
//| Interface ISet<T>.                                               |
//| Usage: Provides the base interface for the abstraction of sets.  |
//+------------------------------------------------------------------+
template<typename T>
interface ISet: public ICollection<T>
  {
//--- methods of changing sets
   void              ExceptWith(ICollection<T>*collection);
   void              ExceptWith(T &array[]);
   void              IntersectWith(ICollection<T>*collection);
   void              IntersectWith(T &array[]);
   void              SymmetricExceptWith(ICollection<T>*collection);
   void              SymmetricExceptWith(T &array[]);
   void              UnionWith(ICollection<T>*collection);
   void              UnionWith(T &array[]);
//--- methods for determining the relationship between sets
   bool              IsProperSubsetOf(ICollection<T>*collection);
   bool              IsProperSubsetOf(T &array[]);
   bool              IsProperSupersetOf(ICollection<T>*collection);
   bool              IsProperSupersetOf(T &array[]);
   bool              IsSubsetOf(ICollection<T>*collection);
   bool              IsSubsetOf(T &array[]);
   bool              IsSupersetOf(ICollection<T>*collection);
   bool              IsSupersetOf(T &array[]);
   bool              Overlaps(ICollection<T>*collection);
   bool              Overlaps(T &array[]);
   bool              SetEquals(ICollection<T>*collection);
   bool              SetEquals(T &array[]);
  };
//+------------------------------------------------------------------+
