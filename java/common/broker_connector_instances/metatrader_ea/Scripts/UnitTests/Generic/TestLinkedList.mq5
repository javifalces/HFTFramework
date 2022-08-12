//+------------------------------------------------------------------+
//|                                               TestLinkedList.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\LinkedList.mqh>
#include <Generic\ArrayList.mqh>
//+------------------------------------------------------------------+
//| TestAddAfter_Node.                                               |
//+------------------------------------------------------------------+
bool TestAddAfter_Node(const int count)
  {
   CLinkedList<int>list_test();
//--- create test arrays
   int check_array[];
   int temp_items[];
   int head_items[];
   int head_reverse_items[];
   int tail_items[];
   ArrayResize(temp_items,count);
   ArrayResize(head_items,count);
   ArrayResize(head_reverse_items,count);
   ArrayResize(tail_items,count);
   for(int i=0; i<count; i++)
     {
      int index=count-1-i;
      int head = MathRand();
      int tail = MathRand();
      head_items[i]=head;
      head_reverse_items[index]=head;
      tail_items[i]=tail;
     }
//--- test verify value is -1     
//--- add values
   CLinkedListNode<int>*node=list_test.AddFirst(head_items[0]);
   list_test.AddAfter(node,-1);
//--- check
   list_test.CopyTo(check_array);
   if(check_array[0]!=head_items[0] && check_array[1]!=-1)
      return(false);
//--- clear
   list_test.Clear();

//--- test node in the head   
//--- add values
   list_test.AddFirst(head_items[0]);
   ArrayCopy(temp_items,head_items);
   ArrayReverse(temp_items,1,count-1);
   for(int i=1; i<count; i++)
      list_test.AddAfter(list_test.First(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node in the tail
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddAfter(list_test.Last(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,head_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is after the head
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   ArrayCopy(temp_items,head_items);
   ArrayReverse(temp_items,2,count-2);
   for(int i=2; i<count;++i)
      list_test.AddAfter(list_test.First().Next(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is before the tail
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   ArrayCopy(temp_items,head_items,1,2,count-2);
   temp_items[0]=head_items[0];
   temp_items[count-1]=head_items[1];
   for(int i=2; i<count;++i)
      list_test.AddAfter(list_test.Last().Previous(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is somewhere in the middle
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   ArrayCopy(temp_items,head_items,1,3,count-3);
   temp_items[0]=head_items[0];
   temp_items[count - 2] = head_items[1];
   temp_items[count - 1] = head_items[2];
   for(int i=3; i<count;++i)
      list_test.AddAfter(list_test.Last().Previous().Previous(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test call AddAfter several times remove some of the items
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddAfter(list_test.Last(),head_items[i]);
//--- remove values
   list_test.Remove(head_items[2]);
   list_test.Remove(head_items[count-3]);
   list_test.Remove(head_items[1]);
   list_test.Remove(head_items[count-2]);
   list_test.RemoveFirst();
   list_test.RemoveLast();
//--- with the above remove we should have removed the first and last 3 items 
   ArrayFree(temp_items);
   ArrayCopy(temp_items,head_items,0,3,count-6);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddAfter(list_test.Last(),tail_items[i]);
   int temp_items2[];
   ArrayCopy(temp_items2,temp_items);
   ArrayCopy(temp_items2,tail_items,ArraySize(temp_items));
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items2)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test call AddAfter several times remove all of the items
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddAfter(list_test.Last(),head_items[i]);
//--- remove values
   for(int i=0; i<count;++i)
      list_test.RemoveFirst();
//--- add values
   list_test.AddFirst(tail_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddAfter(list_test.Last(),tail_items[i]);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test call AddAfter several times then call Clear
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddAfter(list_test.Last(),head_items[i]);
//--- clear
   list_test.Clear();
//--- add values
   list_test.AddFirst(tail_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddAfter(list_test.Last(),tail_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test mix AddBefore and AddAfter calls
//--- add values
   list_test.AddLast(head_items[0]);
   list_test.AddLast(tail_items[0]);
   for(int i=1; i<count;++i)
     {
      list_test.AddBefore(list_test.First(), head_items[i]);
      list_test.AddAfter(list_test.Last(), tail_items[i]);
     }
   ArrayFree(temp_items);
   ArrayCopy(temp_items,head_reverse_items);
   ArrayCopy(temp_items,tail_items,count);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddAfter_Node_Negative.                                      |
//+------------------------------------------------------------------+
bool TestAddAfter_Node_Negative(const int count)
  {
//--- create lists
   CLinkedList<int>list_test();
   CLinkedList<int>list_temp();

//--- test verify null node
//--- check
   if(list_test.AddAfter(NULL,0)!=NULL)
      return(false);
   if(list_test.Count()!=0)
      return(false);

//--- test verify Node that is a new Node
//--- add values
   list_test.AddLast(0);
   CLinkedListNode<int>node(1);
//--- check
   if(list_test.AddAfter(GetPointer(node),-1)!=NULL)
      return(false);
   if(list_test.Count()!=1)
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
//--- add values   
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
   list_temp.AddLast(MathRand());
   list_temp.AddLast(MathRand());
//--- check   
   if(list_test.AddAfter(list_temp.Last(),MathRand())!=NULL)
      return(false);
   if(list_test.Count()!=2)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddAfter.                                                    |
//+------------------------------------------------------------------+
bool TestAddAfter(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Complex validation of AddAfter method for head node, tail node, some node in the middle, "+
               "also testing after remove several element, remove all element and cleaning of list.",test_name);
   if(!TestAddAfter_Node(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of AddAfter method for incorrect node.",test_name);
   if(!TestAddAfter_Node_Negative(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddBefore_Node.                                              |
//+------------------------------------------------------------------+
bool TestAddBefore_Node(const int count)
  {
   CLinkedList<int>list_test();
//--- create test arrays
   int check_array[];
   int temp_items[];
   int head_items[];
   int head_reverse_items[];
   int tail_items[];
   int tail_reverse_items[];
   ArrayResize(temp_items,count);
   ArrayResize(head_items,count);
   ArrayResize(head_reverse_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(tail_reverse_items,count);
   for(int i=0; i<count; i++)
     {
      int index=count-1-i;
      int head = MathRand();
      int tail = MathRand();
      head_items[i]=head;
      head_reverse_items[index]=head;
      tail_items[i]=tail;
      tail_reverse_items[index]=tail;
     }
//--- test verify value is -1
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddBefore(list_test.First(), -1);
//--- check
   list_test.CopyTo(check_array);
   if(check_array[0]!=-1 && check_array[1]!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test node is the head
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.First(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,head_reverse_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is the tail
   list_test.AddFirst(head_items[0]);
   ArrayCopy(temp_items,head_items,0,1,count);
   temp_items[count-1]=head_items[0];
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.Last(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is after the head
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   ArrayFree(temp_items);
   ArrayCopy(temp_items,head_items);
   ArrayReverse(temp_items,1,count-1);
   for(int i=2; i<count;++i)
      list_test.AddBefore(list_test.First().Next(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is before the tail
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   ArrayResize(temp_items,count);
   ArrayCopy(temp_items,head_items,0,2,count-2);
   temp_items[count-2] = head_items[0];
   temp_items[count-1] = head_items[1];
   for(int i=2; i<count;++i)
      list_test.AddBefore(list_test.Last().Previous(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test node is somewhere in the middle
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   ArrayResize(temp_items,count);
   ArrayCopy(temp_items,head_items,0,3,count-3);
   temp_items[count-3] = head_items[0];
   temp_items[count-2] = head_items[1];
   temp_items[count-1] = head_items[2];
   for(int i=3; i<count;++i)
      list_test.AddBefore(list_test.Last().Previous().Previous(),head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test call AddBefore several times remove some of the items
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.First(),head_items[i]);
//--- remove values
   list_test.Remove(head_items[2]);
   list_test.Remove(head_items[count - 3]);
   list_test.Remove(head_items[1]);
   list_test.Remove(head_items[count - 2]);
   list_test.RemoveFirst();
   list_test.RemoveLast();
//--- with the above remove we should have removed the first and last 3 items 
   ArrayResize(temp_items,count-6);
   ArrayCopy(temp_items,head_reverse_items,0,3,count-6);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddBefore(list_test.First(),tail_items[i]);
   int temp_items2[];
   ArrayCopy(temp_items2,tail_reverse_items);
   ArrayCopy(temp_items2,temp_items,count);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items2)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test call AddBefore several times remove all of the items
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.First(),head_items[i]);
//--- remove values
   for(int i=0; i<count;++i)
      list_test.RemoveFirst();
//--- add values
   list_test.AddFirst(tail_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.First(),tail_items[i]);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_reverse_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test call AddBefore several times then call Clear
//--- add values
   list_test.AddFirst(head_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.First(),head_items[i]);
//--- clear
   list_test.Clear();
//--- add values
   list_test.AddFirst(tail_items[0]);
   for(int i=1; i<count;++i)
      list_test.AddBefore(list_test.First(),tail_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_reverse_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test mix AddBefore and AddAfter calls
   list_test.AddLast(head_items[0]);
   list_test.AddLast(tail_items[0]);
   for(int i=1; i<count;++i)
     {
      list_test.AddBefore(list_test.First(), head_items[i]);
      list_test.AddAfter(list_test.Last(), tail_items[i]);
     }
   ArrayResize(temp_items,count*2);
   ArrayCopy(temp_items,head_reverse_items);
   ArrayCopy(temp_items,tail_items,count);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddBefore_Node_Negative.                                     |
//+------------------------------------------------------------------+
bool TestAddBefore_Node_Negative(const int count)
  {
//--- create lists
   CLinkedList<int>list_test();
   CLinkedList<int>list_temp();

//--- test verify null node
//--- check
   if(list_test.AddBefore(NULL,0)!=NULL)
      return(false);
   if(list_test.Count()!=0)
      return(false);

//--- test verify Node that is a new Node
//--- add values
   list_test.AddLast(0);
   CLinkedListNode<int>node(1);
//--- check
   if(list_test.AddBefore(GetPointer(node),-1)!=NULL)
      return(false);
   if(list_test.Count()!=1)
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
//--- add values   
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
   list_temp.AddLast(MathRand());
   list_temp.AddLast(MathRand());
//--- check   
   if(list_test.AddBefore(list_temp.Last(),MathRand())!=NULL)
      return(false);
   if(list_test.Count()!=2)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddBefore.                                                   |
//+------------------------------------------------------------------+
bool TestAddBefore(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Complex validation of AddBefore method for head node, tail node, some node in the middle, "+
               "also testing after remove several element, remove all element and cleaning of list.",test_name);
   if(!TestAddBefore_Node(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of AddBefore method for incorrect node.",test_name);
   if(!TestAddBefore_Node_Negative(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddFirst_Node.                                               |
//+------------------------------------------------------------------+
bool TestAddFirst_Node(const int count)
  {
   CLinkedList<int>list_test();
//--- create test arrays
   int check_array[];
   int temp_items[];
   int head_items[];
   int head_reverse_items[];
   int tail_items[];
   int tail_reverse_items[];
   ArrayResize(temp_items,count);
   ArrayResize(head_items,count);
   ArrayResize(head_reverse_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(tail_reverse_items,count);
   for(int i=0; i<count; i++)
     {
      int index=count-1-i;
      int head = MathRand();
      int tail = MathRand();
      head_items[i]=head;
      head_reverse_items[index]=head;
      tail_items[i]=tail;
      tail_reverse_items[index]=tail;
     }
//--- test verify value is -1
//--- add values
   list_test.AddFirst(-1);
//--- check
   list_test.CopyTo(check_array);
   if(check_array[0]!=-1)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddHead several times
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddFirst(head_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,head_reverse_items)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddHead several times remove some of the items
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddFirst(head_items[i]);
//--- remove values
   list_test.Remove(head_items[2]);
   list_test.Remove(head_items[count-3]);
   list_test.Remove(head_items[1]);
   list_test.Remove(head_items[count-2]);
   list_test.RemoveFirst();
   list_test.RemoveLast();
//--- with the above remove we should have removed the first and last 3 items 
//--- expected items are head_items in reverse order, or a subset of them.
   ArrayResize(temp_items,count-6);
   ArrayCopy(temp_items,head_reverse_items,0,3,count-6);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddFirst(tail_items[i]);
   int temp_items2[];
   ArrayCopy(temp_items2,tail_reverse_items);
   ArrayCopy(temp_items2,temp_items,count);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items2)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddHead several times remove all of the items
//--- add values   
   for(int i=0; i<count;++i)
      list_test.AddFirst(head_items[i]);
//--- remove values
   for(int i=0; i<count;++i)
      list_test.RemoveFirst();
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddFirst(tail_items[i]);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_reverse_items)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddHead several times then call Clear
//--- add vlues
   for(int i=0; i<count;++i)
      list_test.AddFirst(head_items[i]);
//--- clear
   list_test.Clear();
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddFirst(tail_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_reverse_items)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test mix AddHead and AddTail calls
//--- add values
   for(int i=0; i<count;++i)
     {
      list_test.AddFirst(head_items[i]);
      list_test.AddLast(tail_items[i]);
     }
   ArrayResize(temp_items,count*2);
   ArrayCopy(temp_items,head_reverse_items);
   ArrayCopy(temp_items,tail_items,count);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddFirst_Node_Negative.                                      |
//+------------------------------------------------------------------+
bool TestAddFirst_Node_Negative(const int count)
  {
//--- create lists
   CLinkedList<int>list_test();
   CLinkedList<int>list_temp();

//--- test verify null node
//--- check
   if(list_test.AddFirst((CLinkedListNode<int>*)NULL)==true)
      return(false);
   if(list_test.Count()!=0)
      return(false);

//--- test verify Node that is a new Node
//--- add values
   list_test.AddLast(0);
   CLinkedListNode<int>node(1);
//--- check
   if(list_test.AddFirst(list_test.First())==true)
      return(false);
   if(list_test.Count()!=1)
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
//--- add values
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
//--- check   
   if(list_test.AddFirst(list_test.Last())==true)
      return(false);
   if(list_test.Count()!=2)
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
//--- add values   
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
   list_temp.AddLast(MathRand());
   list_temp.AddLast(MathRand());
//--- check   
   if(list_test.AddFirst(list_temp.Last())==true)
      return(false);
   if(list_test.Count()!=2)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddFirst.                                                    |
//+------------------------------------------------------------------+
bool TestAddFirst(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Complex validation of AddFirst method after remove several element, remove all, several calling method of clearing.",test_name);
   if(!TestAddFirst_Node(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of AddFirst method for incorrect node.",test_name);
   if(!TestAddFirst_Node_Negative(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddLast_Node.                                                |
//+------------------------------------------------------------------+
bool TestAddLast_Node(const int count)
  {
   CLinkedList<int>list_test();
//--- create test arrays
   int check_array[];
   int temp_items[];
   int head_items[];
   int head_reverse_items[];
   int tail_items[];
   int tail_reverse_items[];
   ArrayResize(temp_items,count);
   ArrayResize(head_items,count);
   ArrayResize(head_reverse_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(tail_reverse_items,count);
   for(int i=0; i<count; i++)
     {
      int index=count-1-i;
      int head = MathRand();
      int tail = MathRand();
      head_items[i]=head;
      head_reverse_items[index]=head;
      tail_items[i]=tail;
      tail_reverse_items[index]=tail;
     }
//--- test verify value is -1
//--- add values
   list_test.AddLast(-1);
//--- check
   list_test.CopyTo(check_array);
   if(check_array[0]!=-1)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddTail several times
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(tail_items[i]);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,tail_items)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddTail several times remove some of the items
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(tail_items[i]);
//--- remove values
   list_test.Remove(tail_items[2]);
   list_test.Remove(tail_items[count - 3]);
   list_test.Remove(tail_items[1]);
   list_test.Remove(tail_items[count - 2]);
   list_test.RemoveFirst();
   list_test.RemoveLast();
//--- with the above remove we should have removed the first and last 3 items 
   ArrayResize(temp_items,count-6);
   ArrayCopy(temp_items,tail_items,0,3,count-6);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- adding some more items to the tail of the linked list
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   int temp_items2[];
   ArrayCopy(temp_items2,temp_items);
   ArrayCopy(temp_items2,head_items,ArraySize(temp_items));
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items2)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test call AddTail several times then call Clear
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(tail_items[i]);
//--- clear   
   list_test.Clear();
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,head_items)!=0)
      return(false);
//--- clear 
   list_test.Clear();

//--- test mix AddTail and AddTail calls
   for(int i=0; i<count;++i)
     {
      list_test.AddFirst(head_items[i]);
      list_test.AddLast(tail_items[i]);
     }
   ArrayResize(temp_items,count);
//--- adding the head_items in reverse order.
   for(int i=0; i<count; i++)
     {
      int index=count-1-i;
      temp_items[i]=head_items[index];
     }
   ArrayCopy(temp_items2,temp_items);
   ArrayCopy(temp_items2,tail_items,count);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items2)!=0)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddLast_Node_Negative.                                       |
//+------------------------------------------------------------------+
bool TestAddLast_Node_Negative(const int count)
  {
//--- create lists
   CLinkedList<int>list_test();
   CLinkedList<int>list_temp();

//--- test verify null node
//--- check
   if(list_test.AddLast((CLinkedListNode<int>*)NULL)==true)
      return(false);
   if(list_test.Count()!=0)
      return(false);

//--- test verify Node that is a new Node
//--- add values
   list_test.AddLast(0);
   CLinkedListNode<int>node(1);
//--- check
   if(list_test.AddLast(list_test.First())==true)
      return(false);
   if(list_test.Count()!=1)
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
//--- add values
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
//--- check   
   if(list_test.AddLast(list_test.Last())==true)
      return(false);
   if(list_test.Count()!=2)
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
//--- add values   
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
   list_temp.AddLast(MathRand());
   list_temp.AddLast(MathRand());
//--- check   
   if(list_test.AddLast(list_temp.Last())==true)
      return(false);
   if(list_test.Count()!=2)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddLast.                                                     |
//+------------------------------------------------------------------+
bool TestAddLast(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Complex validation of AddLast method after remove several element, remove all, several calling method of clearing.",test_name);
   if(!TestAddLast_Node(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of AddLast method for incorrect node.",test_name);
   if(!TestAddLast_Node_Negative(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestConstructor_Complex.                                         |
//+------------------------------------------------------------------+
bool TestConstructor_Complex(const int count)
  {
//--- create source array and list
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
   CArrayList<int>list(array);
//--- crete linked list
   CLinkedList<int>llist1(array);
   CLinkedList<int>llist2(GetPointer(list));
   CLinkedList<int>llist3(GetPointer(llist1));
   CLinkedList<int>llist4(NULL);
//--- check
   int check_array1[];
   llist1.CopyTo(check_array1);
   if(ArrayCompare(check_array1,array)!=0)
      return(false);
   int check_array2[];
   llist1.CopyTo(check_array2);
   if(ArrayCompare(check_array2,array)!=0)
      return(false);
   int check_array3[];
   llist1.CopyTo(check_array3);
   if(ArrayCompare(check_array3,array)!=0)
      return(false);
   if(llist4.Count()!=0)
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestConstructor.                                                 |
//+------------------------------------------------------------------+
bool TestConstructor(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Complex validation constructor of linked list dased on array, several ICollection objects and NULL object.",test_name);
   if(!TestConstructor_Complex(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Find.                                                   |
//+------------------------------------------------------------------+
bool TestMisc_Find(const int count)
  {
   CLinkedList<int>list_test();
//--- create test arrays
   int check_array[];
   int head_items[];
   int tail_items[];
   int head_prepend_items[];
   int tail_prepend_items[];
   ArrayResize(head_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(head_prepend_items,count+1);
   ArrayResize(tail_prepend_items,count+1);
   head_prepend_items[0]=-1;
   tail_prepend_items[0]=-1;
   for(int i=0; i<count; i++)
     {
      int head = (i+1);
      int tail = (i+1)*100;
      head_items[i]=head;
      tail_items[i]=tail;
      head_prepend_items[i+1]=head;
      tail_prepend_items[i+1]=tail;
     }
//--- test call Find an empty collection
//--- check
   if(list_test.Find(head_items[0])!=NULL)
      return(false);
   if(list_test.Find(-1)!=NULL)
      return(false);

//--- test call Find on a collection with one item in it
//--- add values
   list_test.AddLast(head_items[0]);
//--- check
   if(list_test.Find(head_items[1])!=NULL)
      return(false);
   if(list_test.Find(-1)!=NULL)
      return(false);
   if(list_test.Find(head_items[0]).Value()!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Find on a collection with two items in it
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
//--- check 
   if(list_test.Find(head_items[2])!=NULL)
      return(false);
   if(list_test.Find(-1)!=NULL)
      return(false);
   if(list_test.Find(head_items[0]).Value()!=head_items[0])
      return(false);
   if(list_test.Find(head_items[1]).Value()!=head_items[1])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Find on a collection with three items in it
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
//--- check 
   if(list_test.Find(head_items[3])!=NULL)
      return(false);
   if(list_test.Find(-1)!=NULL)
      return(false);
   if(list_test.Find(head_items[0]).Value()!=head_items[0])
      return(false);
   if(list_test.Find(head_items[1]).Value()!=head_items[1])
      return(false);
   if(list_test.Find(head_items[2]).Value()!=head_items[2])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Find on a collection with multiple items in it
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- check
   if(list_test.Find(tail_items[0])!=NULL)
      return(false);
   if(list_test.Find(-1)!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.Find(head_items[i]).Value()!=head_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call Find on a collection with duplicate items in it
//--- add values  
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   int temp_items[];
   ArrayCopy(temp_items,head_items);
   ArrayCopy(temp_items,head_items,count);
//--- check
   if(list_test.Find(tail_items[0])!=NULL)
      return(false);
   if(list_test.Find(-1)!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.Find(temp_items[i]).Value()!=temp_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call Find with -1 at the beginning
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddFirst(-1);
//--- check
   if(list_test.Find(tail_items[0])!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.Find(head_prepend_items[i]).Value()!=head_prepend_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call Find with -1 in the middle
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddLast(-1);
   for(int i=0; i<count;++i)
      list_test.AddLast(tail_items[i]);
   ArrayFree(temp_items);
   ArrayCopy(temp_items,head_items);
   ArrayCopy(temp_items,tail_prepend_items,count);
//--- check
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.Find(temp_items[i]).Value()!=temp_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call Find on a collection with duplicate items in it
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddLast(-1);
   ArrayResize(temp_items,count+1);
   temp_items[count]=-1;
   ArrayCopy(temp_items,head_items);
//--- check
   if(list_test.Find(tail_items[0])!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.Find(temp_items[i]).Value()!=temp_items[i])
         return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_FindLast.                                               |
//+------------------------------------------------------------------+
bool TestMisc_FindLast(const int count)
  {
   CLinkedList<int>list_test();
//--- create test arrays
   int check_array[];
   int head_items[];
   int tail_items[];
   int head_prepend_items[];
   int tail_prepend_items[];
   ArrayResize(head_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(head_prepend_items,count+1);
   ArrayResize(tail_prepend_items,count+1);
   head_prepend_items[0]=-1;
   tail_prepend_items[0]=-1;
   for(int i=0; i<count; i++)
     {
      int head = (i+1);
      int tail = (i+1)*100;
      head_items[i]=head;
      tail_items[i]=tail;
      head_prepend_items[i+1]=head;
      tail_prepend_items[i+1]=tail;
     }
//--- test call FindLast an empty collection
//--- check
   if(list_test.FindLast(head_items[0])!=NULL)
      return(false);
   if(list_test.FindLast(-1)!=NULL)
      return(false);

//--- test call FindLast on a collection with one item in it
//--- add values
   list_test.AddLast(head_items[0]);
//--- check
   if(list_test.FindLast(head_items[1])!=NULL)
      return(false);
   if(list_test.FindLast(-1)!=NULL)
      return(false);
   if(list_test.FindLast(head_items[0]).Value()!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast on a collection with two items in it
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
//--- check 
   if(list_test.FindLast(head_items[2])!=NULL)
      return(false);
   if(list_test.FindLast(-1)!=NULL)
      return(false);
   if(list_test.FindLast(head_items[0]).Value()!=head_items[0])
      return(false);
   if(list_test.FindLast(head_items[1]).Value()!=head_items[1])
      return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast on a collection with three items in it
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
//--- check 
   if(list_test.FindLast(head_items[3])!=NULL)
      return(false);
   if(list_test.FindLast(-1)!=NULL)
      return(false);
   if(list_test.FindLast(head_items[0]).Value()!=head_items[0])
      return(false);
   if(list_test.FindLast(head_items[1]).Value()!=head_items[1])
      return(false);
   if(list_test.FindLast(head_items[2]).Value()!=head_items[2])
      return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast on a collection with multiple items in it
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- check
   if(list_test.FindLast(tail_items[0])!=NULL)
      return(false);
   if(list_test.FindLast(-1)!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.FindLast(head_items[i]).Value()!=head_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast on a collection with duplicate items in it
//--- add values  
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   int temp_items[];
   ArrayCopy(temp_items,head_items);
   ArrayCopy(temp_items,head_items,count);
//--- check
   if(list_test.FindLast(tail_items[0])!=NULL)
      return(false);
   if(list_test.FindLast(-1)!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.FindLast(temp_items[i]).Value()!=temp_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast with -1 at the beginning
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddFirst(-1);
//--- check
   if(list_test.FindLast(tail_items[0])!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.FindLast(head_prepend_items[i]).Value()!=head_prepend_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast with -1 in the middle
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddLast(-1);
   for(int i=0; i<count;++i)
      list_test.AddLast(tail_items[i]);
   ArrayFree(temp_items);
   ArrayCopy(temp_items,head_items);
   ArrayCopy(temp_items,tail_prepend_items,count);
//--- check
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.FindLast(temp_items[i]).Value()!=temp_items[i])
         return(false);
//--- clear
   list_test.Clear();

//--- test call FindLast on a collection with duplicate items in it
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddLast(-1);
   ArrayResize(temp_items,count+1);
   temp_items[count]=-1;
   ArrayCopy(temp_items,head_items);
//--- check
   if(list_test.FindLast(tail_items[0])!=NULL)
      return(false);
   for(int i=0; i<list_test.Count(); i++)
      if(list_test.FindLast(temp_items[i]).Value()!=temp_items[i])
         return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_RemoveNode.                                             |
//+------------------------------------------------------------------+
bool TestMisc_RemoveNode(const int count)
  {
   CLinkedList<int>list_test();
   int temp_value1;
   int temp_value2;
   int temp_value3;
//--- create test arrays
   int check_array[];
   int head_items[];
   int tail_items[];
   int temp_items[];
   ArrayResize(head_items,count);
   ArrayResize(tail_items,count);
   for(int i=0; i<count; i++)
     {
      int head = (i+1);
      int tail = (i+1)*100;
      head_items[i]=head;
      tail_items[i]=tail;
     }
//--- test call Remove with an item that exists in the collection size=1
//--- add values
   list_test.AddLast(head_items[0]);
   temp_value1=list_test.First().Value();
//--- remove
   list_test.Remove(list_test.First());
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove with the Head collection size=2
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   temp_value1=list_test.First().Value();
//--- remove 
   list_test.Remove(list_test.First());
//--- check
   if(list_test.Count()!=1)
      return(false);
   if(list_test.First().Value()!=head_items[1])
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove with the Tail collection size=2
//--- add values   
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   temp_value1=list_test.Last().Value();
//--- remove
   list_test.Remove(list_test.Last());
//--- check
   if(list_test.Count()!=1)
      return(false);
   if(list_test.First().Value()!=head_items[0])
      return(false);
   if(temp_value1!=head_items[1])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove all the items collection size=2
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   temp_value1 = list_test.First().Value();
   temp_value2 = list_test.Last().Value();
//--- remove
   list_test.Remove(list_test.First());
   list_test.Remove(list_test.Last());
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value2!=head_items[1])
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove with the Head collection size=3
//--- add values   
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   temp_value1=list_test.First().Value();
//--- remove
   list_test.Remove(list_test.First());
//--- check
   if(list_test.Count()!=2)
      return(false);
   if(list_test.First().Value()!=head_items[1])
      return(false);
   if(list_test.First().Next().Value()!=head_items[2])
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove with the middle item collection size=3
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   temp_value1=list_test.First().Next().Value();
//--- remove
   list_test.Remove(list_test.First().Next());
//--- check
   if(list_test.Count()!=2)
      return(false);
   if(list_test.First().Value()!=head_items[0])
      return(false);
   if(list_test.First().Next().Value()!=head_items[2])
      return(false);
   if(temp_value1!=head_items[1])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove with the Tail collection size=3
//--- add values   
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   temp_value1=list_test.Last().Value();
//--- remove   
   list_test.Remove(list_test.Last());
//--- check
   if(list_test.Count()!=2)
      return(false);
   if(list_test.First().Value()!=head_items[0])
      return(false);
   if(list_test.First().Next().Value()!=head_items[1])
      return(false);
   if(temp_value1!=head_items[2])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove all the items collection size=3
//--- add values   
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   temp_value1 = list_test.First().Value();
   temp_value2 = list_test.First().Next().Value();
   temp_value3 = list_test.Last().Value();
//--- remove
   list_test.Remove(list_test.First().Next().Next());
   list_test.Remove(list_test.First().Next());
   list_test.Remove(list_test.First());
//--- check 
   if(list_test.Count()!=0)
      return(false);
   if(temp_value3!=head_items[2])
      return(false);
   if(temp_value2!=head_items[1])
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
//--- clear
   list_test.Clear();

//--- test call Remove all the items starting with the first collection size=16
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- remove and check
   for(int i=0; i<count;++i)
     {
      list_test.Remove(list_test.First());
      int index=i+1;
      int length=count-i-1;
      int expected[];
      int actual[];
      ArrayCopy(expected,head_items,0,index,length);
      list_test.CopyTo(actual);
      if(ArrayCompare(actual,expected)!=0)
         return(false);
     }
//--- clear
   list_test.Clear();

//--- test call Remove all the items starting with the last collection size=16
//--- add values   
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- remove and check
   for(int i=count-1; 0<=i; --i)
     {
      list_test.Remove(list_test.Last());
      int expected[];
      int actual[];
      ArrayCopy(expected,head_items,0,0,i);
      list_test.CopyTo(actual);
      if(ArrayCompare(actual,expected)!=0)
         return(false);
     }
//--- clear
   list_test.Clear();

//--- test remove some items in the middle
//--- add values   
   for(int i=0; i<count;++i)
      list_test.AddFirst(head_items[i]);
//--- remove
   list_test.Remove(list_test.First().Next().Next());
   list_test.Remove(list_test.Last().Previous().Previous());
   list_test.Remove(list_test.First().Next());
   list_test.Remove(list_test.Last().Previous());
   list_test.Remove(list_test.First());
   list_test.Remove(list_test.Last());
//--- with the above remove we should have removed the first and last 3 items 
   int head_reverse_items[];
   ArrayCopy(head_reverse_items,head_items);
   ArrayReverse(head_reverse_items,0,count);
   ArrayCopy(temp_items,head_reverse_items,0,3,count-6);
//--- check
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,temp_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test remove an item with a value of -1
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
   list_test.AddLast(-1);
//--- remove
   list_test.Remove(list_test.Last());
//--- check
   ArrayFree(check_array);
   list_test.CopyTo(check_array);
   if(ArrayCompare(check_array,head_items)!=0)
      return(false);
//--- clear
   list_test.Clear();

//--- test remove node with duplicates
   int items[];
   ArrayResize(items,count);
   for(int i=0; i<count; i++)
      items[i]=MathRand();
//--- add values      
   for(int i=0; i<count; i++)
      list_test.AddLast(items[i]);
   for(int i=0; i<count; i++)
      list_test.AddLast(items[i]);
//--- store nodes
   CLinkedListNode<int>*nodes[];
   CLinkedListNode<int>*current_node=list_test.First();
   int index=0;
   ArrayResize(nodes,count*2);
   while(index<count*2)
     {
      nodes[index] = current_node;
      current_node = current_node.Next();
      index++;
     }
//--- remove
   list_test.Remove(list_test.First().Next().Next());
   list_test.Remove(list_test.Last().Previous().Previous());
   list_test.Remove(list_test.First().Next());
   list_test.Remove(list_test.Last().Previous());
   list_test.Remove(list_test.First());
   list_test.Remove(list_test.Last());
//--- check
   current_node=list_test.First();
   for(int i=3; i<(count*2)-3;i++)
     {
      if(current_node==NULL)
         return(false);
      if(current_node!=nodes[i])
         return(false);
      if(items[i%count]!=current_node.Value())
         return(false);
      current_node=current_node.Next();
     }
//--- clear
   list_test.Clear();

//--- test verify NULL node
//--- check
   if(list_test.Remove((CLinkedListNode<int>*)NULL) && list_test.Remove(0))
      return(false);

//--- test verify Node that is a new Node
//--- add values
   list_test.AddLast(0);
   CLinkedListNode<int>node(1);
//--- check
   if(list_test.Remove(GetPointer(node)))
      return(false);
//--- clear
   list_test.Clear();

//--- test verify Node that already exists in another collection
   list_test.AddLast(MathRand());
   list_test.AddLast(MathRand());
   CLinkedList<int>other();
   other.AddLast(MathRand());
   other.AddLast(MathRand());
   if(list_test.Remove(other.Last()))
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_RemoveFirst.                                            |
//+------------------------------------------------------------------+
bool TestMisc_RemoveFirst(const int count)
  {
   CLinkedList<int>list_test();
   int temp_value1;
   int temp_value2;
   int temp_value3;
//--- create test arrays
   int check_array[];
   int head_items[];
   int tail_items[];
   int temp_items[];
   ArrayResize(head_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(temp_items,count);
   for(int i=0; i<count; i++)
     {
      int head = (i+1);
      int tail = (i+1)*100;
      head_items[i]=head;
      tail_items[i]=tail;
     }
//--- test call RemoveHead on a collection with one item in it
//--- add values
   list_test.AddLast(head_items[0]);
   temp_value1=list_test.First().Value();
   list_test.RemoveFirst();
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);

//--- test call RemoveHead on a collection with two items in it
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   temp_value1 = list_test.First().Value();
   temp_value2 = list_test.Last().Value();
//--- remove 
   list_test.RemoveFirst();
   list_test.RemoveFirst();
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
   if(temp_value2!=head_items[1])
      return(false);

//--- test call RemoveHead on a collection with three items in it
//--- add  values   
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   temp_value1 = list_test.First().Value();
   temp_value2 = list_test.First().Next().Value();
   temp_value3 = list_test.Last().Value();
//--- remove
   list_test.RemoveFirst();
   list_test.RemoveFirst();
   list_test.RemoveFirst();
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
   if(temp_value2!=head_items[1])
      return(false);
   if(temp_value3!=head_items[2])
      return(false);

//--- test call RemoveHead on a collection with 16 items in it
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- remove and check
   for(int i=0; i<count;++i)
     {
      list_test.RemoveFirst();
      int index=i+1;
      int length=count-i-1;
      int expected[];
      int actual[];
      ArrayCopy(expected,head_items,0,index,length);
      list_test.CopyTo(actual);
      if(ArrayCompare(actual,expected)!=0)
         return(false);
     }
//--- clear
   list_test.Clear();

//--- test mix RemoveHead and RemoveTail call
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- remove and check
   for(int i=0; i<count;++i)
     {
      if((i  &1)==0)
         list_test.RemoveFirst();
      else
         list_test.RemoveLast();
      int index=(i/2)+1;
      int length=count-i-1;
      int expected[];
      int actual[];
      ArrayCopy(expected,head_items,0,index,length);
      list_test.CopyTo(actual);
      if(ArrayCompare(actual,expected)!=0)
         return(false);
     }
//--- clear
   list_test.Clear();

//--- test call RemoveHead an empty collection
   if(list_test.RemoveFirst())
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_RemoveLast.                                             |
//+------------------------------------------------------------------+
bool TestMisc_RemoveLast(const int count)
  {
   CLinkedList<int>list_test();
   int temp_value1;
   int temp_value2;
   int temp_value3;
//--- create test arrays
   int check_array[];
   int head_items[];
   int tail_items[];
   int temp_items[];
   ArrayResize(head_items,count);
   ArrayResize(tail_items,count);
   ArrayResize(temp_items,count);
   for(int i=0; i<count; i++)
     {
      int head = (i+1);
      int tail = (i+1)*100;
      head_items[i]=head;
      tail_items[i]=tail;
     }
//--- test call RemoveHead on a collection with one item in it
//--- add values
   list_test.AddLast(head_items[0]);
   temp_value1=list_test.First().Value();
   list_test.RemoveLast();
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);

//--- test call RemoveHead on a collection with two items in it
//--- add values
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   temp_value1 = list_test.First().Value();
   temp_value2 = list_test.Last().Value();
//--- remove and check
   list_test.RemoveLast();
   if(list_test.Last().Value()!=head_items[0])
      return(false);
   list_test.RemoveLast();
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
   if(temp_value2!=head_items[1])
      return(false);

//--- test call RemoveHead on a collection with three items in it
//--- add  values   
   list_test.AddFirst(head_items[0]);
   list_test.AddLast(head_items[1]);
   list_test.AddLast(head_items[2]);
   temp_value1 = list_test.First().Value();
   temp_value2 = list_test.First().Next().Value();
   temp_value3 = list_test.Last().Value();
//--- remove and check
   list_test.RemoveLast();
   if(list_test.Last().Value()!=head_items[1])
      return(false);
   list_test.RemoveLast();
   if(list_test.Last().Value()!=head_items[0])
      return(false);
   list_test.RemoveLast();
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(temp_value1!=head_items[0])
      return(false);
   if(temp_value2!=head_items[1])
      return(false);
   if(temp_value3!=head_items[2])
      return(false);

//--- test call RemoveHead on a collection with 16 items in it
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- remove and check
   for(int i=0; i<count;++i)
     {
      list_test.RemoveLast();
      int length=count-i-1;
      int expected[];
      int actual[];
      ArrayCopy(expected,head_items,0,0,length);
      list_test.CopyTo(actual);
      if(ArrayCompare(actual,expected)!=0)
         return(false);
     }
//--- clear
   list_test.Clear();

//--- test mix RemoveHead and RemoveTail call
//--- add values
   for(int i=0; i<count;++i)
      list_test.AddLast(head_items[i]);
//--- remove and check
   for(int i=0; i<count;++i)
     {
      if((i  &1)==0)
         list_test.RemoveFirst();
      else
         list_test.RemoveLast();
      int index=(i/2)+1;
      int length=count-i-1;
      int expected[];
      int actual[];
      ArrayCopy(expected,head_items,0,index,length);
      list_test.CopyTo(actual);
      if(ArrayCompare(actual,expected)!=0)
         return(false);
     }
//--- clear
   list_test.Clear();

//--- test call RemoveHead an empty collection
   if(list_test.RemoveLast())
      return(false);

//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc.                                                        |
//+------------------------------------------------------------------+
bool TestMisc(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Complex validation of Find method of some element in different positions on the linked list with and without duplicates.",test_name);
   if(!TestMisc_Find(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Complex validation of FindLast method of some element in different positions on the linked list with and without duplicates.",test_name);
   if(!TestMisc_FindLast(16))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Complex validation of Remove method of some node in different positions on the linked list with different count of elements.",test_name);
   if(!TestMisc_RemoveNode(16))
      return(false);
//--- test 4
   PrintFormat("%s: Test 3: Complex validation of RemoveFirst method on the linked list with different count of elements.",test_name);
   if(!TestMisc_RemoveFirst(16))
      return(false);
//--- test 5
   PrintFormat("%s: Test 3: Complex validation of RemoveLast method on the linked list with different count of elements.",test_name);
   if(!TestMisc_RemoveLast(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestLinkedList.                                                  |
//+------------------------------------------------------------------+
void TestLinkedList(int &tests_performed,int &tests_passed)
  {
   string test_name="";
//--- AddAfter functions
   tests_performed++;
   test_name="AddAfter functions test";
   if(TestAddAfter(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- AddBefore functions
   tests_performed++;
   test_name="AddBefore functions test";
   if(TestAddBefore(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- AddFirst functions
   tests_performed++;
   test_name="AddFirst functions test";
   if(TestAddFirst(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- AddLast functions
   tests_performed++;
   test_name="AddLast functions test";
   if(TestAddLast(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- Constructor functions
   tests_performed++;
   test_name="Constructor functions test";
   if(TestConstructor(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- Misc functions
   tests_performed++;
   test_name="Misc functions test";
   if(TestMisc(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   MathSrand(0);
   string package_name="Generic";
   PrintFormat("Unit tests for Package %s\n",package_name);
//--- initial values
   int tests_performed=0;
   int tests_passed=0;
//--- test distributions
   TestLinkedList(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
