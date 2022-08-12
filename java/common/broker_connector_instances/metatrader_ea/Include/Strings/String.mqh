//+------------------------------------------------------------------+
//|                                                       String.mqh |
//|                   Copyright 2009-2013, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CString.                                                   |
//| Appointment: Class-string.                                       |
//|              Derives from class CObject.                         |
//+------------------------------------------------------------------+
class CString : public CObject
  {
protected:
   string            m_string;

public:
                     CString(void);
                    ~CString(void);
   //--- methods access
   string            Str(void)             const { return(m_string);                       };
   uint              Len(void)             const { return(StringLen(m_string));            };
   void              Copy(string &copy) const;
   void              Copy(CString *copy) const;
   //--- methods fill
   bool              Fill(const short character) { return(StringFill(m_string,character)); };
   void              Assign(const string str)    { m_string=str;                           };
   void              Assign(const CString *str)  { m_string=str.Str();                     };
   void              Append(const string str);
   void              Append(const CString *str);
   uint              Insert(const uint pos,const string substring);
   uint              Insert(const uint pos,const CString *substring);
   //--- methods compare
   int               Compare(const string str) const;
   int               Compare(const CString *str) const;
   int               CompareNoCase(const string str) const;
   int               CompareNoCase(const CString *str) const;
   //--- methods prepare
   string            Left(const uint count) const;
   string            Right(const uint count) const;
   string            Mid(const uint pos,const uint count) const;
   //--- methods truncation/deletion
   int               Trim(const string targets);
   int               TrimLeft(const string targets);
   int               TrimRight(const string targets);
   bool              Clear(void)   { return(StringInit(m_string));    };
   //--- methods conversion
   bool              ToUpper(void) { return(StringToUpper(m_string)); };
   bool              ToLower(void) { return(StringToLower(m_string)); };
   void              Reverse(void);
   //--- methods find
   int               Find(const uint start,const string substring) const;
   int               FindRev(const string substring) const;
   uint              Remove(const string substring);
   uint              Replace(const string substring,const string newstring);

protected:
   virtual int       Compare(const CObject *node,const int mode=0) const;
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CString::CString(void) : m_string("")
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CString::~CString(void)
  {
  }
//+------------------------------------------------------------------+
//| Copy the string value member to copy                             |
//+------------------------------------------------------------------+
void CString::Copy(string &copy) const
  {
   copy=m_string;
  }
//+------------------------------------------------------------------+
//| Copy the string value member to copy                             |
//+------------------------------------------------------------------+
void CString::Copy(CString *copy) const
  {
   copy.Assign(m_string);
  }
//+------------------------------------------------------------------+
//| Add a string to the end                                          |
//+------------------------------------------------------------------+
void CString::Append(const string str)
  {
   m_string+=str;
  }
//+------------------------------------------------------------------+
//| Add a string to the end                                          |
//+------------------------------------------------------------------+
void CString::Append(const CString *str)
  {
//--- check
   if(!CheckPointer(str))
      return;
//---
   m_string+=str.Str();
  }
//+------------------------------------------------------------------+
//| Insert a string in specified position                            |
//+------------------------------------------------------------------+
uint CString::Insert(const uint pos,const string substring)
  {
   string tmp=StringSubstr(m_string,0,pos);
//---
   tmp+=substring;
   m_string=tmp+StringSubstr(m_string,pos);
//--- result
   return(StringLen(m_string));
  }
//+------------------------------------------------------------------+
//| Insert a string in specified position                            |
//+------------------------------------------------------------------+
uint CString::Insert(const uint pos,const CString *substring)
  {
//--- check
   if(!CheckPointer(substring))
      return(0);
//---
   string tmp=StringSubstr(m_string,0,pos);
//---
   tmp+=substring.Str();
   m_string=tmp+StringSubstr(m_string,pos);
//--- result
   return(StringLen(m_string));
  }
//+------------------------------------------------------------------+
//| Comparison with the string                                       |
//+------------------------------------------------------------------+
int CString::Compare(const string str) const
  {
   if(m_string<str)
      return(-1);
   if(m_string>str)
      return(1);
//--- equal
   return(0);
  }
//+------------------------------------------------------------------+
//| Comparison with the string                                       |
//+------------------------------------------------------------------+
int CString::Compare(const CString *str) const
  {
//--- check
   if(!CheckPointer(str))
      return(0);
//---
   if(m_string<str.Str())
      return(-1);
   if(m_string>str.Str())
      return(1);
//--- equal
   return(0);
  }
//+------------------------------------------------------------------+
//| Comparison with the string without case                          |
//+------------------------------------------------------------------+
int CString::CompareNoCase(const string str) const
  {
   string tmp1,tmp2;
//---
   tmp1=m_string;
   tmp2=str;
   StringToLower(tmp1);
   StringToLower(tmp2);
//---
   if(tmp1<tmp2)
      return(-1);
   if(tmp1>tmp2)
      return(1);
//--- equal
   return(0);
  }
//+------------------------------------------------------------------+
//| Comparison with the string without case                          |
//+------------------------------------------------------------------+
int CString::CompareNoCase(const CString *str) const
  {
   string tmp1,tmp2;
//--- check
   if(!CheckPointer(str))
      return(0);
//---
   tmp1=m_string;
   tmp2=str.Str();
   StringToLower(tmp1);
   StringToLower(tmp2);
//---
   if(tmp1<tmp2)
      return(-1);
   if(tmp1>tmp2)
      return(1);
//--- equal
   return(0);
  }
//+------------------------------------------------------------------+
//| Find occurrences of substring from the specified position        |
//+------------------------------------------------------------------+
int CString::Find(const uint start,const string substring) const
  {
   return(StringFind(m_string,substring,start));
  }
//+------------------------------------------------------------------+
//| Find last occurrence of substring                                |
//+------------------------------------------------------------------+
int CString::FindRev(const string substring) const
  {
   int result,pos=-1;
//---
   do
     {
      result=pos;
     }
   while((pos=StringFind(m_string,substring,pos+1))>=0);
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Get a substring consisting of count elements of the left string  |
//+------------------------------------------------------------------+
string CString::Left(const uint count) const
  {
   return(StringSubstr(m_string,0,count));
  }
//+------------------------------------------------------------------+
//| Get a substring consisting of count elements of the right string.|
//+------------------------------------------------------------------+
string CString::Right(const uint count) const
  {
   return(StringSubstr(m_string,StringLen(m_string)-count,count));
  }
//+------------------------------------------------------------------+
//| Get a substring consisting of count elements of the pos string   |
//+------------------------------------------------------------------+
string CString::Mid(const uint pos,const uint count) const
  {
   return(StringSubstr(m_string,pos,count));
  }
//+------------------------------------------------------------------+
//| Remove from the string, all characters in the begin and          |
//| in the end if they arein targets, or space, \t,\n or \r          |
//+------------------------------------------------------------------+
int CString::Trim(const string targets)
  {
   return(TrimLeft(targets)+TrimRight(targets));
  }
//+------------------------------------------------------------------+
//| Remove from the string, all characters in the begin if they are  |
//| in targets, or space, \t,\n or \r                                |
//+------------------------------------------------------------------+
int CString::TrimLeft(const string targets)
  {
   ushort ch;
//---
   for(int i=0;i<StringLen(m_string);i++)
     {
      ch=StringGetCharacter(m_string,i);
      if(ch<=' ')
         continue;
      for(int j=0;j<StringLen(targets);j++)
        {
         if(ch==StringGetCharacter(targets,j))
           {
            StringSetCharacter(m_string,i,' ');
            ch=' ';
           }
        }
      if(ch!=' ')
         break;
     }
//--- result
   return(StringTrimLeft(m_string));
  }
//+------------------------------------------------------------------+
//| Remove from the string, all characters in the end if they are    |
//| in targets, or space, \t,\n or \r                                |
//+------------------------------------------------------------------+
int CString::TrimRight(const string targets)
  {
   ushort ch;
//---
   for(int i=StringLen(m_string)-1;i>=0;i--)
     {
      ch=StringGetCharacter(m_string,i);
      if(ch<=' ')
         continue;
      for(int j=0;j<StringLen(targets);j++)
        {
         if(ch==StringGetCharacter(targets,j))
           {
            StringSetCharacter(m_string,i,' ');
            ch=' ';
           }
        }
      if(ch!=' ')
         break;
     }
//--- result
   return(StringTrimRight(m_string));
  }
//+------------------------------------------------------------------+
//| Deploy the sequence of characters in a string                    |
//+------------------------------------------------------------------+
void CString::Reverse(void)
  {
   ushort ch;
   int    i,j;
//---
   for(i=StringLen(m_string)-1,j=0;i>j;i--,j++)
     {
      ch=StringGetCharacter(m_string,i);
      StringSetCharacter(m_string,i,StringGetCharacter(m_string,j));
      StringSetCharacter(m_string,j,ch);
     }
  }
//+------------------------------------------------------------------+
//| Remove all occurrences of the substring                          |
//+------------------------------------------------------------------+
uint CString::Remove(const string substring)
  {
   int    result=0,len,pos=-1;
   string tmp;
//---
   len=StringLen(substring);
   while((pos=StringFind(m_string,substring,pos))>=0)
     {
      tmp=StringSubstr(m_string,0,pos);
      m_string=tmp+StringSubstr(m_string,pos+len);
      result++;
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Replace all occurrences of a substring in the specified string   |
//+------------------------------------------------------------------+
uint CString::Replace(const string substring,const string newstring)
  {
   int    result=0,len,pos=-1;
   string tmp;
//---
   len=StringLen(substring);
   while((pos=StringFind(m_string,substring,pos))>=0)
     {
      tmp=StringSubstr(m_string,0,pos)+newstring;
      m_string=tmp+StringSubstr(m_string,pos+len);
      // to eliminate possible loops
      pos+=StringLen(newstring);
      result++;
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Comparison with the string  by algorithm                         |
//+------------------------------------------------------------------+
int CString::Compare(const CObject *node,const int mode=0) const
  {
   CString *str=(CString*)node;
//--- check
   if(str==NULL)
      return(0);
//---
   switch(mode)
     {
      case 0: return(Compare(str));
      case 1: return(CompareNoCase(str));
     }
//--- equal
   return(0);
  }
//+------------------------------------------------------------------+
