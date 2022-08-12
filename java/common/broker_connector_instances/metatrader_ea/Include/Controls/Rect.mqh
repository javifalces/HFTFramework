//+------------------------------------------------------------------+
//|                                                         Rect.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Structure CPoint                                                 |
//| Usage: point of chart in Cartesian coordinates                   |
//+------------------------------------------------------------------+
struct CPoint
  {
   int               x;                   // horizontal coordinate
   int               y;                   // vertical coordinate
  };
//+------------------------------------------------------------------+
//| Structure CSize                                                  |
//| Usage: size of area of chart in Cartesian coordinates            |
//+------------------------------------------------------------------+
struct CSize
  {
   int               cx;                  // horizontal size
   int               cy;                  // vertical size
  };
//+------------------------------------------------------------------+
//| Structure CRect                                                  |
//| Usage: area of chart in Cartesian coordinates                    |
//+------------------------------------------------------------------+
struct CRect
  {
   int               left;                // left coordinate
   int               top;                 // top coordinate
   int               right;               // right coordinate
   int               bottom;              // bottom coordinate

   //--- methods
   CPoint            LeftTop(void)            const;
   void              LeftTop(const int x,const int y);
   void              LeftTop(const CPoint& point);
   CPoint            RightBottom(void)        const;
   void              RightBottom(const int x,const int y);
   void              RightBottom(const CPoint& point);
   CPoint            CenterPoint(void) const;
   int               Width(void)              const { return(right-left); }
   void              Width(const int w)             { right=left+w;       }
   int               Height(void)             const { return(bottom-top); }
   void              Height(const int h)            { bottom=top+h;       }
   CSize             Size(void)               const;
   void              Size(const int cx,const int cy);
   void              Size(const CSize& size);
   void              SetBound(const int l,const int t,const int r,const int b);
   void              SetBound(const CRect& rect);
   void              SetBound(const CPoint& point,const CSize& size);
   void              SetBound(const CPoint& left_top,const CPoint& right_bottom);
   void              Move(const int x,const int y);
   void              Move(const CPoint& point);
   void              Shift(const int dx,const int dy);
   void              Shift(const CPoint& point);
   void              Shift(const CSize& size);
   bool              Contains(const int x,const int y) const;
   bool              Contains(const CPoint& point) const;
   void              Normalize(void);
  };
//+------------------------------------------------------------------+
//| Get parameters of area                                           |
//+------------------------------------------------------------------+
CPoint CRect::LeftTop(void) const
  {
   CPoint point;
//--- action
   point.x=left;
   point.y=top;
//--- result
   return(point);
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::LeftTop(const int x,const int y)
  {
   left=x;
   top =y;
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::LeftTop(const CPoint& point)
  {
   left=point.x;
   top =point.y;
  }
//+------------------------------------------------------------------+
//| Get parameters of area                                           |
//+------------------------------------------------------------------+
CPoint CRect::RightBottom(void) const
  {
   CPoint point;
//--- action
   point.x=right;
   point.y=bottom;
//--- result
   return(point);
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::RightBottom(const int x,const int y)
  {
   right =x;
   bottom=y;
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::RightBottom(const CPoint& point)
  {
   right =point.x;
   bottom=point.y;
  }
//+------------------------------------------------------------------+
//| Get parameters of area                                           |
//+------------------------------------------------------------------+
CPoint CRect::CenterPoint(void) const
  {
   CPoint point;
//--- action
   point.x=left+Width()/2;
   point.y=top+Height()/2;
//--- result
   return(point);
  }
//+------------------------------------------------------------------+
//| Get parameters of area                                           |
//+------------------------------------------------------------------+
CSize CRect::Size(void) const
  {
   CSize size;
//--- action
   size.cx=right-left;
   size.cy=bottom-top;
//--- result
   return(size);
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::Size(const int cx,const int cy)
  {
   right =left+cx;
   bottom=top+cy;
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::Size(const CSize& size)
  {
   right =left+size.cx;
   bottom=top+size.cy;
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::SetBound(const int l,const int t,const int r,const int b)
  {
   left  =l;
   top   =t;
   right =r;
   bottom=b;
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::SetBound(const CRect& rect)
  {
   left  =rect.left;
   top   =rect.top;
   right =rect.right;
   bottom=rect.bottom;
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::SetBound(const CPoint& point,const CSize& size)
  {
   LeftTop(point);
   Size(size);
  }
//+------------------------------------------------------------------+
//| Set parameters of area                                           |
//+------------------------------------------------------------------+
void CRect::SetBound(const CPoint& left_top,const CPoint& right_bottom)
  {
   LeftTop(left_top);
   RightBottom(right_bottom);
  }
//+------------------------------------------------------------------+
//| Absolute movement of area                                        |
//+------------------------------------------------------------------+
void CRect::Move(const int x,const int y)
  {
   right +=x-left;
   bottom+=y-top;
   left   =x;
   top    =y;
  }
//+------------------------------------------------------------------+
//| Absolute movement of area                                        |
//+------------------------------------------------------------------+
void CRect::Move(const CPoint& point)
  {
   right +=point.x-left;
   bottom+=point.y-top;
   left   =point.x;
   top    =point.y;
  }
//+------------------------------------------------------------------+
//| Relative movement of area                                        |
//+------------------------------------------------------------------+
void CRect::Shift(const int dx,const int dy)
  {
   left  +=dx;
   top   +=dy;
   right +=dx;
   bottom+=dy;
  }
//+------------------------------------------------------------------+
//| Relative movement of area                                        |
//+------------------------------------------------------------------+
void CRect::Shift(const CPoint& point)
  {
   left  +=point.x;
   top   +=point.y;
   right +=point.x;
   bottom+=point.y;
  }
//+------------------------------------------------------------------+
//| Relative movement of area                                        |
//+------------------------------------------------------------------+
void CRect::Shift(const CSize& size)
  {
   left  +=size.cx;
   top   +=size.cy;
   right +=size.cx;
   bottom+=size.cy;
  }
//+------------------------------------------------------------------+
//| Check if a point is within the area                              |
//+------------------------------------------------------------------+
bool CRect::Contains(const int x,const int y) const
  {
//--- check and return the result
   return(x>=left && x<=right && y>=top && y<=bottom);
  }
//+------------------------------------------------------------------+
//| Check if a point is within the area                              |
//+------------------------------------------------------------------+
bool CRect::Contains(const CPoint& point) const
  {
//--- check and return the result
   return(point.x>=left && point.x<=right && point.y>=top && point.y<=bottom);
  }
//+------------------------------------------------------------------+
//| Standardizes the height and width                                |
//+------------------------------------------------------------------+
void CRect::Normalize(void)
  {
   if(left>right)
     {
      int tmp1=left;
      left=right;
      right=tmp1;
     }
   if(top>bottom)
     {
      int tmp2=top;
      top=bottom;
      bottom=tmp2;
     }
  }
//+------------------------------------------------------------------+
