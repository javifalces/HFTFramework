//+------------------------------------------------------------------+
//|                                                      FileBMP.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>

//+------------------------------------------------------------------+
//| Bitmap headers                                                   |
//+------------------------------------------------------------------+
struct BITMAPFILEHEADER
  {
   ushort            bfType;
   uint              bfSize;
   ushort            bfReserved1;
   ushort            bfReserved2;
   uint              bfOffBits;
  };
struct BITMAPINFOHEADER
  {
   uint              biSize;
   int               biWidth;
   int               biHeight;
   ushort            biPlanes;
   ushort            biBitCount;
   uint              biCompression;
   uint              biSizeImage;
   int               biXPelsPerMeter;
   int               biYPelsPerMeter;
   uint              biClrUsed;
   uint              biClrImportant;
  };
#define BM 0x4D42
//+------------------------------------------------------------------+
//| Class CFileBMP                                                   |
//| Purpose: Special class to read and write bmp file                |
//|          Derives from class CObject.                             |
//+------------------------------------------------------------------+
class CFileBMP : public CObject
  {
protected:
   int               m_handle;
   BITMAPFILEHEADER  m_file_header;
   BITMAPINFOHEADER  m_info_header;

public:
                     CFileBMP(void);
                    ~CFileBMP(void);
   int               OpenWrite(const string file_name,bool common_flag=false);
   int               OpenRead(const string file_name,bool common_flag=false);
   int               Write32BitsArray(uint& uint_array[],const int width,const int height);
   int               Read32BitsArray(uint& uint_array[],int& width,int& height);
   void              Close(void);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CFileBMP::CFileBMP(void) : m_handle(INVALID_HANDLE)
  {
   ZeroMemory(m_file_header);
   ZeroMemory(m_info_header);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CFileBMP::~CFileBMP(void)
  {
   Close();
  }
//+------------------------------------------------------------------+
//| Open the file                                                    |
//+------------------------------------------------------------------+
int CFileBMP::OpenWrite(const string file_name,bool common_flag)
  {
   Close();
//--- action
   int open_flags=FILE_BIN|FILE_WRITE|FILE_SHARE_READ|FILE_SHARE_WRITE;
   if(common_flag)
      open_flags|=FILE_COMMON;
//--- open
   m_handle=FileOpen(file_name,open_flags);
//--- result
   return(m_handle);
  }
//+------------------------------------------------------------------+
//| Open the file                                                    |
//+------------------------------------------------------------------+
int CFileBMP::OpenRead(const string file_name,bool common_flag)
  {
   Close();
//--- action
   int open_flags=FILE_BIN|FILE_READ|FILE_SHARE_READ|FILE_SHARE_WRITE;
   if(common_flag)
      open_flags|=FILE_COMMON;
//--- open
   m_handle=FileOpen(file_name,open_flags);
//--- check bmp headers
   if(m_handle!=INVALID_HANDLE)
     {
      uint fileheader_size=FileReadStruct(m_handle,m_file_header);
      uint infoheader_size=FileReadStruct(m_handle,m_info_header);
      //--- it should be a simple 32-bit bmp
      if(fileheader_size!=sizeof(m_file_header) ||
         infoheader_size!=sizeof(m_info_header) ||
         m_file_header.bfType!=BM ||
         m_file_header.bfOffBits!=sizeof(m_file_header)+sizeof(m_info_header) ||
         m_info_header.biBitCount!=32 ||
         m_info_header.biClrUsed!=0)
         Close();
     }
//--- result
   return(m_handle);
  }
//+------------------------------------------------------------------+
//| Write the file                                                   |
//+------------------------------------------------------------------+
int CFileBMP::Write32BitsArray(uint& uint_array[],const int width,const int height)
  {
   if(m_handle==INVALID_HANDLE)
      return(-1);
//--- check size
   int size=width*height;
   if(size==0)
      return(0);
   if(size<0)
      size=-size;
   if(ArraySize(uint_array)<size)
      return(-2);
//--- prepare headers
   ZeroMemory(m_file_header);
   ZeroMemory(m_info_header);
   m_file_header.bfType=BM;
   m_file_header.bfSize=sizeof(m_file_header)+sizeof(m_info_header)+size*sizeof(uint);
   m_file_header.bfOffBits=sizeof(m_file_header)+sizeof(m_info_header);
   m_info_header.biSize=sizeof(m_info_header);
   m_info_header.biWidth=width;
   m_info_header.biHeight=height;
   m_info_header.biPlanes=1;
   m_info_header.biBitCount=32;
   m_info_header.biSizeImage=size*32;
//--- write bmp-file
   FileSeek(m_handle,0,SEEK_SET);
   FileWriteStruct(m_handle,m_file_header);
   FileWriteStruct(m_handle,m_info_header);
   uint written=FileWriteArray(m_handle,uint_array,0,size);
//--- bytes written
   return((int)written*sizeof(uint));
  }
//+------------------------------------------------------------------+
//| Read the file                                                    |
//+------------------------------------------------------------------+
int CFileBMP::Read32BitsArray(uint& uint_array[],int& width,int& height)
  {
   if(m_handle==INVALID_HANDLE)
      return(-1);
//--- store dimensions from header
   width=m_info_header.biWidth;
   height=m_info_header.biHeight;
//--- check size
   int size=width*height;
   if(size==0)
      return(0);
   if(size<0)
      size=-size;
//--- read bmp-file
   FileSeek(m_handle,sizeof(m_file_header)+sizeof(m_info_header),SEEK_SET);
   uint read=FileReadArray(m_handle,uint_array,0,size);
//--- bytes read
   return((int)read*sizeof(uint));
  }
//+------------------------------------------------------------------+
//| Close the file                                                   |
//+------------------------------------------------------------------+
void CFileBMP::Close(void)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      FileClose(m_handle);
      m_handle=INVALID_HANDLE;
     }
  }
//+------------------------------------------------------------------+
