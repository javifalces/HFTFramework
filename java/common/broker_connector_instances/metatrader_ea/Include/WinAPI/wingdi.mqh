//+------------------------------------------------------------------+
//|                                                       wingdi.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>

//---
#define MM_MAX_AXES_NAMELEN   16
#define MM_MAX_NUMAXES        16
#define CCHDEVICENAME         32
#define LF_FACESIZE           32
#define LF_FULLFACESIZE       64
#define ELF_VENDOR_SIZE       4
#define CCHFORMNAME           32

//---
enum DISPLAYCONFIG_COLOR_ENCODING
  {
   DISPLAYCONFIG_COLOR_ENCODING_RGB=0,
   DISPLAYCONFIG_COLOR_ENCODING_YCBCR444=1,
   DISPLAYCONFIG_COLOR_ENCODING_YCBCR422=2,
   DISPLAYCONFIG_COLOR_ENCODING_YCBCR420=3,
   DISPLAYCONFIG_COLOR_ENCODING_INTENSITY=4,
   DISPLAYCONFIG_COLOR_ENCODING_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_DEVICE_INFO_TYPE
  {
   DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME=1,
   DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME=2,
   DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_PREFERRED_MODE=3,
   DISPLAYCONFIG_DEVICE_INFO_GET_ADAPTER_NAME=4,
   DISPLAYCONFIG_DEVICE_INFO_SET_TARGET_PERSISTENCE=5,
   DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_BASE_TYPE=6,
   DISPLAYCONFIG_DEVICE_INFO_GET_SUPPORT_VIRTUAL_RESOLUTION=7,
   DISPLAYCONFIG_DEVICE_INFO_SET_SUPPORT_VIRTUAL_RESOLUTION=8,
   DISPLAYCONFIG_DEVICE_INFO_GET_ADVANCED_COLOR_INFO=9,
   DISPLAYCONFIG_DEVICE_INFO_SET_ADVANCED_COLOR_STATE=10,
   DISPLAYCONFIG_DEVICE_INFO_GET_SDR_WHITE_LEVEL=11,
   DISPLAYCONFIG_DEVICE_INFO_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_MODE_INFO_TYPE
  {
   DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE=1,
   DISPLAYCONFIG_MODE_INFO_TYPE_TARGET=2,
   DISPLAYCONFIG_MODE_INFO_TYPE_DESKTOP_IMAGE=3,
   DISPLAYCONFIG_MODE_INFO_TYPE_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_PIXELFORMAT
  {
   DISPLAYCONFIG_PIXELFORMAT_8BPP=1,
   DISPLAYCONFIG_PIXELFORMAT_16BPP=2,
   DISPLAYCONFIG_PIXELFORMAT_24BPP=3,
   DISPLAYCONFIG_PIXELFORMAT_32BPP=4,
   DISPLAYCONFIG_PIXELFORMAT_NONGDI=5,
   DISPLAYCONFIG_PIXELFORMAT_FORCE_UINT32=0xffffffff
  };
//---
enum DISPLAYCONFIG_ROTATION
  {
   DISPLAYCONFIG_ROTATION_IDENTITY=1,
   DISPLAYCONFIG_ROTATION_ROTATE90=2,
   DISPLAYCONFIG_ROTATION_ROTATE180=3,
   DISPLAYCONFIG_ROTATION_ROTATE270=4,
   DISPLAYCONFIG_ROTATION_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_SCALING
  {
   DISPLAYCONFIG_SCALING_IDENTITY=1,
   DISPLAYCONFIG_SCALING_CENTERED=2,
   DISPLAYCONFIG_SCALING_STRETCHED=3,
   DISPLAYCONFIG_SCALING_ASPECTRATIOCENTEREDMAX=4,
   DISPLAYCONFIG_SCALING_CUSTOM=5,
   DISPLAYCONFIG_SCALING_PREFERRED=128,
   DISPLAYCONFIG_SCALING_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_SCANLINE_ORDERING
  {
   DISPLAYCONFIG_SCANLINE_ORDERING_UNSPECIFIED=0,
   DISPLAYCONFIG_SCANLINE_ORDERING_PROGRESSIVE=1,
   DISPLAYCONFIG_SCANLINE_ORDERING_INTERLACED=2,
   DISPLAYCONFIG_SCANLINE_ORDERING_INTERLACED_UPPERFIELDFIRST=DISPLAYCONFIG_SCANLINE_ORDERING_INTERLACED,
   DISPLAYCONFIG_SCANLINE_ORDERING_INTERLACED_LOWERFIELDFIRST=3,
   DISPLAYCONFIG_SCANLINE_ORDERING_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_TOPOLOGY_ID
  {
   DISPLAYCONFIG_TOPOLOGY_INTERNAL=0x00000001,
   DISPLAYCONFIG_TOPOLOGY_CLONE=0x00000002,
   DISPLAYCONFIG_TOPOLOGY_EXTEND=0x00000004,
   DISPLAYCONFIG_TOPOLOGY_EXTERNAL=0x00000008,
   DISPLAYCONFIG_TOPOLOGY_FORCE_UINT32=0xFFFFFFFF
  };
//---
enum DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY
  {
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_OTHER=-1,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_HD15=0,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_SVIDEO=1,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_COMPOSITE_VIDEO=2,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_COMPONENT_VIDEO=3,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_DVI=4,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_HDMI=5,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_LVDS=6,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_D_JPN=8,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_SDI=9,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_DISPLAYPORT_EXTERNAL=10,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_DISPLAYPORT_EMBEDDED=11,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_UDI_EXTERNAL=12,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_UDI_EMBEDDED=13,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_SDTVDONGLE=14,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_MIRACAST=15,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_INDIRECT_WIRED=16,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_INTERNAL=0x80000000,
   DISPLAYCONFIG_OUTPUT_TECHNOLOGY_FORCE_UINT32=0xFFFFFFFF
  };
//---
struct ABC
  {
   int               abcA;
   uint              abcB;
   int               abcC;
  };
//---
struct ABCFLOAT
  {
   float             abcfA;
   float             abcfB;
   float             abcfC;
  };
//---
struct AXISINFOW
  {
   int               axMinValue;
   int               axMaxValue;
   short             axAxisName[MM_MAX_AXES_NAMELEN];
  };
//---
struct AXESLISTW
  {
   uint              axlReserved;
   uint              axlNumAxes;
   AXISINFOW         axlAxisInfo[MM_MAX_NUMAXES];
  };
//---
struct BITMAP
  {
   int               bmType;
   int               bmWidth;
   int               bmHeight;
   int               bmWidthBytes;
   ushort            bmPlanes;
   ushort            bmBitsPixel;
   PVOID             bmBits;
  };
//---
struct BITMAPCOREHEADER
  {
   uint              bcSize;
   ushort            bcWidth;
   ushort            bcHeight;
   ushort            bcPlanes;
   ushort            bcBitCount;
  };
//---
struct BITMAPFILEHEADER
  {
   ushort            bfType;
   uint              bfSize;
   ushort            bfReserved1;
   ushort            bfReserved2;
   uint              bfOffBits;
  };
//---
struct RGBTRIPLE
  {
   uchar             rgbtBlue;
   uchar             rgbtGreen;
   uchar             rgbtRed;
  };
//---
struct BITMAPCOREINFO
  {
   BITMAPCOREHEADER  bmciHeader;
   RGBTRIPLE         bmciColors[1];
  };
//---
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
//---
struct RGBQUAD
  {
   uchar             rgbBlue;
   uchar             rgbGreen;
   uchar             rgbRed;
   uchar             rgbReserved;
  };
//---
struct BITMAPINFO
  {
   BITMAPINFOHEADER  bmiHeader;
   RGBQUAD           bmiColors[1];
  };
//---
struct CIEXYZ
  {
   int               ciexyzX;
   int               ciexyzY;
   int               ciexyzZ;
  };
//---
struct CIEXYZTRIPLE
  {
   CIEXYZ            ciexyzRed;
   CIEXYZ            ciexyzGreen;
   CIEXYZ            ciexyzBlue;
  };
//---
struct BITMAPV4HEADER
  {
   uint              bV4Size;
   int               bV4Width;
   int               bV4Height;
   ushort            bV4Planes;
   ushort            bV4BitCount;
   uint              bV4V4Compression;
   uint              bV4SizeImage;
   int               bV4XPelsPerMeter;
   int               bV4YPelsPerMeter;
   uint              bV4ClrUsed;
   uint              bV4ClrImportant;
   uint              bV4RedMask;
   uint              bV4GreenMask;
   uint              bV4BlueMask;
   uint              bV4AlphaMask;
   uint              bV4CSType;
   CIEXYZTRIPLE      bV4Endpoints;
   uint              bV4GammaRed;
   uint              bV4GammaGreen;
   uint              bV4GammaBlue;
  };
//---
struct BITMAPV5HEADER
  {
   uint              bV5Size;
   int               bV5Width;
   int               bV5Height;
   ushort            bV5Planes;
   ushort            bV5BitCount;
   uint              bV5Compression;
   uint              bV5SizeImage;
   int               bV5XPelsPerMeter;
   int               bV5YPelsPerMeter;
   uint              bV5ClrUsed;
   uint              bV5ClrImportant;
   uint              bV5RedMask;
   uint              bV5GreenMask;
   uint              bV5BlueMask;
   uint              bV5AlphaMask;
   uint              bV5CSType;
   CIEXYZTRIPLE      bV5Endpoints;
   uint              bV5GammaRed;
   uint              bV5GammaGreen;
   uint              bV5GammaBlue;
   uint              bV5Intent;
   uint              bV5ProfileData;
   uint              bV5ProfileSize;
   uint              bV5Reserved;
  };
//---
struct BLENDFUNCTION
  {
   uchar             BlendOp;
   uchar             BlendFlags;
   uchar             SourceConstantAlpha;
   uchar             AlphaFormat;
  };
//---
struct FONTSIGNATURE
  {
   uint              fsUsb[4];
   uint              fsCsb[2];
  };
//---
struct CHARSETINFO
  {
   uint              ciCharset;
   uint              ciACP;
   FONTSIGNATURE     fs;
  };
//---
struct COLORADJUSTMENT
  {
   ushort            caSize;
   ushort            caFlags;
   ushort            caIlluminantIndex;
   ushort            caRedGamma;
   ushort            caGreenGamma;
   ushort            caBlueGamma;
   ushort            caReferenceBlack;
   ushort            caReferenceWhite;
   short             caContrast;
   short             caBrightness;
   short             caColorfulness;
   short             caRedGreenTint;
  };
//---
struct DESIGNVECTOR
  {
   uint              dvReserved;
   uint              dvNumAxes;
   int               dvValues[MM_MAX_NUMAXES];
  };
//---
struct DIBSECTION
  {
   BITMAP            dsBm;
   BITMAPINFOHEADER  dsBmih;
   uint              dsBitfields[3];
   HANDLE            dshSection;
   uint              dsOffset;
  };
//---
struct DISPLAY_DEVICEA
  {
   uint              cb;
   char              DeviceName[32];
   char              DeviceString[128];
   uint              StateFlags;
   char              DeviceID[128];
   char              DeviceKey[128];
  };
//---
struct DISPLAY_DEVICEW
  {
   uint              cb;
   short             DeviceName[32];
   short             DeviceString[128];
   uint              StateFlags;
   short             DeviceID[128];
   short             DeviceKey[128];
  };
//---
struct DISPLAYCONFIG_2DREGION
  {
   uint              cx;
   uint              cy;
  };
//---
struct DISPLAYCONFIG_DEVICE_INFO_HEADER
  {
   DISPLAYCONFIG_DEVICE_INFO_TYPE type;
   uint              size;
   LUID              adapterId;
   uint              id;
  };
//---
struct DISPLAYCONFIG_ADAPTER_NAME
  {
   DISPLAYCONFIG_DEVICE_INFO_HEADER header;
   short             adapterDevicePath[128];
  };
//---
struct DISPLAYCONFIG_DESKTOP_IMAGE_INFO
  {
   POINTL            PathSourceSize;
   RECTL             DesktopImageRegion;
   RECTL             DesktopImageClip;
  };
//---
struct DISPLAYCONFIG_PATH_SOURCE_INFO
  {
   LUID              adapterId;
   uint              id;
   uint              modeInfoIdx;
   uint              statusFlags;
  };
//---
struct DISPLAYCONFIG_RATIONAL
  {
   uint              Numerator;
   uint              Denominator;
  };
//---
struct DISPLAYCONFIG_PATH_TARGET_INFO
  {
   LUID              adapterId;
   uint              id;
   uint              modeInfoIdx;
   DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY outputTechnology;
   DISPLAYCONFIG_ROTATION rotation;
   DISPLAYCONFIG_SCALING scaling;
   DISPLAYCONFIG_RATIONAL refreshRate;
   DISPLAYCONFIG_SCANLINE_ORDERING scanLineOrdering;
   int               targetAvailable;
   uint              statusFlags;
  };
//---
struct DISPLAYCONFIG_PATH_INFO
  {
   DISPLAYCONFIG_PATH_SOURCE_INFO sourceInfo;
   DISPLAYCONFIG_PATH_TARGET_INFO targetInfo;
   uint              flags;
  };
//---
struct DISPLAYCONFIG_SDR_WHITE_LEVEL
  {
   DISPLAYCONFIG_DEVICE_INFO_HEADER header;
   uint              SDRWhiteLevel;
  };
//---
struct DISPLAYCONFIG_SOURCE_DEVICE_NAME
  {
   DISPLAYCONFIG_DEVICE_INFO_HEADER header;
   short             viewGdiDeviceName[CCHDEVICENAME];
  };
//---
struct DISPLAYCONFIG_SOURCE_MODE
  {
   uint              width;
   uint              height;
   DISPLAYCONFIG_PIXELFORMAT pixelFormat;
   POINTL            position;
  };
//---
struct DISPLAYCONFIG_TARGET_BASE_TYPE
  {
   DISPLAYCONFIG_DEVICE_INFO_HEADER header;
   DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY baseOutputTechnology;
  };
//---
struct DISPLAYCONFIG_TARGET_DEVICE_NAME_FLAGS
  {
   uint              value;
  };
//---
struct DISPLAYCONFIG_TARGET_DEVICE_NAME
  {
   DISPLAYCONFIG_DEVICE_INFO_HEADER header;
   DISPLAYCONFIG_TARGET_DEVICE_NAME_FLAGS flags;
   DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY outputTechnology;
   ushort            edidManufactureId;
   ushort            edidProductCodeId;
   uint              connectorInstance;
   short             monitorFriendlyDeviceName[64];
   short             monitorDevicePath[128];
  };
//---
struct DISPLAYCONFIG_VIDEO_SIGNAL_INFO
  {
   ulong             pixelRate;
   DISPLAYCONFIG_RATIONAL hSyncFreq;
   DISPLAYCONFIG_RATIONAL vSyncFreq;
   DISPLAYCONFIG_2DREGION activeSize;
   DISPLAYCONFIG_2DREGION totalSize;
   uint              videoStandard;
   DISPLAYCONFIG_SCANLINE_ORDERING scanLineOrdering;
  };
//---
struct DISPLAYCONFIG_TARGET_MODE
  {
   DISPLAYCONFIG_VIDEO_SIGNAL_INFO targetVideoSignalInfo;
  };
//---
struct DISPLAYCONFIG_TARGET_PREFERRED_MODE
  {
   DISPLAYCONFIG_DEVICE_INFO_HEADER header;
   uint              width;
   uint              height;
   DISPLAYCONFIG_TARGET_MODE targetMode;
  };
//---
struct DOCINFOW
  {
   int               cbSize;
   const string      lpszDocName;
   const string      lpszOutput;
   const string      lpszDatatype;
   uint              fwType;
  };
//---
struct DRAWPATRECT
  {
   POINT             ptPosition;
   POINT             ptSize;
   ushort            wStyle;
   ushort            wPattern;
  };
//---
struct EMR
  {
   uint              iType;
   uint              nSize;
  };
//---
struct EMRABORTPATH
  {
   EMR               emr;
  };
//---
struct XFORM
  {
   float             eM11;
   float             eM12;
   float             eM21;
   float             eM22;
   float             eDx;
   float             eDy;
  };
//---
struct EMRALPHABLEND
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               cxDest;
   int               cyDest;
   uint              dwRop;
   int               xSrc;
   int               ySrc;
   XFORM             xformSrc;
   uint              crBkColorSrc;
   uint              iUsageSrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   int               cxSrc;
   int               cySrc;
  };
//---
struct EMRANGLEARC
  {
   EMR               emr;
   POINTL            ptlCenter;
   uint              nRadius;
   float             eStartAngle;
   float             eSweepAngle;
  };
//---
struct EMRARC
  {
   EMR               emr;
   RECTL             rclBox;
   POINTL            ptlStart;
   POINTL            ptlEnd;
  };
//---
struct EMRBITBLT
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               cxDest;
   int               cyDest;
   uint              dwRop;
   int               xSrc;
   int               ySrc;
   XFORM             xformSrc;
   uint              crBkColorSrc;
   uint              iUsageSrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
  };
//---
struct EMRCOLORCORRECTPALETTE
  {
   EMR               emr;
   uint              ihPalette;
   uint              nFirstEntry;
   uint              nPalEntries;
   uint              nReserved;
  };
//---
struct EMRCOLORMATCHTOTARGET
  {
   EMR               emr;
   uint              dwAction;
   uint              dwFlags;
   uint              cbName;
   uint              cbData;
   uchar             Data[1];
  };
//---
struct LOGBRUSH
  {
   uint              lbStyle;
   uint              lbColor;
   ulong             lbHatch;
  };
//---
struct EMRCREATEBRUSHINDIRECT
  {
   EMR               emr;
   uint              ihBrush;
   LOGBRUSH          lb;
  };
//---
struct LOGCOLORSPACEW
  {
   uint              lcsSignature;
   uint              lcsVersion;
   uint              lcsSize;
   int               lcsCSType;
   int               lcsIntent;
   CIEXYZTRIPLE      lcsEndpoints;
   uint              lcsGammaRed;
   uint              lcsGammaGreen;
   uint              lcsGammaBlue;
   short             lcsFilename[MAX_PATH];
  };
//---
struct EMRCREATECOLORSPACEW
  {
   EMR               emr;
   uint              ihCS;
   LOGCOLORSPACEW    lcs;
   uint              dwFlags;
   uint              cbData;
   uchar             Data[1];
  };
//---
struct EMRCREATEDIBPATTERNBRUSHPT
  {
   EMR               emr;
   uint              ihBrush;
   uint              iUsage;
   uint              offBmi;
   uint              cbBmi;
   uint              offBits;
   uint              cbBits;
  };
//---
struct EMRCREATEMONOBRUSH
  {
   EMR               emr;
   uint              ihBrush;
   uint              iUsage;
   uint              offBmi;
   uint              cbBmi;
   uint              offBits;
   uint              cbBits;
  };
//---
struct LOGPALETTE
  {
   ushort            palVersion;
   ushort            palNumEntries;
  };
//---
struct LOGPEN
  {
   uint              lopnStyle;
   POINT             lopnWidth;
   uint              lopnColor;
  };
//---
struct EMRCREATEPALETTE
  {
   EMR               emr;
   uint              ihPal;
   LOGPALETTE        lgpl;
  };
//---
struct EMRCREATEPEN
  {
   EMR               emr;
   uint              ihPen;
   LOGPEN            lopn;
  };
//---
struct EMRELLIPSE
  {
   EMR               emr;
   RECTL             rclBox;
  };
//---
struct EMREOF
  {
   EMR               emr;
   uint              nPalEntries;
   uint              offPalEntries;
   uint              nSizeLast;
  };
//---
struct EMREXCLUDECLIPRECT
  {
   EMR               emr;
   RECTL             rclClip;
  };
//---
struct LOGFONTW
  {
   int               lfHeight;
   int               lfWidth;
   int               lfEscapement;
   int               lfOrientation;
   int               lfWeight;
   uchar             lfItalic;
   uchar             lfUnderline;
   uchar             lfStrikeOut;
   uchar             lfCharSet;
   uchar             lfOutPrecision;
   uchar             lfClipPrecision;
   uchar             lfQuality;
   uchar             lfPitchAndFamily;
   short             lfFaceName[LF_FACESIZE];
  };
//---
struct PANOSE
  {
   uchar             bFamilyType;
   uchar             bSerifStyle;
   uchar             bWeight;
   uchar             bProportion;
   uchar             bContrast;
   uchar             bStrokeVariation;
   uchar             bArmStyle;
   uchar             bLetterform;
   uchar             bMidline;
   uchar             bXHeight;
  };
//---
struct EXTLOGFONTW
  {
   LOGFONTW          elfLogFont;
   short             elfFullName[LF_FULLFACESIZE];
   short             elfStyle[LF_FACESIZE];
   uint              elfVersion;
   uint              elfStyleSize;
   uint              elfMatch;
   uint              elfReserved;
   uchar             elfVendorId[ELF_VENDOR_SIZE];
   uint              elfCulture;
   PANOSE            elfPanose;
  };
//---
struct EMREXTCREATEFONTINDIRECTW
  {
   EMR               emr;
   uint              ihFont;
   EXTLOGFONTW       elfw;
  };
//---
struct EXTLOGPEN
  {
   uint              elpPenStyle;
   uint              elpWidth;
   uint              elpBrushStyle;
   uint              elpColor;
   ulong             elpHatch;
   uint              elpNumEntries;
   uint              elpStyleEntry[1];
  };
//---
struct EMREXTCREATEPEN
  {
   EMR               emr;
   uint              ihPen;
   uint              offBmi;
   uint              cbBmi;
   uint              offBits;
   uint              cbBits;
   EXTLOGPEN         elp;
  };
//---
struct EMREXTESCAPE
  {
   EMR               emr;
   int               iEscape;
   int               cbEscData;
   uchar             EscData[1];
  };
//---
struct EMREXTFLOODFILL
  {
   EMR               emr;
   POINTL            ptlStart;
   uint              crColor;
   uint              iMode;
  };
//---
struct EMREXTSELECTCLIPRGN
  {
   EMR               emr;
   uint              cbRgnData;
   uint              iMode;
   uchar             RgnData[1];
  };
//---
struct EMRTEXT
  {
   POINTL            ptlReference;
   uint              nChars;
   uint              offString;
   uint              fOptions;
   RECTL             rcl;
   uint              offDx;
  };
//---
struct EMREXTTEXTOUTA
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              iGraphicsMode;
   float             exScale;
   float             eyScale;
   EMRTEXT           emrtext;
  };
//---
struct EMRFILLPATH
  {
   EMR               emr;
   RECTL             rclBounds;
  };
//---
struct EMRFILLRGN
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cbRgnData;
   uint              ihBrush;
   uchar             RgnData[1];
  };
//---
struct EMRFORMAT
  {
   uint              dSignature;
   uint              nVersion;
   uint              cbData;
   uint              offData;
  };
//---
struct EMRFRAMERGN
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cbRgnData;
   uint              ihBrush;
   SIZE              szlStroke;
   uchar             RgnData[1];
  };
//---
struct EMRGDICOMMENT
  {
   EMR               emr;
   uint              cbData;
   uchar             Data[1];
  };
//---
struct EMRGLSBOUNDEDRECORD
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cbData;
   uchar             Data[1];
  };
