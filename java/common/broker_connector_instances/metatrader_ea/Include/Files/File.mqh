//+------------------------------------------------------------------+
//|                                                         File.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CFile.                                                     |
//| Purpose: Base class of file operations.                          |
//|          Derives from class CObject.                             |
//+------------------------------------------------------------------+
class CFile : public CObject
  {
protected:
   int               m_handle;             // handle of file
   string            m_name;               // name of opened file
   int               m_flags;              // flags of opened file

public:
                     CFile(void);
                    ~CFile(void);
   //--- methods of access to protected data
   int               Handle(void)              const { return(m_handle); };
   string            FileName(void)            const { return(m_name);   };
   int               Flags(void)               const { return(m_flags);  };
   void              SetUnicode(const bool unicode);
   void              SetCommon(const bool common);
   //--- general methods for working with files
   int               Open(const string file_name,int open_flags,const short delimiter='\t');
   void              Close(void);
   void              Delete(void);
   ulong             Size(void);
   ulong             Tell(void);
   void              Seek(const long offset,const ENUM_FILE_POSITION origin);
   void              Flush(void);
   bool              IsEnding(void);
   bool              IsLineEnding(void);
   //--- general methods for working with files
   void              Delete(const string file_name,const int common_flag=0);
   bool              IsExist(const string file_name,const int common_flag=0);
   bool              Copy(const string src_name,const int common_flag,const string dst_name,const int mode_flags);
   bool              Move(const string src_name,const int common_flag,const string dst_name,const int mode_flags);
   //--- general methods of working with folders
   bool              FolderCreate(const string folder_name);
   bool              FolderDelete(const string folder_name);
   bool              FolderClean(const string folder_name);
   //--- general methods of finding files
   long              FileFindFirst(const string file_filter,string &returned_filename);
   bool              FileFindNext(const long search_handle,string &returned_filename);
   void              FileFindClose(const long search_handle);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CFile::CFile(void) : m_handle(INVALID_HANDLE),
                     m_name(""),
                     m_flags(FILE_ANSI)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CFile::~CFile(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      Close();
  }
//+------------------------------------------------------------------+
//| Set the FILE_UNICODE flag                                        |
//+------------------------------------------------------------------+
void CFile::SetUnicode(const bool unicode)
  {
//--- check handle
   if(m_handle==INVALID_HANDLE)
     {
      if(unicode)
         m_flags|=FILE_UNICODE;
      else
         m_flags&=~FILE_UNICODE;
     }
  }
//+------------------------------------------------------------------+
//| Set the "Common Folder" flag                                     |
//+------------------------------------------------------------------+
void CFile::SetCommon(const bool common)
  {
//--- check handle
   if(m_handle==INVALID_HANDLE)
     {
      if(common)
         m_flags|=FILE_COMMON;
      else
         m_flags&=~FILE_COMMON;
     }
  }
//+------------------------------------------------------------------+
//| Open the file                                                    |
//+------------------------------------------------------------------+
int CFile::Open(const string file_name,int open_flags,const short delimiter)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      Close();
//--- action
   if((open_flags &(FILE_BIN|FILE_CSV))==0)
      open_flags|=FILE_TXT;
//--- open
   m_handle=FileOpen(file_name,open_flags|m_flags,delimiter);
   if(m_handle!=INVALID_HANDLE)
     {
      //--- store options of the opened file
      m_flags|=open_flags;
      m_name=file_name;
     }
//--- result
   return(m_handle);
  }
//+------------------------------------------------------------------+
//| Close the file                                                   |
//+------------------------------------------------------------------+
void CFile::Close(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      //--- closing the file and resetting all the variables to the initial state
      FileClose(m_handle);
      m_handle=INVALID_HANDLE;
      m_name="";
      //--- reset all flags except the text
      m_flags&=FILE_ANSI|FILE_UNICODE;
     }
  }
