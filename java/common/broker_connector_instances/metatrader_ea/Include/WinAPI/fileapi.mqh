//+------------------------------------------------------------------+
//|                                                      fileapi.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>

//---
enum STREAM_INFO_LEVELS
  {
   FindStreamInfoStandard,
   FindStreamInfoMaxInfoLevel
  };
//---
struct BY_HANDLE_FILE_INFORMATION
  {
   uint              dwFileAttributes;
   FILETIME          ftCreationTime;
   FILETIME          ftLastAccessTime;
   FILETIME          ftLastWriteTime;
   uint              dwVolumeSerialNumber;
   uint              nFileSizeHigh;
   uint              nFileSizeLow;
   uint              nNumberOfLinks;
   uint              nFileIndexHigh;
   uint              nFileIndexLow;
  };
//---
struct CREATEFILE2_EXTENDED_PARAMETERS
  {
   uint              dwSize;
   uint              dwFileAttributes;
   uint              dwFileFlags;
   uint              dwSecurityQosFlags;
   PVOID             lpSecurityAttributes;
   HANDLE            hTemplateFile;
  };
//---
struct FILE_ATTRIBUTE_DATA
  {
   uint              dwFileAttributes;
   FILETIME          ftCreationTime;
   FILETIME          ftLastAccessTime;
   FILETIME          ftLastWriteTime;
   uint              nFileSizeHigh;
   uint              nFileSizeLow;
  };
//---
struct FIND_STREAM_DATA
  {
   long              StreamSize;
   short             cStreamName[MAX_PATH+36];
  };
