//+------------------------------------------------------------------+
//|                                                   SortedList.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\ISet.mqh>
#include <Generic\Internal\Introsort.mqh>
#include "RedBlackTree.mqh"
#include "HashSet.mqh"
//+------------------------------------------------------------------+
//| Class CSortedSet<T>.                                             |
//| Usage: Represents a collection of objects that is maintained in  |
//|        sorted order.                                             |
//+------------------------------------------------------------------+
template<typename T>
class CSortedSet: public ISet<T>
  {
protected:
   CRedBlackTree<T>*m_tree;

public:
                     CSortedSet(void);
                     CSortedSet(IComparer<T>*comparer);
                     CSortedSet(ICollection<T>*collection);
                     CSortedSet(ICollection<T>*collection,IComparer<T>*comparer);
                     CSortedSet(T &array[]);
                     CSortedSet(T &array[],IComparer<T>*comparer);
                    ~CSortedSet(void);
   //--- methods of filling data 
   bool              Add(T value) { return(m_tree.Add(value));     }
   //--- methods of access to protected data
   int               Count(void)          { return(m_tree.Count());        }
   bool              Contains(T item)     { return(m_tree.Contains(item)); }
   IComparer<T>     *Comparer(void) const { return(m_tree.Comparer());     }
   bool              TryGetMin(T &min)    { return(m_tree.TryGetMin(min)); }
   bool              TryGetMax(T &max)    { return(m_tree.TryGetMax(max)); }
   //--- methods of copy data from collection   
   int               CopyTo(T &dst_array[],const int dst_start=0);
   //--- methods of cleaning and deleting
   void              Clear(void)        { m_tree.Clear();              }
   bool              Remove(T item)     { return(m_tree.Remove(item)); }
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
   //--- methods for working with an ordered set
   bool              GetViewBetween(T &array[],T lower_value,T upper_value);
   bool              GetReverse(T &array[]);
  };
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedSet<T> class that is    |
//| empty and uses the default equality comparer for the set type.   |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::CSortedSet(void)
  {
   m_tree=new CRedBlackTree<T>();
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedSet<T> class that is    |
//| empty and uses the specified equality comparer for the set type. |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::CSortedSet(IComparer<T>*comparer)
  {
   m_tree=new CRedBlackTree<T>(comparer);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedSet<T> class that uses  |
//| the default equality comparer for the set type, contains elements|
//| copied from the specified collection, and has sufficient capacity|
//| to accommodate the number of elements copied.                    |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::CSortedSet(ICollection<T>*collection)
  {
   m_tree=new CRedBlackTree<T>(collection);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedSet<T> class that uses  |
//| the specified equality comparer for the set type, contains       |
//| elements copied from the specified collection, and has sufficient|
//| capacity to accommodate the number of elements copied.           |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::CSortedSet(ICollection<T>*collection,IComparer<T>*comparer)
  {
   m_tree=new CRedBlackTree<T>(collection,comparer);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedSet<T> class that uses  |
//| the default equality comparer for the set type, contains         |
//| elements copied from the specified array, and has sufficient     |
//| capacity to accommodate the number of elements copied.           |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::CSortedSet(T &array[])
  {
   m_tree=new CRedBlackTree<T>(array);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedSet<T> class that uses  |
//| the specified equality comparer for the set type, contains       |
//| elements copied from the specified array, and has sufficient     |
//| capacity to accommodate the number of elements copied.           |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::CSortedSet(T &array[],IComparer<T>*comparer)
  {
   m_tree=new CRedBlackTree<T>(array,comparer);
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
template<typename T>
CSortedSet::~CSortedSet(void)
  {
   delete m_tree;
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the set to a compatible          |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename T>
int CSortedSet::CopyTo(T &dst_array[],const int dst_start=0)
  {
   return(m_tree.CopyTo(dst_array, dst_start));
  }
//+------------------------------------------------------------------+
//| Removes all elements in the specified collection from the current|
//| set.                                                             |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::ExceptWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- check tree count
   if(m_tree.Count()==0)
      return;
//--- special case if collection is this
//--- a set minus itself is the empty set
   if(collection==GetPointer(this))
     {
      Clear();
      return;
     }
//--- copy collection to array
   T array[];
   int size=collection.CopyTo(array);
//--- find max and min value
   T max;
   T min;
//--- get comaprer
   IComparer<T>*comparer=Comparer();
   if(!m_tree.TryGetMax(max))
      return;
   if(!m_tree.TryGetMin(min))
      return;
//--- remove elements
   for(int i=0; i<size; i++)
     {
      T item=array[i];
      if(!(comparer.Compare(item,min)<0 || comparer.Compare(item,max)>0) && Contains(item))
         m_tree.Remove(item);
     }
  }
//+------------------------------------------------------------------+
//| Removes all elements in the specified array from the current set.|
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::ExceptWith(T &array[])
  {
//--- check tree count
   if(m_tree.Count()==0)
      return;
//--- get array size
   int size=ArraySize(array);
//--- find max and min value
   T max;
   T min;
//--- get comparer
   IComparer<T>*comparer=Comparer();
   if(!m_tree.TryGetMax(max))
      return;
   if(!m_tree.TryGetMin(min))
      return;
//--- remove elements   
   for(int i=0; i<size; i++)
     {
      T item=array[i];
      if(!(comparer.Compare(item,min)<0 || comparer.Compare(item,max)>0) && Contains(item))
         m_tree.Remove(item);
     }
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present in that object and in the specified collection.          |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::IntersectWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- check tree count
   if(m_tree.Count()==0)
      return;
//--- special case if collection is this
//--- a set minus itself is the empty set
   if(collection==GetPointer(this))
      return;
//--- copy collection to array
   T array[];
   int size=collection.CopyTo(array);
//--- create emty tree
   CRedBlackTree<T>*tree=new CRedBlackTree<T>();
//--- store values conatin in tree and array
   for(int i=0; i<size; i++)
      if(m_tree.Contains(array[i]))
         tree.Add(array[i]);
//--- overwrite tree
   delete m_tree;
   m_tree=tree;
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present in that object and in the specified array.               |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::IntersectWith(T &array[])
  {
//--- check tree count
   if(m_tree.Count()==0)
      return;
//--- get array size
   int size=ArraySize(array);
//--- create emty tree
   CRedBlackTree<T>*tree=new CRedBlackTree<T>();
//--- store values conatin in tree and array
   for(int i=0; i<size; i++)
      if(m_tree.Contains(array[i]))
         tree.Add(array[i]);
//--- overwrite tree
   delete m_tree;
   m_tree=tree;
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present either in that set or in the specified collection, but   |
//| not both.                                                        |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::SymmetricExceptWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- check collection count
   if(collection.Count()==0)
      return;
//--- check tree count
   if(m_tree.Count()==0)
     {
      UnionWith(collection);
      return;
     }
//--- special case if collection is this
//--- a set minus itself is the empty set
   if(collection==GetPointer(this))
     {
      Clear();
      return;
     }
//--- copy colleaction to array
   T array[];
   int size=collection.CopyTo(array);
//--- get comparer
   IComparer<T>*comparer=m_tree.Comparer();
//--- sort array
   Introsort<T,T>sort;
   ArrayCopy(sort.keys,array);
   sort.comparer=comparer;
   sort.Sort(0, size);
   ArrayCopy(array,sort.keys);
//--- modify tree
   T last=array[0];
   for(int i=0; i<size; i++)
     {
      while(i<size && i!=0 && comparer.Compare(array[i],last)==0)
         i++;
      if(i>=size)
         break;

      if(m_tree.Contains(array[i]))
         m_tree.Remove(array[i]);
      else
         m_tree.Add(array[i]);

      last=array[i];
     }
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present either in that set or in the specified array, but not    |
//| both.                                                            |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::SymmetricExceptWith(T &array[])
  {
//--- check array size
   if(ArraySize(array)==0)
      return;
//--- check tree count
   if(m_tree.Count()==0)
     {
      UnionWith(array);
      return;
     }
//--- get size
   int size=ArraySize(array);
//--- get comparer 
   IComparer<T>*comparer=m_tree.Comparer();
//--- sort array
   Introsort<T,T>sort;
   ArrayCopy(sort.keys,array);
   sort.comparer=comparer;
   sort.Sort(0, size);
   ArrayReverse(sort.keys,0,ArraySize(sort.keys));
//--- modify tree
   T last=sort.keys[0];
   for(int i=0; i<size; i++)
     {
      while(i<size && i!=0 && comparer.Compare(sort.keys[i],last)==0)
         i++;
      if(i>=size)
         break;

      if(m_tree.Contains(sort.keys[i]))
         m_tree.Remove(sort.keys[i]);
      else
         m_tree.Add(sort.keys[i]);

      last=sort.keys[i];
     }
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain all elements that are present|
//| in itself, the specified collection, or both.                    |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::UnionWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- copy all elements from collecton to array
   T array[];
   int size=collection.CopyTo(array);
//--- add all elemets from array to set
   for(int i=0; i<size; i++)
      m_tree.Add(array[i]);
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain all elements that are present|
//| in itself, the specified array, or both.                         |
//+------------------------------------------------------------------+
template<typename T>
void CSortedSet::UnionWith(T &array[])
  {
//--- get array size
   int size=ArraySize(array);
//--- add all elemets from array to set
   for(int i=0; i<size; i++)
      m_tree.Add(array[i]);
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper subset of the specified     |
//| collection.                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsProperSubsetOf(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(false);
//--- check tree count
   if(m_tree.Count()==0)
      return(collection.Count() > 0);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return(ptr_set.IsProperSupersetOf(m_tree));
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return(set.IsProperSupersetOf(m_tree));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper subset of the specified     |
//| array.                                                           |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsProperSubsetOf(T &array[])
  {
   if(m_tree.Count()==0)
      return(ArraySize(array) > 0);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   if(m_tree.Count()>=set.Count())
      return(false);
   return(set.IsProperSupersetOf(m_tree));
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper superset of the specified   |
//| collection.                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsProperSupersetOf(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(m_tree.Count()>0);
//--- check tree count
   if(m_tree.Count()==0)
      return(false);
//--- check collection count
   if(collection.Count()==0)
      return(true);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return(ptr_set.IsProperSubsetOf(m_tree));
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return(set.IsProperSubsetOf(m_tree));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper superset of the specified   |
//| array.                                                           |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsProperSupersetOf(T &array[])
  {
   if(m_tree.Count()==0)
      return(false);
   if(ArraySize(array)==0)
      return(true);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   return(set.IsProperSubsetOf(m_tree));
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a subset of the specified collection.|
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsSubsetOf(ICollection<T>*collection)
  {
//--- cehck collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(m_tree.Count()==0);
//--- check tree count
   if(m_tree.Count()==0)
      return(true);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)==POINTER_DYNAMIC)
     {
      return(ptr_set.IsProperSupersetOf(m_tree));
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return(set.IsProperSupersetOf(m_tree));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a subset of the specified array.     |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsSubsetOf(T &array[])
  {
//--- check tree count
   if(m_tree.Count()==0)
      return(true);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   if(m_tree.Count()>set.Count())
      return(false);
   return(set.IsProperSupersetOf(m_tree));
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a superset of the specified          |
//| collection.                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsSupersetOf(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(m_tree.Count()>=0);
//--- check collection count
   if(collection.Count()==0)
      return(true);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return(ptr_set.IsSupersetOf(m_tree));
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return(set.IsSupersetOf(m_tree));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a superset of the specified array.   |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::IsSupersetOf(T &array[])
  {
//--- check array size
   if(ArraySize(array)==0)
      return(true);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   return(set.IsSupersetOf(m_tree));
  }
//+------------------------------------------------------------------+
//| Determines whether the current set and a specified collection    |
//| share common elements.                                           |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::Overlaps(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(false);
//--- check tree count
   if(m_tree.Count()==0)
      return(false);
//--- check collection count
   if(collection.Count()==0)
      return(false);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return(ptr_set.Overlaps(m_tree));
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return(set.Overlaps(m_tree));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether the current set and a specified array share   |
//| common elements.                                                 |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::Overlaps(T &array[])
  {
//--- check tree count
   if(m_tree.Count()==0)
      return(false);
//--- check array size
   if(ArraySize(array)==0)
      return(false);
//--- convert array to set
   CHashSet<T>set(array);
   return(set.Overlaps(m_tree));
  }
//+------------------------------------------------------------------+
//| Determines whether a set and the specified collection contain the|
//| same elements.                                                   |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::SetEquals(ICollection<T>*collection)
  {
   if(CheckPointer(collection)==POINTER_INVALID)
      return(false);
//--- get array from collection
   T array[];
   collection.CopyTo(array);
//--- check current set is equal specified array
   return SetEquals(array);
  }
//+------------------------------------------------------------------+
//| Determines whether a set and the specified array contain the same|
//| elements.                                                        |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::SetEquals(T &array[])
  {
//--- try find all elements in the tree
   for(int i=0; i<ArraySize(array); i++)
      if(!m_tree.Contains(array[i]))
         return(false);
   return(true);
  }
//+------------------------------------------------------------------+
//| Copy a view of a subset in a CSortedSet<T> to array.             |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::GetViewBetween(T &array[],T lower_value,T upper_value)
  {
//--- get comparer
   IComparer<T>*comparer=m_tree.Comparer();
   if(comparer.Compare(lower_value,upper_value)>0)
      return(false);
//--- copy all element from tree to array
   T buff[];
   int size=m_tree.CopyTo(buff);
//--- check range
   if(size==0 || comparer.Compare(buff[0],upper_value)>0 || comparer.Compare(buff[size-1],lower_value)<0)
      return(false);
//--- find first element greater than lower_value
   int index_lower=0;
   while(index_lower<size && comparer.Compare(buff[index_lower],lower_value)<0)
      index_lower++;
//--- find first element less than upper_value
   int index_upper=size-1;
   while(index_upper>0 && comparer.Compare(buff[index_upper],upper_value)>0)
      index_upper--;
//--- check indices
   if(index_lower>index_upper)
      return(false);
//--- copy view between lower_value and upper_value to array
   return(ArrayCopy(array,buff,0,index_lower,index_upper-index_lower+1)>=0);
  }
//+------------------------------------------------------------------+
//| Copy the CSortedSet<T> in reverse order to array.                |
//+------------------------------------------------------------------+
template<typename T>
bool CSortedSet::GetReverse(T &array[])
  {
   int size=m_tree.CopyTo(array);
   return ArrayReverse(array,0,size);
  }
//+------------------------------------------------------------------+
