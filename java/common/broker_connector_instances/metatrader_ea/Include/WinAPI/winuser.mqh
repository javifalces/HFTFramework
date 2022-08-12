//+------------------------------------------------------------------+
//|                                                      WinUser.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\wingdi.mqh>

//---
#define POINTER_DEVICE_PRODUCT_STRING_MAX 520
#define KL_NAMELENGTH                     9

//---
enum AR_STATE
  {
   AR_ENABLED=0x0,
   AR_DISABLED=0x1,
   AR_SUPPRESSED=0x2,
   AR_REMOTESESSION=0x4,
   AR_MULTIMON=0x8,
   AR_NOSENSOR=0x10,
   AR_NOT_SUPPORTED=0x20,
   AR_DOCKED=0x40,
   AR_LAPTOP=0x80
  };
//---
enum DIALOG_CONTROL_DPI_CHANGE_BEHAVIORS
  {
   DCDC_DEFAULT=0x0000,
   DCDC_DISABLE_FONT_UPDATE=0x0001,
   DCDC_DISABLE_RELAYOUT=0x0002
  };
//---
enum DIALOG_DPI_CHANGE_BEHAVIORS
  {
   DDC_DEFAULT=0x0000,
   DDC_DISABLE_ALL=0x0001,
   DDC_DISABLE_RESIZE=0x0002,
   DDC_DISABLE_CONTROL_RELAYOUT=0x0004
  };
//---
enum EDIT_CONTROL_FEATURE
  {
   EDIT_CONTROL_FEATURE_ENTERPRISE_DATA_PROTECTION_PASTE_SUPPORT=0,
   EDIT_CONTROL_FEATURE_PASTE_NOTIFICATIONS=1
  };
//---
enum FEEDBACK_TYPE
  {
   FEEDBACK_TOUCH_CONTACTVISUALIZATION=1,
   FEEDBACK_PEN_BARRELVISUALIZATION=2,
   FEEDBACK_PEN_TAP=3,
   FEEDBACK_PEN_DOUBLETAP=4,
   FEEDBACK_PEN_PRESSANDHOLD=5,
   FEEDBACK_PEN_RIGHTTAP=6,
   FEEDBACK_TOUCH_TAP=7,
   FEEDBACK_TOUCH_DOUBLETAP=8,
   FEEDBACK_TOUCH_PRESSANDHOLD=9,
   FEEDBACK_TOUCH_RIGHTTAP=10,
   FEEDBACK_GESTURE_PRESSANDTAP=11,
   FEEDBACK_MAX=0xFFFFFFFF
  };
//---
enum HANDEDNESS
  {
   HANDEDNESS_LEFT=0,
   HANDEDNESS_RIGHT
  };
//---
enum INPUT_MESSAGE_DEVICE_TYPE
  {
   IMDT_UNAVAILABLE=0x00000000,
   IMDT_KEYBOARD=0x00000001,
   IMDT_MOUSE=0x00000002,
   IMDT_TOUCH=0x00000004,
   IMDT_PEN=0x00000008,
   IMDT_TOUCHPAD=0x00000010
  };
//---
enum INPUT_MESSAGE_ORIGIN_ID
  {
   IMO_UNAVAILABLE=0x00000000,
   IMO_HARDWARE=0x00000001,
   IMO_INJECTED=0x00000002,
   IMO_SYSTEM=0x00000004
  };
//---
enum ORIENTATION_PREFERENCE
  {
   ORIENTATION_PREFERENCE_NONE=0x0,
   ORIENTATION_PREFERENCE_LANDSCAPE=0x1,
   ORIENTATION_PREFERENCE_PORTRAIT=0x2,
   ORIENTATION_PREFERENCE_LANDSCAPE_FLIPPED=0x4,
   ORIENTATION_PREFERENCE_PORTRAIT_FLIPPED=0x8
  };
//---
enum POINTER_BUTTON_CHANGE_TYPE
  {
   POINTER_CHANGE_NONE,
   POINTER_CHANGE_FIRSTBUTTON_DOWN,
   POINTER_CHANGE_FIRSTBUTTON_UP,
   POINTER_CHANGE_SECONDBUTTON_DOWN,
   POINTER_CHANGE_SECONDBUTTON_UP,
   POINTER_CHANGE_THIRDBUTTON_DOWN,
   POINTER_CHANGE_THIRDBUTTON_UP,
   POINTER_CHANGE_FOURTHBUTTON_DOWN,
   POINTER_CHANGE_FOURTHBUTTON_UP,
   POINTER_CHANGE_FIFTHBUTTON_DOWN,
   POINTER_CHANGE_FIFTHBUTTON_UP
  };
//---
enum POINTER_DEVICE_CURSOR_TYPE
  {
   POINTER_DEVICE_CURSOR_TYPE_UNKNOWN=0x00000000,
   POINTER_DEVICE_CURSOR_TYPE_TIP=0x00000001,
   POINTER_DEVICE_CURSOR_TYPE_ERASER=0x00000002,
   POINTER_DEVICE_CURSOR_TYPE_MAX=0xFFFFFFFF
  };
//---
enum POINTER_DEVICE_TYPE
  {
   POINTER_DEVICE_TYPE_INTEGRATED_PEN=0x00000001,
   POINTER_DEVICE_TYPE_EXTERNAL_PEN=0x00000002,
   POINTER_DEVICE_TYPE_TOUCH=0x00000003,
   POINTER_DEVICE_TYPE_TOUCH_PAD=0x00000004,
   POINTER_DEVICE_TYPE_MAX=0xFFFFFFFF
  };
//---
struct ACCEL
  {
   uchar             fVirt;
   ushort            key;
   ushort            cmd;
  };
//---
struct ACCESSTIMEOUT
  {
   uint              cbSize;
   uint              dwFlags;
   uint              iTimeOutMSec;
  };
//---
struct ALTTABINFO
  {
   uint              cbSize;
   int               cItems;
   int               cColumns;
   int               cRows;
   int               iColFocus;
   int               iRowFocus;
   int               cxItem;
   int               cyItem;
   POINT             ptStart;
  };
//---
struct ANIMATIONINFO
  {
   uint              cbSize;
   int               iMinAnimate;
  };
//---
struct AUDIODESCRIPTION
  {
   uint              cbSize;
   int               Enabled;
   uint              Locale;
  };
//---
struct BSMINFO
  {
   uint              cbSize;
   HANDLE            hdesk;
   HANDLE            hwnd;
   LUID              luid;
  };
//---
struct CBT_CREATEWNDA
  {
   HANDLE            hwndInsertAfter;
  };
//---
struct CBT_CREATEWNDW
  {
   HANDLE            hwndInsertAfter;
  };
//---
struct CBTACTIVATESTRUCT
  {
   int               fMouse;
   HANDLE            hWndActive;
  };
//---
struct CHANGEFILTERSTRUCT
  {
   uint              cbSize;
   uint              ExtStatus;
  };
//---
struct CLIENTCREATESTRUCT
  {
   HANDLE            hWindowMenu;
   uint              idFirstChild;
  };
//---
struct COMBOBOXINFO
  {
   uint              cbSize;
   RECT              rcItem;
   RECT              rcButton;
   uint              stateButton;
   HANDLE            hwndCombo;
   HANDLE            hwndItem;
   HANDLE            hwndList;
  };
//---
struct COMPAREITEMSTRUCT
  {
   uint              CtlType;
   uint              CtlID;
   HANDLE            hwndItem;
   uint              itemID1;
   ulong             itemData1;
   uint              itemID2;
   ulong             itemData2;
   uint              dwLocaleId;
  };
//---
struct COPYDATASTRUCT
  {
   ulong             dwData;
   uint              cbData;
  };
//---
struct CREATESTRUCTW pack(8)
  {
   PVOID             lpCreateParams;
   HANDLE            hInstance;
   HANDLE            hMenu;
   HANDLE            hwndParent;
   int               cy;
   int               cx;
   int               y;
   int               x;
   int               style;
   PVOID             lpszName;
   PVOID             lpszClass;
   uint              dwExStyle;
  };
//---
struct CURSORINFO
  {
   uint              cbSize;
   uint              flags;
   HANDLE            hCursor;
   POINT             ptScreenPos;
  };
//---
struct CURSORSHAPE
  {
   int               xHotSpot;
   int               yHotSpot;
   int               cx;
   int               cy;
   int               cbWidth;
   uchar             Planes;
   uchar             BitsPixel;
  };
//---
struct CWPRETSTRUCT
  {
   PVOID             lResult;
   PVOID             lParam;
   PVOID             wParam;
   uint              message;
   HANDLE            hwnd;
  };
//---
struct CWPSTRUCT
  {
   PVOID             lParam;
   PVOID             wParam;
   uint              message;
   HANDLE            hwnd;
  };
//---
struct DEBUGHOOKINFO
  {
   uint              idThread;
   uint              idThreadInstaller;
   PVOID             lParam;
   PVOID             wParam;
   int               code;
  };
//---
struct DELETEITEMSTRUCT
  {
   uint              CtlType;
   uint              CtlID;
   uint              itemID;
   HANDLE            hwndItem;
   ulong             itemData;
  };
//---
struct DLGITEMTEMPLATE
  {
   uint              style;
   uint              dwExtendedStyle;
   short             x;
   short             y;
   short             cx;
   short             cy;
   ushort            id;
  };
//---
struct DLGTEMPLATE
  {
   uint              style;
   uint              dwExtendedStyle;
   ushort            cdit;
   short             x;
   short             y;
   short             cx;
   short             cy;
  };
//---
struct DRAWITEMSTRUCT
  {
   uint              CtlType;
   uint              CtlID;
   uint              itemID;
   uint              itemAction;
   uint              itemState;
   HANDLE            hwndItem;
   HANDLE            hDC;
   RECT              rcItem;
   ulong             itemData;
  };
//---
struct DRAWTEXTPARAMS
  {
   uint              cbSize;
   int               iTabLength;
   int               iLeftMargin;
   int               iRightMargin;
   uint              uiLengthDrawn;
  };
//---
struct DROPSTRUCT
  {
   HANDLE            hwndSource;
   HANDLE            hwndSink;
   uint              wFmt;
   ulong             dwData;
   POINT             ptDrop;
   uint              dwControlData;
  };
