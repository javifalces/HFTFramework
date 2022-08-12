//+------------------------------------------------------------------+
//|                                                     ArrayObj.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Array.mqh"
//+------------------------------------------------------------------+
//| Class CArrayObj.                                                 |
//| Puprose: Class of dynamic array of pointers to instances         |
//|          of the CObject class and its derivatives.               |
//|          Derives from class CArray.                              |
//+------------------------------------------------------------------+
class CArrayObj : public CArray
  {
protected:
   CObject          *m_data[];           // data array
   bool              m_free_mode;        // flag of necessity of "physical" deletion of object

public:
                     CArrayObj(void);
                    ~CArrayObj(void);
   //--- methods of access to protected data
   bool              FreeMode(void) const { return(m_free_mode); }
   void              FreeMode(const bool mode) { m_free_mode=mode; }
   //--- method of identifying the object
   virtual int       Type(void) const { return(0x7778); }
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);
   //--- method of creating an element of array
   virtual bool      CreateElement(const int index) { return(false); }
   //--- methods of managing dynamic memory
   bool              Reserve(const int size);
   bool              Resize(const int size);
   bool              Shutdown(void);
   //--- methods of filling the array
   bool              Add(CObject *element);
   bool              AddArray(const CArrayObj *src);
   bool              Insert(CObject *element,const int pos);
   bool              InsertArray(const CArrayObj *src,const int pos);
   bool              AssignArray(const CArrayObj *src);
   //--- method of access to thre array
   CObject          *At(const int index) const;
   //--- methods of changing
   bool              Update(const int index,CObject *element);
   bool              Shift(const int index,const int shift);
   //--- methods of deleting
   CObject          *Detach(const int index);
   bool              Delete(const int index);
   bool              DeleteRange(int from,int to);
   void              Clear(void);
   //--- method for comparing arrays
   bool              CompareArray(const CArrayObj *array) const;
   //--- methods for working with the sorted array
   bool              InsertSort(CObject *element);
   int               Search(const CObject *element) const;
   int               SearchGreat(const CObject *element) const;
   int               SearchLess(const CObject *element) const;
   int               SearchGreatOrEqual(const CObject *element) const;
   int               SearchLessOrEqual(const CObject *element) const;
   int               SearchFirst(const CObject *element) const;
   int               SearchLast(const CObject *element) const;