//---
struct EMRGLSRECORD
  {
   EMR               emr;
   uint              cbData;
   uchar             Data[1];
  };
//---
struct PIXELFORMATDESCRIPTOR
  {
   ushort            nSize;
   ushort            nVersion;
   uint              dwFlags;
   uchar             iPixelType;
   uchar             cColorBits;
   uchar             cRedBits;
   uchar             cRedShift;
   uchar             cGreenBits;
   uchar             cGreenShift;
   uchar             cBlueBits;
   uchar             cBlueShift;
   uchar             cAlphaBits;
   uchar             cAlphaShift;
   uchar             cAccumBits;
   uchar             cAccumRedBits;
   uchar             cAccumGreenBits;
   uchar             cAccumBlueBits;
   uchar             cAccumAlphaBits;
   uchar             cDepthBits;
   uchar             cStencilBits;
   uchar             cAuxBuffers;
   uchar             iLayerType;
   uchar             bReserved;
   uint              dwLayerMask;
   uint              dwVisibleMask;
   uint              dwDamageMask;
  };
//---
struct TRIVERTEX
  {
   int               x;
   int               y;
   ushort            red;
   ushort            green;
   ushort            blue;
   ushort            alpha;
  };
//---
struct EMRGRADIENTFILL
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              nVer;
   uint              nTri;
   uint              ulMode;
   TRIVERTEX         Ver[1];
  };