//---
struct EVENTMSG
  {
   uint              message;
   uint              paramL;
   uint              paramH;
   uint              time;
   HANDLE            hwnd;
  };
//---
struct FILTERKEYS
  {
   uint              cbSize;
   uint              dwFlags;
   uint              iWaitMSec;
   uint              iDelayMSec;
   uint              iRepeatMSec;
   uint              iBounceMSec;
  };
//---
struct FLASHWINFO
  {
   uint              cbSize;
   HANDLE            hwnd;
   uint              dwFlags;
   uint              uCount;
   uint              dwTimeout;
  };
//---
struct GESTURECONFIG
  {
   uint              dwID;
   uint              dwWant;
   uint              dwBlock;
  };
//---
struct GESTUREINFO
  {
   uint              cbSize;
   uint              dwFlags;
   uint              dwID;
   HANDLE            hwndTarget;
   POINTS            ptsLocation;
   uint              dwInstanceID;
   uint              dwSequenceID;
   ulong             ullArguments;
   uint              cbExtraArgs;
  };
//---
struct GESTURENOTIFYSTRUCT
  {
   uint              cbSize;
   uint              dwFlags;
   HANDLE            hwndTarget;
   POINTS            ptsLocation;
   uint              dwInstanceID;
  };
//---
struct GUITHREADINFO
  {
   uint              cbSize;
   uint              flags;
   HANDLE            hwndActive;
   HANDLE            hwndFocus;
   HANDLE            hwndCapture;
   HANDLE            hwndMenuOwner;
   HANDLE            hwndMoveSize;
   HANDLE            hwndCaret;
   RECT              rcCaret;
  };
//---
struct HARDWAREHOOKSTRUCT
  {
   HANDLE            hwnd;
   uint              message;
   PVOID             wParam;
   PVOID             lParam;
  };
//---
struct HARDWAREINPUT
  {
   uint              uMsg;
   ushort            wParamL;
   ushort            wParamH;
  };
//---
struct HELPINFO
  {
   uint              cbSize;
   int               iContextType;
   int               iCtrlId;
   HANDLE            hItemHandle;
   uint              dwContextId;
   POINT             MousePos;
  };
//---
struct HELPWININFOA
  {
   int               wStructSize;
   int               x;
   int               y;
   int               dx;
   int               dy;
   int               wMax;
   char              rgchMember[2];
  };
//---
struct HELPWININFOW
  {
   int               wStructSize;
   int               x;
   int               y;
   int               dx;
   int               dy;
   int               wMax;
   short             rgchMember[2];
  };
//---
struct HIGHCONTRASTW
  {
   uint              cbSize;
   uint              dwFlags;
   string            lpszDefaultScheme;
  };
//---
struct ICONINFO
  {
   int               fIcon;
   uint              xHotspot;
   uint              yHotspot;
   HANDLE            hbmMask;
   HANDLE            hbmColor;
  };
//---
struct ICONINFOEXW
  {
   uint              cbSize;
   int               fIcon;
   uint              xHotspot;
   uint              yHotspot;
   HANDLE            hbmMask;
   HANDLE            hbmColor;
   ushort            wResID;
   short             szModName[MAX_PATH];
   short             szResName[MAX_PATH];
  };
//---
struct ICONMETRICSW
  {
   uint              cbSize;
   int               iHorzSpacing;
   int               iVertSpacing;
   int               iTitleWrap;
   LOGFONTW          lfFont;
  };
//---
struct INPUT_INJECTION_VALUE
  {
   ushort            page;
   ushort            usage;
   int               value;
   ushort            index;
  };
//---
struct INPUT_MESSAGE_SOURCE
  {
   INPUT_MESSAGE_DEVICE_TYPE deviceType;
   INPUT_MESSAGE_ORIGIN_ID originId;
  };
//---
struct KBDLLHOOKSTRUCT
  {
   uint              vkCode;
   uint              scanCode;
   uint              flags;
   uint              time;
   ulong             dwExtraInfo;
  };
//---
struct KEYBDINPUT
  {
   ushort            wVk;
   ushort            wScan;
   uint              dwFlags;
   uint              time;
   ulong             dwExtraInfo;
  };
//---
struct LASTINPUTINFO
  {
   uint              cbSize;
   uint              dwTime;
  };
//---
struct MDICREATESTRUCTW
  {
   PVOID             szClass;
   PVOID             szTitle;
   HANDLE            hOwner;
   int               x;
   int               y;
   int               cx;
   int               cy;
   uint              style;
   PVOID             lParam;
  };
//---
struct MDINEXTMENU
  {
   HANDLE            hmenuIn;
   HANDLE            hmenuNext;
   HANDLE            hwndNext;
  };
//---
struct MEASUREITEMSTRUCT
  {
   uint              CtlType;
   uint              CtlID;
   uint              itemID;
   uint              itemWidth;
   uint              itemHeight;
   ulong             itemData;
  };
//---
struct MENUBARINFO
  {
   uint              cbSize;
   RECT              rcBar;
   HANDLE            hMenu;
   HANDLE            hwndMenu;
   int               Focused;
  };
//---
struct MENUGETOBJECTINFO
  {
   uint              dwFlags;
   uint              uPos;
   HANDLE            hmenu;
   PVOID             riid;
   PVOID             pvObj;
  };
//---
struct MENUINFO
  {
   uint              cbSize;
   uint              fMask;
   uint              dwStyle;
   uint              cyMax;
   HANDLE            hbrBack;
   uint              dwContextHelpID;
   ulong             dwMenuData;
  };
//---
struct MENUITEMINFOW
  {
   uint              cbSize;
   uint              fMask;
   uint              fType;
   uint              fState;
   uint              wID;
   HANDLE            hSubMenu;
   HANDLE            hbmpChecked;
   HANDLE            hbmpUnchecked;
   ulong             dwItemData;
   string            dwTypeData;
   uint              cch;
   HANDLE            hbmpItem;
  };
//---
struct MENUITEMTEMPLATE
  {
   ushort            mtOption;
   ushort            mtID;
   short             mtString[1];
  };
//---
struct MENUITEMTEMPLATEHEADER
  {
   ushort            versionNumber;
   ushort            offset;
  };
//---
struct MINIMIZEDMETRICS
  {
   uint              cbSize;
   int               iWidth;
   int               iHorzGap;
   int               iVertGap;
   int               iArrange;
  };
//---
struct MINMAXINFO
  {
   POINT             ptReserved;
   POINT             ptMaxSize;
   POINT             ptMaxPosition;
   POINT             ptMinTrackSize;
   POINT             ptMaxTrackSize;
  };
//---
struct MONITORINFO
  {
   uint              cbSize;
   RECT              rcMonitor;
   RECT              rcWork;
   uint              dwFlags;
  };
//---
struct MOUSEHOOKSTRUCT
  {
   POINT             pt;
   HANDLE            hwnd;
   uint              wHitTestCode;
   ulong             dwExtraInfo;
  };
//---
struct MOUSEHOOKSTRUCTEX: public MONITORINFO
  {
   uint              mouseData;
  };
//---
struct MOUSEINPUT pack(8)
  {
   int               dx;
   int               dy;
   uint              mouseData;
   uint              dwFlags;
   uint              time;
   ulong             dwExtraInfo;
  };
//---
struct MOUSEKEYS
  {
   uint              cbSize;
   uint              dwFlags;
   uint              iMaxSpeed;
   uint              iTimeToMaxSpeed;
   uint              iCtrlSpeed;
   uint              dwReserved1;
   uint              dwReserved2;
  };
//---
struct MOUSEMOVEPOINT
  {
   int               x;
   int               y;
   uint              time;
   ulong             dwExtraInfo;
  };
//---
struct MSG
  {
   HANDLE            hwnd;
   uint              message;
   PVOID             wParam;
   PVOID             lParam;
   uint              time;
   POINT             pt;
   uint              lPrivate;
  };
//---
struct MSGBOXPARAMSW
  {
   uint              cbSize;
   HANDLE            hwndOwner;
   HANDLE            hInstance;
   PVOID             lpszText;
   PVOID             lpszCaption;
   uint              dwStyle;
   PVOID             lpszIcon;
   uint              dwContextHelpId;
   PVOID             lpfnMsgBoxCallback;
   uint              dwLanguageId;
  };
//---
struct MSLLHOOKSTRUCT
  {
   POINT             pt;
   uint              mouseData;
   uint              flags;
   uint              time;
   ulong             dwExtraInfo;
  };
//---
struct MULTIKEYHELPW
  {
   short             mkKeylist;
   short             szKeyphrase[1];
  };
//---
struct NCCALCSIZE_PARAMS
  {
   RECT              rgrc[3];
   PVOID             lppos;
  };
//---
struct NMHDR
  {
   HANDLE            hwndFrom;
   ulong             idFrom;
   uint              code;
  };
//---
struct NONCLIENTMETRICSW
  {
   uint              cbSize;
   int               iBorderWidth;
   int               iScrollWidth;
   int               iScrollHeight;
   int               iCaptionWidth;
   int               iCaptionHeight;
   LOGFONTW          lfCaptionFont;
   int               iSmCaptionWidth;
   int               iSmCaptionHeight;
   LOGFONTW          lfSmCaptionFont;
   int               iMenuWidth;
   int               iMenuHeight;
   LOGFONTW          lfMenuFont;
   LOGFONTW          lfStatusFont;
   LOGFONTW          lfMessageFont;
   int               iPaddedBorderWidth;
  };
//---
struct PAINTSTRUCT
  {
   HANDLE            hdc;
   int               fErase;
   RECT              rcPaint;
   int               fRestore;
   int               fIncUpdate;
   uchar             rgbReserved[32];
  };
//---
struct POINTER_DEVICE_CURSOR_INFO
  {
   uint              cursorId;
   POINTER_DEVICE_CURSOR_TYPE cursor;
  };
//---
struct POINTER_DEVICE_INFO pack(8)
  {
   uint              displayOrientation;
   HANDLE            device;
   POINTER_DEVICE_TYPE pointerDeviceType;
   HANDLE            monitor;
   uint              startingCursorId;
   ushort            maxActiveContacts;
   short             productString[POINTER_DEVICE_PRODUCT_STRING_MAX];
  };
