//+------------------------------------------------------------------+
//|                                                   LinkedList.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\ICollection.mqh>
#include <Generic\Internal\EqualFunction.mqh>
//+------------------------------------------------------------------+
//| Class CLinkedListNode<T>.                                        |
//| Usage: Represents a node of linked list.                         |
//+------------------------------------------------------------------+
template<typename T>
class CLinkedListNode
  {
protected:
   CLinkedList<T>*m_list;
   CLinkedListNode<T>*m_next;
   CLinkedListNode<T>*m_prev;
   T                 m_item;

public:
                     CLinkedListNode(T value): m_item(value) { }
                     CLinkedListNode(CLinkedList<T>*list,T value): m_list(list),m_item(value) { }
                    ~CLinkedListNode(void) { }
   //--- methods of access to protected data
   CLinkedList<T>*   List(void)                         { return(m_list); }
   void              List(CLinkedList<T>*value)         { m_list=value;   }
   CLinkedListNode<T>*Next(void)                        { return(m_next); }
   void              Next(CLinkedListNode<T>*value)     { m_next=value;   }
   CLinkedListNode<T>*Previous(void)                    { return(m_prev); }
   void              Previous(CLinkedListNode<T>*value) { m_prev=value;   }
   T                 Value(void)                        { return(m_item); }
   void              Value(T value)                     { m_item=value;   }
  };
//+------------------------------------------------------------------+
//| Class CLinkedList<T>.                                            |
//| Usage: Represents a doubly linked list.                          |
//+------------------------------------------------------------------+
template<typename T>
class CLinkedList: public ICollection<T>
  {
protected:
   CLinkedListNode<T>*m_head;
   int               m_count;

public:
                     CLinkedList(void);
                     CLinkedList(ICollection<T>*collection);
                     CLinkedList(T &array[]);
                    ~CLinkedList(void);
   //--- methods of filling data 
   bool              Add(T value);
   CLinkedListNode<T>*AddAfter(CLinkedListNode<T>*node,T value);
   bool              AddAfter(CLinkedListNode<T>*node,CLinkedListNode<T>*new_node);
   CLinkedListNode<T>*AddBefore(CLinkedListNode<T>*node,T value);
   bool              AddBefore(CLinkedListNode<T>*node,CLinkedListNode<T>*new_node);
   CLinkedListNode<T>*AddFirst(T value);
   bool              AddFirst(CLinkedListNode<T>*node);
   CLinkedListNode<T>*AddLast(T value);
   bool              AddLast(CLinkedListNode<T>*node);
   //--- methods of access to protected data
   int               Count(void);
   CLinkedListNode<T>*Head(void) {return(m_head);}
   CLinkedListNode<T>*First(void);
   CLinkedListNode<T>*Last(void);
   bool              Contains(T item);
   //--- methods of copy data from collection   
   int               CopyTo(T &dst_array[],const int dst_start=0);
   //--- methods of cleaning and deleting
   void              Clear(void);
   bool              Remove(T item);
   bool              Remove(CLinkedListNode<T>*node);
   bool              RemoveFirst(void);
   bool              RemoveLast(void);
   //--- method for searching
   CLinkedListNode<T>*Find(T value);
   CLinkedListNode<T>*FindLast(T value);

private:
   bool              ValidateNode(CLinkedListNode<T>*node);
   bool              ValidateNewNode(CLinkedListNode<T>*node);
   void              InternalInsertNodeBefore(CLinkedListNode<T>*node,CLinkedListNode<T>*new_node);
   void              InternalInsertNodeToEmptyList(CLinkedListNode<T>*new_node);
   void              InternalRemoveNode(CLinkedListNode<T>*node);
  };