//---
struct FIND_DATAW
  {
   uint              dwFileAttributes;
   FILETIME          ftCreationTime;
   FILETIME          ftLastAccessTime;
   FILETIME          ftLastWriteTime;
   uint              nFileSizeHigh;
   uint              nFileSizeLow;
   uint              dwReserved0;
   uint              dwReserved1;
   short             cFileName[MAX_PATH];
   short             cAlternateFileName[14];
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int    AreFileApisANSI(void);
int    CompareFileTime(FILETIME &file_time1,FILETIME &file_time2);
int    CreateDirectoryW(const string path_name,PVOID security_attributes);
HANDLE CreateFile2(const string file_name,uint desired_access,uint share_mode,uint creation_disposition,CREATEFILE2_EXTENDED_PARAMETERS &create_ex_params);
HANDLE CreateFileW(const string file_name,uint desired_access,uint share_mode,PVOID security_attributes,uint creation_disposition,uint flags_and_attributes,HANDLE template_file);
int    DefineDosDeviceW(uint flags,const string device_name,const string target_path);
int    DeleteFileW(const string file_name);
int    DeleteVolumeMountPointW(const string volume_mount_point);
int    FileTimeToLocalFileTime(FILETIME &file_time,FILETIME &local_file_time);
int    FindClose(HANDLE find_file);
int    FindCloseChangeNotification(HANDLE change_handle);
HANDLE FindFirstChangeNotificationW(const string path_name,int watch_subtree,uint notify_filter);
HANDLE FindFirstFileExW(const string file_name,FINDEX_INFO_LEVELS info_level_id,FIND_DATAW &find_file_data,FINDEX_SEARCH_OPS search_op,PVOID search_filter,uint additional_flags);
HANDLE FindFirstFileNameW(const string file_name,uint flags,uint &StringLength,ushort &LinkName[]);
HANDLE FindFirstFileW(const string file_name,FIND_DATAW &find_file_data);
HANDLE FindFirstStreamW(const string file_name,STREAM_INFO_LEVELS InfoLevel,FIND_STREAM_DATA &find_stream_data,uint flags);
HANDLE FindFirstVolumeW(ushort &volume_name[],uint &buffer_length);
int    FindNextChangeNotification(HANDLE change_handle);
int    FindNextFileNameW(HANDLE find_stream,uint &StringLength,ushort &LinkName[]);
int    FindNextFileW(HANDLE find_file,FIND_DATAW &find_file_data);
int    FindNextStreamW(HANDLE find_stream,FIND_STREAM_DATA &find_stream_data);
int    FindNextVolumeW(HANDLE find_volume,ushort &volume_name[],uint &buffer_length);
int    FindVolumeClose(HANDLE find_volume);
int    FlushFileBuffers(HANDLE file);
uint   GetCompressedFileSizeW(const string file_name,uint &file_size_high);
int    GetDiskFreeSpaceExW(const string directory_name,ulong &free_bytes_available_to_caller,ulong &total_number_of_bytes,ulong &total_number_of_free_bytes);
int    GetDiskFreeSpaceW(const string root_path_name,uint &sectors_per_cluster,uint &bytes_per_sector,uint &number_of_free_clusters,uint &total_number_of_clusters);
uint   GetDriveTypeW(const string root_path_name);
int    GetFileAttributesExW(const string file_name,GET_FILEEX_INFO_LEVELS info_level_id,FILE_ATTRIBUTE_DATA &file_information);
uint   GetFileAttributesW(const string file_name);
int    GetFileInformationByHandle(HANDLE file,BY_HANDLE_FILE_INFORMATION &file_information);
uint   GetFileSize(HANDLE file,uint &file_size_high);
int    GetFileSizeEx(HANDLE file,long &file_size);
int    GetFileTime(HANDLE file,FILETIME &creation_time,FILETIME &last_access_time,FILETIME &last_write_time);
uint   GetFileType(HANDLE file);
uint   GetFinalPathNameByHandleW(HANDLE file,ushort &file_path[],uint file_path,uint flags);
uint   GetFullPathNameW(const string file_name,uint buffer_length,ushort &buffer[],ushort &file_part[]);
uint   GetLogicalDrives(void);
uint   GetLogicalDriveStringsW(uint buffer_length,ushort &buffer[]);
uint   GetLongPathNameW(const string short_path,string &long_path,uint buffer);
uint   GetShortPathNameW(const string long_path,string &short_path,uint buffer);
uint   GetTempFileNameW(const string path_name,const string prefix_string,uint unique,ushort &temp_file_name[]);
uint   GetTempPathW(uint buffer_length,ushort &buffer[]);
int    GetVolumeInformationByHandleW(HANDLE file,ushort &volume_name_buffer[],uint volume_name_size,uint &volume_serial_number,uint &maximum_component_length,uint &file_system_flags,ushort &file_system_name_buffer[],uint file_system_name_size);
int    GetVolumeInformationW(const string root_path_name,ushort &volume_name_buffer[],uint volume_name_size,uint &volume_serial_number,uint &maximum_component_length,uint &file_system_flags,ushort &file_system_name_buffer[],uint file_system_name_size);
int    GetVolumeNameForVolumeMountPointW(const string volume_mount_point,string volume_name,uint buffer_length);
int    GetVolumePathNamesForVolumeNameW(const string volume_name,string volume_path_names,uint buffer_length,uint &return_length);
int    GetVolumePathNameW(const string file_name,ushort &volume_path_name[],uint buffer_length);
int    LocalFileTimeToFileTime(FILETIME &local_file_time,FILETIME &file_time);
int    LockFile(HANDLE file,uint file_offset_low,uint file_offset_high,uint number_of_bytes_to_lock_low,uint number_of_bytes_to_lock_high);
int    LockFileEx(HANDLE file,uint flags,uint reserved,uint number_of_bytes_to_lock_low,uint number_of_bytes_to_lock_high,OVERLAPPED &overlapped);
uint   QueryDosDeviceW(const string device_name,ushort &target_path[],uint max);
int    ReadFile(HANDLE file,ushort &buffer[],uint number_of_bytes_to_read,uint &number_of_bytes_read,OVERLAPPED &overlapped);
int    ReadFile(HANDLE file,ushort &buffer[],uint number_of_bytes_to_read,uint &number_of_bytes_read,PVOID overlapped);
int    ReadFileScatter(HANDLE file,FILE_SEGMENT_ELEMENT &segment_array[],uint number_of_bytes_to_read,uint &reserved,OVERLAPPED &overlapped);
int    ReadFileScatter(HANDLE file,FILE_SEGMENT_ELEMENT &segment_array[],uint number_of_bytes_to_read,uint &reserved,PVOID overlapped);
int    RemoveDirectoryW(const string path_name);
int    SetEndOfFile(HANDLE file);
void   SetFileApisToANSI(void);
void   SetFileApisToOEM(void);
int    SetFileAttributesW(const string file_name,uint file_attributes);
int    SetFileInformationByHandle(HANDLE file,FILE_INFO_BY_HANDLE_CLASS FileInformationClass,FILE_INFO &file_information,uint buffer_size);
int    SetFileIoOverlappedRange(HANDLE FileHandle,uchar &OverlappedRangeStart,uint Length);
uint   SetFilePointer(HANDLE file,long distance_to_move,long &distance_to_move_high,uint move_method);
int    SetFilePointerEx(HANDLE file,long distance_to_move,long &new_file_pointer,uint move_method);
int    SetFileTime(HANDLE file,FILETIME &creation_time,FILETIME &last_access_time,FILETIME &last_write_time);
int    SetFileValidData(HANDLE file,long ValidDataLength);
int    UnlockFile(HANDLE file,uint file_offset_low,uint file_offset_high,uint number_of_bytes_to_unlock_low,uint number_of_bytes_to_unlock_high);
int    UnlockFileEx(HANDLE file,uint reserved,uint number_of_bytes_to_unlock_low,uint number_of_bytes_to_unlock_high,OVERLAPPED &overlapped);
int    WriteFile(HANDLE file,const ushort &buffer[],uint number_of_bytes_to_write,uint &number_of_bytes_written,OVERLAPPED &overlapped);
int    WriteFile(HANDLE file,const ushort &buffer[],uint number_of_bytes_to_write,uint &number_of_bytes_written,PVOID overlapped);
int    WriteFileGather(HANDLE file,FILE_SEGMENT_ELEMENT &segment_array[],uint number_of_bytes_to_write,uint &reserved,OVERLAPPED &overlapped);
int    WriteFileGather(HANDLE file,FILE_SEGMENT_ELEMENT &segment_array[],uint number_of_bytes_to_write,uint &reserved,PVOID overlapped);
#import
//+------------------------------------------------------------------+