//---
struct POINTER_DEVICE_PROPERTY
  {
   int               logicalMin;
   int               logicalMax;
   int               physicalMin;
   int               physicalMax;
   uint              unit;
   uint              unitExponent;
   ushort            usagePageId;
   ushort            usageId;
  };
//---
struct POINTER_INFO
  {
   uint              pointerType;
   uint              pointerId;
   uint              frameId;
   uint              pointerFlags;
   HANDLE            sourceDevice;
   HANDLE            hwndTarget;
   POINT             ptPixelLocation;
   POINT             ptHimetricLocation;
   POINT             ptPixelLocationRaw;
   POINT             ptHimetricLocationRaw;
   uint              dwTime;
   uint              historyCount;
   int               InputData;
   uint              dwKeyStates;
   ulong             PerformanceCount;
   POINTER_BUTTON_CHANGE_TYPE ButtonChangeType;
  };
//---
struct POINTER_PEN_INFO
  {
   POINTER_INFO      pointerInfo;
   uint              penFlags;
   uint              penMask;
   uint              pressure;
   uint              rotation;
   int               tiltX;
   int               tiltY;
  };
//---
struct POINTER_TOUCH_INFO
  {
   POINTER_INFO      pointerInfo;
   uint              touchFlags;
   uint              touchMask;
   RECT              rcContact;
   RECT              rcContactRaw;
   uint              orientation;
   uint              pressure;
  };
//---
struct POWERBROADCAST_SETTING
  {
   GUID              PowerSetting;
   uint              DataLength;
   uchar             Data[1];
  };
//---
struct RAWINPUTDEVICE
  {
   ushort            usUsagePage;
   ushort            usUsage;
   uint              dwFlags;
   HANDLE            hwndTarget;
  };
//---
struct RAWINPUTDEVICELIST
  {
   HANDLE            hDevice;
   uint              dwType;
  };
//---
struct RAWINPUTHEADER
  {
   uint              dwType;
   uint              dwSize;
   HANDLE            hDevice;
   PVOID             wParam;
  };
//---
struct RID_DEVICE_INFO_HID
  {
   uint              dwVendorId;
   uint              dwProductId;
   uint              dwVersionNumber;
   ushort            usUsagePage;
   ushort            usUsage;
  };
//---
struct RID_DEVICE_INFO_KEYBOARD
  {
   uint              dwType;
   uint              dwSubType;
   uint              dwKeyboardMode;
   uint              dwNumberOfFunctionKeys;
   uint              dwNumberOfIndicators;
   uint              dwNumberOfKeysTotal;
  };
//---
struct RID_DEVICE_INFO_MOUSE
  {
   uint              dwId;
   uint              dwNumberOfButtons;
   uint              dwSampleRate;
   int               fHasHorizontalWheel;
  };
//---
struct SCROLLBARINFO
  {
   uint              cbSize;
   RECT              rcScrollBar;
   int               dxyLineButton;
   int               xyThumbTop;
   int               xyThumbBottom;
   int               reserved;
  };
//---
struct SCROLLINFO
  {
   uint              cbSize;
   uint              fMask;
   int               nMin;
   int               nMax;
   uint              nPage;
   int               nPos;
   int               nTrackPos;
  };
//---
struct SERIALKEYSW
  {
   uint              cbSize;
   uint              dwFlags;
   string            lpszActivePort;
   string            lpszPort;
   uint              iBaudRate;
   uint              iPortState;
   uint              iActive;
  };
//---
struct SHELLHOOKINFO
  {
   HANDLE            hwnd;
   RECT              rc;
  };
//---
struct SOUNDSENTRYW
  {
   uint              cbSize;
   uint              dwFlags;
   uint              iFSTextEffect;
   uint              iFSTextEffectMSec;
   uint              iFSTextEffectColorBits;
   uint              iFSGrafEffect;
   uint              iFSGrafEffectMSec;
   uint              iFSGrafEffectColor;
   uint              iWindowsEffect;
   uint              iWindowsEffectMSec;
   string            lpszWindowsEffectDLL;
   uint              iWindowsEffectOrdinal;
  };
//---
struct STICKYKEYS
  {
   uint              cbSize;
   uint              dwFlags;
  };
//---
struct STYLESTRUCT
  {
   uint              styleOld;
   uint              styleNew;
  };
//---
struct TITLEBARINFO
  {
   uint              cbSize;
   RECT              rcTitleBar;
  };
//---
struct TITLEBARINFOEX
  {
   uint              cbSize;
   RECT              rcTitleBar;
  };
//---
struct TOGGLEKEYS
  {
   uint              cbSize;
   uint              dwFlags;
  };
//---
struct TOUCH_HIT_TESTING_INPUT
  {
   uint              pointerId;
   POINT             point;
   RECT              boundingBox;
   RECT              nonOccludedBoundingBox;
   uint              orientation;
  };
//---
struct TOUCH_HIT_TESTING_PROXIMITY_EVALUATION
  {
   ushort            score;
   POINT             adjustedPoint;
  };
//---
struct TOUCHINPUT
  {
   int               x;
   int               y;
   HANDLE            hSource;
   uint              dwID;
   uint              dwFlags;
   uint              dwMask;
   uint              dwTime;
   ulong             dwExtraInfo;
   uint              cxContact;
   uint              cyContact;
  };
//---
struct TOUCHPREDICTIONPARAMETERS
  {
   uint              cbSize;
   uint              dwLatency;
   uint              dwSampleTime;
   uint              bUseHWTimeStamp;
  };
//---
struct TPMPARAMS
  {
   uint              cbSize;
   RECT              rcExclude;
  };
//---
struct TRACKMOUSEEVENT
  {
   uint              cbSize;
   uint              dwFlags;
   HANDLE            hwndTrack;
   uint              dwHoverTime;
  };
//---
struct UPDATELAYEREDWINDOWINFO
  {
   uint              cbSize;
   HANDLE            hdcDst;
   HANDLE            hdcSrc;
   uint              crKey;
   uint              dwFlags;
  };
//---
struct USAGE_PROPERTIES
  {
   ushort            level;
   ushort            page;
   ushort            usage;
   int               logicalMinimum;
   int               logicalMaximum;
   ushort            unit;
   ushort            exponent;
   uchar             count;
   int               physicalMinimum;
   int               physicalMaximum;
  };
//---
struct USEROBJECTFLAGS
  {
   int               fInherit;
   int               fReserved;
   uint              dwFlags;
  };
//---
struct WINDOWINFO
  {
   uint              cbSize;
   RECT              rcWindow;
   RECT              rcClient;
   uint              dwStyle;
   uint              dwExStyle;
   uint              dwWindowStatus;
   uint              cxWindowBorders;
   uint              cyWindowBorders;
   ushort            atomWindowType;
   ushort            wCreatorVersion;
  };
//---
struct WINDOWPLACEMENT
  {
   uint              length;
   uint              flags;
   uint              showCmd;
   POINT             ptMinPosition;
   POINT             ptMaxPosition;
   RECT              rcNormalPosition;
   RECT              rcDevice;
  };
//---
struct WINDOWPOS
  {
   HANDLE            hwnd;
   HANDLE            hwndInsertAfter;
   int               x;
   int               y;
   int               cx;
   int               cy;
   uint              flags;
  };
//---
struct WNDCLASSEXW pack(8)
  {
   uint              cbSize;
   uint              style;
   PVOID             lpfnWndProc;
   int               cbClsExtra;
   int               cbWndExtra;
   HANDLE            hInstance;
   HANDLE            hIcon;
   HANDLE            hCursor;
   HANDLE            hbrBackground;
   PVOID             lpszMenuName;
   PVOID             lpszClassName;
   HANDLE            hIconSm;
  };
//---
struct WNDCLASSW pack(8)
  {
   uint              style;
   PVOID             lpfnWndProc;
   int               cbClsExtra;
   int               cbWndExtra;
   HANDLE            hInstance;
   HANDLE            hIcon;
   HANDLE            hCursor;
   HANDLE            hbrBackground;
   PVOID             lpszMenuName;
   PVOID             lpszClassName;
  };
//---
struct WTSSESSION_NOTIFICATION
  {
   uint              cbSize;
   uint              dwSessionId;
  };
//---
struct RAWMOUSE pack(4)
  {
   ushort            usFlags;
   uint              ulButtons;
   uint              ulRawButtons;
   int               lLastX;
   int               lLastY;
   uint              ulExtraInformation;
  };
//---
struct RAWKEYBOARD
  {
   ushort            MakeCode;
   ushort            Flags;
   ushort            Reserved;
   ushort            VKey;
   uint              Message;
   uint              ExtraInformation;
  };
//---
struct RAWHID
  {
   uint              dwSizeHid;
   uint              dwCount;
   uchar             bRawData[1];
  };
//---
union RAWFORMAT
  {
   RAWMOUSE          mouse;
   RAWKEYBOARD       keyboard;
   RAWHID            hid;
  };
//---
struct RAWINPUT
  {
   RAWINPUTHEADER    header;
   RAWFORMAT         data;
  };
//---
struct INPUT_TRANSFORM
  {
   float             _11;
   float             _12;
   float             _13;
   float             _14;
   float             _21;
   float             _22;
   float             _23;
   float             _24;
   float             _31;
   float             _32;
   float             _33;
   float             _34;
   float             _41;
   float             _42;
   float             _43;
   float             _44;
  };
//---
struct MENUITEMINFO
  {
   uint              cbSize;
   uint              fMask;
   uint              fType;
   uint              fState;
   uint              wID;
   HANDLE            hSubMenu;
   HANDLE            hbmpChecked;
   HANDLE            hbmpUnchecked;
   uint              dwItemData;
   string            dwTypeData;
   uint              cch;
  };
//---
union INPUT_TYPE
  {
   MOUSEINPUT        mi;
   KEYBDINPUT        ki;
   HARDWAREINPUT     hi;
  };