//---
struct EMRINVERTRGN
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cbRgnData;
   uchar             RgnData[1];
  };
//---
struct EMRLINETO
  {
   EMR               emr;
   POINTL            ptl;
  };
//---
struct EMRMASKBLT
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               cxDest;
   int               cyDest;
   uint              dwRop;
   int               xSrc;
   int               ySrc;
   XFORM             xformSrc;
   uint              crBkColorSrc;
   uint              iUsageSrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   int               xMask;
   int               yMask;
   uint              iUsageMask;
   uint              offBmiMask;
   uint              cbBmiMask;
   uint              offBitsMask;
   uint              cbBitsMask;
  };
//---
struct EMRMODIFYWORLDTRANSFORM
  {
   EMR               emr;
   XFORM             xform;
   uint              iMode;
  };
//---
struct EMRNAMEDESCAPE
  {
   EMR               emr;
   int               iEscape;
   int               cbDriver;
   int               cbEscData;
   uchar             EscData[1];
  };
//---
struct EMROFFSETCLIPRGN
  {
   EMR               emr;
   POINTL            ptlOffset;
  };
//---
struct EMRPIXELFORMAT
  {
   EMR               emr;
   PIXELFORMATDESCRIPTOR pfd;
  };
//---
struct EMRPLGBLT
  {
   EMR               emr;
   RECTL             rclBounds;
   POINTL            aptlDest[3];
   int               xSrc;
   int               ySrc;
   int               cxSrc;
   int               cySrc;
   XFORM             xformSrc;
   uint              crBkColorSrc;
   uint              iUsageSrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   int               xMask;
   int               yMask;
   uint              iUsageMask;
   uint              offBmiMask;
   uint              cbBmiMask;
   uint              offBitsMask;
   uint              cbBitsMask;
  };
