//+------------------------------------------------------------------+
//|                                                TestArrayList.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\ArrayList.mqh>
#include <Generic\HashSet.mqh>
//+------------------------------------------------------------------+
//| TestAddRange_AsArrayList.                                        |
//+------------------------------------------------------------------+
bool TestAddRange_AsArrayList(const int count,const int add_length)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone for source list
   CArrayList<int>list_clone(GetPointer(list_test));
//--- crete added list
   CArrayList<int>list_added(add_length);
   for(int i=0; i<add_length; i++)
      list_added.Add(MathRand());
//--- add range
   list_test.AddRange(GetPointer(list_added));
//--- check first path
   for(int i=0; i<count; i++)
     {
      int value_test;
      int value_clone;
      list_test.TryGetValue(i,value_test);
      list_clone.TryGetValue(i,value_clone);
      if(value_test!=value_clone)
         return(false);
     }
//--- check second path
   for(int i=0; i<add_length; i++)
     {
      int value_test;
      int value_added;
      list_test.TryGetValue(i+count,value_test);
      list_added.TryGetValue(i,value_added);
      if(value_test!=value_added)
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddRange_AsArray.                                            |
//+------------------------------------------------------------------+
bool TestAddRange_AsArray(const int count,const int add_length)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone for source list
   CArrayList<int>list_clone(GetPointer(list_test));
//--- crete added array
   int array_added[];
   ArrayResize(array_added,add_length);
   for(int i=0; i<add_length; i++)
      array_added[i]=MathRand();
//--- add range
   list_test.AddRange(array_added);
//--- check first path
   for(int i=0; i<count; i++)
     {
      int value_test;
      int value_clone;
      list_test.TryGetValue(i,value_test);
      list_clone.TryGetValue(i,value_clone);
      if(value_test!=value_clone)
         return(false);
     }
//--- check second path
   for(int i=0; i<add_length; i++)
     {
      int value_test;
      list_test.TryGetValue(i+count,value_test);
      if(value_test!=array_added[i])
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddRange_AsNULL.                                             |
//+------------------------------------------------------------------+
bool TestAddRange_AsNULL(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone for source list
   CArrayList<int>list_clone(GetPointer(list_test));
//--- crete added collection
   ICollection<int>*null=NULL;
//--- add range
   list_test.AddRange(null);
//--- check first path
   for(int i=0; i<count; i++)
     {
      int value_test;
      int value_clone;
      list_test.TryGetValue(i,value_test);
      list_clone.TryGetValue(i,value_clone);
      if(value_test!=value_clone)
         return(false);
     }
//--- check count
   if(count!=list_test.Count())
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestAddRange.                                                    |
//+------------------------------------------------------------------+
bool TestAddRange(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Adds the elements of the first list to the end of the second list.",test_name);
   if(!TestAddRange_AsArrayList(10,7))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Adds the elements of the array to the end of the list.",test_name);
   if(!TestAddRange_AsArray(10,7))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Adds NULL object as ICollection to the end of the list.",test_name);
   if(!TestAddRange_AsNULL(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestBinarySearch_Validations.                                    |
//+------------------------------------------------------------------+
bool TestBinarySearch_Validations(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- sort list
   list_test.Sort();
//--- copy list to array
   int array[];
   if(list_test.CopyTo(array)!=count)
      return(false);
//--- create random element
   int element=MathRand();
//--- find element in array
   int index1=ArrayBsearch(array,element);
//--- find element in list
   int index2= list_test.BinarySearch(0,count,element,NULL);
   if(index1!=index2)
      return(false);
   if(list_test.BinarySearch(0,count+1,element,NULL)!=-1)
      return(false);
   if(list_test.BinarySearch(-1,count,element,NULL)!=-1)
      return(false);
   if(list_test.BinarySearch(0,-1,element,NULL)!=-1)
      return(false);
   if(list_test.BinarySearch(count+1,count,element,NULL)!=-1)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestBinarySearch_WithoutDuplicates.                              |
//+------------------------------------------------------------------+
bool TestBinarySearch_WithoutDuplicates(const int count)
  {
//--- create set
   CHashSet<int>set();
   for(int i=0; i<count; i++)
      set.Add(MathRand());
//--- create source list
   CArrayList<int>list_test(GetPointer(set));
//--- sort list
   list_test.Sort();
//--- find all elements
   for(int i=0; i<list_test.Count(); i++)
     {
      int value;
      list_test.TryGetValue(i,value);
      if(i!=list_test.BinarySearch(value))
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestBinarySearch_WithDuplicates.                                 |
//+------------------------------------------------------------------+
bool TestBinarySearch_WithDuplicates(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- add duplicate
   int value0;
   list_test.TryGetValue(0, value0);
   list_test.Add(value0);
//--- sort list
   list_test.Sort();
//--- find all elements
   for(int i=0; i<list_test.Count(); i++)
     {
      int value;
      list_test.TryGetValue(i,value);
      if(list_test.BinarySearch(value)<0)
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestBinarySearch.                                                |
//+------------------------------------------------------------------+
bool TestBinarySearch(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Validation of incorrect input parameters for BinarySearch method.",test_name);
   if(!TestBinarySearch_Validations(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of BinarySearch method on sorted list without duplicate values.",test_name);
   if(!TestBinarySearch_WithoutDuplicates(10))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Validation of BinarySearch method on sorted list with duplicate values.",test_name);
   if(!TestBinarySearch_WithDuplicates(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestIndexOf_NonExistingValues.                                   |
//+------------------------------------------------------------------+
bool TestIndexOf_NonExistingValues(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create value
   int value=-1;
//--- try find index of value
   if(list_test.IndexOf(value)>=0)
      return(false);
   if(list_test.LastIndexOf(value)>=0)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestIndexOf_OrderIsCorrect.                                      |
//+------------------------------------------------------------------+
bool TestIndexOf_OrderIsCorrect(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone
   CArrayList<int>list_clone(GetPointer(list_test));
//--- add duplicates
   list_test.AddRange(GetPointer(list_test));
   list_test.AddRange(GetPointer(list_test));
   list_test.AddRange(GetPointer(list_test));
//--- find values
   for(int i=0; i<count; i++)
     {
      for(int j=0; j<4; j++)
        {
         int index=(j *count)+i;
         int value;
         list_clone.TryGetValue(i,value);
         if(index!=list_test.IndexOf(value,(count*j)))
            return(false);
         if(index!=list_test.IndexOf(value,(count*j),count))
            return(false);
        }
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestIndexOf_OutOfRange.                                          |
//+------------------------------------------------------------------+
bool TestIndexOf_OutOfRange(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- crete some element
   int element=MathRand();
//--- try to find index of element
   if(list_test.IndexOf(element,count+1)!=-1 || 
      list_test.IndexOf(element,count+10)!=-1 ||
      list_test.IndexOf(element, count-1)!=-1 ||
      list_test.IndexOf(element,INT_MIN)!=-1 ||
      list_test.IndexOf(element,count,1)!=-1 ||
      list_test.IndexOf(element,count+1,1)!=-1 || 
      list_test.IndexOf(element,count/2,count/2+2)!=-1 || 
      list_test.IndexOf(element, 0, -1)!=-1 ||
      list_test.IndexOf(element, -1, 1)!=-1)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestIndexOf.                                                     |
//+------------------------------------------------------------------+
bool TestIndexOf(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Validation of IndexOf and LastIndexOf methods for value in the list which does not contains it.",test_name);
   if(!TestIndexOf_NonExistingValues(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of IndexOf method on the list with duplicate values and correct input parameters.",test_name);
   if(!TestIndexOf_OrderIsCorrect(10))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Validation of IndexOf method on the list with duplicate values and incorrect input parameters.",test_name);
   if(!TestIndexOf_OutOfRange(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_BasicInsert.                                            |
//+------------------------------------------------------------------+
bool TestMisc_BasicInsert(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone
   CArrayList<int>list_clone(GetPointer(list_test));
//--- create random array
   int element=MathRand();
//--- insert 
   int index=count/2;
   for(int i=0; i<count; i++)
      list_test.Insert(index,element);
//--- check
   if(!list_test.Contains(element))
      return(false);
   if(list_test.Count()!=2*count)
      return(false);
   for(int i=0; i<index; i++)
     {
      int value_test;
      int value_clone;
      list_test.TryGetValue(i,value_test);
      list_clone.TryGetValue(i,value_clone);
      if(value_test!=value_clone)
         return(false);
     }
   for(int i=index; i<index+count; i++)
     {
      int value;
      list_test.TryGetValue(i,value);
      if(value!=element)
         return(false);
     }
   for(int i=index+count; i<2*count; i++)
     {
      int value_test;
      int value_clone;
      list_test.TryGetValue(i,value_test);
      list_clone.TryGetValue(i-count,value_clone);
      if(value_test!=value_clone)
         return(false);
     }
//--- create bad indexes
   int bad[6];
   bad[0] = list_test.Count()+1;
   bad[1] = list_test.Count()+2;
   bad[2] = INT_MAX;
   bad[3] = -1;
   bad[4] = -2;
   bad[5] = INT_MIN;
//--- try insert by bad indexes
   for(int i=0; i<ArraySize(bad); i++)
      if(list_test.Insert(bad[i],MathRand()))
         return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_InsertRange.                                            |
//+------------------------------------------------------------------+
bool TestMisc_InsertRange(const int count)
  {
//--- create x list
   CArrayList<int>list_x(count);
   for(int i=0; i<count; i++)
      list_x.Add(MathRand());
//--- create y list
   CArrayList<int>list_y(count);
   for(int i=0; i<count; i++)
      list_y.Add(MathRand());
//--- insert y to x
   int index=count/2;
   list_x.InsertRange(index,GetPointer(list_y));
//--- check elements
   for(int i=index; i<index+count; i++)
     {
      int value_x;
      int value_y;
      list_x.TryGetValue(i,value_x);
      list_y.TryGetValue(i-index,value_y);
      if(value_x!=value_y)
         return(false);
     }
//--- insert range into itself
   CArrayList<int>list(GetPointer(list_y));
   list.InsertRange(index,GetPointer(list));
//--- check elements
   for(int i=0; i<index; i++)
     {
      int value1;
      int value2;
      list.TryGetValue(i,value1);
      list.TryGetValue(i+index,value2);
      if(value1!=value2)
         return(false);
     }
//--- test arrays
   int array_x[];
   int array_y[];
   ArrayResize(array_x,count);
   ArrayResize(array_y,count);
   for(int i=0; i<count; i++)
     {
      array_x[i]=MathRand();
      array_y[i]=MathRand();
     }
   CArrayList<int>list_new_x(array_x);
//--- insert array
   list_new_x.InsertRange(index,array_y);
//--- check elements
   for(int i=0; i<index; i++)
     {
      int value;
      list_new_x.TryGetValue(i,value);
      if(value!=array_x[i])
         return(false);
     }
   for(int i=index; i<index+count; i++)
     {
      int value;
      list_new_x.TryGetValue(i,value);
      if(value!=array_y[i-index])
         return(false);
     }
   for(int i=index+count; i<2*count; i++)
     {
      int value;
      list_new_x.TryGetValue(i,value);
      if(value!=array_x[i-count])
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Contains.                                               |
//+------------------------------------------------------------------+
bool TestMisc_Contains(const int count)
  {
//--- create source array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source list
   CArrayList<int>list_test(array);
//--- check elements
   for(int i=0; i<count; i++)
      if(!list_test.Contains(array[i]))
         return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Remove.                                                 |
//+------------------------------------------------------------------+
bool TestMisc_Remove(const int count)
  {
//--- create source array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source list
   CArrayList<int>list_test(array);
   list_test.Sort();
//--- remove elements
   for(int i=0; i<count; i++)
      if(!list_test.Remove(array[i]))
         return(false);
//--- check count
   if(list_test.Count()>0)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Clear.                                                  |
//+------------------------------------------------------------------+
bool TestMisc_Clear(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
//--- check
   if(list_test.Count()!=0)
      return(false);
   if(list_test.Capacity()!=count)
      return(false);
//--- fill list
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- check
   if(list_test.Count()!=count)
      return(false);
//--- clear list
   list_test.Clear();
//--- check
   if(list_test.Count()!=0)
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
   PrintFormat("%s: Test 1: Complex validation of Insert method with correct and incorrect input parameters.",test_name);
   if(!TestMisc_BasicInsert(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Inserts the array and ICollection object into the list at the specified index.",test_name);
   if(!TestMisc_InsertRange(10))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Testing Contains method for each element of the list.",test_name);
   if(!TestMisc_Contains(10))
      return(false);
//--- test 4
   PrintFormat("%s: Test 4: Remove all element the list and check count after.",test_name);
   if(!TestMisc_Remove(10))
      return(false);
//--- test 5
   PrintFormat("%s: Test 5: Clear the list and check count after.",test_name);
   if(!TestMisc_Clear(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove_Range.                                                |
//+------------------------------------------------------------------+
bool TestRemove_Range(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone
   CArrayList<int>list_clone(GetPointer(list_test));
//--- create paremeters
   int values[9][2]=
     {
        {3, 3},
        {0, 10},
        {10, 0},
        {5, 5},
        {0, 5},
        {1, 9},
        {9, 1},
        {2, 8},
        {8, 2}
     };
//--- remove range
   for(int j=0; j<9; j++)
     {
      CArrayList<int>list_actual(GetPointer(list_test));
      int rindex = values[j][0];
      int rcount = values[j][1];
      if(!list_actual.RemoveRange(rindex,rcount))
         return(false);
      if(list_actual.Count()!=count-rcount)
         return(false);
      for(int i=0; i<rindex; i++)
        {
         int value_actual;
         int value_clone;
         list_actual.TryGetValue(i,value_actual);
         list_clone.TryGetValue(i,value_clone);
         if(value_actual!=value_clone)
            return(false);
        }
      for(int i=rindex; i<count-rcount;i++)
        {
         int value_actual;
         int value_clone;
         list_actual.TryGetValue(i,value_actual);
         list_clone.TryGetValue(i+rcount,value_clone);
         if(value_actual!=value_clone)
            return(false);
        }
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove_Invalid.                                              |
//+------------------------------------------------------------------+
bool TestRemove_Invalid(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create invalid paremeters
   int values[21][2];
   values[0][0]=count;      values[0][1]=1;
   values[1][0]=count+1;    values[1][1]=0;
   values[2][0]=count+1;    values[2][1]=1;
   values[3][0]=count;      values[3][1]=2;
   values[4][0]=count/2;    values[4][1]=count/2+1;
   values[5][0]=count-1;    values[5][1]=2;
   values[6][0]=count-2;    values[6][1]=3;
   values[7][0]=1;          values[7][1]=count;
   values[8][0]=0;          values[8][1]=count+1;
   values[9][0]=1;          values[9][1]=count+1;
   values[10][0]=2;         values[10][1]=count;
   values[11][0]=count/2+1; values[11][1]=count/2;
   values[12][0]=2;         values[12][1]=count-1;
   values[13][0]=3;         values[13][1]=count-2;
   values[14][0]=-1;        values[14][1]=-1;
   values[15][0]=-1;        values[15][1]=0;
   values[16][0]=-1;        values[16][1]=1;
   values[17][0]=-1;        values[17][1]=2;
   values[18][0]=0;         values[18][1]=-1;
   values[19][0]=1;         values[19][1]=-1;
   values[20][0]=2;         values[20][1]=-1;
//--- remove range
   for(int j=0; j<21; j++)
     {
      int rindex = values[j][0];
      int rcount = values[j][1];
      if(list_test.RemoveRange(rindex,rcount))
         return(false);
     }
//--- check count
   if(list_test.Count()!=count)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove.                                                      |
//+------------------------------------------------------------------+
bool TestRemove(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Testing RemoveRange method on the list with correct input parameters.",test_name);
   if(!TestRemove_Range(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Testing RemoveRange method on the list with incorrect input parameters.",test_name);
   if(!TestRemove_Invalid(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestReverse_Range.                                               |
//+------------------------------------------------------------------+
bool TestReverse_Range(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create clone
   CArrayList<int>list_clone(GetPointer(list_test));
//--- create paremeters
   int values[9][2]=
     {
        {3, 3},
        {0, 10},
        {10, 0},
        {5, 5},
        {0, 5},
        {1, 9},
        {9, 1},
        {2, 8},
        {8, 2}
     };
//--- reverse list
   for(int j=0; j<9; j++)
     {
      int rindex = values[j][0];
      int rcount = values[j][1];
      CArrayList<int>list_actual(GetPointer(list_test));
      list_actual.Reverse(rindex,rcount);
      for(int i=0; i<rindex; i++)
        {
         int value_actual;
         int value_clone;
         list_actual.TryGetValue(i,value_actual);
         list_clone.TryGetValue(i,value_clone);
         if(value_actual!=value_clone)
            return(false);
        }
      int k=0;
      for(int i=rindex; i<rindex+rcount; i++)
        {
         int value_actual;
         int value_clone;
         list_actual.TryGetValue(i,value_actual);
         list_clone.TryGetValue(rindex+rcount-(k+1),value_clone);
         if(value_actual!=value_clone)
            return(false);
         k++;
        }
      for(int i=rindex+rcount; i<list_clone.Count(); i++)
        {
         int value_actual;
         int value_clone;
         list_actual.TryGetValue(i,value_actual);
         list_clone.TryGetValue(i,value_clone);
         if(value_actual!=value_clone)
            return(false);
        }
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestReverse_Invalid.                                             |
//+------------------------------------------------------------------+
bool TestReverse_Invalid(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create invalid paremeters
   int values[21][2];
   values[0][0]=count;      values[0][1]=1;
   values[1][0]=count+1;    values[1][1]=0;
   values[2][0]=count+1;    values[2][1]=1;
   values[3][0]=count;      values[3][1]=2;
   values[4][0]=count/2;    values[4][1]=count/2+1;
   values[5][0]=count-1;    values[5][1]=2;
   values[6][0]=count-2;    values[6][1]=3;
   values[7][0]=1;          values[7][1]=count;
   values[8][0]=0;          values[8][1]=count+1;
   values[9][0]=1;          values[9][1]=count+1;
   values[10][0]=2;         values[10][1]=count;
   values[11][0]=count/2+1; values[11][1]=count/2;
   values[12][0]=2;         values[12][1]=count-1;
   values[13][0]=3;         values[13][1]=count-2;
   values[14][0]=-1;        values[14][1]=-1;
   values[15][0]=-1;        values[15][1]=0;
   values[16][0]=-1;        values[16][1]=1;
   values[17][0]=-1;        values[17][1]=2;
   values[18][0]=0;         values[18][1]=-1;
   values[19][0]=1;         values[19][1]=-1;
   values[20][0]=2;         values[20][1]=-1;
//--- remove range
   for(int j=0; j<21; j++)
     {
      int rindex = values[j][0];
      int rcount = values[j][1];
      if(list_test.Reverse(rindex,rcount))
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestReverse.                                                     |
//+------------------------------------------------------------------+
bool TestReverse(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Testing Reverse method on the list with correct input parameters.",test_name);
   if(!TestReverse_Range(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Testing Reverse method on the list with correct input parameters.",test_name);
   if(!TestReverse_Invalid(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestSort_WithDuplicates.                                         |
//+------------------------------------------------------------------+
bool TestSort_WithDuplicates(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- add duplicates
   int value0;
   list_test.TryGetValue(0, value0);
   list_test.Add(value0);
//--- create comparer
   CDefaultComparer<int>comaprer;
//--- sort list
   list_test.Sort();
//--- check
   for(int i=0; i<count-1; i++)
     {
      int value1;
      int value2;
      list_test.TryGetValue(i, value1);
      list_test.TryGetValue(i+1, value2);
      if(comaprer.Compare(value1,value2)>0)
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestSort_Invalid.                                                |
//+------------------------------------------------------------------+
bool TestSort_Invalid(const int count)
  {
//--- create source list
   CArrayList<int>list_test(count);
   for(int i=0; i<count; i++)
      list_test.Add(MathRand());
//--- create invalid paremeters
   int values[11][2]=
     {
        {-1,-1},
        {-1,0},
        {-1,1},
        {-1,2},
        {-2,0},
        {INT_MIN, 0},
        {0,-1},
        {0,-2},
        {0,INT_MIN},
        {1,-1},
        {2,-1},
     };
//--- reverse list
   for(int j=0; j<11; j++)
     {
      int rindex = values[j][0];
      int rcount = values[j][1];
      if(list_test.Reverse(rindex,rcount))
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestSort.                                                        |
//+------------------------------------------------------------------+
bool TestSort(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Testing Sort method on the list with correct input parameters.",test_name);
   if(!TestSort_WithDuplicates(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Testing Sort method on the list with incorrect input parameters.",test_name);
   if(!TestSort_Invalid(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestArrayList.                                                   |
//+------------------------------------------------------------------+
void TestArrayList(int &tests_performed,int &tests_passed)
  {
   string test_name="";
//--- AddRange functions
   tests_performed++;
   test_name="AddRange functions test";
   if(TestAddRange(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- BinarySearch functions
   tests_performed++;
   test_name="BinarySearch functions test";
   if(TestBinarySearch(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- IndexOf functions
   tests_performed++;
   test_name="IndexOf functions test";
   if(TestIndexOf(test_name))
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

//--- Remove functions
   tests_performed++;
   test_name="Remove functions test";
   if(TestRemove(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- Reverse functions
   tests_performed++;
   test_name="Reverse functions test";
   if(TestReverse(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- Sort functions
   tests_performed++;
   test_name="Sort functions test";
   if(TestSort(test_name))
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
   TestArrayList(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
