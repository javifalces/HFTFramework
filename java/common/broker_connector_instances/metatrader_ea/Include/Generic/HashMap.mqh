//+------------------------------------------------------------------+
//|                                                      HashMap.mqh |
//|                   Copyright 2016-2021, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\IMap.mqh>
#include <Generic\Interfaces\IEqualityComparer.mqh>
#include <Generic\Internal\DefaultEqualityComparer.mqh>
#include <Generic\Interfaces\IComparable.mqh>
#include <Generic\Internal\CompareFunction.mqh>
#include "HashSet.mqh"
//+------------------------------------------------------------------+
//| Struct Entry<TKey, TValue>.                                      |
//| Usage: Internal structure for organization CHashMap<T>.          |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
struct Entry: public Slot<TValue>
  {
public:
   TKey              key;
                     Entry(void): key((TKey)NULL) {}
  };
//+------------------------------------------------------------------+
//| Class CKeyValuePair<TKey, TValue>.                               |
//| Usage: Defines a key/value pair that can be set or retrieved.    |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
class CKeyValuePair: public IComparable<CKeyValuePair<TKey,TValue>*>
  {
protected:
   TKey              m_key;
   TValue            m_value;

public:
                     CKeyValuePair(void)                                              {   }
                     CKeyValuePair(TKey key,TValue value): m_key(key), m_value(value) {   }
                    ~CKeyValuePair(void)                                              {   }
   //--- methods to access protected data
   TKey              Key(void)           { return(m_key);   }
   void              Key(TKey key)       { m_key=key;       }
   TValue            Value(void)         { return(m_value); }
   void              Value(TValue value) { m_value=value;   }
   //--- method to create clone of current instance
   CKeyValuePair<TKey,TValue>*Clone(void) { return new CKeyValuePair<TKey,TValue>(m_key,m_value); }
   //--- method to compare keys
   int               Compare(CKeyValuePair<TKey,TValue>*pair) { return ::Compare(m_key,pair.m_key); }
   //--- method for determining equality
   bool              Equals(CKeyValuePair<TKey,TValue>*pair) { return ::Equals(m_key,pair.m_key); }
   //--- method to calculate hash code   
   int               HashCode(void) { return ::GetHashCode(m_key); }
  };
//+------------------------------------------------------------------+
//| Class CKeyValuePairComparer<TKey, TValue>.                       |
//| Usage: Provides a comparer class for convertation IComparer<TKey>|
//|        to the IComparer<CKeyValuePair<TKey, TValue>*> interface. |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
class CKeyValuePairComparer: public IComparer<CKeyValuePair<TKey,TValue>*>
  {
private:
   IComparer<TKey>*m_comparer;

public:
                     CKeyValuePairComparer(IComparer<TKey>*comaprer)                      { m_comparer=comaprer;                          }
   int               Compare(CKeyValuePair<TKey,TValue>* x,CKeyValuePair<TKey,TValue>* y) { return(m_comparer.Compare(x.Key(), y.Key())); }
  };