//---
struct EMRPOLYDRAW
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cptl;
   POINTL            aptl[1];
   uchar             abTypes[1];
  };
//---
struct EMRPOLYDRAW16
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cpts;
   POINTS            apts[1];
   uchar             abTypes[1];
  };
//---
struct EMRPOLYLINE
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cptl;
   POINTL            aptl[1];
  };
//---
struct EMRPOLYLINE16
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              cpts;
   POINTS            apts[1];
  };
//---
struct EMRPOLYPOLYLINE
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              nPolys;
   uint              cptl;
   uint              aPolyCounts[1];
   POINTL            aptl[1];
  };
//---
struct EMRPOLYPOLYLINE16
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              nPolys;
   uint              cpts;
   uint              aPolyCounts[1];
   POINTS            apts[1];
  };
//---
struct EMRPOLYTEXTOUTW
  {
   EMR               emr;
   RECTL             rclBounds;
   uint              iGraphicsMode;
   float             exScale;
   float             eyScale;
   int               cStrings;
   EMRTEXT           aemrtext[1];
  };
//---
struct EMRRESIZEPALETTE
  {
   EMR               emr;
   uint              ihPal;
   uint              cEntries;
  };
//---
struct EMRRESTOREDC
  {
   EMR               emr;
   int               iRelative;
  };
//---
struct EMRROUNDRECT
  {
   EMR               emr;
   RECTL             rclBox;
   SIZE              szlCorner;
  };
//---
struct EMRSCALEVIEWPORTEXTEX
  {
   EMR               emr;
   int               xNum;
   int               xDenom;
   int               yNum;
   int               yDenom;
  };
//---
struct EMRSELECTCLIPPATH
  {
   EMR               emr;
   uint              iMode;
  };
//---
struct EMRSELECTOBJECT
  {
   EMR               emr;
   uint              ihObject;
  };
//---
struct EMRSELECTPALETTE
  {
   EMR               emr;
   uint              ihPal;
  };
//---
struct EMRSETARCDIRECTION
  {
   EMR               emr;
   uint              iArcDirection;
  };
//---
struct EMRSETBKCOLOR
  {
   EMR               emr;
   uint              crColor;
  };
//---
struct EMRSETCOLORADJUSTMENT
  {
   EMR               emr;
   COLORADJUSTMENT   ColorAdjustment;
  };
//---
struct EMRSETCOLORSPACE
  {
   EMR               emr;
   uint              ihCS;
  };
//---
struct EMRSETDIBITSTODEVICE
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               xSrc;
   int               ySrc;
   int               cxSrc;
   int               cySrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   uint              iUsageSrc;
   uint              iStartScan;
   uint              cScans;
  };
//---
struct EMRSETICMPROFILE
  {
   EMR               emr;
   uint              dwFlags;
   uint              cbName;
   uint              cbData;
   uchar             Data[1];
  };
//---
struct EMRSETMAPPERFLAGS
  {
   EMR               emr;
   uint              dwFlags;
  };
//---
struct EMRSETMITERLIMIT
  {
   EMR               emr;
   float             eMiterLimit;
  };
//---
struct PALETTEENTRY
  {
   uchar             red;
   uchar             green;
   uchar             blue;
   uchar             flags;
  };
//---
struct EMRSETPALETTEENTRIES
  {
   EMR               emr;
   uint              ihPal;
   uint              iStart;
   uint              cEntries;
   PALETTEENTRY      aPalEntries[1];
  };
//---
struct EMRSETPIXELV
  {
   EMR               emr;
   POINTL            ptlPixel;
   uint              crColor;
  };
//---
struct EMRSETVIEWPORTEXTEX
  {
   EMR               emr;
   SIZE              szlExtent;
  };
//---
struct EMRSETVIEWPORTORGEX
  {
   EMR               emr;
   POINTL            ptlOrigin;
  };
//---
struct EMRSETWORLDTRANSFORM
  {
   EMR               emr;
   XFORM             xform;
  };
//---
struct EMRSTRETCHBLT
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               cxDest;
   int               cyDest;
   uint              dwRop;
   int               xSrc;
   int               ySrc;
   XFORM             xformSrc;
   uint              crBkColorSrc;
   uint              iUsageSrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   int               cxSrc;
   int               cySrc;
  };
//---
struct EMRSTRETCHDIBITS
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               xSrc;
   int               ySrc;
   int               cxSrc;
   int               cySrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   uint              iUsageSrc;
   uint              dwRop;
   int               cxDest;
   int               cyDest;
  };
//---
struct EMRTRANSPARENTBLT
  {
   EMR               emr;
   RECTL             rclBounds;
   int               xDest;
   int               yDest;
   int               cxDest;
   int               cyDest;
   uint              dwRop;
   int               xSrc;
   int               ySrc;
   XFORM             xformSrc;
   uint              crBkColorSrc;
   uint              iUsageSrc;
   uint              offBmiSrc;
   uint              cbBmiSrc;
   uint              offBitsSrc;
   uint              cbBitsSrc;
   int               cxSrc;
   int               cySrc;
  };
//---
struct ENHMETAHEADER
  {
   uint              iType;
   uint              nSize;
   RECTL             rclBounds;
   RECTL             rclFrame;
   uint              dSignature;
   uint              nVersion;
   uint              nBytes;
   uint              nRecords;
   ushort            nHandles;
   ushort            sReserved;
   uint              nDescription;
   uint              offDescription;
   uint              nPalEntries;
   SIZE              szlDevice;
   SIZE              szlMillimeters;
   uint              cbPixelFormat;
   uint              offPixelFormat;
   uint              bOpenGL;
   SIZE              szlMicrometers;
  };
//---
struct ENHMETARECORD
  {
   uint              iType;
   uint              nSize;
   uint              dParm[1];
  };
//---
struct ENUMLOGFONTEXW
  {
   LOGFONTW          elfLogFont;
   short             elfFullName[LF_FULLFACESIZE];
   short             elfStyle[LF_FACESIZE];
   short             elfScript[LF_FACESIZE];
  };
//---
struct ENUMLOGFONTEXDVW
  {
   ENUMLOGFONTEXW    elfEnumLogfontEx;
   DESIGNVECTOR      elfDesignVector;
  };
//---
struct ENUMLOGFONTW
  {
   LOGFONTW          elfLogFont;
   short             elfFullName[LF_FULLFACESIZE];
   short             elfStyle[LF_FACESIZE];
  };
//---
struct NEWTEXTMETRICW
  {
   int               tmHeight;
   int               tmAscent;
   int               tmDescent;
   int               tmInternalLeading;
   int               tmExternalLeading;
   int               tmAveCharWidth;
   int               tmMaxCharWidth;
   int               tmWeight;
   int               tmOverhang;
   int               tmDigitizedAspectX;
   int               tmDigitizedAspectY;
   short             tmFirstChar;
   short             tmLastChar;
   short             tmDefaultChar;
   short             tmBreakChar;
   uchar             tmItalic;
   uchar             tmUnderlined;
   uchar             tmStruckOut;
   uchar             tmPitchAndFamily;
   uchar             tmCharSet;
   uint              ntmFlags;
   uint              ntmSizeEM;
   uint              ntmCellHeight;
   uint              ntmAvgWidth;
  };
//---
struct NEWTEXTMETRICEXW
  {
   NEWTEXTMETRICW    ntmTm;
   FONTSIGNATURE     ntmFontSig;
  };
//---
struct ENUMTEXTMETRICW
  {
   NEWTEXTMETRICEXW  etmNewTextMetricEx;
   AXESLISTW         etmAxesList;
  };
//---
struct FIXED
  {
   ushort            fract;
   short             value;
  };
//---
struct POINTFLOAT
  {
   float             x;
   float             y;
  };
//---
struct GCP_RESULTSW
  {
   uint              lStructSize;
   string            lpOutString;
   PVOID             lpOrder;    
   PVOID             lpDx;       
   PVOID             lpCaretPos; 
   PVOID             lpClass;    
   PVOID             lpGlyphs;
   uint              nGlyphs;
   int               nMaxFit;
  };
//---
struct GLYPHMETRICS
  {
   uint              gmBlackBoxX;
   uint              gmBlackBoxY;
   POINT             gmptGlyphOrigin;
   short             gmCellIncX;
   short             gmCellIncY;
  };
