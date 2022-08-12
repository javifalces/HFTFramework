//+------------------------------------------------------------------+
//|                                               ColorGenerator.mqh |
//|                   Copyright 2016-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Class CColorGenerator                                            |
//| Usage: class to generate the default colors                      |
//+------------------------------------------------------------------+
class CColorGenerator
  {
private:
   int               m_index;
   bool              m_generate;
   uint              m_current_palette[20];
   const static uint s_default_palette[20];

public:
                     CColorGenerator(void);
                    ~CColorGenerator(void);
   //--- gets the next color                    
   uint              Next(void);
   //--- reset generator
   void              Reset(void);
  };
const uint CColorGenerator::s_default_palette[20]=
  {
   0x3366CC,0xDC3912,0xFF9900,0x109618,0x990099,
   0x3B3EAC,0x0099C6,0xDD4477,0x66AA00,0xB82E2E,
   0x316395,0x994499,0x22AA99,0xAAAA11,0x6633CC,
   0xE67300,0x8B0707,0x329262,0x5574A6,0x3B3EAC
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CColorGenerator::CColorGenerator(void): m_index(0),
                                        m_generate(false)
  {
   ArrayCopy(m_current_palette,s_default_palette);
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CColorGenerator::~CColorGenerator(void)
  {
  }
//+------------------------------------------------------------------+
//| Gets or generates the following color from the palette           |
//+------------------------------------------------------------------+
uint CColorGenerator::Next(void)
  {
//--- check the array out of range
   if(m_index==20)
     {
      m_index=0;
      if(!m_generate)
         m_generate=true;
     }
//--- check the default palette is over
   if(m_generate)
      m_current_palette[m_index]=(m_index==19 ? (m_current_palette[m_index]^m_current_palette[0]):(m_current_palette[m_index]^m_current_palette[m_index+1]));
//--- return next color
   return(m_current_palette[m_index++]);
  }
//+------------------------------------------------------------------+
//| Resets all the new colors, set the index to 0                    |
//+------------------------------------------------------------------+
void CColorGenerator::Reset(void)
  {
   m_index=0;
   m_generate=false;
   ArrayCopy(m_current_palette,s_default_palette);
  }
//+------------------------------------------------------------------+