//+------------------------------------------------------------------+
//| Initializes a new instance of the CLinkedList<T> class that is   |
//| empty.                                                           |
//+------------------------------------------------------------------+
template<typename T>
CLinkedList::CLinkedList(void): m_count(0)
  {
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CLinkedList<T> class that      |
//| contains elements copied from the specified array and has        |
//| sufficient capacity to accommodate the number of elements copied.|
//+------------------------------------------------------------------+
template<typename T>
CLinkedList::CLinkedList(T &array[]): m_count(0)
  {
   for(int i=0; i<ArraySize(array); i++)
      AddLast(array[i]);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CLinkedList<T> class that      |
//| contains elements copied from the specified collection and has   |
//| sufficient capacity to accommodate the number of elements copied.|
//+------------------------------------------------------------------+
template<typename T>
CLinkedList::CLinkedList(ICollection<T>*collection): m_count(0)
  {
//--- check collection
   if(CheckPointer(collection)!=POINTER_INVALID)
     {
      T array[];
      int size=collection.CopyTo(array,0);
      for(int i=0; i<size; i++)
         AddLast(array[i]);
     }
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
template<typename T>
CLinkedList::~CLinkedList(void)
  {
   Clear();
  }
//+------------------------------------------------------------------+
//| Adds an value to the end of the list.                            |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::Add(T value)
  {
   return(CheckPointer(AddLast(value))!=POINTER_INVALID);
  }
//+------------------------------------------------------------------+
//| Adds a new node containing the specified value after the         |
//| specified existing node in the CLinkedList<T>.                   |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::AddAfter(CLinkedListNode<T>*node,T value)
  {
//--- check node
   if(!ValidateNode(node))
      return(NULL);
//--- create new node
   CLinkedListNode<T>*result=new CLinkedListNode<T>(node.List(),value);
//--- insert node to the list
   InternalInsertNodeBefore(node.Next(),result);
   return(result);
  }
//+------------------------------------------------------------------+
//| Adds the specified new node after the specified existing node in |
//| the LinkedList<T>.                                               |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::AddAfter(CLinkedListNode<T>*node,CLinkedListNode<T>*new_node)
  {
//--- check node
   if(!ValidateNode(node))
      return(false);
//--- check new node
   if(!ValidateNewNode(new_node))
      return(false);
//--- insert node to the list
   InternalInsertNodeBefore(node.Next(),new_node);
//--- set the current list as list for new node 
   new_node.List(GetPointer(this));
   return(true);
  }
//+------------------------------------------------------------------+
//| Adds a new node containing the specified value before the        |
//| specified existing node in the CLinkedList<T>.                   |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::AddBefore(CLinkedListNode<T>*node,T value)
  {
//--- check node
   if(!ValidateNode(node))
      return(NULL);
//--- create new node
   CLinkedListNode<T>*result=new CLinkedListNode<T>(node.List(),value);
//--- insert node to the list
   InternalInsertNodeBefore(node,result);
   if(node==m_head)
      m_head=result;
   return(result);
  }
//+------------------------------------------------------------------+
//| Adds the specified new node before the specified existing node in|
//| the LinkedList<T>.                                               |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::AddBefore(CLinkedListNode<T>*node,CLinkedListNode<T>*new_node)
  {
//--- check node
   if(!ValidateNode(node))
      return(false);
//--- check new node
   if(!ValidateNewNode(new_node))
      return(false);
//--- insert node to the list
   InternalInsertNodeBefore(node,new_node);
//--- set the current list as list for new node 
   new_node.List(GetPointer(this));
   if(node==m_head)
      m_head=new_node;
   return(true);
  }
//+------------------------------------------------------------------+
//| Adds a new node containing the specified value at the start of   |
//| the CLinkedList<T>.                                              |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::AddFirst(T value)
  {
//--- create new node
   CLinkedListNode<T>*node=new CLinkedListNode<T>(GetPointer(this),value);
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
     {
      //--- insert node to the empty list   
      InternalInsertNodeToEmptyList(node);
     }
   else
     {
      //--- insert node to the list    
      InternalInsertNodeBefore(m_head,node);
      m_head=node;
     }
   return(node);
  }
//+------------------------------------------------------------------+
//| Adds the specified new node at the start of the CLinkedList<T>.  |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::AddFirst(CLinkedListNode<T>*node)
  {
//--- check node
   if(!ValidateNewNode(node))
      return(false);
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
     {
      //--- insert node to the empty list   
      InternalInsertNodeToEmptyList(node);
     }
   else
     {
      //--- insert node to the list    
      InternalInsertNodeBefore(m_head,node);
      m_head=node;
     }
//--- set the current list as list for node 
   node.List(GetPointer(this));
   return(true);
  }
//+------------------------------------------------------------------+
//| Adds a new node containing the specified value at the end of the |
//| CLinkedList<T>.                                                  |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::AddLast(T value)
  {
//--- create new node
   CLinkedListNode<T>*node=new CLinkedListNode<T>(GetPointer(this),value);
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
     {
      //--- insert node to the empty list   
      InternalInsertNodeToEmptyList(node);
     }
   else
     {
      //--- insert node to the list    
      InternalInsertNodeBefore(m_head,node);
     }
   return(node);
  }
//+------------------------------------------------------------------+
//| Adds the specified new node at the end of the CLinkedList<T>.    |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::AddLast(CLinkedListNode<T>*node)
  {
//--- check node
   if(!ValidateNewNode(node))
      return(false);
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
     {
      //--- insert node to the empty list      
      InternalInsertNodeToEmptyList(node);
     }
   else
     {
      //--- insert node to the list      
      InternalInsertNodeBefore(m_head,node);
     }
//--- set the current list as list for node 
   node.List(GetPointer(this));
   return(true);
  }
//+------------------------------------------------------------------+
//| Determines whether an element is in the linked list.             |
//+------------------------------------------------------------------+
template<typename T>
int CLinkedList::Count(void)
  {
   return(m_count);
  }
//+------------------------------------------------------------------+
//| Gets the first node of the CLinkedList<T>.                       |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::First(void)
  {
   return(m_head);
  }
//+------------------------------------------------------------------+
//| Gets the last node of the CLinkedList<T>.                        |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::Last(void)
  {
   return(CheckPointer(m_head)!=POINTER_INVALID ? m_head.Previous() : NULL);
  }
//+------------------------------------------------------------------+
//| Determines whether a value is in the CLinkedList<T>.             |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::Contains(T item)
  {
   return(CheckPointer(Find(item))!=POINTER_INVALID);
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the linkedlist to a compatible   |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename T>
int CLinkedList::CopyTo(T &dst_array[],const int dst_start=0)
  {
//--- resize array
   if(dst_start+m_count>ArraySize(dst_array))
      ArrayResize(dst_array,dst_start+m_count);
//--- check start index
   if(dst_start>ArraySize(dst_array))
      return(0);
//--- start copy
   CLinkedListNode<T>*node=m_head;
   if(CheckPointer(node)!=POINTER_INVALID)
     {
      int dst_index=dst_start;
      do
        {
         dst_array[dst_index++]=node.Value();
         node=node.Next();
        }
      while(dst_index<ArraySize(dst_array) && node!=m_head);
      return(dst_index-dst_start);
     }
//--- list is empty
   return(0);
  }
//+------------------------------------------------------------------+
//| Removes all nodes from the CLinkedList<T>.                       |
//+------------------------------------------------------------------+
template<typename T>
void CLinkedList::Clear(void)
  {
//--- check count
   if(m_count>0)
     {
      //--- check head node   
      if(CheckPointer(m_head)!=POINTER_INVALID)
        {
         while(m_head.Next()!=m_head)
           {
            CLinkedListNode<T>*node=m_head.Next();
            m_head.Next(node.Next());
            delete node;
           }
         delete m_head;
        }
      //--- reset count
      m_count=0;
     }
  }
//+------------------------------------------------------------------+
//| Removes the first occurrence of the specified value from the     |
//| CLinkedList<T>.                                                  |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::Remove(T item)
  {
//--- find node with specified value
   CLinkedListNode<T>*node=Find(item);
   if(CheckPointer(node)!=POINTER_INVALID)
     {
      //--- remove node
      InternalRemoveNode(node);
      return(true);
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Removes the specified node from the LinkedList<T>.               |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::Remove(CLinkedListNode<T>*node)
  {
//--- check node
   if(ValidateNode(node))
     {
      //--- remove node
      InternalRemoveNode(node);
      return(true);
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Removes the node at the start of the CLinkedList<T>.             |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::RemoveFirst(void)
  {
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
      return(false);
//--- remove head node
   InternalRemoveNode(m_head);
   return(true);
  }
//+------------------------------------------------------------------+
//| Removes the node at the end of the CLinkedList<T>.               |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::RemoveLast(void)
  {
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
      return(false);
//--- remove last node
   InternalRemoveNode(m_head.Previous());
   return(true);
  }
//+------------------------------------------------------------------+
//| Finds the first node that contains the specified value.          |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::Find(T value)
  {
   CLinkedListNode<T>*node=m_head;
//--- start search specified value in the list
   if(CheckPointer(node)!=POINTER_INVALID)
     {
      do
        {
         //--- use default equals function
         if(::Equals(node.Value(),value))
            return(node);
         node=node.Next();
        }
      while(node!=m_head);
     }
   return(NULL);
  }
//+------------------------------------------------------------------+
//| Finds the last node that contains the specified value.           |
//+------------------------------------------------------------------+
template<typename T>
CLinkedListNode<T>*CLinkedList::FindLast(T value)
  {
//--- check head node
   if(CheckPointer(m_head)==POINTER_INVALID)
      return(NULL);
//--- get last node
   CLinkedListNode<T> *last = m_head.Previous();
   CLinkedListNode<T> *node = last;
//--- start search from the end of the list
   if(node!=NULL)
     {
      do
        {
         //--- use default equals function
         if(::Equals(node.Value(),value))
            return(node);
         node=node.Previous();
        }
      while(node!=last);
     }
   return(NULL);
  }
//+------------------------------------------------------------------+
//| Validation of node on not null and belongs in the current list.  |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::ValidateNode(CLinkedListNode<T>*node)
  {
   return(CheckPointer(node)!=POINTER_INVALID && node.List()==GetPointer(this));
  }
//+------------------------------------------------------------------+
//| Validation of new node on not null.                              |
//+------------------------------------------------------------------+
template<typename T>
bool CLinkedList::ValidateNewNode(CLinkedListNode<T>*node)
  {
   return(CheckPointer(node)!=POINTER_INVALID && node.List()==NULL);
  }
//+------------------------------------------------------------------+
//| Insert node before the specified node.                           |
//+------------------------------------------------------------------+
template<typename T>
void CLinkedList::InternalInsertNodeBefore(CLinkedListNode<T>*node,CLinkedListNode<T>*new_node)
  {
//--- set node befor the specified node
   new_node.Next(node);
   new_node.Previous(node.Previous());
   node.Previous().Next(new_node);
   node.Previous(new_node);
//--- increment count
   m_count++;
  }
//+------------------------------------------------------------------+
//| Add first node to the list.                                      |
//+------------------------------------------------------------------+
template<typename T>
void CLinkedList::InternalInsertNodeToEmptyList(CLinkedListNode<T>*new_node)
  {
//--- set node as head of the list
   new_node.Next(new_node);
   new_node.Previous(new_node);
   m_head=new_node;
//--- increment count
   m_count++;
  }
//+------------------------------------------------------------------+
//| Remove specified node from the list.                             |
//+------------------------------------------------------------------+
template<typename T>
void CLinkedList::InternalRemoveNode(CLinkedListNode<T>*node)
  {
//--- check node
   if(node.Next()==node)
     {
      //--- resets the head of the list
      m_head=NULL;
     }
   else
     {
      //--- detach node from the list
      node.Next().Previous(node.Previous());
      node.Previous().Next(node.Next());
      if(m_head==node)
         m_head=node.Next();
     }
//--- decrement count and delete node
   m_count--;
   delete node;
  }
//+------------------------------------------------------------------+
