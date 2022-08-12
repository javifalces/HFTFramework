//+------------------------------------------------------------------+
//|                                                      HashSet.mqh |
//|                   Copyright 2016-2021, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include<Generic\Interfaces\ISet.mqh>
#include <Generic\Internal\PrimeGenerator.mqh>
#include <Generic\Interfaces\IEqualityComparer.mqh>
#include <Generic\Internal\DefaultEqualityComparer.mqh>
//+------------------------------------------------------------------+
//| Struct Slot<T>.                                                  |
//| Usage: Internal structure for organization CHashSet<T>.          |
//+------------------------------------------------------------------+
template<typename T>
struct Slot
  {
public:
   int               hash_code;
   T                 value;
   int               next;
                     Slot(void): hash_code(0),value((T)NULL),next(0) {}
  };
//+------------------------------------------------------------------+
//| Class CHashSet<T>.                                               |
//| Usage: Represents a set of unique values.                        |
//+------------------------------------------------------------------+
template<typename T>
class CHashSet: public ISet<T>
  {
protected:
   int               m_buckets[];
   Slot<T>           m_slots[];
   int               m_count;
   int               m_last_index;
   int               m_free_list;
   IEqualityComparer<T>*m_comparer;
   bool              m_delete_comparer;

public:
                     CHashSet(void);
                     CHashSet(IEqualityComparer<T>*comparer);
                     CHashSet(ICollection<T>*collection);
                     CHashSet(ICollection<T>*collection,IEqualityComparer<T>*comparer);
                     CHashSet(T &array[]);
                     CHashSet(T &array[],IEqualityComparer<T>*comparer);
                    ~CHashSet(void);
   //--- methods of filling data
   bool              Add(T value);
   //--- methods of access to protected data
   int               Count(void)                                       { return(m_count);              }
   IEqualityComparer<T>* Comparer(void)                          const { return(m_comparer);           }
   bool              Contains(T item);
   void              TrimExcess(void);
   //--- methods of copy data from collection
   int               CopyTo(T &ds_array[],const int dst_start=0);
   //--- methods of cleaning and deleting
   void              Clear(void);
   bool              Remove(T item);
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

private:
   void              SetCapacity(const int new_size,bool new_hash_codes);
   bool              AddIfNotPresent(T value);
   void              Initialize(const int capacity);
   void              InternalSymmetricExceptWith(CHashSet<T>*set);
   bool              InternalIsSubsetOf(CHashSet<T>*set);
   bool              InternalIsSupersetOf(CHashSet<T>*set);
   bool              InternalIsProperSubsetOf(CHashSet<T>*set);
   bool              InternalIsProperSupersetOf(CHashSet<T>*set);
  };
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashSet<T> class that is empty|
//| and uses the default equality comparer for the set type.         |
//+------------------------------------------------------------------+
template<typename T>
CHashSet::CHashSet(void): m_count(0),
                          m_last_index(0),
                          m_free_list(-1)
  {
//--- use default equality comaprer
   m_comparer=new CDefaultEqualityComparer<T>();
   m_delete_comparer=true;
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashSet<T> class that is empty|
//| and uses the specified equality comparer for the set type.       |
//+------------------------------------------------------------------+
template<typename T>
CHashSet::CHashSet(IEqualityComparer<T>*comparer): m_count(0),
                                                   m_last_index(0),
                                                   m_free_list(-1)
  {
//--- check equality comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default equality comaprer
      m_comparer=new CDefaultEqualityComparer<T>();
      m_delete_comparer=true;
     }
   else
     {
      //--- use specified equality comaprer
      m_comparer=comparer;
      m_delete_comparer=false;
     }
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashSet<T> class that uses the|
//| default equality comparer for the set type, contains elements    |
//| copied from the specified collection, and has sufficient capacity|
//| to accommodate the number of elements copied.                    |
//+------------------------------------------------------------------+
template<typename T>
CHashSet::CHashSet(ICollection<T>*collection): m_count(0),
                                               m_last_index(0),
                                               m_free_list(-1)
  {
//--- use default equality comaprer
   m_comparer=new CDefaultEqualityComparer<T>();
   m_delete_comparer=true;
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- set capacity for elements of the collection
   int count=collection.Count();
   Initialize(count);
//--- add element from collection to the set
   this.UnionWith(collection);
   if((m_count==0 && ArraySize(m_slots)>3) ||
      (m_count>0 && ArraySize(m_slots)/m_count>3))
      TrimExcess();
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashSet<T> class that uses the|
//| specified equality comparer for the set type, contains elements  |
//| copied from the specified collection, and has sufficient capacity|
//| to accommodate the number of elements copied.                    |
//+------------------------------------------------------------------+
template<typename T>
CHashSet::CHashSet(ICollection<T>*collection,IEqualityComparer<T>*comparer): m_count(0),
                                                                             m_last_index(0),
                                                                             m_free_list(-1)
  {
//--- check equality comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default equality comaprer
      m_comparer=new CDefaultEqualityComparer<T>();
      m_delete_comparer=true;
     }
   else
     {
      //--- use specified comaprer
      m_comparer=comparer;
      m_delete_comparer=false;
     }
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- set capacity for elements of the collection
   int count=collection.Count();
   Initialize(count);
//--- add element from collection to the set
   this.UnionWith(collection);
   if((m_count==0 && ArraySize(m_slots)>3) ||
      (m_count>0 && ArraySize(m_slots)/m_count>3))
      TrimExcess();
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashSet<T> class that uses the|
//| default equality comparer for the set type, contains elements    |
//| copied from the specified array, and has sufficient capacity to  |
//| accommodate the number of elements copied.                       |
//+------------------------------------------------------------------+
template<typename T>
CHashSet::CHashSet(T &array[]): m_count(0),
                                m_last_index(0),
                                m_free_list(-1)
  {
//--- use default equality comaprer
   m_comparer=new CDefaultEqualityComparer<T>();
   m_delete_comparer=true;
//--- set capacity for elements of the array
   int count=ArraySize(array);
   Initialize(count);
//--- add element from array to the set
   this.UnionWith(array);
   if((m_count==0 && ArraySize(m_slots)>3) ||
      (m_count>0 && ArraySize(m_slots)/m_count>3))
      TrimExcess();
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashSet<T> class that uses the|
//| specified equality comparer for the set type, contains elements  |
//| copied from the specified array, and has sufficient capacity to  |
//| accommodate the number of elements copied.                       |
//+------------------------------------------------------------------+
template<typename T>
CHashSet::CHashSet(T &array[],IEqualityComparer<T>*comparer): m_count(0),
                                                              m_last_index(0),
                                                              m_free_list(-1)
  {
//--- check equality comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default equality comaprer
      m_comparer=new CDefaultEqualityComparer<T>();
      m_delete_comparer=true;
     }
   else
     {
      //--- use specified comaprer
      m_comparer=comparer;
      m_delete_comparer=false;
     }
//--- set capacity for elements of the array
   int count=ArraySize(array);
   Initialize(count);
//--- add element from array to the set
   this.UnionWith(array);
   if((m_count==0 && ArraySize(m_slots)>3) ||
      (m_count>0 && ArraySize(m_slots)/m_count>3))
      TrimExcess();
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
template<typename T> CHashSet::~CHashSet(void)
  {
   if(m_delete_comparer)
      delete m_comparer;
  }
//+------------------------------------------------------------------+
//| Adds the specified element to a set.                             |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::Add(T value)
  {
   return AddIfNotPresent(value);
  }
//+------------------------------------------------------------------+
//| Determines whether a set contains the specified element.         |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::Contains(T item)
  {
//--- check buckets
   if(ArraySize(m_buckets)!=0)
     {
      //--- get hash code for item
      int hash_code=m_comparer.HashCode(item)&0x7FFFFFFF;
      //--- search item in the slots
      for(int i=m_buckets[hash_code%ArraySize(m_buckets)]-1; i>=0; i=m_slots[i].next)
         if(m_slots[i].hash_code==hash_code && m_comparer.Equals(m_slots[i].value,item))
            return(true);
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Sets the capacity of a set to the actual number of elements it   |
//| contains, rounded up to a nearby, implementation-specific value. |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::TrimExcess(void)
  {
   if(m_count==0)
     {
      ArrayFree(m_buckets);
      ArrayFree(m_slots);
     }
   else
     {
      //--- calculate min prime size for current count
      int new_size=CPrimeGenerator::GetPrime(m_count);
      //--- resize buckets and slots
      ArrayResize(m_slots,new_size);
      ArrayResize(m_buckets,new_size);
      //--- restore buckets and slots
      int new_index=0;
      for(int i=0; i<m_last_index; i++)
        {
         if(m_slots[i].hash_code>=0)
           {
            m_slots[new_index]=m_slots[i];
            //--- rehash
            int bucket=m_slots[new_index].hash_code%new_size;
            m_slots[new_index].next=m_buckets[bucket]-1;
            m_buckets[bucket]=new_index+1;
            //--- increment index
            new_index++;
           }
        }
      m_last_index=new_index;
      m_free_list=-1;
     }
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the set to a compatible          |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename T>
int CHashSet::CopyTo(T &dst_array[],const int dst_start)
  {
//--- resize array
   if(dst_start+m_count>ArraySize(dst_array))
      ArrayResize(dst_array,dst_start+m_count);
//--- start copy
   int index=0;
   for(int i=0; i<ArraySize(m_slots); i++)
      if(m_slots[i].hash_code>=0)
        {
         if(dst_start+index>=ArraySize(dst_array) || index>=m_count)
            return(index);
         dst_array[dst_start+index++]=m_slots[i].value;
        }
   return(index);
  }
//+------------------------------------------------------------------+
//| Removes all elements from a set.                                 |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::Clear(void)
  {
   if(m_last_index>0)
     {
      ArrayFree(m_slots);
      ArrayFree(m_buckets);
      m_last_index=0;
      m_count=0;
      m_free_list=-1;
     }
  }
//+------------------------------------------------------------------+
//| Removes the specified element from a set.                        |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::Remove(T item)
  {
   if(ArraySize(m_buckets)!=0)
     {
      //--- get hash code for item
      int hash_code=m_comparer.HashCode(item)&0x7FFFFFFF;
      int bucket=hash_code%ArraySize(m_buckets);
      int last=-1;
      //--- search item
      for(int i=m_buckets[bucket]-1; i>=0; last=i,i=m_slots[i].next)
        {
         if(m_slots[i].hash_code==hash_code && m_comparer.Equals(m_slots[i].value,item))
           {
            if(last<0)
               m_buckets[bucket]=m_slots[i].next+1;
            else
               m_slots[last].next=m_slots[i].next;
            //--- remove item
            m_slots[i].hash_code=-1;
            m_slots[i].value=(T)NULL;
            m_slots[i].next =m_free_list;
            //--- decrement count
            m_count--;
            if(m_count==0)
              {
               m_last_index= 0;
               m_free_list = -1;
              }
            else
              {
               m_free_list=i;
              }
            return(true);
           }
        }
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Removes all elements in the specified collection from the current|
//| set.                                                             |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::ExceptWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- this is already the enpty set
   if(m_count==0)
      return;
//--- special case if collecion is this
//--- a set minus itself is the empty set
   if(collection==GetPointer(this))
     {
      Clear();
      return;
     }
//--- copy collection to array
   T array[];
   collection.CopyTo(array,0);
//--- remove every element in collection from this
   for(int i=0; i<ArraySize(array); i++)
      Remove(array[i]);
  }
//+------------------------------------------------------------------+
//| Removes all elements in the specified array from the current set.|
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::ExceptWith(T &array[])
  {
//--- this is already the enpty set
   if(m_count==0)
      return;
//--- remove every element in collection from this
   for(int i=0; i<ArraySize(array); i++)
      Remove(array[i]);
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present in that object and in the specified collection.          |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::IntersectWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- intersection of anything with empty set is empty set, so return if count is 0
   if(m_count==0)
      return;
//--- if collection is empty, intersection is empty set
   if(collection.Count()==0)
     {
      Clear();
      return;
     }
//--- intersect
   for(int i=0; i<m_last_index; i++)
     {
      if(m_slots[i].hash_code>=0)
        {
         T item=m_slots[i].value;
         if(!collection.Contains(item))
            Remove(item);
        }
     }
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present in that object and in the specified array.               |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::IntersectWith(T &array[])
  {
//--- intersection of anything with empty set is empty set, so return if count is 0
   if(m_count==0)
      return;
//--- if collection is empty, intersection is empty set
   if(ArraySize(array)==0)
     {
      Clear();
      return;
     }
//--- intersect
   CHashSet<T>set(array);
   for(int i=0; i<m_last_index; i++)
     {
      if(m_slots[i].hash_code>=0)
        {
         T item=m_slots[i].value;
         if(!set.Contains(item))
            Remove(item);
        }
     }
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present either in that set or in the specified collection, but   |
//| not both.                                                        |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::SymmetricExceptWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- if set is empty, then symmetric difference is other
   if(m_count==0)
     {
      UnionWith(collection);
      return;
     }
//--- special case this; the symmetric difference of a set with itself is the empty set
   if(collection==GetPointer(this))
     {
      Clear();
      return;
     }
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      InternalSymmetricExceptWith(ptr_set);
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      InternalSymmetricExceptWith(GetPointer(set));
     }
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present either in that set or in the specified array, but not    |
//| both.                                                            |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::SymmetricExceptWith(T &array[])
  {
//--- if set is empty, then symmetric difference is other
   if(m_count==0)
     {
      UnionWith(array);
      return;
     }
//--- symmetric except
   CHashSet<T>set(array);
   InternalSymmetricExceptWith(GetPointer(set));
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain all elements that are present|
//| in itself, the specified collection, or both.                    |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::UnionWith(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return;
//--- get array from collection
   T array[];
   collection.CopyTo(array);
//--- union array with the current set
   UnionWith(array);
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain all elements that are present|
//| in itself, the specified array, or both.                         |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::UnionWith(T &array[])
  {
   for(int i=0; i<ArraySize(array); i++)
      AddIfNotPresent(array[i]);
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper subset of the specified     |
//| collection.                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsProperSubsetOf(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(false);
//--- the empty set is a proper subset of anything but the empty set
   if(m_count==0)
      return(collection.Count()>0);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return InternalIsProperSubsetOf(ptr_set);
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return InternalIsProperSubsetOf(GetPointer(set));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper subset of the specified     |
//| array.                                                           |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsProperSubsetOf(T &array[])
  {
//--- the empty set is a proper subset of anything but the empty set
   if(m_count==0)
      return(ArraySize(array)>0);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   return InternalIsProperSubsetOf(GetPointer(set));
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper superset of the specified   |
//| collection.                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsProperSupersetOf(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(m_count>0);
//--- the empty set is a proper subset of anything but the empty set
   if(m_count==0)
      return(false);
//--- if other is the empty set then this is a superset
   if(collection.Count()==0)
      return(true);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return InternalIsProperSupersetOf(ptr_set);
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return InternalIsProperSupersetOf(GetPointer(set));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper superset of the specified   |
//| array.                                                           |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsProperSupersetOf(T &array[])
  {
//--- the empty set is a proper subset of anything but the empty set
   if(m_count==0)
      return(false);
//--- if other is the empty set then this is a superset
   if(ArraySize(array)==0)
      return(true);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   return InternalIsProperSupersetOf(GetPointer(set));
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a subset of the specified collection.|
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsSubsetOf(ICollection<T>*collection)
  {
   if(CheckPointer(collection)==POINTER_INVALID)
      return(m_count==0);
//--- The empty set is a subset of any set
   if(m_count==0)
      return(true);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return InternalIsSubsetOf(ptr_set);
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return InternalIsSubsetOf(GetPointer(set));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a subset of the specified array.     |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsSubsetOf(T &array[])
  {
//--- The empty set is a subset of any set
   if(m_count==0)
      return(true);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   return InternalIsSubsetOf(GetPointer(set));
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a superset of the specified          |
//| collection.                                                      |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsSupersetOf(ICollection<T>*collection)
  {
   if(CheckPointer(collection)==POINTER_INVALID)
      return(m_count>=0);
//--- if other is the empty set then this is a superset
   if(collection.Count()==0)
      return(true);
//--- check collection is set
   CHashSet<T>*ptr_set=dynamic_cast<CHashSet<T>*>(collection);
   if(CheckPointer(ptr_set)!=POINTER_INVALID)
     {
      return InternalIsSupersetOf(ptr_set);
     }
   else
     {
      //--- create a set based on a specified collection
      CHashSet<T>set(collection);
      return InternalIsSupersetOf(GetPointer(set));
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a superset of the specified array.   |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::IsSupersetOf(T &array[])
  {
//--- if other is the empty set then this is a superset
   if(ArraySize(array)==0)
      return(true);
//--- create a set based on a specified array
   CHashSet<T>set(array);
   return InternalIsSupersetOf(GetPointer(set));
  }
//+------------------------------------------------------------------+
//| Determines whether the current set and a specified collection    |
//| share common elements.                                           |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::Overlaps(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(false);
//--- check current count
   if(m_count==0)
      return(false);
//--- get array from collection
   T array[];
   collection.CopyTo(array);
//--- check overlaps between current set and array
   return Overlaps(array);
  }
//+------------------------------------------------------------------+
//| Determines whether the current set and a specified array share   |
//| common elements.                                                 |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::Overlaps(T &array[])
  {
//--- check current count
   if(m_count==0)
      return(false);
//--- try to find any elements from specified array in current set
   for(int i=0; i<ArraySize(array); i++)
      if(Contains(array[i]))
         return(true);
   return(false);
  }
//+------------------------------------------------------------------+
//| Determines whether a set and the specified collection contain the|
//| same elements.                                                   |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::SetEquals(ICollection<T>*collection)
  {
//--- check collection
   if(CheckPointer(collection)==POINTER_INVALID)
      return(false);
//--- check current set is equal specified collection
   if(collection==GetPointer(this))
      return(true);
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
bool CHashSet::SetEquals(T &array[])
  {
//--- check size
   if(ArraySize(array)!=m_count)
      return(false);
//--- check current set is equal specified array
   for(int i=0; i<ArraySize(array); i++)
      if(!Contains(array[i]))
         return(false);
   return(true);
  }
//+------------------------------------------------------------------+
//| Set the underlying buckets array to size new_size and rehash.    |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::SetCapacity(const int new_size,bool new_hash_codes)
  {
//--- resize slots
   ArrayResize(m_slots,new_size);
//--- restore slots
   if(new_hash_codes)
      for(int i=0; i<m_last_index; i++)
         if(m_slots[i].hash_code!=-1)
            m_slots[i].hash_code=m_comparer.HashCode(m_slots[i].value)&0x7FFFFFFF;
//--- resize buckets
   ArrayResize(m_buckets,new_size);
   ArrayFill(m_buckets,0,new_size,0);
//--- restore buckets
   for(int i=0; i<m_last_index; i++)
     {
      int bucket=m_slots[i].hash_code%new_size;
      m_slots[i].next=m_buckets[bucket]-1;
      m_buckets[bucket]=i+1;
     }
  }
//+------------------------------------------------------------------+
//| Adds value to set if not contained already. Returns true if added|
//| and false if already present.                                    |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::AddIfNotPresent(T value)
  {
//--- set minimum capacity
   if(ArraySize(m_buckets)==0)
      Initialize(0);
//--- get hash code and bucket for value
   int hash_code=m_comparer.HashCode(value)&0x7FFFFFFF;
   int bucket=hash_code%ArraySize(m_buckets);
//--- check value already in the set
   for(int i=m_buckets[hash_code%ArraySize(m_buckets)]-1; i>=0; i=m_slots[i].next)
      if(m_slots[i].hash_code==hash_code && m_comparer.Equals(m_slots[i].value,value))
         return(false);
//--- calculate index for value
   int index=0;
   if(m_free_list>=0)
     {
      index=m_free_list;
      m_free_list=m_slots[index].next;
     }
   else
     {
      if(m_last_index==ArraySize(m_slots))
        {
         int new_size=CPrimeGenerator::ExpandPrime(m_count);
         SetCapacity(new_size,false);
         bucket=hash_code%ArraySize(m_buckets);
        }
      index=m_last_index;
      m_last_index++;
     }
//--- set value
   m_slots[index].hash_code=hash_code;
   m_slots[index].value=value;
   m_slots[index].next=m_buckets[bucket]-1;
   m_buckets[bucket]=index+1;
//--- increase count
   m_count++;
   return(true);
  }
//+------------------------------------------------------------------+
//| Initialize set with specified capacity.                          |
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::Initialize(const int capacity)
  {
   int size=CPrimeGenerator::GetPrime(capacity);
   ArrayResize(m_buckets,size);
   ArrayResize(m_slots,size);
   ZeroMemory(m_buckets);
   ZeroMemory(m_slots);
  }
//+------------------------------------------------------------------+
//| Modifies the current set to contain only elements that are       |
//| present either in that set or in the specified set, but not both.|
//+------------------------------------------------------------------+
template<typename T>
void CHashSet::InternalSymmetricExceptWith(CHashSet<T>*set)
  {
   for(int i=0; i<ArraySize(set.m_slots); i++)
     {
      T item=set.m_slots[i].value;
      if(!Remove(item))
         AddIfNotPresent(item);
     }
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a subset of the specified set.       |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::InternalIsSubsetOf(CHashSet<T>*set)
  {
//--- if this has more elements then it can't be a subset
   if(m_count>set.m_count)
      return(false);
//--- try to find any elements from current set in specified set
   for(int i=0; i<m_count; i++)
     {
      T item=m_slots[i].value;
      if(!set.Contains(item))
         return(false);
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a superset of the specified set.     |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::InternalIsSupersetOf(CHashSet<T>*set)
  {
//--- if this has less elements then it can't be a superset
   if(set.m_count>m_count)
      return(false);
//--- try to find any elements from specified set in current set
   for(int i=0; i<set.m_count; i++)
     {
      T item=set.m_slots[i].value;
      if(!Contains(item))
         return(false);
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper subset of the specified set.|
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::InternalIsProperSubsetOf(CHashSet<T>*set)
  {
//--- if this has more or equal elements then it can't be a proper subset
   if(m_count>=set.m_count)
      return(false);
//--- try to find any elements from current set in specified set
   for(int i=0; i<m_count; i++)
     {
      T item=m_slots[i].value;
      if(!set.Contains(item))
         return(false);
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| Determines whether a set is a proper superset of the specified   |
//| set.                                                             |
//+------------------------------------------------------------------+
template<typename T>
bool CHashSet::InternalIsProperSupersetOf(CHashSet<T>*set)
  {
//--- if this has less or equal elements then it can't be a proper superset
   if(m_count<=set.m_count)
      return(false);
//--- try to find any elements from specified set in current set
   for(int i=0; i<set.m_count; i++)
     {
      T item=set.m_slots[i].value;
      if(!Contains(item))
         return(false);
     }
   return(true);
  }
//+------------------------------------------------------------------+