//+------------------------------------------------------------------+
//| Deleting an open file                                            |
//+------------------------------------------------------------------+
void CFile::Delete(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      string file_name=m_name;
      int    common_flag=m_flags&FILE_COMMON;
      //--- close before deleting
      Close();
      //--- delete
      FileDelete(file_name,common_flag);
     }
  }
//+------------------------------------------------------------------+
//| Get size of opened file                                          |
//+------------------------------------------------------------------+
ulong CFile::Size(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileSize(m_handle));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Get current position of pointer in file                          |
//+------------------------------------------------------------------+
ulong CFile::Tell(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileTell(m_handle));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Set position of pointer in file                                  |
//+------------------------------------------------------------------+
void CFile::Seek(const long offset,const ENUM_FILE_POSITION origin)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      FileSeek(m_handle,offset,origin);
  }
//+------------------------------------------------------------------+
//| Flush data from the file buffer of input-output to disk          |
//+------------------------------------------------------------------+
void CFile::Flush(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      FileFlush(m_handle);
  }
//+------------------------------------------------------------------+
//| Detect the end of file                                           |
//+------------------------------------------------------------------+
bool CFile::IsEnding(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileIsEnding(m_handle));
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Detect the end of string                                         |
//+------------------------------------------------------------------+
bool CFile::IsLineEnding(void)
  {
//--- checking
   if(m_handle!=INVALID_HANDLE)
      if((m_flags&FILE_BIN)==0)
         return(FileIsLineEnding(m_handle));
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Deleting a file                                                  |
//+------------------------------------------------------------------+
void CFile::Delete(const string file_name,const int common_flag)
  {
//--- checking
   if(file_name==m_name)
     {
      int flag=m_flags&FILE_COMMON;
      if(flag==common_flag)
         Close();
     }
//--- delete
   FileDelete(file_name,common_flag);
  }
//+------------------------------------------------------------------+
//| Check if file exists                                             |
//+------------------------------------------------------------------+
bool CFile::IsExist(const string file_name,const int common_flag)
  {
   return(FileIsExist(file_name,common_flag));
  }
//+------------------------------------------------------------------+
//| Copying file                                                     |
//+------------------------------------------------------------------+
bool CFile::Copy(const string src_name,const int common_flag,const string dst_name,const int mode_flags)
  {
   return(FileCopy(src_name,common_flag,dst_name,mode_flags));
  }
//+------------------------------------------------------------------+
//| Move/rename file                                                 |
//+------------------------------------------------------------------+
bool CFile::Move(const string src_name,const int common_flag,const string dst_name,const int mode_flags)
  {
   return(FileMove(src_name,common_flag,dst_name,mode_flags));
  }
//+------------------------------------------------------------------+
//| Create folder                                                    |
//+------------------------------------------------------------------+
bool CFile::FolderCreate(const string folder_name)
  {
   return(::FolderCreate(folder_name,m_flags));
  }
//+------------------------------------------------------------------+
//| Delete folder                                                    |
//+------------------------------------------------------------------+
bool CFile::FolderDelete(const string folder_name)
  {
   return(::FolderDelete(folder_name,m_flags));
  }
//+------------------------------------------------------------------+
//| Clean folder                                                     |
//+------------------------------------------------------------------+
bool CFile::FolderClean(const string folder_name)
  {
   return(::FolderClean(folder_name,m_flags));
  }
//+------------------------------------------------------------------+
//| Start search of files                                            |
//+------------------------------------------------------------------+
long CFile::FileFindFirst(const string file_filter,string &returned_filename)
  {
   return(::FileFindFirst(file_filter,returned_filename,m_flags));
  }
//+------------------------------------------------------------------+
//| Continue search of files                                         |
//+------------------------------------------------------------------+
bool CFile::FileFindNext(const long search_handle,string &returned_filename)
  {
   return(::FileFindNext(search_handle,returned_filename));
  }
//+------------------------------------------------------------------+
//| End search of files                                              |
//+------------------------------------------------------------------+
void CFile::FileFindClose(const long search_handle)
  {
   ::FileFindClose(search_handle);
  }
//+------------------------------------------------------------------+