//+------------------------------------------------------------------+
//| Class CHashMap<TKey, TValue>.                                    |
//| Usage: Represents a collection of keys and values.               |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
class CHashMap: public IMap<TKey,TValue>
  {
protected:
   int               m_buckets[];
   Entry<TKey,TValue>m_entries[];
   int               m_count;
   int               m_capacity;
   int               m_free_list;
   int               m_free_count;
   IEqualityComparer<TKey>*m_comparer;
   bool              m_delete_comparer;

public:
                     CHashMap(void);
                     CHashMap(const int capacity);
                     CHashMap(IEqualityComparer<TKey>*comparer);
                     CHashMap(const int capacity,IEqualityComparer<TKey>*comparer);
                     CHashMap(IMap<TKey,TValue>*map);
                     CHashMap(IMap<TKey,TValue>*map,IEqualityComparer<TKey>*comparer);
                    ~CHashMap(void);
   //--- methods of filling data 
   bool              Add(CKeyValuePair<TKey,TValue>*pair);
   bool              Add(TKey key,TValue value);
   //--- methods of access to protected data
   int               Count(void)                                       { return(m_count-m_free_count); }
   IEqualityComparer<TKey>*Comparer(void)                        const { return(m_comparer);           }
   bool              Contains(CKeyValuePair<TKey,TValue>*item);
   bool              Contains(TKey key,TValue value);
   bool              ContainsKey(TKey key);
   bool              ContainsValue(TValue value);
   //--- methods of copy data from collection   
   int               CopyTo(CKeyValuePair<TKey,TValue>*&dst_array[],const int dst_start=0);
   int               CopyTo(TKey &dst_keys[],TValue &dst_values[],const int dst_start=0);
   //--- methods of cleaning and deleting
   void              Clear(void);
   bool              Remove(CKeyValuePair<TKey,TValue>*item);
   bool              Remove(TKey key);
   //--- method of access to the data
   bool              TryGetValue(TKey key,TValue &value);
   bool              TrySetValue(TKey key,TValue value);

private:
   void              Initialize(const int capacity);
   void              Resize(int new_size);
   int               FindEntry(TKey key);
   bool              Insert(TKey key,TValue value,const bool add);
   static int        m_collision_threshold;
  };
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashMap<TKey,TValue> class    |
//| that is empty, has the default initial capacity, and uses the    |
//| default equality comparer for the key type.                      |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::CHashMap(void): m_count(0),
                          m_free_list(0),
                          m_free_count(0),
                          m_capacity(0)
  {
//--- use default equality comaprer   
   m_comparer=new CDefaultEqualityComparer<TKey>();
   m_delete_comparer=true;
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashMap<TKey,TValue> class    |
//| that is empty, has the specified initial capacity, and uses the  |
//| default equality comparer for the key type.                      |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::CHashMap(const int capacity): m_count(0),
                                        m_free_list(0),
                                        m_free_count(0),
                                        m_capacity(0)
  {
//--- set capacity 
   if(capacity>0)
      Initialize(capacity);
//--- use default equality comaprer    
   m_comparer=new CDefaultEqualityComparer<TKey>();
   m_delete_comparer=true;
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashMap<TKey,TValue> class    |
//| that is empty, has the default initial capacity, and uses the    |
//| specified IEqualityComparer<TKey>.                               |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::CHashMap(IEqualityComparer<TKey>*comparer): m_count(0),
                                                      m_free_list(0),
                                                      m_free_count(0),
                                                      m_capacity(0)
  {
//--- check equality comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default equality comaprer    
      m_comparer=new CDefaultEqualityComparer<TKey>();
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
//| Initializes a new instance of the CHashMap<TKey,TValue> class    |
//| that is empty, has the specified initial capacity, and uses the  |
//| specified IEqualityComparer<TKey>.                               |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::CHashMap(const int capacity,IEqualityComparer<TKey>*comparer): m_count(0),
                                                                         m_free_list(0),
                                                                         m_free_count(0),
                                                                         m_capacity(0)
  {
   if(capacity>0)
      Initialize(capacity);
//--- check equality comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default equality comaprer    
      m_comparer=new CDefaultEqualityComparer<TKey>();
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
//| Initializes a new instance of the CHashMap<TKey,TValue> class    |
//| that contains elements copied from the specified                 |
//| IMap<TKey,TValue> and uses the default equality comparer for the |
//| key type.                                                        |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::CHashMap(IMap<TKey,TValue>*map): m_count(0),
                                           m_free_list(0),
                                           m_free_count(0),
                                           m_capacity(0)
  {
//--- use default equality comaprer    
   m_comparer=new CDefaultEqualityComparer<TKey>();
   m_delete_comparer=true;
//--- check map
   if(CheckPointer(map)!=POINTER_INVALID && map.Count()>0)
     {
      //--- set capacity 
      Initialize(map.Count());
      TKey keys[];
      TValue values[];
      map.CopyTo(keys,values);
      //--- copy all keys and values from specified map to current map      
      for(int i=0; i<map.Count(); i++)
         Add(keys[i],values[i]);
     }
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CHashMap<TKey,TValue> class    |
//| that contains elements copied from the specified                 |
//| IMap<TKey,TValue> and uses the specified IEqualityComparer<TKey>.|
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::CHashMap(IMap<TKey,TValue>*map,IEqualityComparer<TKey>*comparer): m_count(0),
                                                                            m_free_list(0),
                                                                            m_free_count(0),
                                                                            m_capacity(0)
  {
//--- check equality comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default equality comaprer    
      m_comparer=new CDefaultEqualityComparer<TKey>();
      m_delete_comparer=true;
     }
   else
     {
      //--- use specified equality comaprer
      m_comparer=comparer;
      m_delete_comparer=false;
     }
//--- check map
   if(CheckPointer(map)!=POINTER_INVALID && map.Count()>0)
     {
      //--- set capacity 
      Initialize(map.Count());
      TKey keys[];
      TValue values[];
      map.CopyTo(keys,values);
      //--- copy all keys and values from specified map to current map      
      for(int i=0; i<map.Count(); i++)
         Add(keys[i],values[i]);
     }
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CHashMap::~CHashMap(void)
  {
   if(m_delete_comparer)
      delete m_comparer;
  }
//+------------------------------------------------------------------+
//| Adds the specified key-value pair to the map.                    |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Add(CKeyValuePair<TKey,TValue>*pair)
  {
//--- check pair
   if(CheckPointer(pair)==POINTER_INVALID)
      return(false);
   return Add(pair.Key(),pair.Value());
  }
//+------------------------------------------------------------------+
//| Adds the specified key and value to the map.                     |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Add(TKey key,TValue value)
  {
   return Insert(key,value,true);
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified key-value pair.|
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Contains(CKeyValuePair<TKey,TValue>*item)
  {
//--- check pair
   if(CheckPointer(item)==POINTER_INVALID)
      return(false);
//--- find pair with specified key
   int i=FindEntry(item.Key());
//--- create default equality value comparer
   CDefaultEqualityComparer<TValue>comparer;
//--- check value is equal value from the found pair      
   if(i>=0 && comparer.Equals(m_entries[i].value,item.Value()))
      return(true);
   else
      return(false);
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified key with value.|
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Contains(TKey key,TValue value)
  {
//--- find pair with specified key
   int i=FindEntry(key);
//--- create default equality value comparer
   CDefaultEqualityComparer<TValue>comparer;
//--- check value is equal value from the found pair      
   if(i>=0 && comparer.Equals(m_entries[i].value,value))
      return(true);
   else
      return(false);
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified key.           |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::ContainsKey(TKey key)
  {
   return(FindEntry(key)>=0);
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified value.         |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::ContainsValue(TValue value)
  {
//--- create default equality value comparer
   CDefaultEqualityComparer<TValue>comparer_value();
//--- try to find pair contains specified value
   for(int i=0; i<m_count; i++)
      if(m_entries[i].hash_code>=0 && comparer_value.Equals(m_entries[i].value,value))
         return(true);
   return(false);
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the map to a compatible          |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
int CHashMap::CopyTo(CKeyValuePair<TKey,TValue>*&dst_array[],const int dst_start=0)
  {
//--- resize array
   if(dst_start+m_count>ArraySize(dst_array))
      ArrayResize(dst_array,dst_start+m_count);
//--- start copy
   int index=0;
   for(int i=0; i<ArraySize(m_entries); i++)
      if(m_entries[i].hash_code>=0)
        {
         //--- check indexes
         if(dst_start+index>=ArraySize(dst_array) || index>=m_count)
            return(index);
         dst_array[dst_start+index++]=new CKeyValuePair<TKey,TValue>(m_entries[i].key,m_entries[i].value);
        }
   return(index);
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the map to a compatible          |
//| one-dimensionals keys and values arrays.                         |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
int CHashMap::CopyTo(TKey &dst_keys[],TValue &dst_values[],const int dst_start=0)
  {
   int count=m_count-m_free_count;
//--- resize keys array
   if(dst_start+count>ArraySize(dst_keys))
      ArrayResize(dst_keys,dst_start+count);
//--- resize values array
   if(dst_start+count>ArraySize(dst_values))
      ArrayResize(dst_values,MathMin(ArraySize(dst_keys),dst_start+count));
//--- start copy
   int index=0;
   for(int i=0; i<ArraySize(m_entries); i++)
      if(m_entries[i].hash_code>=0)
        {
         //--- check indexes
         if(dst_start+index>=ArraySize(dst_keys) || dst_start+index>=ArraySize(dst_values) || index>=count)
            return(index);
         dst_keys[dst_start+index]=m_entries[i].key;
         dst_values[dst_start+index]=m_entries[i].value;
         index++;
        }
   return(index);
  }
//+------------------------------------------------------------------+
//| Removes all keys and values from the map.                        |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
void CHashMap::Clear(void)
  {
//--- check count
   if(m_count>0)
     {
      ArrayFill(m_buckets,0,m_capacity,-1);
      ArrayFree(m_entries);
      m_count=0;
      m_free_list=-1;
      m_free_count=0;
     }
  }
//+------------------------------------------------------------------+
//| Removes the specified key-value pair from map.                   |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Remove(CKeyValuePair<TKey,TValue>*item)
  {
//--- check pair
   if(CheckPointer(item)==POINTER_INVALID)
      return(false);
//--- find pair with specified key
   int i=FindEntry(item.Key());
//--- create default equality value comparer
   CDefaultEqualityComparer<TValue>comparer_value();
//--- remove pair   
   if(i>=0 && comparer_value.Equals(m_entries[i].value,item.Value()))
      return Remove(item.Key());
   return(false);
  }
//+------------------------------------------------------------------+
//| Removes the value with the specified key from the map.           |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Remove(TKey key)
  {
   if(m_capacity!=0)
     {
      int hash_code=m_comparer.HashCode(key)&0x7FFFFFFF;
      int bucket=hash_code%m_capacity;
      int last=-1;
      //--- search pair with specified key
      for(int i=m_buckets[bucket]; i>=0; last=i,i=m_entries[i].next)
        {
         if(m_entries[i].hash_code==hash_code && m_comparer.Equals(m_entries[i].key,key))
           {
            if(last<0)
               m_buckets[bucket]=m_entries[i].next;
            else
               m_entries[last].next=m_entries[i].next;
            //--- remove pair
            m_entries[i].hash_code=-1;
            m_entries[i].next=m_free_list;
            m_entries[i].key=(TKey)NULL;
            m_entries[i].value=(TValue)NULL;
            //--- incremet free count
            m_free_list=i;
            m_free_count++;
            return(true);
           }
        }
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Gets the value associated with the specified key.                |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::TryGetValue(TKey key,TValue &value)
  {
//--- find pair with specified key
   int i=FindEntry(key);
//--- check index
   if(i>=0)
     {
      //--- get value     
      value=m_entries[i].value;
      return(true);
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Sets the value associated with the specified key.                |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::TrySetValue(TKey key,TValue value)
  {
   return Insert(key, value, false);
  }
//+------------------------------------------------------------------+
//| Initialize map with specified capacity.                          |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
void CHashMap::Initialize(const int capacity)
  {
   m_capacity=CPrimeGenerator::GetPrime(capacity);
   ArrayResize(m_buckets,m_capacity);
   ArrayFill(m_buckets,0,m_capacity,-1);
   ArrayResize(m_entries,m_capacity);
   m_free_list=-1;
  }
//+------------------------------------------------------------------+
//| Resize map.                                                      |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
void CHashMap::Resize(const int new_size)
  {
//--- resize buckets
   ArrayResize(m_buckets,new_size);
   ArrayFill(m_buckets,0,new_size,-1);
//--- resize entries
   ArrayResize(m_entries,new_size);
//--- restore buckets
   for(int i=0; i<m_count; i++)
      if(m_entries[i].hash_code>=0)
        {
         int bucket=m_entries[i].hash_code%new_size;
         m_entries[i].next = m_buckets[bucket];
         m_buckets[bucket] = i;
        }
//--- restore capacity
   m_capacity=new_size;
  }
//+------------------------------------------------------------------+
//| Find index of entry with specified key.                          |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
int CHashMap::FindEntry(TKey key)
  {
   if(m_capacity!=NULL)
     {
      //--- get hash code from key
      int hash_code=m_comparer.HashCode(key)&0x7FFFFFFF;
      //--- search pair with specified key
      for(int i=m_buckets[hash_code%m_capacity]; i>=0; i=m_entries[i].next)
         if(m_entries[i].hash_code==hash_code && m_comparer.Equals(m_entries[i].key,key))
            return(i);
     }
   return(-1);
  }
//+------------------------------------------------------------------+
//| Insert the value with the specified key from the map.            |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CHashMap::Insert(TKey key,TValue value,const bool add)
  {
   if(m_capacity==0)
      Initialize(0);
//--- get hash code from key
   int hash_code=m_comparer.HashCode(key)&0x7FFFFFFF;
   int target_bucket=hash_code%m_capacity;
//--- collisions count in one bucket with different hashes
   int collision_count=0;
//--- search pair with specified key
   for(int i=m_buckets[target_bucket]; i>=0; i=m_entries[i].next)
     {
      //--- hash compare      
      if(m_entries[i].hash_code!=hash_code)
        {
         collision_count++;
         continue;
        }
      //--- value compare     
      if(m_comparer.Equals(m_entries[i].key,key))
        {
         //--- adding duplicate
         if(add)
            return(false);
         m_entries[i].value=value;
         return(true);
        }
     }
//--- check collision
   if(collision_count>=m_collision_threshold)
     {
      int new_size=CPrimeGenerator::ExpandPrime(m_count);
      Resize(new_size);
     }
//--- calculate index
   int index;
   if(m_free_count>0)
     {
      index=m_free_list;
      m_free_list=m_entries[index].next;
      m_free_count--;
     }
   else
     {
      if(m_count==ArraySize(m_entries))
        {
         int new_size=CPrimeGenerator::ExpandPrime(m_count);
         Resize(new_size);
         target_bucket=hash_code%new_size;
        }
      index=m_count;
      m_count++;
     }
//--- set pair
   m_entries[index].hash_code=hash_code;
   m_entries[index].next=m_buckets[target_bucket];
   m_entries[index].key=key;
   m_entries[index].value=value;
   m_buckets[target_bucket]=index;
   return(true);
  }
template<typename TKey,typename TValue>
static int CHashMap::m_collision_threshold=8;
//+------------------------------------------------------------------+