//---
struct INPUT
  {
   uint              type;
   INPUT_TYPE        in;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "user32.dll"
HANDLE                              ActivateKeyboardLayout(HANDLE hkl,uint Flags);
int                                 AddClipboardFormatListener(HANDLE hwnd);
int                                 AdjustWindowRect(RECT &rect,uint style,int menu);
int                                 AdjustWindowRectEx(RECT &rect,uint style,int menu,uint ex_style);
int                                 AdjustWindowRectExForDpi(RECT &rect,uint style,int menu,uint ex_style,uint dpi);
int                                 AllowSetForegroundWindow(uint process_id);
int                                 AnimateWindow(HANDLE wnd,uint time,uint flags);
int                                 AnyPopup(void);
int                                 AppendMenuW(HANDLE menu,uint flags,ulong uIDNewItem,const string new_item);
int                                 AreDpiAwarenessContextsEqual(HANDLE dpiContextA,HANDLE dpiContextB);
uint                                ArrangeIconicWindows(HANDLE wnd);
int                                 AttachThreadInput(uint attach,uint attach_to,int attach);
HANDLE                              BeginDeferWindowPos(int num_windows);
HANDLE                              BeginPaint(HANDLE wnd,PAINTSTRUCT &paint);
int                                 BlockInput(int block_it);
int                                 BringWindowToTop(HANDLE wnd);
int                                 BroadcastSystemMessage(uint flags,uint &info,uint Msg,PVOID param,PVOID param);
int                                 BroadcastSystemMessageExW(uint flags,uint &info,uint Msg,PVOID param,PVOID param,BSMINFO &info);
int                                 BroadcastSystemMessageW(uint flags,uint &info,uint Msg,PVOID param,PVOID param);
int                                 CalculatePopupWindowPosition(const POINT &point,const SIZE &size,uint flags,RECT &rect,RECT &window_position);
int                                 CallMsgFilterW(MSG &msg,int code);
PVOID                               CallNextHookEx(HANDLE hhk,int code,PVOID param,PVOID param);
PVOID                               CallWindowProcW(PVOID prev_wnd_func,HANDLE wnd,uint Msg,PVOID param,PVOID param);
int                                 CancelShutdown(void);
ushort                              CascadeWindows(HANDLE parent,uint how,const RECT &rect,uint kids,const HANDLE &kids[]);
int                                 ChangeClipboardChain(HANDLE wnd_remove,HANDLE wnd_new_next);
int                                 ChangeDisplaySettingsExW(const string device_name,DEVMODEW &dev_mode,HANDLE hwnd,uint dwflags,PVOID param);
int                                 ChangeDisplaySettingsW(DEVMODEW &dev_mode,uint flags);
int                                 ChangeMenuW(HANDLE menu,uint cmd,const string new_item,uint insert,uint flags);
int                                 ChangeWindowMessageFilter(uint message,uint flag);
int                                 ChangeWindowMessageFilterEx(HANDLE hwnd,uint message,uint action,CHANGEFILTERSTRUCT &change_filter_struct);
uint                                CharLowerBuffW(string &lpsz,uint length);
PVOID                               CharLowerW(string &lpsz);
PVOID                               CharNextW(PVOID lpsz);
PVOID                               CharNextW(string lpsz);
PVOID                               CharPrevW(const PVOID start,const PVOID current);
PVOID                               CharPrevW(const string start,const string current);
int                                 CharToOemBuffW(const string src,char &dst[],uint dst_length);
int                                 CharToOemW(const string src,char &dst[]);
uint                                CharUpperBuffW(string &lpsz,uint length);
PVOID                               CharUpperW(string &lpsz);
int                                 CheckDlgButton(HANDLE dlg,int nIDButton,uint check);
uint                                CheckMenuItem(HANDLE menu,uint uIDCheckItem,uint check);
int                                 CheckMenuRadioItem(HANDLE hmenu,uint first,uint last,uint check,uint flags);
int                                 CheckRadioButton(HANDLE dlg,int nIDFirstButton,int nIDLastButton,int nIDCheckButton);
HANDLE                              ChildWindowFromPoint(HANDLE wnd_parent,POINT &point);
HANDLE                              ChildWindowFromPointEx(HANDLE hwnd,POINT &pt,uint flags);
int                                 ClientToScreen(HANDLE wnd,POINT &point);
int                                 ClipCursor(RECT &rect);
int                                 CloseClipboard(void);
int                                 CloseDesktop(HANDLE desktop);
int                                 CloseGestureInfoHandle(HANDLE gesture_info);
int                                 CloseTouchInputHandle(HANDLE touch_input);
int                                 CloseWindow(HANDLE wnd);
int                                 CloseWindowStation(HANDLE win_sta);
int                                 CopyAcceleratorTableW(HANDLE accel_src,ACCEL &accel_dst,int accel_entries);
HANDLE                              CopyIcon(HANDLE icon);
HANDLE                              CopyImage(HANDLE h,uint type,int cx,int cy,uint flags);
int                                 CopyRect(RECT &dst,RECT &src);
int                                 CountClipboardFormats(void);
HANDLE                              CreateAcceleratorTableW(ACCEL &paccel,int accel);
int                                 CreateCaret(HANDLE wnd,HANDLE bitmap,int width,int height);
HANDLE                              CreateCursor(HANDLE inst,int hot_spot,int hot_spot,int width,int height,PVOID pvANDPlane,PVOID pvXORPlane);
HANDLE                              CreateDesktopExW(const string desktop,const PVOID device,PVOID devmode,uint flags,uint desired_access,PVOID lpsa,uint heap_size,PVOID pvoid);
HANDLE                              CreateDesktopExW(const string desktop,const string device,PVOID devmode,uint flags,uint desired_access,PVOID lpsa,uint heap_size,PVOID pvoid);
HANDLE                              CreateDesktopExW(const string desktop,const PVOID device,DEVMODEW &devmode,uint flags,uint desired_access,PVOID lpsa,uint heap_size,PVOID pvoid);
HANDLE                              CreateDesktopExW(const string desktop,const string device,DEVMODEW &devmode,uint flags,uint desired_access,PVOID lpsa,uint heap_size,PVOID pvoid);
HANDLE                              CreateDesktopW(const string desktop,const PVOID device,PVOID devmode,uint flags,uint desired_access,PVOID lpsa);
HANDLE                              CreateDesktopW(const string desktop,const string device,PVOID devmode,uint flags,uint desired_access,PVOID lpsa);
HANDLE                              CreateDesktopW(const string desktop,const PVOID device,DEVMODEW &devmode,uint flags,uint desired_access,PVOID lpsa);
HANDLE                              CreateDesktopW(const string desktop,const string device,DEVMODEW &devmode,uint flags,uint desired_access,PVOID lpsa);
HANDLE                              CreateDialogIndirectParamW(HANDLE instance,const DLGTEMPLATE &dlg_template,HANDLE wnd_parent,PVOID dialog_func,PVOID init_param);
HANDLE                              CreateDialogParamW(HANDLE instance,const string template_name,HANDLE wnd_parent,PVOID dialog_func,PVOID init_param);
HANDLE                              CreateIcon(HANDLE instance,int width,int height,uchar planes,uchar bits_pixel,const uchar &lpbANDbits[],const uchar &lpbXORbits[]);
HANDLE                              CreateIconFromResource(uchar &presbits,uint res_size,int icon,uint ver);
HANDLE                              CreateIconFromResourceEx(uchar &presbits,uint res_size,int icon,uint ver,int desired,int desired,uint Flags);
HANDLE                              CreateIconIndirect(ICONINFO &piconinfo);
HANDLE                              CreateMDIWindowW(const string class_name,const string window_name,uint style,int X,int Y,int width,int height,HANDLE wnd_parent,HANDLE instance,PVOID param);
HANDLE                              CreateMenu(void);
HANDLE                              CreatePopupMenu(void);
HANDLE                              CreateWindowExW(uint ex_style,const PVOID class_name,const PVOID window_name,uint style,int X,int Y,int width,int height,HANDLE wnd_parent,HANDLE menu,HANDLE instance,PVOID param);
HANDLE                              CreateWindowExW(uint ex_style,const string class_name,const string window_name,uint style,int X,int Y,int width,int height,HANDLE wnd_parent,HANDLE menu,HANDLE instance,PVOID param);
HANDLE                              CreateWindowStationW(const string lpwinsta,uint flags,uint desired_access,PVOID lpsa);
PVOID                               DefDlgProcW(HANDLE dlg,uint Msg,PVOID param,PVOID param);
HANDLE                              DeferWindowPos(HANDLE win_pos_info,HANDLE wnd,HANDLE wnd_insert_after,int x,int y,int cx,int cy,uint flags);
PVOID                               DefFrameProcW(HANDLE wnd,HANDLE hWndMDIClient,uint msg,PVOID param,PVOID param);
PVOID                               DefMDIChildProcW(HANDLE wnd,uint msg,PVOID param,PVOID param);
PVOID                               DefRawInputProc(RAWINPUT &raw_input[],int inp,uint size_header);
PVOID                               DefWindowProcW(HANDLE wnd,uint Msg,PVOID param,PVOID param);
int                                 DeleteMenu(HANDLE menu,uint position,uint flags);
int                                 DeregisterShellHookWindow(HANDLE hwnd);
int                                 DestroyAcceleratorTable(HANDLE accel);
int                                 DestroyCaret(void);
int                                 DestroyCursor(HANDLE cursor);
int                                 DestroyIcon(HANDLE icon);
int                                 DestroyMenu(HANDLE menu);
int                                 DestroyWindow(HANDLE wnd);
long                                DialogBoxIndirectParamW(HANDLE instance,DLGTEMPLATE &dialog_template,HANDLE wnd_parent,PVOID dialog_func,PVOID init_param);
long                                DialogBoxParamW(HANDLE instance,const string template_name,HANDLE wnd_parent,PVOID dialog_func,PVOID init_param);
void                                DisableProcessWindowsGhosting(void);
PVOID                               DispatchMessageW(MSG &msg);
int                                 DisplayConfigGetDeviceInfo(DISPLAYCONFIG_DEVICE_INFO_HEADER &packet);
int                                 DisplayConfigSetDeviceInfo(DISPLAYCONFIG_DEVICE_INFO_HEADER &packet);
int                                 DlgDirListComboBoxW(HANDLE dlg,string path_spec,int nIDComboBox,int nIDStaticPath,uint filetype);
int                                 DlgDirListW(HANDLE dlg,string path_spec,int nIDListBox,int nIDStaticPath,uint file_type);
int                                 DlgDirSelectComboBoxExW(HANDLE dlg,string str,int out,int combo_box);
int                                 DlgDirSelectExW(HANDLE dlg,string str,int count,int list_box);
int                                 DragDetect(HANDLE hwnd,POINT &pt);
uint                                DragObject(HANDLE parent,HANDLE from,uint fmt,ulong data,HANDLE hcur);
int                                 DrawAnimatedRects(HANDLE hwnd,int ani,RECT &from,RECT &to);
int                                 DrawCaption(HANDLE hwnd,HANDLE hdc,RECT &lprect,uint flags);
int                                 DrawEdge(HANDLE hdc,RECT &qrc,uint edge,uint flags);
int                                 DrawFocusRect(HANDLE hDC,RECT &lprc);
int                                 DrawFrameControl(HANDLE,RECT &,uint,uint);
int                                 DrawIcon(HANDLE hDC,int X,int Y,HANDLE icon);
int                                 DrawIconEx(HANDLE hdc,int left,int top,HANDLE icon,int width,int width,uint if_ani_cur,HANDLE flicker_free_draw,uint flags);
int                                 DrawMenuBar(HANDLE wnd);
int                                 DrawStateW(HANDLE hdc,HANDLE fore,PVOID call_back,PVOID data,PVOID data,int x,int y,int cx,int cy,uint flags);
int                                 DrawTextExW(HANDLE hdc,string text,int text,RECT &lprc,uint format,DRAWTEXTPARAMS &lpdtp);
int                                 DrawTextW(HANDLE hdc,const string text,int text,RECT &lprc,uint format);
int                                 EmptyClipboard(void);
int                                 EnableMenuItem(HANDLE menu,uint uIDEnableItem,uint enable);
int                                 EnableMouseInPointer(int enable);
int                                 EnableNonClientDpiScaling(HANDLE hwnd);
int                                 EnableScrollBar(HANDLE wnd,uint wSBflags,uint arrows);
int                                 EnableWindow(HANDLE wnd,int enable);
int                                 EndDeferWindowPos(HANDLE win_pos_info);
int                                 EndDialog(HANDLE dlg,long result);
int                                 EndMenu(void);
int                                 EndPaint(HANDLE wnd,PAINTSTRUCT &paint);
int                                 EndTask(HANDLE wnd,int shut_down,int force);
int                                 EnumChildWindows(HANDLE wnd_parent,PVOID enum_func,PVOID param);
uint                                EnumClipboardFormats(uint format);
int                                 EnumDesktopsW(HANDLE hwinsta,PVOID enum_func,PVOID param);
int                                 EnumDesktopWindows(HANDLE desktop,PVOID lpfn,PVOID param);
int                                 EnumDisplayDevicesW(const string device,uint dev_num,DISPLAY_DEVICEW &display_device,uint flags);
int                                 EnumDisplayMonitors(HANDLE hdc,const RECT &clip,PVOID enum_obj,PVOID data);
int                                 EnumDisplaySettingsExW(const string device_name,uint mode_num,DEVMODEW &dev_mode,uint flags);
int                                 EnumDisplaySettingsW(const string device_name,uint mode_num,DEVMODEW &dev_mode);
int                                 EnumPropsExW(HANDLE wnd,PVOID enum_func,PVOID param);
int                                 EnumPropsW(HANDLE wnd,PVOID enum_func);
int                                 EnumThreadWindows(uint thread_id,PVOID lpfn,PVOID param);
int                                 EnumWindows(PVOID enum_func,PVOID param);
int                                 EnumWindowStationsW(PVOID enum_func,PVOID param);
int                                 EqualRect(RECT &lprc1,RECT &lprc2);
int                                 EvaluateProximityToPolygon(uint vertices,const POINT &polygon[],const TOUCH_HIT_TESTING_INPUT &hit_testing_input[],TOUCH_HIT_TESTING_PROXIMITY_EVALUATION &proximity_eval);
int                                 EvaluateProximityToRect(const RECT &bounding_box[],const TOUCH_HIT_TESTING_INPUT &hit_testing_input[],TOUCH_HIT_TESTING_PROXIMITY_EVALUATION &proximity_eval);
int                                 ExcludeUpdateRgn(HANDLE hDC,HANDLE wnd);
int                                 ExitWindowsEx(uint flags,uint reason);
int                                 FillRect(HANDLE hDC,RECT &lprc,HANDLE hbr);
HANDLE                              FindWindowExW(HANDLE wnd_parent,HANDLE wnd_child_after,const string class_name,const string window);
HANDLE                              FindWindowW(const string class_name,const string window_name);
int                                 FlashWindow(HANDLE wnd,int invert);
int                                 FlashWindowEx(FLASHWINFO &pfwi);
int                                 FrameRect(HANDLE hDC,RECT &lprc,HANDLE hbr);
HANDLE                              GetActiveWindow(void);
int                                 GetAltTabInfoW(HANDLE hwnd,int item,ALTTABINFO &pati,string item_text,uint item_text);
HANDLE                              GetAncestor(HANDLE hwnd,uint flags);
short                               GetAsyncKeyState(int key);
int                                 GetAutoRotationState(AR_STATE &state);
DPI_AWARENESS                       GetAwarenessFromDpiAwarenessContext(HANDLE value);
HANDLE                              GetCapture(void);
uint                                GetCaretBlinkTime(void);
int                                 GetCaretPos(POINT &point);
int                                 GetCIMSSM(INPUT_MESSAGE_SOURCE &message_source);
int                                 GetClassInfoExW(HANDLE instance,const string class_name,WNDCLASSEXW &lpwcx);
int                                 GetClassInfoW(HANDLE instance,const string class_name,WNDCLASSW &wnd_class);
ulong                               GetClassLongPtrW(HANDLE wnd,int index);
uint                                GetClassLongW(HANDLE wnd,int index);
int                                 GetClassNameW(HANDLE wnd,ushort &class_name[],int max_count);
ushort                              GetClassWord(HANDLE wnd,int index);
int                                 GetClientRect(HANDLE wnd,RECT &rect);
HANDLE                              GetClipboardData(uint format);
int                                 GetClipboardFormatNameW(uint format,ushort &format_name[],int max_count);
HANDLE                              GetClipboardOwner(void);
uint                                GetClipboardSequenceNumber(void);
HANDLE                              GetClipboardViewer(void);
int                                 GetClipCursor(RECT &rect);
int                                 GetComboBoxInfo(HANDLE combo,COMBOBOXINFO &pcbi);
int                                 GetCurrentInputMessageSource(INPUT_MESSAGE_SOURCE &message_source);
HANDLE                              GetCursor(void);
int                                 GetCursorInfo(CURSORINFO &pci);
int                                 GetCursorPos(POINT &point);
HANDLE                              GetDC(HANDLE wnd);
HANDLE                              GetDCEx(HANDLE wnd,HANDLE clip,uint flags);
HANDLE                              GetDesktopWindow(void);
int                                 GetDialogBaseUnits(void);
DIALOG_CONTROL_DPI_CHANGE_BEHAVIORS GetDialogControlDpiChangeBehavior(HANDLE wnd);
DIALOG_DPI_CHANGE_BEHAVIORS         GetDialogDpiChangeBehavior(HANDLE dlg);
int                                 GetDisplayAutoRotationPreferences(ORIENTATION_PREFERENCE &orientation);
int                                 GetDisplayConfigBufferSizes(uint flags,uint &path_array_elements,uint &mode_info_array_elements);
int                                 GetDlgCtrlID(HANDLE wnd);
HANDLE                              GetDlgItem(HANDLE dlg,int nIDDlgItem);
uint                                GetDlgItemInt(HANDLE dlg,int nIDDlgItem,int &translated,int signed);
uint                                GetDlgItemTextW(HANDLE dlg,int nIDDlgItem,string str,int max);
uint                                GetDoubleClickTime(void);
uint                                GetDpiForSystem(void);
uint                                GetDpiForWindow(HANDLE hwnd);
uint                                GetDpiFromDpiAwarenessContext(HANDLE value);
HANDLE                              GetFocus(void);
HANDLE                              GetForegroundWindow(void);
int                                 GetGestureConfig(HANDLE hwnd,uint reserved,uint flags,uint &pcIDs,GESTURECONFIG &gesture_config[],uint size);
int                                 GetGestureExtraArgs(HANDLE gesture_info,uint extra_args,uchar &extra_args);
int                                 GetGestureInfo(HANDLE gesture_info,GESTUREINFO &gesture_info);
uint                                GetGuiResources(HANDLE process,uint flags);
int                                 GetGUIThreadInfo(uint thread,GUITHREADINFO &pgui);
int                                 GetIconInfo(HANDLE icon,ICONINFO &piconinfo);
int                                 GetIconInfoExW(HANDLE hicon,ICONINFOEXW &piconinfo);
int                                 GetInputState(void);
uint                                GetKBCodePage(void);
HANDLE                              GetKeyboardLayout(uint thread);
int                                 GetKeyboardLayoutList(int buff,HANDLE &list[]);
int                                 GetKeyboardLayoutNameW(ushort &pwszKLID[]);
int                                 GetKeyboardState(uchar &key_state[]);
int                                 GetKeyboardType(int type_flag);
int                                 GetKeyNameTextW(long param,ushort &str[],int size);
short                               GetKeyState(int virt_key);
HANDLE                              GetLastActivePopup(HANDLE wnd);
int                                 GetLastInputInfo(LASTINPUTINFO &plii);
int                                 GetLayeredWindowAttributes(HANDLE hwnd,uint &key,uchar &alpha,uint &flags);
uint                                GetListBoxInfo(HANDLE hwnd);
HANDLE                              GetMenu(HANDLE wnd);
int                                 GetMenuBarInfo(HANDLE hwnd,int object,int item,MENUBARINFO &pmbi);
int                                 GetMenuCheckMarkDimensions(void);
uint                                GetMenuContextHelpId(HANDLE);
uint                                GetMenuDefaultItem(HANDLE menu,uint by_pos,uint flags);
int                                 GetMenuInfo(HANDLE,MENUINFO &);
int                                 GetMenuItemCount(HANDLE menu);
uint                                GetMenuItemID(HANDLE menu,int pos);
int                                 GetMenuItemInfoW(HANDLE hmenu,uint item,int by_position,MENUITEMINFOW &lpmii);
int                                 GetMenuItemRect(HANDLE wnd,HANDLE menu,uint item,RECT &item);
uint                                GetMenuState(HANDLE menu,uint id,uint flags);
int                                 GetMenuStringW(HANDLE menu,uint uIDItem,string str,int max,uint flags);
PVOID                               GetMessageExtraInfo(void);
uint                                GetMessagePos(void);
int                                 GetMessageTime(void);
int                                 GetMessageW(MSG &msg,HANDLE wnd,uint msg_filter_min,uint msg_filter_max);
int                                 GetMonitorInfoW(HANDLE monitor,MONITORINFO &lpmi);
int                                 GetMouseMovePointsEx(uint size,MOUSEMOVEPOINT &lppt,MOUSEMOVEPOINT &buf,int buf_points,uint resolution);
HANDLE                              GetNextDlgGroupItem(HANDLE dlg,HANDLE ctl,int previous);
HANDLE                              GetNextDlgTabItem(HANDLE dlg,HANDLE ctl,int previous);
HANDLE                              GetOpenClipboardWindow(void);
HANDLE                              GetParent(HANDLE wnd);
int                                 GetPhysicalCursorPos(POINT &point);
int                                 GetPointerCursorId(uint pointer_id,uint &cursor_id);
int                                 GetPointerDevice(HANDLE device,POINTER_DEVICE_INFO &device);
int                                 GetPointerDeviceCursors(HANDLE device,uint &count,POINTER_DEVICE_CURSOR_INFO &cursors[]);
int                                 GetPointerDeviceProperties(HANDLE device,uint &count,POINTER_DEVICE_PROPERTY &properties[]);
int                                 GetPointerDeviceRects(HANDLE device,RECT &device_rect,RECT &rect);
int                                 GetPointerDevices(uint &count,POINTER_DEVICE_INFO &devices[]);
int                                 GetPointerFrameInfo(uint id,uint &count,POINTER_INFO &info[]);
int                                 GetPointerFrameInfoHistory(uint id,uint &count,uint &count,POINTER_INFO &info[]);
int                                 GetPointerFramePenInfo(uint id,uint &count,POINTER_PEN_INFO &info[]);
int                                 GetPointerFramePenInfoHistory(uint id,uint &count,uint &count,POINTER_PEN_INFO &info[]);
int                                 GetPointerFrameTouchInfo(uint id,uint &count,POINTER_TOUCH_INFO &info[]);
int                                 GetPointerFrameTouchInfoHistory(uint id,uint &count,uint &count,POINTER_TOUCH_INFO &info[]);
int                                 GetPointerInfo(uint id,POINTER_INFO &info[]);
int                                 GetPointerInfoHistory(uint id,uint &count,POINTER_INFO &info[]);
int                                 GetPointerInputTransform(uint id,uint count,INPUT_TRANSFORM &transform);
int                                 GetPointerPenInfo(uint id,POINTER_PEN_INFO &info);
int                                 GetPointerPenInfoHistory(uint id,uint &count,POINTER_PEN_INFO &info);
int                                 GetPointerTouchInfo(uint id,POINTER_TOUCH_INFO &info[]);
int                                 GetPointerTouchInfoHistory(uint id,uint &count,POINTER_TOUCH_INFO &info[]);
int                                 GetPointerType(uint id,uint &type);
int                                 GetPriorityClipboardFormat(uint &format_priority_list[],int formats);
int                                 GetProcessDefaultLayout(uint &default_layout);
HANDLE                              GetProcessWindowStation(void);
HANDLE                              GetPropW(HANDLE wnd,const string str);
uint                                GetQueueStatus(uint flags);
uint                                GetRawInputBuffer(RAWINPUT &data,uint &size,uint size_header);
uint                                GetRawInputData(HANDLE raw_input,uint command,PVOID data,uint &size,uint size_header);
uint                                GetRawInputDeviceInfoW(HANDLE device,uint command,PVOID data,uint &size);
uint                                GetRawInputDeviceList(RAWINPUTDEVICELIST &raw_input_device_list,uint &num_devices,uint size);
int                                 GetRawPointerDeviceData(uint id,uint count,uint count,POINTER_DEVICE_PROPERTY &properties[],int &values[]);
uint                                GetRegisteredRawInputDevices(RAWINPUTDEVICE &raw_input_devices,uint &num_devices,uint size);
int                                 GetScrollBarInfo(HANDLE hwnd,int object,SCROLLBARINFO &psbi);
int                                 GetScrollInfo(HANDLE hwnd,int bar,SCROLLINFO &lpsi);
int                                 GetScrollPos(HANDLE wnd,int bar);
int                                 GetScrollRange(HANDLE wnd,int bar,int &min_pos,int &max_pos);
HANDLE                              GetShellWindow(void);
HANDLE                              GetSubMenu(HANDLE menu,int pos);
uint                                GetSysColor(int index);
HANDLE                              GetSysColorBrush(int index);
uint                                GetSystemDpiForProcess(HANDLE process);
HANDLE                              GetSystemMenu(HANDLE wnd,int revert);
int                                 GetSystemMetrics(int index);
int                                 GetSystemMetricsForDpi(int index,uint dpi);
uint                                GetTabbedTextExtentW(HANDLE hdc,const string str,int count,int tab_positions,const int &tab_stop_positions[]);
HANDLE                              GetThreadDesktop(uint thread_id);
HANDLE                              GetThreadDpiAwarenessContext(void);
DPI_HOSTING_BEHAVIOR                GetThreadDpiHostingBehavior(void);
int                                 GetTitleBarInfo(HANDLE hwnd,TITLEBARINFO &pti);
HANDLE                              GetTopWindow(HANDLE wnd);
int                                 GetTouchInputInfo(HANDLE touch_input,uint inputs,TOUCHINPUT &inputs,int size);
uint                                GetUnpredictedMessagePos(void);
int                                 GetUpdatedClipboardFormats(uint &formats[],uint formats_number,uint &formats_out);
int                                 GetUpdateRect(HANDLE wnd,RECT &rect,int erase);
int                                 GetUpdateRgn(HANDLE wnd,HANDLE rgn,int erase);
int                                 GetUserObjectInformationW(HANDLE obj,int index,PVOID info,uint length,uint &length_needed);
int                                 GetUserObjectSecurity(HANDLE obj,uint &pSIRequested,SECURITY_DESCRIPTOR &pSID,uint length,uint &length_needed);
HANDLE                              GetWindow(HANDLE wnd,uint cmd);
uint                                GetWindowContextHelpId(HANDLE);
HANDLE                              GetWindowDC(HANDLE wnd);
int                                 GetWindowDisplayAffinity(HANDLE wnd,uint &affinity);
HANDLE                              GetWindowDpiAwarenessContext(HANDLE hwnd);
DPI_HOSTING_BEHAVIOR                GetWindowDpiHostingBehavior(HANDLE hwnd);
int                                 GetWindowFeedbackSetting(HANDLE hwnd,FEEDBACK_TYPE feedback,uint flags,uint size,int &config);
int                                 GetWindowInfo(HANDLE hwnd,WINDOWINFO &pwi);
long                                GetWindowLongPtrW(HANDLE wnd,int index);
int                                 GetWindowLongW(HANDLE wnd,int index);
uint                                GetWindowModuleFileNameW(HANDLE hwnd,ushort &file_name[],uint file_name_max);
int                                 GetWindowPlacement(HANDLE wnd,WINDOWPLACEMENT &lpwndpl);
int                                 GetWindowRect(HANDLE wnd,RECT &rect);
int                                 GetWindowRgn(HANDLE wnd,HANDLE rgn);
int                                 GetWindowRgnBox(HANDLE wnd,RECT &lprc);
int                                 GetWindowTextLengthW(HANDLE wnd);
int                                 GetWindowTextW(HANDLE wnd,ushort &str[],int max_count);
uint                                GetWindowThreadProcessId(HANDLE wnd,uint &process_id);
ushort                              GetWindowWord(HANDLE wnd,int index);
int                                 GrayStringW(HANDLE hDC,HANDLE brush,PVOID output_func,uchar &data[],int count,int X,int Y,int width,int height);
int                                 GrayStringW(HANDLE hDC,HANDLE brush,PVOID output_func,PVOID data,int count,int X,int Y,int width,int height);
int                                 HideCaret(HANDLE wnd);
int                                 HiliteMenuItem(HANDLE wnd,HANDLE menu,uint uIDHiliteItem,uint hilite);
int                                 InflateRect(RECT &lprc,int dx,int dy);
int                                 InheritWindowMonitor(HANDLE hwnd,HANDLE inherit);
int                                 InitializeTouchInjection(uint count,uint mode);
int                                 InjectTouchInput(uint count,POINTER_TOUCH_INFO &contacts);
int                                 InSendMessage(void);
uint                                InSendMessageEx(PVOID reserved);
int                                 InsertMenuItemW(HANDLE hmenu,uint item,int by_position,const MENUITEMINFO &lpmi);
int                                 InsertMenuW(HANDLE menu,uint position,uint flags,ulong uIDNewItem,const string new_item);
int                                 InternalGetWindowText(HANDLE wnd,string str,int max_count);
int                                 IntersectRect(RECT &dst,RECT &src1,RECT &src2);
int                                 InvalidateRect(HANDLE wnd,RECT &rect,int erase);
int                                 InvalidateRgn(HANDLE wnd,HANDLE rgn,int erase);
int                                 InvertRect(HANDLE hDC,RECT &lprc);
int                                 IsCharAlphaNumericW(short ch);
int                                 IsCharAlphaW(short ch);
int                                 IsCharLowerW(short ch);
int                                 IsCharUpperW(short ch);
int                                 IsChild(HANDLE wnd_parent,HANDLE wnd);
int                                 IsClipboardFormatAvailable(uint format);
int                                 IsDialogMessageW(HANDLE dlg,MSG &msg);
uint                                IsDlgButtonChecked(HANDLE dlg,int nIDButton);
int                                 IsGUIThread(int convert);
int                                 IsHungAppWindow(HANDLE hwnd);
int                                 IsIconic(HANDLE wnd);
int                                 IsImmersiveProcess(HANDLE process);
int                                 IsMenu(HANDLE menu);
int                                 IsMouseInPointerEnabled(void);
int                                 IsProcessDPIAware(void);
int                                 IsRectEmpty(RECT &lprc);
int                                 IsTouchWindow(HANDLE hwnd,uint &flags);
int                                 IsValidDpiAwarenessContext(HANDLE value);
int                                 IsWindow(HANDLE wnd);
int                                 IsWindowEnabled(HANDLE wnd);
int                                 IsWindowUnicode(HANDLE wnd);
int                                 IsWindowVisible(HANDLE wnd);
int                                 IsWinEventHookInstalled(uint event);
int                                 IsWow64Message(void);
int                                 IsZoomed(HANDLE wnd);
void                                keybd_event(uchar vk,uchar scan,uint flags,ulong extra_info);
int                                 KillTimer(HANDLE wnd,ulong uIDEvent);
HANDLE                              LoadAcceleratorsW(HANDLE instance,const string table_name);
HANDLE                              LoadBitmapW(HANDLE instance,const string bitmap_name);
HANDLE                              LoadCursorFromFileW(const string file_name);
HANDLE                              LoadCursorW(HANDLE instance,const string cursor_name);
HANDLE                              LoadIconW(HANDLE instance,const string icon_name);
HANDLE                              LoadImageW(HANDLE inst,const string name,uint type,int cx,int cy,uint load);
HANDLE                              LoadKeyboardLayoutW(const string pwszKLID,uint Flags);
HANDLE                              LoadMenuIndirectW(const PVOID menu_template);
HANDLE                              LoadMenuW(HANDLE instance,const string menu_name);
int                                 LoadStringW(HANDLE instance,uint uID,string buffer,int buffer_max);
int                                 LockSetForegroundWindow(uint lock_code);
int                                 LockWindowUpdate(HANDLE wnd_lock);
int                                 LockWorkStation(void);
int                                 LogicalToPhysicalPoint(HANDLE wnd,POINT &point);
int                                 LogicalToPhysicalPointForPerMonitorDPI(HANDLE wnd,POINT &point);
int                                 LookupIconIdFromDirectory(uchar &presbits[],int icon);
int                                 LookupIconIdFromDirectoryEx(uchar &presbits,int icon,int desired,int desired,uint Flags);
int                                 MapDialogRect(HANDLE dlg,RECT &rect);
uint                                MapVirtualKeyExW(uint code,uint map_type,HANDLE dwhkl);
uint                                MapVirtualKeyW(uint code,uint map_type);
int                                 MapWindowPoints(HANDLE wnd_from,HANDLE wnd_to,POINT &points[],uint points_count);
int                                 MenuItemFromPoint(HANDLE wnd,HANDLE menu,POINT &screen);
int                                 MessageBeep(uint type);
int                                 MessageBoxExW(HANDLE wnd,const string text,const string caption,uint type,ushort language_id);
int                                 MessageBoxIndirectW(MSGBOXPARAMSW &lpmbp);
int                                 MessageBoxW(HANDLE wnd,const string text,const string caption,uint type);
int                                 ModifyMenuW(HANDLE mnu,uint position,uint flags,ulong uIDNewItem,const string new_item);
HANDLE                              MonitorFromPoint(POINT &pt,uint flags);
HANDLE                              MonitorFromRect(const RECT &lprc,uint flags);
HANDLE                              MonitorFromWindow(HANDLE hwnd,uint flags);
void                                mouse_event(uint flags,uint dx,uint dy,uint data,ulong extra_info);
int                                 MoveWindow(HANDLE wnd,int X,int Y,int width,int height,int repaint);
uint                                MsgWaitForMultipleObjects(uint count,const HANDLE &handles[],int wait_all,uint milliseconds,uint wake_mask);
uint                                MsgWaitForMultipleObjectsEx(uint count,const HANDLE &handles[],uint milliseconds,uint wake_mask,uint flags);
void                                NotifyWinEvent(uint event,HANDLE hwnd,int object,int child);
uint                                OemKeyScan(ushort oem_char);
int                                 OemToCharBuffW(const char &src[],ushort &dst[],uint dst_length);
int                                 OemToCharW(const char &src[],ushort &dst[]);
int                                 OffsetRect(RECT &lprc,int dx,int dy);
int                                 OpenClipboard(HANDLE wnd_new_owner);
HANDLE                              OpenDesktopW(const string desktop,uint flags,int inherit,uint desired_access);
int                                 OpenIcon(HANDLE wnd);
HANDLE                              OpenInputDesktop(uint flags,int inherit,uint desired_access);
HANDLE                              OpenWindowStationW(const string win_sta,int inherit,uint desired_access);
PVOID                               PackTouchHitTestingProximityEvaluation(const TOUCH_HIT_TESTING_INPUT &hit_testing_input[],const TOUCH_HIT_TESTING_PROXIMITY_EVALUATION &proximity_eval[]);
int                                 PaintDesktop(HANDLE hdc);
int                                 PeekMessageW(MSG &msg,HANDLE wnd,uint msg_filter_min,uint msg_filter_max,uint remove_msg);
int                                 PhysicalToLogicalPoint(HANDLE wnd,POINT &point);
int                                 PhysicalToLogicalPointForPerMonitorDPI(HANDLE wnd,POINT &point);
int                                 PostMessageW(HANDLE wnd,uint Msg,PVOID param,PVOID param);
void                                PostQuitMessage(int exit_code);
int                                 PostThreadMessageW(uint thread,uint Msg,PVOID param,PVOID param);
int                                 PrintWindow(HANDLE hwnd,HANDLE blt,uint flags);
uint                                PrivateExtractIconsW(const string file_name,int icon_index,int icon,int icon,HANDLE &phicon,uint &piconid,uint icons,uint flags);
int                                 PtInRect(const RECT &lprc,long pt);
long                                QueryDisplayConfig(uint flags,uint &path_array_elements,DISPLAYCONFIG_PATH_INFO &array,uint &mode_info_array_elements,DISPLAYCONFIG_MODE_INFO &info_array[],DISPLAYCONFIG_TOPOLOGY_ID &topology_id);
HANDLE                              RealChildWindowFromPoint(HANDLE parent,long parent_client_coords);
uint                                RealGetWindowClassW(HANDLE hwnd,ushort &class_name[],uint class_name_max);
int                                 RedrawWindow(HANDLE wnd,RECT &rect,HANDLE update,uint flags);
ushort                              RegisterClassExW(const WNDCLASSEXW &lpwcx);
ushort                              RegisterClassW(const WNDCLASSW &wnd_class);
uint                                RegisterClipboardFormatW(const string format);
PVOID                               RegisterDeviceNotificationW(HANDLE recipient,PVOID NotificationFilter,uint Flags);
int                                 RegisterHotKey(HANDLE wnd,int id,uint modifiers,uint vk);
int                                 RegisterPointerDeviceNotifications(HANDLE window,int range);
int                                 RegisterPointerInputTarget(HANDLE hwnd,uint type);
int                                 RegisterPointerInputTargetEx(HANDLE hwnd,uint type,int observe);
PVOID                               RegisterPowerSettingNotification(HANDLE recipient,const GUID &PowerSettingGuid,uint Flags);
int                                 RegisterRawInputDevices(const RAWINPUTDEVICE &raw_input_devices[],uint num_devices,uint size);
int                                 RegisterShellHookWindow(HANDLE hwnd);
PVOID                               RegisterSuspendResumeNotification(HANDLE recipient,uint Flags);
int                                 RegisterTouchHitTestingWindow(HANDLE hwnd,uint value);
int                                 RegisterTouchWindow(HANDLE hwnd,uint flags);
uint                                RegisterWindowMessageW(const string str);
int                                 ReleaseCapture(void);
int                                 ReleaseDC(HANDLE wnd,HANDLE hDC);
int                                 RemoveClipboardFormatListener(HANDLE hwnd);
int                                 RemoveMenu(HANDLE menu,uint position,uint flags);
HANDLE                              RemovePropW(HANDLE wnd,const string str);
int                                 ReplyMessage(PVOID result);
int                                 ScreenToClient(HANDLE wnd,POINT &point);
int                                 ScrollDC(HANDLE hDC,int dx,int dy,RECT &scroll,RECT &clip,HANDLE update,RECT &update);
int                                 ScrollWindow(HANDLE wnd,int XAmount,int YAmount,RECT &rect,RECT &clip_rect);
int                                 ScrollWindowEx(HANDLE wnd,int dx,int dy,RECT &scroll,RECT &clip,HANDLE update,RECT &update,uint flags);
PVOID                               SendDlgItemMessageW(HANDLE dlg,int nIDDlgItem,uint Msg,PVOID param,PVOID param);
uint                                SendInput(uint inputs,INPUT &inputs[],int size);
int                                 SendMessageCallbackW(HANDLE wnd,uint Msg,PVOID param,PVOID param,PVOID result_call_back,ulong data);
PVOID                               SendMessageTimeoutW(HANDLE wnd,uint Msg,PVOID param,PVOID param,uint flags,uint timeout,PVOID result);
PVOID                               SendMessageW(HANDLE wnd,uint Msg,PVOID param,PVOID param);
int                                 SendNotifyMessageW(HANDLE wnd,uint Msg,PVOID param,PVOID param);
HANDLE                              SetActiveWindow(HANDLE wnd);
HANDLE                              SetCapture(HANDLE wnd);
int                                 SetCaretBlinkTime(uint uMSeconds);
int                                 SetCaretPos(int X,int Y);
ulong                               SetClassLongPtrW(HANDLE wnd,int index,long new_long);
uint                                SetClassLongW(HANDLE wnd,int index,int new_long);
ushort                              SetClassWord(HANDLE wnd,int index,ushort new_word);
HANDLE                              SetClipboardData(uint format,HANDLE mem);
HANDLE                              SetClipboardViewer(HANDLE wnd_new_viewer);
ulong                               SetCoalescableTimer(HANDLE wnd,ulong nIDEvent,uint elapse,PVOID timer_func,uint tolerance_delay);
HANDLE                              SetCursor(HANDLE cursor);
int                                 SetCursorPos(int X,int Y);
void                                SetDebugErrorLevel(uint level);
int                                 SetDialogControlDpiChangeBehavior(HANDLE wnd,DIALOG_CONTROL_DPI_CHANGE_BEHAVIORS mask,DIALOG_CONTROL_DPI_CHANGE_BEHAVIORS values);
int                                 SetDialogDpiChangeBehavior(HANDLE dlg,DIALOG_DPI_CHANGE_BEHAVIORS mask,DIALOG_DPI_CHANGE_BEHAVIORS values);
int                                 SetDisplayAutoRotationPreferences(ORIENTATION_PREFERENCE orientation);
int                                 SetDisplayConfig(uint path_array_elements,DISPLAYCONFIG_PATH_INFO &array,uint mode_info_array_elements,DISPLAYCONFIG_MODE_INFO &info_array[],uint flags);
int                                 SetDlgItemInt(HANDLE dlg,int nIDDlgItem,uint value,int signed);
int                                 SetDlgItemTextW(HANDLE dlg,int nIDDlgItem,const string str);
int                                 SetDoubleClickTime(uint);
HANDLE                              SetFocus(HANDLE wnd);
int                                 SetForegroundWindow(HANDLE wnd);
int                                 SetGestureConfig(HANDLE hwnd,uint reserved,uint cIDs,GESTURECONFIG &gesture_config[],uint size);
int                                 SetKeyboardState(uchar &key_state[]);
void                                SetLastErrorEx(uint err_code,uint type);
int                                 SetLayeredWindowAttributes(HANDLE hwnd,uint key,uchar alpha,uint flags);
int                                 SetMenu(HANDLE wnd,HANDLE menu);
int                                 SetMenuContextHelpId(HANDLE,uint);
int                                 SetMenuDefaultItem(HANDLE menu,uint item,uint by_pos);
int                                 SetMenuInfo(HANDLE,const MENUINFO &);
int                                 SetMenuItemBitmaps(HANDLE menu,uint position,uint flags,HANDLE bitmap_unchecked,HANDLE bitmap_checked);
int                                 SetMenuItemInfoW(HANDLE hmenu,uint item,int by_positon,const MENUITEMINFOW &lpmii);
PVOID                               SetMessageExtraInfo(PVOID param);
int                                 SetMessageQueue(int messages_max);
HANDLE                              SetParent(HANDLE wnd_child,HANDLE wnd_new_parent);
int                                 SetPhysicalCursorPos(int X,int Y);
int                                 SetProcessDefaultLayout(uint default_layout);
int                                 SetProcessDPIAware(void);
int                                 SetProcessDpiAwarenessContext(HANDLE value);
int                                 SetProcessRestrictionExemption(int enable_exemption);
int                                 SetProcessWindowStation(HANDLE win_sta);
int                                 SetPropW(HANDLE wnd,const string str,HANDLE data);
int                                 SetRect(RECT &lprc,int left,int top,int right,int bottom);
int                                 SetRectEmpty(RECT &lprc);
int                                 SetScrollInfo(HANDLE hwnd,int bar,const SCROLLINFO &lpsi,int redraw);
int                                 SetScrollPos(HANDLE wnd,int bar,int pos,int redraw);
int                                 SetScrollRange(HANDLE wnd,int bar,int min_pos,int max_pos,int redraw);
int                                 SetSysColors(int elements_count,int &elements[],uint &rgb_values[]);
int                                 SetSystemCursor(HANDLE hcur,uint id);
int                                 SetThreadDesktop(HANDLE desktop);
HANDLE                              SetThreadDpiAwarenessContext(HANDLE context);
DPI_HOSTING_BEHAVIOR                SetThreadDpiHostingBehavior(DPI_HOSTING_BEHAVIOR value);
ulong                               SetTimer(HANDLE wnd,ulong nIDEvent,uint elapse,PVOID timer_func);
int                                 SetUserObjectInformationW(HANDLE obj,int index,PVOID info,uint length);
int                                 SetUserObjectSecurity(HANDLE obj,uint pSIRequested,SECURITY_DESCRIPTOR &pSID);
int                                 SetWindowContextHelpId(HANDLE,uint);
int                                 SetWindowDisplayAffinity(HANDLE wnd,uint affinity);
int                                 SetWindowFeedbackSetting(HANDLE hwnd,FEEDBACK_TYPE feedback,uint flags,uint size,PVOID configuration);
long                                SetWindowLongPtrW(HANDLE wnd,int index,long new_long);
int                                 SetWindowLongW(HANDLE wnd,int index,int new_long);
int                                 SetWindowPlacement(HANDLE wnd,WINDOWPLACEMENT &lpwndpl);
int                                 SetWindowPos(HANDLE wnd,HANDLE wnd_insert_after,int X,int Y,int cx,int cy,uint flags);
int                                 SetWindowRgn(HANDLE wnd,HANDLE rgn,int redraw);
HANDLE                              SetWindowsHookExW(int hook,PVOID lpfn,HANDLE hmod,uint thread_id);
HANDLE                              SetWindowsHookW(int filter_type,PVOID filter_proc);
int                                 SetWindowTextW(HANDLE wnd,const string str);
ushort                              SetWindowWord(HANDLE wnd,int index,ushort &new_word);
HANDLE                              SetWinEventHook(uint min,uint max,HANDLE win_event_proc,PVOID win_event_proc,uint process,uint thread,uint flags);
int                                 ShowCaret(HANDLE wnd);
int                                 ShowCursor(int show);
int                                 ShowOwnedPopups(HANDLE wnd,int show);
int                                 ShowScrollBar(HANDLE wnd,int bar,int show);
int                                 ShowWindow(HANDLE wnd,int cmd_show);
int                                 ShowWindowAsync(HANDLE wnd,int cmd_show);
int                                 ShutdownBlockReasonCreate(HANDLE wnd,const string reason);
int                                 ShutdownBlockReasonDestroy(HANDLE wnd);
int                                 ShutdownBlockReasonQuery(HANDLE wnd,string buff,uint &buff[]);
int                                 SkipPointerFrameMessages(uint id);
int                                 SoundSentry(void);
int                                 SubtractRect(RECT &dst,RECT &src1,RECT &src2);
int                                 SwapMouseButton(int swap);
int                                 SwitchDesktop(HANDLE desktop);
void                                SwitchToThisWindow(HANDLE hwnd,int unknown);
int                                 SystemParametersInfoForDpi(uint action,uint param,PVOID param,uint win_ini,uint dpi);
int                                 SystemParametersInfoW(uint action,uint param,PVOID param,uint win_ini);
int                                 TabbedTextOutW(HANDLE hdc,int x,int y,const string str,int count,int tab_positions,const int &tab_stop_positions[],int tab_origin);
ushort                              TileWindows(HANDLE parent,uint how,const RECT &rect,uint kids,const HANDLE &kids[]);
int                                 ToAscii(uint virt_key,uint scan_code,const uchar &key_state[],ushort &symbol,uint flags);
int                                 ToAsciiEx(uint virt_key,uint scan_code,const uchar &key_state[],ushort &symbol,uint flags,HANDLE dwhkl);
int                                 ToUnicode(uint virt_key,uint scan_code,const uchar &key_state[],ushort &buff[],int buff_size,uint flags);
int                                 ToUnicodeEx(uint virt_key,uint scan_code,const uchar &key_state[],ushort &buff[],int buff_size,uint flags,HANDLE dwhkl);
int                                 TrackMouseEvent(TRACKMOUSEEVENT &event_track);
int                                 TrackPopupMenu(HANDLE menu,uint flags,int x,int y,int reserved,HANDLE wnd,RECT &rect);
int                                 TrackPopupMenuEx(HANDLE menu,uint flags,int x,int y,HANDLE hwnd,TPMPARAMS &lptpm);
int                                 TranslateAcceleratorW(HANDLE wnd,HANDLE acc_table,MSG &msg);
int                                 TranslateMDISysAccel(HANDLE wnd_client,MSG &msg);
int                                 TranslateMessage(MSG &msg);
int                                 UnhookWindowsHook(int code,PVOID filter_proc);
int                                 UnhookWindowsHookEx(HANDLE hhk);
int                                 UnhookWinEvent(HANDLE win_event_hook);
int                                 UnionRect(RECT &dst,RECT &src1,RECT &src2);
int                                 UnloadKeyboardLayout(HANDLE hkl);
int                                 UnregisterClassW(const PVOID class_name,HANDLE instance);
int                                 UnregisterClassW(const string class_name,HANDLE instance);
int                                 UnregisterDeviceNotification(PVOID Handle);
int                                 UnregisterHotKey(HANDLE wnd,int id);
int                                 UnregisterPointerInputTarget(HANDLE hwnd,uint type);
int                                 UnregisterPointerInputTargetEx(HANDLE hwnd,uint type);
int                                 UnregisterPowerSettingNotification(PVOID Handle);
int                                 UnregisterSuspendResumeNotification(PVOID Handle);
int                                 UnregisterTouchWindow(HANDLE hwnd);
int                                 UpdateLayeredWindow(HANDLE wnd,HANDLE dst,POINT &dst,SIZE &psize,HANDLE src,POINT &src,uint key,BLENDFUNCTION &pblend,uint flags);
int                                 UpdateLayeredWindowIndirect(HANDLE wnd,const UPDATELAYEREDWINDOWINFO &pULWInfo[]);
int                                 UpdateWindow(HANDLE wnd);
int                                 UserHandleGrantAccess(HANDLE user_handle,HANDLE job,int grant);
int                                 ValidateRect(HANDLE wnd,RECT &rect);
int                                 ValidateRgn(HANDLE wnd,HANDLE rgn);
short                               VkKeyScanExW(ushort ch,HANDLE dwhkl);
short                               VkKeyScanW(ushort ch);
uint                                WaitForInputIdle(HANDLE process,uint milliseconds);
int                                 WaitMessage(void);
HANDLE                              WindowFromDC(HANDLE hDC);
HANDLE                              WindowFromPhysicalPoint(long point);
HANDLE                              WindowFromPoint(long point);
int                                 WinHelpW(HANDLE wnd_main,const string help,uint command,ulong data);
int                                 wvsprintfW(ushort &[],const string,PVOID &arglist[]);
#import
//+------------------------------------------------------------------+