//---
struct GLYPHMETRICSFLOAT
  {
   float             gmfBlackBoxX;
   float             gmfBlackBoxY;
   POINTFLOAT        gmfptGlyphOrigin;
   float             gmfCellIncX;
   float             gmfCellIncY;
  };
//---
struct WCRANGE
  {
   short             wcLow;
   ushort            cGlyphs;
  };
//---
struct GLYPHSET
  {
   uint              cbThis;
   uint              flAccel;
   uint              cGlyphsSupported;
   uint              cRanges;
   WCRANGE           ranges[1];
  };
//---
struct GRADIENT_RECT
  {
   uint              UpperLeft;
   uint              LowerRight;
  };
//---
struct GRADIENT_TRIANGLE
  {
   uint              Vertex1;
   uint              Vertex2;
   uint              Vertex3;
  };
//---
struct HANDLETABLE
  {
   PVOID             objectHandle[1];
  };
//---
struct KERNINGPAIR
  {
   ushort            wFirst;
   ushort            wSecond;
   int               iKernAmount;
  };
//---
struct LAYERPLANEDESCRIPTOR
  {
   ushort            nSize;
   ushort            nVersion;
   uint              dwFlags;
   uchar             iPixelType;
   uchar             cColorBits;
   uchar             cRedBits;
   uchar             cRedShift;
   uchar             cGreenBits;
   uchar             cGreenShift;
   uchar             cBlueBits;
   uchar             cBlueShift;
   uchar             cAlphaBits;
   uchar             cAlphaShift;
   uchar             cAccumBits;
   uchar             cAccumRedBits;
   uchar             cAccumGreenBits;
   uchar             cAccumBlueBits;
   uchar             cAccumAlphaBits;
   uchar             cDepthBits;
   uchar             cStencilBits;
   uchar             cAuxBuffers;
   uchar             iLayerPlane;
   uchar             bReserved;
   uint              crTransparent;
  };
//---
struct LOCALESIGNATURE
  {
   uint              lsUsb[4];
   uint              lsCsbDefault[2];
   uint              lsCsbSupported[2];
  };
//---
struct MAT2
  {
   FIXED             eM11;
   FIXED             eM12;
   FIXED             eM21;
   FIXED             eM22;
  };
//---
struct METAFILEPICT
  {
   int               mm;
   int               xExt;
   int               yExt;
   HANDLE            hMF;
  };
//---
struct METAHEADER
  {
   ushort            mtType;
   ushort            mtHeaderSize;
   ushort            mtVersion;
   uint              mtSize;
   ushort            mtNoObjects;
   uint              mtMaxRecord;
   ushort            mtNoParameters;
  };
//---
struct METARECORD
  {
   uint              rdSize;
   ushort            rdFunction;
   ushort            rdParm[1];
  };
//---
struct TEXTMETRICW
  {
   int               tmHeight;
   int               tmAscent;
   int               tmDescent;
   int               tmInternalLeading;
   int               tmExternalLeading;
   int               tmAveCharWidth;
   int               tmMaxCharWidth;
   int               tmWeight;
   int               tmOverhang;
   int               tmDigitizedAspectX;
   int               tmDigitizedAspectY;
   short             tmFirstChar;
   short             tmLastChar;
   short             tmDefaultChar;
   short             tmBreakChar;
   uchar             tmItalic;
   uchar             tmUnderlined;
   uchar             tmStruckOut;
   uchar             tmPitchAndFamily;
   uchar             tmCharSet;
  };
//---
struct OUTLINETEXTMETRICW
  {
   uint              otmSize;
   TEXTMETRICW       otmTextMetrics;
   uchar             otmFiller;
   PANOSE            otmPanoseNumber;
   uint              otmfsSelection;
   uint              otmfsType;
   int               otmsCharSlopeRise;
   int               otmsCharSlopeRun;
   int               otmItalicAngle;
   uint              otmEMSquare;
   int               otmAscent;
   int               otmDescent;
   uint              otmLineGap;
   uint              otmsCapEmHeight;
   uint              otmsXHeight;
   RECT              otmrcFontBox;
   int               otmMacAscent;
   int               otmMacDescent;
   uint              otmMacLineGap;
   uint              otmusMinimumPPEM;
   POINT             otmptSubscriptSize;
   POINT             otmptSubscriptOffset;
   POINT             otmptSuperscriptSize;
   POINT             otmptSuperscriptOffset;
   uint              otmsStrikeoutSize;
   int               otmsStrikeoutPosition;
   int               otmsUnderscoreSize;
   int               otmsUnderscorePosition;
   PVOID             otmpFamilyName;   //char              otmpFamilyName[];
   PVOID             otmpFaceName;     //char              otmpFaceName[];
   PVOID             otmpStyleName;    //char              otmpStyleName[];
   PVOID             otmpFullName;     //char              otmpFullName[];
  };
//---
struct PELARRAY
  {
   int               paXCount;
   int               paYCount;
   int               paXExt;
   int               paYExt;
   uchar             paRGBs;
  };
//---
struct POINTFX
  {
   FIXED             x;
   FIXED             y;
  };
//---
struct POLYTEXTW
  {
   int               x;
   int               y;
   uint              n;
   const string      lpstr;
   uint              uiFlags;
   RECT              rcl;
   PVOID             pdx;
  };
//---
struct PSFEATURE_CUSTPAPER
  {
   int               lOrientation;
   int               lWidth;
   int               lHeight;
   int               lWidthOffset;
   int               lHeightOffset;
  };
//---
struct PSFEATURE_OUTPUT
  {
   int               bPageIndependent;
   int               bSetPageDevice;
  };
//---
struct PSINJECTDATA
  {
   uint              DataBytes;
   ushort            InjectionPoint;
   ushort            PageNumber;
  };
//---
struct RASTERIZER_STATUS
  {
   short             nSize;
   short             wFlags;
   short             nLanguageID;
  };
//---
struct RGNDATAHEADER
  {
   uint              dwSize;
   uint              iType;
   uint              nCount;
   uint              nRgnSize;
   RECT              rcBound;
  };
//---
struct RGNDATA
  {
   RGNDATAHEADER     rdh;
   char              Buffer[1];
  };
//---
struct TTPOLYCURVE
  {
   ushort            wType;
   ushort            cpfx;
   POINTFX           apfx[1];
  };
//---
struct TTPOLYGONHEADER
  {
   uint              cb;
   uint              dwType;
   POINTFX           pfxStart;
  };
//---
struct DEVMODEW
  {
   short             dmDeviceName[CCHDEVICENAME];
   ushort            dmSpecVersion;
   ushort            dmDriverVersion;
   ushort            dmSize;
   ushort            dmDriverExtra;
   uint              dmFields;
   short             dmOrientation;
   short             dmPaperSize;
   short             dmPaperLength;
   short             dmPaperWidth;
   short             dmScale;
   short             dmCopies;
   short             dmDefaultSource;
   short             dmPrintQuality;
   short             dmColor;
   short             dmDuplex;
   short             dmYResolution;
   short             dmTTOption;
   short             dmCollate;
   short             dmFormName[CCHFORMNAME];
   ushort            dmLogPixels;
   uint              dmBitsPerPel;
   uint              dmPelsWidth;
   uint              dmPelsHeight;
   uint              dmDisplayFlags;
   uint              dmDisplayFrequency;
   uint              dmICMMethod;
   uint              dmICMIntent;
   uint              dmMediaType;
   uint              dmDitherType;
   uint              dmReserved1;
   uint              dmReserved2;
   uint              dmPanningWidth;
   uint              dmPanningHeight;
  };
//---
struct WGLSWAP
  {
   HANDLE            hdc;
   uint              uiFlags;
  };
//---
union DISPLAYCONFIG_MODE
  {
   DISPLAYCONFIG_TARGET_MODE targetMode;
   DISPLAYCONFIG_SOURCE_MODE sourceMode;
   DISPLAYCONFIG_DESKTOP_IMAGE_INFO desktopImageInfo;
  };