protected:
   void              QuickSort(int beg,int end,const int mode);
   int               QuickSearch(const CObject *element) const;
   int               MemMove(const int dest,const int src,int count);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CArrayObj::CArrayObj(void) : m_free_mode(true)
  {
//--- initialize protected data
   m_data_max=ArraySize(m_data);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CArrayObj::~CArrayObj(void)
  {
   if(m_data_max!=0)
      Shutdown();
  }
//+------------------------------------------------------------------+
//| Moving the memory within a single array                          |
//+------------------------------------------------------------------+
int CArrayObj::MemMove(const int dest,const int src,int count)
  {
   int i;
//--- check parameters
   if(dest<0 || src<0 || count<0)
      return(-1);
//--- check count
   if(src+count>m_data_total)
      count=m_data_total-src;
   if(count<0)
      return(-1);
//--- no need to copy
   if(dest==src || count==0)
      return(dest);
//--- check data total
   if(dest+count>m_data_total)
     {
      if(m_data_max<dest+count)
         return(-1);
      m_data_total=dest+count;
     }
//--- copy
   if(dest<src)
     {
      //--- copy from left to right
      for(i=0;i<count;i++)
        {
         //--- "physical" removal of the object (if necessary and possible)
         if(m_free_mode && CheckPointer(m_data[dest+i])==POINTER_DYNAMIC)
            delete m_data[dest+i];
         //---
         m_data[dest+i]=m_data[src+i];
         m_data[src+i]=NULL;
        }
     }
   else
     {
      //--- copy from right to left
      for(i=count-1;i>=0;i--)
        {
         //--- "physical" removal of the object (if necessary and possible)
         if(m_free_mode && CheckPointer(m_data[dest+i])==POINTER_DYNAMIC)
            delete m_data[dest+i];
         //---
         m_data[dest+i]=m_data[src+i];
         m_data[src+i]=NULL;
        }
     }
//--- successful
   return(dest);
  }
//+------------------------------------------------------------------+
//| Request for more memory in an array. Checks if the requested     |
//| number of free elements already exists; allocates additional     |
//| memory with a given step                                         |
//+------------------------------------------------------------------+
bool CArrayObj::Reserve(const int size)
  {
   int new_size;
//--- check
   if(size<=0)
      return(false);
//--- resize array
   if(Available()<size)
     {
      new_size=m_data_max+m_step_resize*(1+(size-Available())/m_step_resize);
      if(new_size<0)
         //--- overflow occurred when calculating new_size
         return(false);
      if((m_data_max=ArrayResize(m_data,new_size))==-1)
         m_data_max=ArraySize(m_data);
      //--- explicitly zeroize all the loose items in the array
      for(int i=m_data_total;i<m_data_max;i++)
         m_data[i]=NULL;
     }
//--- result
   return(Available()>=size);
  }
//+------------------------------------------------------------------+
//| Resizing (with removal of elements on the right)                 |
//+------------------------------------------------------------------+
bool CArrayObj::Resize(const int size)
  {
   int new_size;
//--- check
   if(size<0)
      return(false);
//--- resize array
   new_size=m_step_resize*(1+size/m_step_resize);
   if(m_data_total>size)
     {
      //--- "physical" removal of the object (if necessary and possible)
      if(m_free_mode)
         for(int i=size;i<m_data_total;i++)
            if(CheckPointer(m_data[i])==POINTER_DYNAMIC)
               delete m_data[i];
      m_data_total=size;
     }
   if(m_data_max!=new_size)
     {
      if((m_data_max=ArrayResize(m_data,new_size))==-1)
        {
         m_data_max=ArraySize(m_data);
         return(false);
        }
     }
//--- result
   return(m_data_max==new_size);
  }
//+------------------------------------------------------------------+
//| Complete cleaning of the array with the release of memory        |
//+------------------------------------------------------------------+
bool CArrayObj::Shutdown(void)
  {
//--- check
   if(m_data_max==0)
      return(true);
//--- clean
   Clear();
   if(ArrayResize(m_data,0)==-1)
      return(false);
   m_data_max=0;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Adding an element to the end of the array                        |
//+------------------------------------------------------------------+
bool CArrayObj::Add(CObject *element)
  {
//--- check
   if(!CheckPointer(element))
      return(false);
//--- check/reserve elements of array
   if(!Reserve(1))
      return(false);
//--- add
   m_data[m_data_total++]=element;
   m_sort_mode=-1;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Adding an element to the end of the array from another array     |
//+------------------------------------------------------------------+
bool CArrayObj::AddArray(const CArrayObj *src)
  {
   int num;
//--- check
   if(!CheckPointer(src))
      return(false);
//--- check/reserve elements of array
   num=src.Total();
   if(!Reserve(num))
      return(false);
//--- add
   for(int i=0;i<num;i++)
      m_data[m_data_total++]=src.m_data[i];
   m_sort_mode=-1;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Inserting an element in the specified position                   |
//+------------------------------------------------------------------+
bool CArrayObj::Insert(CObject *element,const int pos)
  {
//--- check
   if(pos<0 || !CheckPointer(element))
      return(false);
//--- check/reserve elements of array
   if(!Reserve(1))
      return(false);
//--- insert
   m_data_total++;
   if(pos<m_data_total-1)
     {
      if(MemMove(pos+1,pos,m_data_total-pos-1)<0)
         return(false);
      m_data[pos]=element;
     }
   else
      m_data[m_data_total-1]=element;
   m_sort_mode=-1;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Inserting elements in the specified position                     |
//+------------------------------------------------------------------+
bool CArrayObj::InsertArray(const CArrayObj *src,const int pos)
  {
   int num;
//--- check
   if(!CheckPointer(src))
      return(false);
//--- check/reserve elements of array
   num=src.Total();
   if(!Reserve(num)) return(false);
//--- insert
   if(MemMove(num+pos,pos,m_data_total-pos)<0)
      return(false);
   for(int i=0;i<num;i++)
      m_data[i+pos]=src.m_data[i];
   m_sort_mode=-1;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Assignment (copying) of another array                            |
//+------------------------------------------------------------------+
bool CArrayObj::AssignArray(const CArrayObj *src)
  {
   int num;
//--- check
   if(!CheckPointer(src))
      return(false);
//--- check/reserve elements of array
   num=src.m_data_total;
   Clear();
   if(m_data_max<num)
     {
      if(!Reserve(num))
         return(false);
     }
   else
      Resize(num);
//--- copy array
   for(int i=0;i<num;i++)
     {
      m_data[i]=src.m_data[i];
      m_data_total++;
     }
   m_sort_mode=src.SortMode();
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Access to data in the specified position                         |
//+------------------------------------------------------------------+
CObject *CArrayObj::At(const int index) const
  {
//--- check
   if(index<0 || index>=m_data_total)
      return(NULL);
//--- result
   return(m_data[index]);
  }
//+------------------------------------------------------------------+
//| Updating element in the specified position                       |
//+------------------------------------------------------------------+
bool CArrayObj::Update(const int index,CObject *element)
  {
//--- check
   if(index<0 || !CheckPointer(element) || index>=m_data_total)
      return(false);
//--- "physical" removal of the object (if necessary and possible)
   if(m_free_mode && CheckPointer(m_data[index])==POINTER_DYNAMIC)
      delete m_data[index];
//--- update
   m_data[index]=element;
   m_sort_mode=-1;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Moving element from the specified position                       |
//| on the specified shift                                           |
//+------------------------------------------------------------------+
bool CArrayObj::Shift(const int index,const int shift)
  {
   CObject *tmp_node;
//--- check
   if(index<0 || index+shift<0 || index+shift>=m_data_total)
      return(false);
   if(shift==0)
      return(true);
//--- move
   tmp_node=m_data[index];
   m_data[index]=NULL;
   if(shift>0)
     {
      if(MemMove(index,index+1,shift)<0)
         return(false);
     }
   else
     {
      if(MemMove(index+shift+1,index+shift,-shift)<0)
         return(false);
     }
   m_data[index+shift]=tmp_node;
   m_sort_mode=-1;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Deleting element from the specified position                     |
//+------------------------------------------------------------------+
bool CArrayObj::Delete(const int index)
  {
//--- check
   if(index>=m_data_total)
      return(false);
//--- delete
   if(index<m_data_total-1)
     {
      if(index>=0 && MemMove(index,index+1,m_data_total-index-1)<0)
         return(false);
     }
   else
   if(m_free_mode && CheckPointer(m_data[index])==POINTER_DYNAMIC)
      delete m_data[index];
   m_data_total--;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Detach element from the specified position                       |
//+------------------------------------------------------------------+
CObject *CArrayObj::Detach(const int index)
  {
   CObject *result;
//--- check
   if(index>=m_data_total)
      return(NULL);
//--- detach
   result=m_data[index];
//--- reset the array element, so as not remove the method MemMove
   m_data[index]=NULL;
   if(index<m_data_total-1 && MemMove(index,index+1,m_data_total-index-1)<0)
      return(NULL);
   m_data_total--;
//--- successful
   return(result);
  }
//+------------------------------------------------------------------+
//| Deleting range of elements                                       |
//+------------------------------------------------------------------+
bool CArrayObj::DeleteRange(int from,int to)
  {
//--- check
   if(from<0 || to<0)
      return(false);
   if(from>to || from>=m_data_total)
      return(false);
//--- delete
   if(to>=m_data_total-1)
      to=m_data_total-1;
   if(MemMove(from,to+1,m_data_total-to-1)<0)
      return(false);
   for(int i=to-from+1;i>0;i--,m_data_total--)
      if(m_free_mode && CheckPointer(m_data[m_data_total-1])==POINTER_DYNAMIC)
         delete m_data[m_data_total-1];
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Clearing of array without the release of memory                  |
//+------------------------------------------------------------------+
void CArrayObj::Clear(void)
  {
//--- "physical" removal of the object (if necessary and possible)
   if(m_free_mode)
     {
      for(int i=0;i<m_data_total;i++)
        {
         if(CheckPointer(m_data[i])==POINTER_DYNAMIC)
            delete m_data[i];
         m_data[i]=NULL;
        }
     }
   m_data_total=0;
  }
//+------------------------------------------------------------------+
//| Equality comparison of two arrays                                |
//+------------------------------------------------------------------+
bool CArrayObj::CompareArray(const CArrayObj *array) const
  {
//--- check
   if(!CheckPointer(array))
      return(false);
//--- compare
   if(m_data_total!=array.m_data_total)
      return(false);
   for(int i=0;i<m_data_total;i++)
      if(m_data[i].Compare(array.m_data[i],0)!=0)
         return(false);
//--- equal
   return(true);
  }
//+------------------------------------------------------------------+
//| Method QuickSort                                                 |
//+------------------------------------------------------------------+
void CArrayObj::QuickSort(int beg,int end,const int mode)
  {
   int      i,j;
   CObject *p_node;
   CObject *t_node;
//--- sort
   i=beg;
   j=end;
   while(i<end)
     {
      //--- ">>1" is quick division by 2
      p_node=m_data[(beg+end)>>1];
      while(i<j)
        {
         while(m_data[i].Compare(p_node,mode)<0)
           {
            //--- control the output of the array bounds
            if(i==m_data_total-1)
               break;
            i++;
           }
         while(m_data[j].Compare(p_node,mode)>0)
           {
            //--- control the output of the array bounds
            if(j==0)
               break;
            j--;
           }
         if(i<=j)
           {
            t_node=m_data[i];
            m_data[i++]=m_data[j];
            m_data[j]=t_node;
            //--- control the output of the array bounds
            if(j==0)
               break;
            j--;
           }
        }
      if(beg<j)
         QuickSort(beg,j,mode);
      beg=i;
      j=end;
     }
  }
//+------------------------------------------------------------------+
//| Inserting element in a sorted array                              |
//+------------------------------------------------------------------+
bool CArrayObj::InsertSort(CObject *element)
  {
   int pos;
//--- check
   if(!CheckPointer(element) || m_sort_mode==-1)
      return(false);
//--- check/reserve elements of array
   if(!Reserve(1))
      return(false);
//--- if the array is empty, add an element
   if(m_data_total==0)
     {
      m_data[m_data_total++]=element;
      return(true);
     }
//--- find position and insert
   int mode=m_sort_mode;
   pos=QuickSearch(element);
   if(m_data[pos].Compare(element,m_sort_mode)>0)
      Insert(element,pos);
   else
      Insert(element,pos+1);
//--- restore the sorting flag after Insert(...)
   m_sort_mode=mode;
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Quick search of position of element in a sorted array            |
//+------------------------------------------------------------------+
int CArrayObj::QuickSearch(const CObject *element) const
  {
   int      i,j,m=-1;
   CObject *t_node;
//--- search
   i=0;
   j=m_data_total-1;
   while(j>=i)
     {
      //--- ">>1" is quick division by 2
      m=(j+i)>>1;
      if(m<0 || m==m_data_total-1)
         break;
      t_node=m_data[m];
      if(t_node.Compare(element,m_sort_mode)==0)
         break;
      if(t_node.Compare(element,m_sort_mode)>0)
         j=m-1;
      else
         i=m+1;
     }
//--- position
   return(m);
  }
//+------------------------------------------------------------------+
//| Search of position of element in a sorted array                  |
//+------------------------------------------------------------------+
int CArrayObj::Search(const CObject *element) const
  {
   int pos;
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   pos=QuickSearch(element);
   if(m_data[pos].Compare(element,m_sort_mode)==0)
      return(pos);
//--- not found
   return(-1);
  }
//+------------------------------------------------------------------+
//| Search position of the first element which is greater than       |
//| specified in a sorted array                                      |
//+------------------------------------------------------------------+
int CArrayObj::SearchGreat(const CObject *element) const
  {
   int pos;
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   pos=QuickSearch(element);
   while(m_data[pos].Compare(element,m_sort_mode)<=0)
      if(++pos==m_data_total)
         return(-1);
//--- position
   return(pos);
  }
//+------------------------------------------------------------------+
//| Search position of the first element which is less than          |
//| specified in the sorted array                                    |
//+------------------------------------------------------------------+
int CArrayObj::SearchLess(const CObject *element) const
  {
   int pos;
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   pos=QuickSearch(element);
   while(m_data[pos].Compare(element,m_sort_mode)>=0)
      if(pos--==0)
         return(-1);
//--- position
   return(pos);
  }
//+------------------------------------------------------------------+
//| Search position of the first element which is greater than or    |
//| equal to the specified in a sorted array                         |
//+------------------------------------------------------------------+
int CArrayObj::SearchGreatOrEqual(const CObject *element) const
  {
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   for(int pos=QuickSearch(element);pos<m_data_total;pos++)
      if(m_data[pos].Compare(element,m_sort_mode)>=0)
         return(pos);
//--- not found
   return(-1);
  }
//+------------------------------------------------------------------+
//| Search position of the first element which is less than or equal |
//| to the specified in a sorted array                               |
//+------------------------------------------------------------------+
int CArrayObj::SearchLessOrEqual(const CObject *element) const
  {
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   for(int pos=QuickSearch(element);pos>=0;pos--)
      if(m_data[pos].Compare(element,m_sort_mode)<=0)
         return(pos);
//--- not found
   return(-1);
  }
//+------------------------------------------------------------------+
//| Find position of first appearance of element in a sorted array   |
//+------------------------------------------------------------------+
int CArrayObj::SearchFirst(const CObject *element) const
  {
   int pos;
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   pos=QuickSearch(element);
   if(m_data[pos].Compare(element,m_sort_mode)==0)
     {
      while(m_data[pos].Compare(element,m_sort_mode)==0)
         if(pos--==0)
            break;
      return(pos+1);
     }
//--- not found
   return(-1);
  }
//+------------------------------------------------------------------+
//| Find position of last appearance of element in a sorted array    |
//+------------------------------------------------------------------+
int CArrayObj::SearchLast(const CObject *element) const
  {
   int pos;
//--- check
   if(m_data_total==0 || !CheckPointer(element) || m_sort_mode==-1)
      return(-1);
//--- search
   pos=QuickSearch(element);
   if(m_data[pos].Compare(element,m_sort_mode)==0)
     {
      while(m_data[pos].Compare(element,m_sort_mode)==0)
         if(++pos==m_data_total)
            break;
      return(pos-1);
     }
//--- not found
   return(-1);
  }
//+------------------------------------------------------------------+
//| Writing array to file                                            |
//+------------------------------------------------------------------+
bool CArrayObj::Save(const int file_handle)
  {
   int i=0;
//--- check
   if(!CArray::Save(file_handle))
      return(false);
//--- write array length
   if(FileWriteInteger(file_handle,m_data_total,INT_VALUE)!=INT_VALUE)
      return(false);
//--- write array
   for(i=0;i<m_data_total;i++)
      if(m_data[i].Save(file_handle)!=true)
         break;
//--- result
   return(i==m_data_total);
  }
//+------------------------------------------------------------------+
//| Reading array from file                                          |
//+------------------------------------------------------------------+
bool CArrayObj::Load(const int file_handle)
  {
   int i=0,num;
//--- check
   if(!CArray::Load(file_handle))
      return(false);
//--- read array length
   num=FileReadInteger(file_handle,INT_VALUE);
//--- read array
   Clear();
   if(num!=0)
     {
      if(!Reserve(num))
         return(false);
      for(i=0;i<num;i++)
        {
         //--- create new element
         if(!CreateElement(i))
            break;
         if(m_data[i].Load(file_handle)!=true)
            break;
         m_data_total++;
        }
     }
   m_sort_mode=-1;
//--- result
   return(m_data_total==num);
  }
//+------------------------------------------------------------------+
