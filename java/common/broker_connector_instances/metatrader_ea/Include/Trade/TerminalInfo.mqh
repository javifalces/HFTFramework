//+------------------------------------------------------------------+
//|                                                 TerminalInfo.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CTerminalInfo.                                             |
//| Appointment: Class for access to terminal info.                  |
//|              Derives from class CObject.                         |
//+------------------------------------------------------------------+
class CTerminalInfo : public CObject
  {
public:
                     CTerminalInfo(void);
                    ~CTerminalInfo(void);
   //--- fast access methods to the integer terminal propertyes
   int               Build(void) const;
   bool              IsConnected(void) const;
   bool              IsDLLsAllowed(void) const;
   bool              IsTradeAllowed(void) const;
   bool              IsEmailEnabled(void) const;
   bool              IsFtpEnabled(void) const;
   int               MaxBars(void) const;
   int               CodePage(void) const;
   int               CPUCores(void) const;
   int               MemoryPhysical(void) const;
   int               MemoryTotal(void) const;
   int               MemoryAvailable(void) const;
   int               MemoryUsed(void) const;
   bool              IsX64(void) const;
   int               OpenCLSupport(void) const;
   int               DiskSpace(void) const;
   //--- fast access methods to the string terminal propertyes
   string            Language(void) const;
   string            Name(void) const;
   string            Company(void) const;
   string            Path(void) const;
   string            DataPath(void) const;
   string            CommonDataPath(void) const;
   //--- access methods to the API MQL5 functions
   long              InfoInteger(const ENUM_TERMINAL_INFO_INTEGER prop_id) const;
   string            InfoString(const ENUM_TERMINAL_INFO_STRING prop_id) const;
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CTerminalInfo::CTerminalInfo(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CTerminalInfo::~CTerminalInfo(void)
  {
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_BUILD"                          |
//+------------------------------------------------------------------+
int CTerminalInfo::Build(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_BUILD));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_CONNECTED"                      |
//+------------------------------------------------------------------+
bool CTerminalInfo::IsConnected(void) const
  {
   return((bool)TerminalInfoInteger(TERMINAL_CONNECTED));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_DLLS_ALLOWED"                   |
//+------------------------------------------------------------------+
bool CTerminalInfo::IsDLLsAllowed(void) const
  {
   return((bool)TerminalInfoInteger(TERMINAL_DLLS_ALLOWED));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_TRADE_ALLOWED"                  |
//+------------------------------------------------------------------+
bool CTerminalInfo::IsTradeAllowed(void) const
  {
   return((bool)TerminalInfoInteger(TERMINAL_TRADE_ALLOWED));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_EMAIL_ENABLED"                  |
//+------------------------------------------------------------------+
bool CTerminalInfo::IsEmailEnabled(void) const
  {
   return((bool)TerminalInfoInteger(TERMINAL_EMAIL_ENABLED));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_FTP_ENABLED"                    |
//+------------------------------------------------------------------+
bool CTerminalInfo::IsFtpEnabled(void) const
  {
   return((bool)TerminalInfoInteger(TERMINAL_FTP_ENABLED));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_MAXBARS"                        |
//+------------------------------------------------------------------+
int CTerminalInfo::MaxBars(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_MAXBARS));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_CODEPAGE"                       |
//+------------------------------------------------------------------+
int CTerminalInfo::CodePage(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_CODEPAGE));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_CPU_CORES"                      |
//+------------------------------------------------------------------+
int CTerminalInfo::CPUCores(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_CPU_CORES));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_MEMORY_PHYSICAL"                |
//+------------------------------------------------------------------+
int CTerminalInfo::MemoryPhysical(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_MEMORY_PHYSICAL));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_MEMORY_TOTAL"                   |
//+------------------------------------------------------------------+
int CTerminalInfo::MemoryTotal(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_MEMORY_TOTAL));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_MEMORY_AVAILABLE"               |
//+------------------------------------------------------------------+
int CTerminalInfo::MemoryAvailable(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_MEMORY_AVAILABLE));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_MEMORY_USED"                    |
//+------------------------------------------------------------------+
int CTerminalInfo::MemoryUsed(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_MEMORY_USED));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_X64"                            |
//+------------------------------------------------------------------+
bool CTerminalInfo::IsX64(void) const
  {
   return((bool)TerminalInfoInteger(TERMINAL_X64));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_OPENCL_SUPPORT"                 |
//+------------------------------------------------------------------+
int CTerminalInfo::OpenCLSupport(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_OPENCL_SUPPORT));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_DISK_SPACE"                     |
//+------------------------------------------------------------------+
int CTerminalInfo::DiskSpace(void) const
  {
   return((int)TerminalInfoInteger(TERMINAL_DISK_SPACE));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_LANGUAGE"                       |
//+------------------------------------------------------------------+
string CTerminalInfo::Language(void) const
  {
   return(TerminalInfoString(TERMINAL_LANGUAGE));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_NAME"                           |
//+------------------------------------------------------------------+
string CTerminalInfo::Name(void) const
  {
   return(TerminalInfoString(TERMINAL_NAME));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_COMPANY"                        |
//+------------------------------------------------------------------+
string CTerminalInfo::Company(void) const
  {
   return(TerminalInfoString(TERMINAL_COMPANY));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_PATH"                           |
//+------------------------------------------------------------------+
string CTerminalInfo::Path(void) const
  {
   return(TerminalInfoString(TERMINAL_PATH));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_DATA_PATH"                      |
//+------------------------------------------------------------------+
string CTerminalInfo::DataPath(void) const
  {
   return(TerminalInfoString(TERMINAL_DATA_PATH));
  }
//+------------------------------------------------------------------+
//| Get the property value "TERMINAL_COMMONDATA_PATH"                |
//+------------------------------------------------------------------+
string CTerminalInfo::CommonDataPath(void) const
  {
   return(TerminalInfoString(TERMINAL_COMMONDATA_PATH));
  }
//+------------------------------------------------------------------+
//| Access functions AccountInfoInteger(...)                         |
//+------------------------------------------------------------------+
long CTerminalInfo::InfoInteger(const ENUM_TERMINAL_INFO_INTEGER prop_id) const
  {
   return(TerminalInfoInteger(prop_id));
  }
//+------------------------------------------------------------------+
//| Access functions AccountInfoString(...)                          |
//+------------------------------------------------------------------+
string CTerminalInfo::InfoString(const ENUM_TERMINAL_INFO_STRING prop_id) const
  {
   return(TerminalInfoString(prop_id));
  }
//+------------------------------------------------------------------+