//---
struct DISPLAYCONFIG_MODE_INFO
  {
   DISPLAYCONFIG_MODE_INFO_TYPE infoType;
   uint              id;
   LUID              adapterId;
   DISPLAYCONFIG_MODE mode;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "gdi32.dll"
int          AbortDoc(HANDLE hdc);
int          AbortPath(HANDLE hdc);
HANDLE       AddFontMemResourceEx(PVOID file_view,uint size,PVOID resrved,uint &num_fonts);
int          AddFontResourceExW(const string name,uint fl,PVOID res);
int          AddFontResourceW(string);
int          AngleArc(HANDLE hdc,int x,int y,uint r,float StartAngle,float SweepAngle);
int          AnimatePalette(HANDLE pal,uint start_index,uint entries,PALETTEENTRY &ppe);
int          Arc(HANDLE hdc,int x1,int y1,int x2,int y2,int x3,int y3,int x4,int y4);
int          ArcTo(HANDLE hdc,int left,int top,int right,int bottom,int xr1,int yr1,int xr2,int yr2);
int          BeginPath(HANDLE hdc);
int          BitBlt(HANDLE hdc,int x,int y,int cx,int cy,HANDLE src,int x1,int y1,uint rop);
int          CancelDC(HANDLE hdc);
int          CheckColorsInGamut(HANDLE hdc,RGBTRIPLE &lpRGBTriple,PVOID buffer,uint count);
int          ChoosePixelFormat(HANDLE hdc,PIXELFORMATDESCRIPTOR &ppfd);
int          Chord(HANDLE hdc,int x1,int y1,int x2,int y2,int x3,int y3,int x4,int y4);
HANDLE       CloseEnhMetaFile(HANDLE hdc);
int          CloseFigure(HANDLE hdc);
HANDLE       CloseMetaFile(HANDLE hdc);
int          ColorCorrectPalette(HANDLE hdc,HANDLE pal,uint first,uint num);
int          ColorMatchToTarget(HANDLE hdc,HANDLE target,uint action);
int          CombineRgn(HANDLE dst,HANDLE src1,HANDLE src2,int mode);
int          CombineTransform(XFORM &out,XFORM &lpxf1,XFORM &lpxf2);
HANDLE       CopyEnhMetaFileW(HANDLE enh,const string file_name);
HANDLE       CopyMetaFileW(HANDLE,string LPCWSTR);
HANDLE       CreateBitmap(int width,int height,uint planes,uint bit_count,PVOID bits);
HANDLE       CreateBitmap(int width,int height,uint planes,uint bit_count,char &bits[]);
HANDLE       CreateBitmapIndirect(BITMAP &pbm);
HANDLE       CreateBrushIndirect(LOGBRUSH &plbrush);
HANDLE       CreateColorSpaceW(LOGCOLORSPACEW &lplcs);
HANDLE       CreateCompatibleBitmap(HANDLE hdc,int cx,int cy);
HANDLE       CreateCompatibleDC(HANDLE hdc);
HANDLE       CreateDCW(const string driver,const string device,const string port,DEVMODEW &pdm);
HANDLE       CreateDIBitmap(HANDLE hdc,BITMAPINFOHEADER &pbmih,uint init,PVOID bits,BITMAPINFO &pbmi,uint usage);
HANDLE       CreateDIBPatternBrush(HANDLE h,uint usage);
HANDLE       CreateDIBPatternBrushPt(PVOID lpPackedDIB,uint usage);
HANDLE       CreateDIBSection(HANDLE hdc,BITMAPINFO &pbmi,uint usage,PVOID bits,HANDLE section,uint offset);
HANDLE       CreateDiscardableBitmap(HANDLE hdc,int cx,int cy);
HANDLE       CreateEllipticRgn(int x1,int y1,int x2,int y2);
HANDLE       CreateEllipticRgnIndirect(RECT &lprect);
HANDLE       CreateEnhMetaFileW(HANDLE hdc,const string filename,RECT &lprc,const string desc);
HANDLE       CreateFontIndirectExW(ENUMLOGFONTEXDVW &);
HANDLE       CreateFontIndirectW(LOGFONTW &lplf);
HANDLE       CreateFontW(int height,int width,int escapement,int orientation,int weight,uint italic,uint underline,uint strike_out,uint char_set,uint out_precision,uint clip_precision,uint quality,uint pitch_and_family,const string face_name);
HANDLE       CreateHalftonePalette(HANDLE hdc);
HANDLE       CreateHatchBrush(int hatch,uint clr);
HANDLE       CreateICW(const string driver,const string device,const string port,DEVMODEW &pdm);
HANDLE       CreateMetaFileW(const string file);
HANDLE       CreatePalette(LOGPALETTE &plpal);
HANDLE       CreatePatternBrush(HANDLE hbm);
HANDLE       CreatePen(int style,int width,uint clr);
HANDLE       CreatePenIndirect(LOGPEN &plpen);
HANDLE       CreatePolygonRgn(POINT &pptl[],int point,int mode);
HANDLE       CreatePolyPolygonRgn(const POINT &pptl[],const int &pc[],int poly,int mode);
HANDLE       CreateRectRgn(int x1,int y1,int x2,int y2);
HANDLE       CreateRectRgnIndirect(RECT &lprect);
HANDLE       CreateRoundRectRgn(int x1,int y1,int x2,int y2,int w,int h);
int          CreateScalableFontResourceW(uint hidden,const string font,const string file,const string path);
HANDLE       CreateSolidBrush(uint clr);
int          DeleteColorSpace(HANDLE hcs);
int          DeleteDC(HANDLE hdc);
int          DeleteEnhMetaFile(HANDLE hmf);
int          DeleteMetaFile(HANDLE hmf);
int          DeleteObject(PVOID ho);
int          DescribePixelFormat(HANDLE hdc,int pixel_format,uint bytes,PIXELFORMATDESCRIPTOR &ppfd);
int          DPtoLP(HANDLE hdc,POINT &lppt,int c);
int          DrawEscape(HANDLE hdc,int escape,int in,uchar &in[]);
int          Ellipse(HANDLE hdc,int left,int top,int right,int bottom);
int          EndDoc(HANDLE hdc);
int          EndPage(HANDLE hdc);
int          EndPath(HANDLE hdc);
int          EnumEnhMetaFile(HANDLE hdc,HANDLE hmf,PVOID proc,PVOID param,RECT &rect);
int          EnumFontFamiliesExW(HANDLE hdc,LOGFONTW &logfont,PVOID proc,PVOID param,uint flags);
int          EnumFontFamiliesW(HANDLE hdc,const string logfont,PVOID proc,PVOID param);
int          EnumFontsW(HANDLE hdc,const string logfont,PVOID proc,PVOID param);
int          EnumICMProfilesW(HANDLE hdc,PVOID proc,PVOID param);
int          EnumMetaFile(HANDLE hdc,HANDLE hmf,PVOID proc,PVOID param);
int          EnumObjects(HANDLE hdc,int type,PVOID func,PVOID param);
int          EqualRgn(HANDLE hrgn1,HANDLE hrgn2);
int          Escape(HANDLE hdc,int escape,int in,uchar &in[],PVOID out);
int          ExcludeClipRect(HANDLE hdc,int left,int top,int right,int bottom);
HANDLE       ExtCreatePen(uint pen_style,uint width,LOGBRUSH &plbrush,uint style,const uint &pstyle[]);
HANDLE       ExtCreateRegion(XFORM &lpx,uint count,RGNDATA &data);
int          ExtEscape(HANDLE hdc,int escape,int inp,uchar &in_data[],int output,uchar &out_data[]);
int          ExtFloodFill(HANDLE hdc,int x,int y,uint clr,uint type);
int          ExtSelectClipRgn(HANDLE hdc,HANDLE hrgn,int mode);
int          ExtTextOutW(HANDLE hdc,int x,int y,uint options,const RECT &lprect,const string str,uint c,const int &dx[]);
int          FillPath(HANDLE hdc);
int          FillRgn(HANDLE hdc,HANDLE hrgn,HANDLE hbr);
int          FixBrushOrgEx(HANDLE hdc,int x,int y,POINT &ptl);
int          FlattenPath(HANDLE hdc);
int          FloodFill(HANDLE hdc,int x,int y,uint clr);
int          FrameRgn(HANDLE hdc,HANDLE hrgn,HANDLE hbr,int w,int h);
int          GdiAlphaBlend(HANDLE dest,int dest,int dest,int dest,int dest,HANDLE src,int src,int src,int src,int src,BLENDFUNCTION &ftn);
int          GdiComment(HANDLE hdc,uint size,const uchar &data[]);
int          GdiFlush(void);
uint         GdiGetBatchLimit(void);
int          GdiGradientFill(HANDLE hdc,TRIVERTEX &vertex,uint vertex,PVOID mesh,uint count,uint mode);
uint         GdiSetBatchLimit(uint dw);
int          GdiTransparentBlt(HANDLE dest,int dest,int dest,int dest,int dest,HANDLE src,int src,int src,int src,int src,uint transparent);
int          GetArcDirection(HANDLE hdc);
int          GetAspectRatioFilterEx(HANDLE hdc,SIZE &lpsize);
int          GetBitmapBits(HANDLE hbit,int cb,PVOID bits);
int          GetBitmapDimensionEx(HANDLE hbit,SIZE &lpsize);
uint         GetBkColor(HANDLE hdc);
int          GetBkMode(HANDLE hdc);
uint         GetBoundsRect(HANDLE hdc,RECT &lprect,uint flags);
int          GetBrushOrgEx(HANDLE hdc,POINT &lppt);
int          GetCharABCWidthsFloatW(HANDLE hdc,uint first,uint last,ABCFLOAT &lpABC[]);
int          GetCharABCWidthsI(HANDLE hdc,uint first,uint cgi,ushort &pgi[],ABC &pabc[]);
int          GetCharABCWidthsW(HANDLE hdc,uint first,uint last,ABC &lpABC[]);
uint         GetCharacterPlacementW(HANDLE hdc,const string str,int count,int mex_extent,GCP_RESULTSW &results,uint flags);
int          GetCharWidthFloatW(HANDLE hdc,uint first,uint last,float &buffer[]);
int          GetCharWidthI(HANDLE hdc,uint first,uint cgi,ushort &pgi[],int &widths[]);
int          GetCharWidthW(HANDLE hdc,uint first,uint last,int &buffer[]);
int          GetClipBox(HANDLE hdc,RECT &lprect);
int          GetClipRgn(HANDLE hdc,HANDLE hrgn);
int          GetColorAdjustment(HANDLE hdc,COLORADJUSTMENT &lpca);
HANDLE       GetColorSpace(HANDLE hdc);
PVOID        GetCurrentObject(HANDLE hdc,uint type);
int          GetCurrentPositionEx(HANDLE hdc,POINT &lppt);
uint         GetDCBrushColor(HANDLE hdc);
int          GetDCOrgEx(HANDLE hdc,POINT &lppt);
uint         GetDCPenColor(HANDLE hdc);
int          GetDeviceCaps(HANDLE hdc,int index);
int          GetDeviceGammaRamp(HANDLE hdc,PVOID ramp);
int          GetDeviceGammaRamp(HANDLE hdc,ushort &ramp[]);
uint         GetDIBColorTable(HANDLE hdc,uint start,uint entries,RGBQUAD &prgbq[]);
int          GetDIBits(HANDLE hdc,HANDLE hbm,uint start,uint lines,PVOID bits,BITMAPINFO &lpbmi,uint usage);
uint         GetEnhMetaFileBits(HANDLE hEMF,uint size,uchar &data[]);
uint         GetEnhMetaFileDescriptionW(HANDLE hemf,uint buffer,string description);
uint         GetEnhMetaFileHeader(HANDLE hemf,uint size,ENHMETAHEADER &enh_meta_header);
uint         GetEnhMetaFilePaletteEntries(HANDLE hemf,uint num_entries,PALETTEENTRY &palette_entries);
uint         GetEnhMetaFilePixelFormat(HANDLE hemf,uint buffer,PIXELFORMATDESCRIPTOR &ppfd);
HANDLE       GetEnhMetaFileW(const string name);
uint         GetFontData(HANDLE hdc,uint table,uint offset,PVOID buffer,uint buffer);
uint         GetFontLanguageInfo(HANDLE hdc);
uint         GetFontUnicodeRanges(HANDLE hdc,PVOID lpgs);
uint         GetFontUnicodeRanges(HANDLE hdc,GLYPHSET &lpgs);
uint         GetGlyphIndicesW(HANDLE hdc,const string lpstr,int c,ushort &pgi[],uint fl);
uint         GetGlyphOutlineW(HANDLE hdc,uint symbol,uint format,GLYPHMETRICS &lpgm,uint buffer,PVOID buffer,MAT2 &lpmat2);
int          GetGraphicsMode(HANDLE hdc);
int          GetICMProfileW(HANDLE hdc,uint &buf_size,ushort &filename[]);
uint         GetKerningPairsW(HANDLE hdc,uint pairs,KERNINGPAIR &kern_pair);
uint         GetLayout(HANDLE hdc);
int          GetLogColorSpaceW(HANDLE color_space,LOGCOLORSPACEW &buffer,uint size);
int          GetMapMode(HANDLE hdc);
uint         GetMetaFileBitsEx(HANDLE hMF,uint buffer,PVOID data);
HANDLE       GetMetaFileW(const string name);
int          GetMetaRgn(HANDLE hdc,HANDLE hrgn);
int          GetMiterLimit(HANDLE hdc,float &plimit);
uint         GetNearestColor(HANDLE hdc,uint clr);
uint         GetNearestPaletteIndex(HANDLE h,uint clr);
uint         GetObjectType(PVOID h);
int          GetObjectW(HANDLE h,int c,PVOID pv);
uint         GetOutlineTextMetricsW(HANDLE hdc,uint copy,OUTLINETEXTMETRICW &potm);
uint         GetPaletteEntries(HANDLE hpal,uint start,uint entries,PALETTEENTRY &pal_entries);
int          GetPath(HANDLE hdc,POINT &apt,uchar &aj,int cpt);
uint         GetPixel(HANDLE hdc,int x,int y);
int          GetPixelFormat(HANDLE hdc);
int          GetPolyFillMode(HANDLE hdc);
int          GetRandomRgn(HANDLE hdc,HANDLE hrgn,int i);
int          GetRasterizerCaps(RASTERIZER_STATUS &lpraststat,uint bytes);
uint         GetRegionData(HANDLE hrgn,uint count,RGNDATA &rgn_data);
uint         GetRegionData(HANDLE hrgn,uint count,PVOID rgn_data);
int          GetRgnBox(HANDLE hrgn,RECT &lprc);
int          GetROP2(HANDLE hdc);
PVOID        GetStockObject(int i);
int          GetStretchBltMode(HANDLE hdc);
uint         GetSystemPaletteEntries(HANDLE hdc,uint start,uint entries,PALETTEENTRY &pal_entries);
uint         GetSystemPaletteUse(HANDLE hdc);
uint         GetTextAlign(HANDLE hdc);
int          GetTextCharacterExtra(HANDLE hdc);
int          GetTextCharset(HANDLE hdc);
int          GetTextCharsetInfo(HANDLE hdc,FONTSIGNATURE &sig,uint flags);
uint         GetTextColor(HANDLE hdc);
int          GetTextExtentExPointI(HANDLE hdc,ushort &str[],int str_size,int max_extent,int &fit,int &dx[],SIZE &size);
int          GetTextExtentExPointW(HANDLE hdc,const string str,int str,int max_extent,int &fit,int &dx[],SIZE &size);
int          GetTextExtentPoint32W(HANDLE hdc,const string str,int c,SIZE &psizl);
int          GetTextExtentPointI(HANDLE hdc,ushort &in[],int cgi,SIZE &psize);
int          GetTextExtentPointW(HANDLE hdc,const string str,int c,SIZE &lpsz);
int          GetTextFaceW(HANDLE hdc,int c,ushort &name[]);
int          GetTextMetricsW(HANDLE hdc,TEXTMETRICW &lptm);
int          GetViewportExtEx(HANDLE hdc,SIZE &lpsize);
int          GetViewportOrgEx(HANDLE hdc,POINT &lppoint);
int          GetWindowExtEx(HANDLE hdc,SIZE &lpsize);
int          GetWindowOrgEx(HANDLE hdc,POINT &lppoint);
uint         GetWinMetaFileBits(HANDLE hemf,uint data16,uchar &data16,int map_mode,HANDLE ref);
int          GetWorldTransform(HANDLE hdc,XFORM &lpxf);
int          IntersectClipRect(HANDLE hdc,int left,int top,int right,int bottom);
int          InvertRgn(HANDLE hdc,HANDLE hrgn);
int          LineTo(HANDLE hdc,int x,int y);
int          LPtoDP(HANDLE hdc,POINT &lppt[],int c);
int          MaskBlt(HANDLE dest,int dest,int dest,int width,int height,HANDLE src,int src,int src,HANDLE mask,int mask,int mask,uint rop);
int          ModifyWorldTransform(HANDLE hdc,XFORM &lpxf,uint mode);
int          MoveToEx(HANDLE hdc,int x,int y,POINT &lppt);
int          OffsetClipRgn(HANDLE hdc,int x,int y);
int          OffsetRgn(HANDLE hrgn,int x,int y);
int          OffsetViewportOrgEx(HANDLE hdc,int x,int y,POINT &lppt);
int          OffsetWindowOrgEx(HANDLE hdc,int x,int y,POINT &lppt);
int          PaintRgn(HANDLE hdc,HANDLE hrgn);
int          PatBlt(HANDLE hdc,int x,int y,int w,int h,uint rop);
HANDLE       PathToRegion(HANDLE hdc);
int          Pie(HANDLE hdc,int left,int top,int right,int bottom,int xr1,int yr1,int xr2,int yr2);
int          PlayEnhMetaFile(HANDLE hdc,HANDLE hmf,RECT &lprect);
int          PlayEnhMetaFileRecord(HANDLE hdc,HANDLETABLE &pht,ENHMETARECORD &pmr,uint cht);
int          PlayMetaFile(HANDLE hdc,HANDLE hmf);
int          PlayMetaFileRecord(HANDLE hdc,HANDLETABLE &handle_table,METARECORD &lpMR,uint objs);
int          PlgBlt(HANDLE dest,POINT &point,HANDLE src,int src,int src,int width,int height,HANDLE mask,int mask,int mask);
int          PolyBezier(HANDLE hdc,POINT &apt,uint cpt);
int          PolyBezierTo(HANDLE hdc,POINT &apt,uint cpt);
int          PolyDraw(HANDLE hdc,const POINT &apt,const uchar &aj[],int cpt);
int          Polygon(HANDLE hdc,const POINT &apt,int cpt);
int          Polyline(HANDLE hdc,const POINT &apt,int cpt);
int          PolylineTo(HANDLE hdc,const POINT &apt,uint cpt);
int          PolyPolygon(HANDLE hdc,const POINT &apt,int &asz[],int csz);
int          PolyPolyline(HANDLE hdc,const POINT &apt,uint &asz[],uint csz);
int          PolyTextOutW(HANDLE hdc,POLYTEXTW &ppt,int nstrings);
int          PtInRegion(HANDLE hrgn,int x,int y);
int          PtVisible(HANDLE hdc,int x,int y);
uint         RealizePalette(HANDLE hdc);
int          Rectangle(HANDLE hdc,int left,int top,int right,int bottom);
int          RectInRegion(HANDLE hrgn,RECT &lprect);
int          RectVisible(HANDLE hdc,RECT &lprect);
int          RemoveFontMemResourceEx(HANDLE h);
int          RemoveFontResourceExW(const string name,uint fl,PVOID pdv);
int          RemoveFontResourceW(const string file_name);
HANDLE       ResetDCW(HANDLE hdc,DEVMODEW &lpdm);
int          ResizePalette(HANDLE hpal,uint n);
int          RestoreDC(HANDLE hdc,int nSavedDC);
int          RoundRect(HANDLE hdc,int left,int top,int right,int bottom,int width,int height);
int          SaveDC(HANDLE hdc);
int          ScaleViewportExtEx(HANDLE hdc,int xn,int dx,int yn,int yd,SIZE &lpsz);
int          ScaleWindowExtEx(HANDLE hdc,int xn,int xd,int yn,int yd,SIZE &lpsz);
int          SelectClipPath(HANDLE hdc,int mode);
int          SelectClipRgn(HANDLE hdc,HANDLE hrgn);
PVOID        SelectObject(HANDLE hdc,PVOID h);
HANDLE       SelectPalette(HANDLE hdc,HANDLE pal,int force_bkgd);
int          SetAbortProc(HANDLE hdc,PVOID proc);
int          SetArcDirection(HANDLE hdc,int dir);
int          SetBitmapBits(HANDLE hbm,uint cb,const uchar &bits[]);
int          SetBitmapDimensionEx(HANDLE hbm,int w,int h,SIZE &lpsz);
uint         SetBkColor(HANDLE hdc,uint clr);
int          SetBkMode(HANDLE hdc,int mode);
uint         SetBoundsRect(HANDLE hdc,RECT &lprect,uint flags);
int          SetBrushOrgEx(HANDLE hdc,int x,int y,POINT &lppt);
int          SetColorAdjustment(HANDLE hdc,COLORADJUSTMENT &lpca);
HANDLE       SetColorSpace(HANDLE hdc,HANDLE hcs);
uint         SetDCBrushColor(HANDLE hdc,uint clr);
uint         SetDCPenColor(HANDLE hdc,uint clr);
int          SetDeviceGammaRamp(HANDLE hdc,PVOID ramp);
uint         SetDIBColorTable(HANDLE hdc,uint start,uint entries,RGBQUAD &prgbq);
int          SetDIBits(HANDLE hdc,HANDLE hbm,uint start,uint lines,PVOID bits,BITMAPINFO &lpbmi,uint ColorUse);
int          SetDIBitsToDevice(HANDLE hdc,int dest,int dest,uint w,uint h,int src,int src,uint StartScan,uint lines,PVOID bits,BITMAPINFO &lpbmi,uint ColorUse);
HANDLE       SetEnhMetaFileBits(uint size,const uchar &pb[]);
int          SetGraphicsMode(HANDLE hdc,int mode);
int          SetICMMode(HANDLE hdc,int mode);
int          SetICMProfileW(HANDLE hdc,string file_name);
uint         SetLayout(HANDLE hdc,uint l);
int          SetMapMode(HANDLE hdc,int mode);
uint         SetMapperFlags(HANDLE hdc,uint flags);
HANDLE       SetMetaFileBitsEx(uint buffer,const uchar &data[]);
int          SetMetaRgn(HANDLE hdc);
int          SetMiterLimit(HANDLE hdc,float limit,float &old);
uint         SetPaletteEntries(HANDLE hpal,uint start,uint entries,PALETTEENTRY &pal_entries);
uint         SetPixel(HANDLE hdc,int x,int y,uint clr);
int          SetPixelFormat(HANDLE hdc,int format,PIXELFORMATDESCRIPTOR &ppfd);
int          SetPixelV(HANDLE hdc,int x,int y,uint clr);
int          SetPolyFillMode(HANDLE hdc,int mode);
int          SetRectRgn(HANDLE hrgn,int left,int top,int right,int bottom);
int          SetROP2(HANDLE hdc,int rop2);
int          SetStretchBltMode(HANDLE hdc,int mode);
uint         SetSystemPaletteUse(HANDLE hdc,uint use);
uint         SetTextAlign(HANDLE hdc,uint align);
int          SetTextCharacterExtra(HANDLE hdc,int extra);
uint         SetTextColor(HANDLE hdc,uint clr);
int          SetTextJustification(HANDLE hdc,int extra,int count);
int          SetViewportExtEx(HANDLE hdc,int x,int y,SIZE &lpsz);
int          SetViewportOrgEx(HANDLE hdc,int x,int y,POINT &lppt);
int          SetWindowExtEx(HANDLE hdc,int x,int y,SIZE &lpsz);
int          SetWindowOrgEx(HANDLE hdc,int x,int y,POINT &lppt);
HANDLE       SetWinMetaFileBits(uint size,const uchar &lpMeta16Data[],HANDLE ref,const METAFILEPICT &lpMFP);
int          SetWorldTransform(HANDLE hdc,XFORM &lpxf);
int          StartDocW(HANDLE hdc,DOCINFOW &lpdi);
int          StartPage(HANDLE hdc);
int          StretchBlt(HANDLE dest,int dest,int dest,int dest,int dest,HANDLE src,int src,int src,int src,int src,uint rop);
int          StretchDIBits(HANDLE hdc,int dest,int dest,int DestWidth,int DestHeight,int src,int src,int SrcWidth,int SrcHeight,PVOID bits,BITMAPINFO &lpbmi,uint usage,uint rop);
int          StrokeAndFillPath(HANDLE hdc);
int          StrokePath(HANDLE hdc);
int          SwapBuffers(HANDLE);
int          TextOutW(HANDLE hdc,int x,int y,const string str,int c);
int          TranslateCharsetInfo(PVOID src,CHARSETINFO &cs,uint flags);
int          UnrealizeObject(PVOID h);
int          UpdateColors(HANDLE hdc);
int          UpdateICMRegKeyW(uint reserved,string lpszCMID,string file_name,uint command);
int          WidenPath(HANDLE hdc);
#import

#import "Opengl32.dll"
int          wglCopyContext(HANDLE,HANDLE,uint);
HANDLE       wglCreateContext(HANDLE);
HANDLE       wglCreateLayerContext(HANDLE,int);
int          wglDeleteContext(HANDLE);
int          wglDescribeLayerPlane(HANDLE,int,int,uint,LAYERPLANEDESCRIPTOR &);
HANDLE       wglGetCurrentContext(void);
HANDLE       wglGetCurrentDC(void);
int          wglGetLayerPaletteEntries(HANDLE,int,int,int,const uint &[]);
PVOID        wglGetProcAddress(string);
int          wglMakeCurrent(HANDLE,HANDLE);
int          wglRealizeLayerPalette(HANDLE,int,int);
int          wglSetLayerPaletteEntries(HANDLE,int,int,int,const uint &[]);
int          wglShareLists(HANDLE,HANDLE);
int          wglSwapLayerBuffers(HANDLE,uint);
uint         wglSwapMultipleBuffers(uint,WGLSWAP &);
int          wglUseFontBitmapsW(HANDLE,uint,uint,uint);
int          wglUseFontOutlinesW(HANDLE,uint,uint,uint,float,float,int,GLYPHMETRICSFLOAT &);
#import
//+------------------------------------------------------------------+