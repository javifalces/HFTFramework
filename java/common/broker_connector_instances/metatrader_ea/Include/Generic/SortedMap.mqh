//+------------------------------------------------------------------+
//|                                                    SortedMap.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\IMap.mqh>
#include <Generic\Interfaces\IComparer.mqh>
#include <Generic\Internal\DefaultComparer.mqh>
#include <Generic\Internal\CompareFunction.mqh>
#include "HashMap.mqh"
#include "SortedSet.mqh"
//+------------------------------------------------------------------+
//| Class CSortedMap<TKey, TValue>.                                  |
//| Usage: Represents a collection of key/value pairs that are sorted|
//|        on the key.                                               |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
class CSortedMap: public IMap<TKey,TValue>
  {
protected:
   CRedBlackTree<CKeyValuePair<TKey,TValue>*>*m_tree;
   IComparer<TKey>*m_comparer;
   bool              m_delete_comparer;

public:
                     CSortedMap(void);
                     CSortedMap(IComparer<TKey>*comparer);
                     CSortedMap(IMap<TKey,TValue>*map);
                     CSortedMap(IMap<TKey,TValue>*map,IComparer<TKey>*comparer);
                    ~CSortedMap(void);
   //--- methods of filling data 
   bool              Add(CKeyValuePair<TKey,TValue>*value) { return m_tree.Add(value);     }
   bool              Add(TKey key,TValue value);
   //--- methods of access to protected data
   int               Count(void)                               { return m_tree.Count();        }
   bool              Contains(CKeyValuePair<TKey,TValue>*item) { return m_tree.Contains(item); }
   bool              Contains(TKey key,TValue value);
   bool              ContainsKey(TKey key);
   bool              ContainsValue(TValue value);
   IComparer<TKey>  *Comparer(void) const { return(m_comparer); }
   //--- methods of copy data from collection   
   int               CopyTo(CKeyValuePair<TKey,TValue>*&dst_array[],const int dst_start=0);
   int               CopyTo(TKey &dst_keys[],TValue &dst_values[],const int dst_start=0);
   //--- methods of cleaning and deleting
   void              Clear(void);
   bool              Remove(CKeyValuePair<TKey,TValue>*item) { return m_tree.Remove(item); }
   bool              Remove(TKey key);
   //--- method of access to the data
   bool              TryGetValue(TKey key,TValue &value);
   bool              TrySetValue(TKey key,TValue value);

private:
   static void       ClearNodes(CRedBlackTreeNode<CKeyValuePair<TKey,TValue>*>*node);
  };
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedMap<TKey,TValue> class  |
//| that is empty, has the default initial capacity, and uses the    |
//| default comparer for the key type.                              |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CSortedMap::CSortedMap(void)
  {
//--- use default comaprer      
   m_comparer=new CDefaultComparer<TKey>();
   m_delete_comparer=true;
   m_tree=new CRedBlackTree<CKeyValuePair<TKey,TValue>*>(new CKeyValuePairComparer<TKey,TValue>(m_comparer));
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedMap<TKey,TValue> class  |
//| that is empty, has the default initial capacity, and uses the    |
//| specified IComparer<TKey>.                                       |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CSortedMap::CSortedMap(IComparer<TKey>*comparer)
  {
//--- check comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default comaprer      
      m_comparer=new CDefaultComparer<TKey>();
      m_delete_comparer=true;
     }
   else
     {
      //--- use specified comaprer
      m_comparer=comparer;
      m_delete_comparer=false;
     }
   m_tree=new CRedBlackTree<CKeyValuePair<TKey,TValue>*>(new CKeyValuePairComparer<TKey,TValue>(m_comparer));
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedMap<TKey,TValue> class  |
//| that contains elements copied from the specified                 |
//| IMap<TKey,TValue> and uses the default comparer for the key type.|
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CSortedMap::CSortedMap(IMap<TKey,TValue>*map)
  {
//--- use default comaprer      
   m_comparer=new CDefaultComparer<TKey>();
   m_delete_comparer=true;
   m_tree=new CRedBlackTree<CKeyValuePair<TKey,TValue>*>(map,new CKeyValuePairComparer<TKey,TValue>(m_comparer));
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CSortedMap<TKey,TValue> class  |
//| that contains elements copied from the specified                 |
//| IMap<TKey,TValue> and uses the specified IComparer<TKey>.        |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CSortedMap::CSortedMap(IMap<TKey,TValue>*map,IComparer<TKey>*comparer)
  {
//--- check comaprer
   if(CheckPointer(comparer)==POINTER_INVALID)
     {
      //--- use default comaprer      
      m_comparer=new CDefaultComparer<TKey>();
      m_delete_comparer=true;
     }
   else
     {
      //--- use specified comaprer
      m_comparer=comparer;
      m_delete_comparer=false;
     }
   m_tree=new CRedBlackTree<CKeyValuePair<TKey,TValue>*>(map,new CKeyValuePairComparer<TKey,TValue>(m_comparer));
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
CSortedMap::~CSortedMap(void)
  {
//--- delete comparer
   if(m_delete_comparer)
      delete m_comparer;
//--- delete tree comparer
   delete m_tree.Comparer();
//--- delete nodes values      
   ClearNodes(m_tree.Root());
//--- delete tree and nodes
   delete m_tree;
  }
//+------------------------------------------------------------------+
//| Walk all nodes of tree and delete their value.                   |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
static void CSortedMap::ClearNodes(CRedBlackTreeNode<CKeyValuePair<TKey,TValue>*>*node)
  {
//--- check node
   if(CheckPointer(node)==POINTER_INVALID)
      return;
//--- walk of a right subtree
   if(!node.Right().IsLeaf())
      ClearNodes(node.Right());
//--- delete value   
   delete node.Value();
//--- walk of a left subtree
   if(!node.Left().IsLeaf())
      ClearNodes(node.Left());
  }
//+------------------------------------------------------------------+
//| Adds the specified key and value to the map.                     |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::Add(TKey key,TValue value)
  {
//--- create pair  
   CKeyValuePair<TKey,TValue>*pair=new CKeyValuePair<TKey,TValue>(key,value);
//--- add pair to tree   
   bool success=m_tree.Add(pair);
//--- if addition was not successful delte pair
   if(!success)
      delete pair;
   return(success);
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified key with value.|
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::Contains(TKey key,TValue value)
  {
//--- find node with specified key
   CKeyValuePair<TKey,TValue>pair(key,NULL);
   CRedBlackTreeNode<CKeyValuePair<TKey,TValue>*>*node=m_tree.Find(GetPointer(pair));
//--- create value comparer
   CDefaultEqualityComparer<TValue>comaprer;
//--- determine whether the finding node contains specified value
   if(CheckPointer(node)!=POINTER_INVALID && comaprer.Equals(value,node.Value().Value()))
      return(true);
   return(false);
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified key.           |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::ContainsKey(TKey key)
  {
//--- crete pair
   CKeyValuePair<TKey,TValue>pair(key,NULL);
//--- determines whether the tree contains the pair.
   return m_tree.Contains(GetPointer(pair));
  }
//+------------------------------------------------------------------+
//| Determines whether the map contains the specified value.         |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::ContainsValue(TValue value)
  {
//--- copy all pairs in array
   CKeyValuePair<TKey,TValue>*array[];
   int count=m_tree.CopyTo(array);
//--- create value comparer
   CDefaultEqualityComparer<TValue>comaprer;
//--- determines whether the array contains the specified value
   for(int i=0; i<count; i++)
      if(comaprer.Equals(value,array[i].Value()))
         return(true);
   return(false);
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the map to a compatible          |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
int CSortedMap::CopyTo(CKeyValuePair<TKey,TValue>*&dst_array[],const int dst_start=0)
  {
   int result=m_tree.CopyTo(dst_array,dst_start);
   if(result>0)
     {
      //--- create clones for each pair
      for(int i=0; i<result; i++)
         dst_array[dst_start+i]=dst_array[dst_start+i].Clone();
     }
   return(result);
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the map to a compatible          |
//| one-dimensionals keys and values arrays.                         |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
int CSortedMap::CopyTo(TKey &dst_keys[],TValue &dst_values[],const int dst_start=0)
  {
//--- create array and copy all values from tree to there
   CKeyValuePair<TKey,TValue>*array[];
   int count=m_tree.CopyTo(array);
//--- check real cout
   if(count>0)
     {
      //--- resize keys array
      if(dst_start+count>ArraySize(dst_keys))
         ArrayResize(dst_keys,dst_start+count);
      //--- resize values array
      if(dst_start+count>ArraySize(dst_values))
         ArrayResize(dst_values,MathMin(ArraySize(dst_keys),dst_start+count));
      //--- start copy
      int index=0;
      while(index<count && dst_start+index<ArraySize(dst_keys) && dst_start+index<ArraySize(dst_values))
        {
         dst_keys[dst_start+index]=array[index].Key();
         dst_values[dst_start+index]=array[index].Value();
         index++;
        }
      return(index);
     }
   return(0);
  }
//+------------------------------------------------------------------+
//| Clear and delete all values from map.                            |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
void CSortedMap::Clear(void)
  {
//--- check count
   if(m_tree.Count()>0)
     {
      //--- delete nodes values      
      ClearNodes(m_tree.Root());
      //--- claer th tree
      m_tree.Clear();
     }
  }
//+------------------------------------------------------------------+
//| Removes the value with the specified key from the map.           |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::Remove(TKey key)
  {
//--- create pair with specified key
   CKeyValuePair<TKey,TValue>pair(key,NULL);
//--- find node
   CRedBlackTreeNode<CKeyValuePair<TKey,TValue>*>*node=m_tree.Find(GetPointer(pair));
//--- check node
   if(CheckPointer(node)!=POINTER_INVALID)
     {
      CKeyValuePair<TKey,TValue>*real_pair=node.Value();
      //--- remove node from tree
      if(m_tree.Remove(node))
        {
         //--- check and delete node value
         if(CheckPointer(real_pair)==POINTER_DYNAMIC)
            delete real_pair;
         return(true);
        }
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Gets the value associated with the specified key.                |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::TryGetValue(TKey key,TValue &value)
  {
//--- create pair with specified key
   CKeyValuePair<TKey,TValue>pair(key,NULL);
//--- find node with specified pair in the tree
   CRedBlackTreeNode<CKeyValuePair<TKey,TValue>*>*node=m_tree.Find(GetPointer(pair));
//--- check node
   if(CheckPointer(node)==POINTER_INVALID)
      return(false);
//--- get value   
   value=node.Value().Value();
   return(true);
  }
//+------------------------------------------------------------------+
//| Sets the value associated with the specified key.                |
//+------------------------------------------------------------------+
template<typename TKey,typename TValue>
bool CSortedMap::TrySetValue(TKey key,TValue value)
  {
//--- create pair with specified key
   CKeyValuePair<TKey,TValue>pair(key,NULL);
//--- find node with specified pair in the tree
   CRedBlackTreeNode<CKeyValuePair<TKey,TValue>*>*node=m_tree.Find(GetPointer(pair));
//--- check node
   if(CheckPointer(node)==POINTER_INVALID)
      return(false);
//--- set value   
   node.Value().Value(value);
   return(true);
  }
//+------------------------------------------------------------------+
