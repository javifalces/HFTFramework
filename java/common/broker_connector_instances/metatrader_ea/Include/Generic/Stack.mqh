//+------------------------------------------------------------------+
//|                                                        Stack.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Interfaces\ICollection.mqh>
#include <Generic\Internal\ArrayFunction.mqh>
#include <Generic\Internal\EqualFunction.mqh>
//+------------------------------------------------------------------+
//| Class CStack<T>.                                                 |
//| Usage: Represents a variable size last-in-first-out (LIFO)       |
//|        collection of instances of the same specified type.       |
//+------------------------------------------------------------------+
template<typename T>
class CStack: public ICollection<T>
  {
protected:
   T                 m_array[];
   int               m_size;
   const int         m_default_capacity;

public:
                     CStack(void);
                     CStack(const int capacity);
                     CStack(ICollection<T>&collection[]);
                     CStack(T &array[]);
                    ~CStack(void);
   //--- methods of filling data 
   bool              Add(T value);
   bool              Push(T value);
   //--- methods of access to protected data
   int               Count(void);
   bool              Contains(T item);
   void              TrimExcess(void);
   //--- methods of copy data from collection   
   int               CopyTo(T &dst_array[],const int dst_start=0);
   //--- methods of cleaning and removing
   void              Clear(void);
   bool              Remove(T item);   
   //--- methods of access to protected data
   T                 Peek(void);
   T                 Pop(void);
  };
//+------------------------------------------------------------------+
//| Initializes a new instance of the CStack<T> class that is empty  |
//| and has the default initial capacity.                            |
//+------------------------------------------------------------------+
template<typename T>
CStack::CStack(void): m_default_capacity(4),
                      m_size(0)
  {
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CStack<T> class that is empty  |
//| and has the specified initial capacity or the default initial    |
//| capacity, whichever is greater.                                  |
//+------------------------------------------------------------------+
template<typename T>
CStack::CStack(const int capacity): m_default_capacity(4),
                                    m_size(0)
  {
   ArrayResize(m_array,capacity);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CStack<T> class that contains  |
//| elements copied from the specified array and has sufficient      |
//| capacity to accommodate the number of elements copied.           |
//+------------------------------------------------------------------+
template<typename T>
CStack::CStack(T &array[]): m_default_capacity(4),
                            m_size(0)
  {
   m_size=ArrayCopy(m_array,array);
  }
//+------------------------------------------------------------------+
//| Initializes a new instance of the CStack<T> class that contains  |
//| elements copied from the specified collection and has sufficient |
//| capacity to accommodate the number of elements copied.           |
//+------------------------------------------------------------------+
template<typename T>
CStack::CStack(ICollection<T>*collection): m_default_capacity(4),
                                           m_size(0)
  {
//--- check collection   
   if(CheckPointer(collection)!=POINTER_INVALID)
      m_size=collection.CopyTo(m_array,0);
  }
//+------------------------------------------------------------------+
//| Destructor.                                                      |
//+------------------------------------------------------------------+
template<typename T>
CStack::~CStack(void)
  {
  }
//+------------------------------------------------------------------+
//| Inserts an value at the top of the CStack<T>.                    |
//+------------------------------------------------------------------+
template<typename T>
bool CStack::Add(T value)
  {
   return Push(value);
  }
//+------------------------------------------------------------------+
//| Gets the number of elements.                                     |
//+------------------------------------------------------------------+
template<typename T>
int CStack::Count(void)
  {
   return(m_size);
  }
//+------------------------------------------------------------------+
//| Removes all values from the CStack<T>.                           |
//+------------------------------------------------------------------+
template<typename T>
bool CStack::Contains(T item)
  {
   int count=m_size;
//--- try to find item in array
   while(count-->0)
     {
      //--- use default equality function
      if(::Equals(m_array[count],item))
         return(true);
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Copies a range of elements from the stack to a compatible        |
//| one-dimensional array.                                           |
//+------------------------------------------------------------------+
template<typename T>
int CStack::CopyTo(T &dst_array[],const int dst_start=0)
  {
//--- resize array
   if(dst_start+m_size>ArraySize(dst_array))
      ArrayResize(dst_array,dst_start+m_size);
//--- start copy
   int src_index = m_size-1;
   int dst_index = dst_start;
   while(src_index>=0 && dst_index<ArraySize(dst_array))
      dst_array[dst_index++]=m_array[src_index--];
   return(dst_index-dst_start);
  }
//+------------------------------------------------------------------+
//| Removes all values from the CStack<T>.                           |
//+------------------------------------------------------------------+
template<typename T>
void CStack::Clear(void)
  {
//--- check current size
   if(m_size>0)
     {
      ZeroMemory(m_array);
      m_size=0;
     }
  }
//+------------------------------------------------------------------+
//| Removes the first occurrence of a specific value from the stack. |
//+------------------------------------------------------------------+
template<typename T>
bool CStack::Remove(T item)
  {
//--- find index of item
   int index=ArrayIndexOf(m_array,item,0,m_size);
//--- check index
   if(index==-1)
      return(false);
//--- shift the values to the left
   ArrayCopy(m_array,m_array,index,index+1);
//--- decrement size
   m_size--;
   return(true);
  }
//+------------------------------------------------------------------+
//| Inserts an values at the top of the CStack<T>.                   |
//+------------------------------------------------------------------+
template<typename T>
bool CStack::Push(T value)
  {
   int size=ArraySize(m_array);
//--- check array size
   if(m_size==size)
     {
      //--- increase capacity
      if(size==0)
         ArrayResize(m_array,m_default_capacity);
      else
         ArrayResize(m_array,2*size);
     }
//--- add value to the end
   m_array[m_size++]=value;
   return(true);
  }
//+------------------------------------------------------------------+
//| Returns the value at the top of the CStack<T> without removing.  |
//+------------------------------------------------------------------+
template<typename T>
T CStack::Peek(void)
  {
//--- return last value 
   return(m_array[m_size-1]);
  }
//+------------------------------------------------------------------+
//| Removes and returns the value at the top of the CStack<T>.       |
//+------------------------------------------------------------------+
template<typename T>
T CStack::Pop(void)
  {
//--- return last value and decrement size
   T item=m_array[--m_size];
   return(item);
  }
//+------------------------------------------------------------------+
//| Sets the capacity to the actual number of elements in the        |
//| CStack<T>, if that number is less than 90 percent of current     |
//| capacity.                                                        |
//+------------------------------------------------------------------+
template<typename T>
void CStack::TrimExcess(void)
  {
//--- calculate threshold value
   int threshold=(int)(((double)ArraySize(m_array)*0.9));
//--- calculate resize array
   if(m_size<threshold)
      ArrayResize(m_array,m_size);
  }
//+------------------------------------------------------------------+
