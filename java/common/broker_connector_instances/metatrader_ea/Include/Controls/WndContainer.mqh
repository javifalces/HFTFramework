//+------------------------------------------------------------------+
//|                                                 WndContainer.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "Wnd.mqh"
#include <Arrays\ArrayObj.mqh>
//+------------------------------------------------------------------+
//| Class CWndContainer                                              |
//| Usage: base class of the combined control                        |
//+------------------------------------------------------------------+
class CWndContainer : public CWnd
  {
private:
   CArrayObj         m_controls;            // container of the control

public:
                     CWndContainer(void);
                    ~CWndContainer(void);
   //--- release memory
   virtual void      Destroy(const int reason=0);
   //--- chart event handler
   virtual bool      OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam);
   virtual bool      OnMouseEvent(const int x,const int y,const int flags);
   //--- access the contents of container
   int               ControlsTotal(void)               const { return(m_controls.Total()); }
   CWnd*             Control(const int ind)            const { return(dynamic_cast<CWnd *>(m_controls.At(ind))); }
   virtual CWnd*     ControlFind(const long id);
   //--- for mouse cursor focus
   virtual bool      MouseFocusKill(const long id=-1);
   //--- fill
   bool              Add(CWnd *control);
   bool              Add(CWnd &control);
   //--- underflowing
   bool              Delete(CWnd *control);
   bool              Delete(CWnd &control);
   //--- geometry
   virtual bool      Move(const int x,const int y);
   virtual bool      Move(const CPoint &point);
   virtual bool      Shift(const int dx,const int dy);
   //--- ID
   virtual long      Id(const long id);
   long              Id(void) const { return(CWnd::Id()); }
   //--- state
   virtual bool      Enable(void);
   virtual bool      Disable(void);
   virtual bool      Show(void);
   virtual bool      Hide(void);
   //--- methods for working with files
   virtual bool      Save(const int file_handle);
   virtual bool      Load(const int file_handle);

protected:
   //--- internal event handlers
   virtual bool      OnResize(void);
   virtual bool      OnActivate(void);
   virtual bool      OnDeactivate(void);
  };
//+------------------------------------------------------------------+
//| Common handler of chart events                                   |
//+------------------------------------------------------------------+
bool CWndContainer::OnEvent(const int id,const long &lparam,const double &dparam,const string &sparam)
  {
//--- if an object is being dragged, pass control to the special drag object
   if(m_drag_object!=NULL && m_drag_object.OnEvent(id,lparam,dparam,sparam))
      return(true);
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=total-1;i>=0;i--)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      if(control.OnEvent(id,lparam,dparam,sparam))
         return(true);
     }
//--- not handled
   return(false);
  }
//+------------------------------------------------------------------+
//| Common handler of mouse events                                   |
//+------------------------------------------------------------------+
bool CWndContainer::OnMouseEvent(const int x,const int y,const int flags)
  {
   if(!IS_VISIBLE)
      return(false);
//--- if an object is being dragged, pass control to the special drag object
   if(m_drag_object!=NULL && m_drag_object.OnMouseEvent(x,y,flags))
      return(true);
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=total-1;i>=0;i--)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      if(control.OnMouseEvent(x,y,flags))
         return(true);
     }
//--- call of the method of the parent class
   return(CWnd::OnMouseEvent(x,y,flags));
  }
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CWndContainer::CWndContainer(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CWndContainer::~CWndContainer(void)
  {
  }
//+------------------------------------------------------------------+
//| Delete group of controls                                         |
//+------------------------------------------------------------------+
void CWndContainer::Destroy(const int reason)
  {
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(0);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Destroy();
      m_controls.Delete(0);
     }
  }
//+------------------------------------------------------------------+
//| Find control by specified ID                                     |
//+------------------------------------------------------------------+
CWnd* CWndContainer::ControlFind(const long id)
  {
   CWnd *result=CWnd::ControlFind(id);
//---
   if(result!=NULL)
      return(result);
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      result=control.ControlFind(id);
      if(result!=NULL)
         break;
     }
//--- return the result
   return(result);
  }
//+------------------------------------------------------------------+
//| Remove the mouse focus from control                              |
//+------------------------------------------------------------------+
bool CWndContainer::MouseFocusKill(const long id=-1)
  {
   if(!IS_ACTIVE)
      return(false);
   Deactivate();
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.MouseFocusKill();
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Add control to the group (by pointer)                            |
//+------------------------------------------------------------------+
bool CWndContainer::Add(CWnd *control)
  {
//--- check of pointer
   if(control==NULL)
      return(false);
//--- correct the coordinates of added control
   control.Shift(Left(),Top());
 //--- "projecting" the group flag "visibility" to the added element
   if(IS_VISIBLE && control.IsVisible())
     {
      //--- element will be "visible" only if the group is "visible" and the element is completely "within" this group
      control.Visible(Contains(control));
     }
   else
      control.Hide();
//--- "projecting" the group flag "enabled" to the added element
   if(IS_ENABLED)
      control.Enable();
   else
      control.Disable();
//--- adding
   return(m_controls.Add(control));
  }
//+------------------------------------------------------------------+
//| Add control to the group (by reference)                          |
//+------------------------------------------------------------------+
bool CWndContainer::Add(CWnd &control)
  {
//--- add by pointer
   return(Add((CWnd*)GetPointer(control)));
  }
//+------------------------------------------------------------------+
//| Delete control from the group (by pointer)                       |
//+------------------------------------------------------------------+
bool CWndContainer::Delete(CWnd *control)
  {
//--- check of pointer
   if(control==NULL)
      return(false);
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *pointer=Control(i);
      //--- check of pointer
      if(pointer==NULL)
         continue;
      //--- delete item from group
      if(pointer==control)
         return(m_controls.Delete(i));
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Delete control from the group (by reference)                     |
//+------------------------------------------------------------------+
bool CWndContainer::Delete(CWnd &control)
  {
//--- delete by pointer
   return(Delete((CWnd*)GetPointer(control)));
  }
//+------------------------------------------------------------------+
//| Absolute movement of the controls group                          |
//+------------------------------------------------------------------+
bool CWndContainer::Move(const int x,const int y)
  {
//--- relative movement
   return(Shift(x-Left(),y-Top()));
  }
//+------------------------------------------------------------------+
//| Absolute movement of the controls group                          |
//+------------------------------------------------------------------+
bool CWndContainer::Move(const CPoint &point)
  {
//--- relative movement
   return(Shift(point.x-Left(),point.y-Top()));
  }
//+------------------------------------------------------------------+
//| Relative movement of the controls group                          |
//+------------------------------------------------------------------+
bool CWndContainer::Shift(const int dx,const int dy)
  {
//--- call of the method of the parent class
   if(!CWnd::Shift(dx,dy)) return(false);
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      //--- move the group item
      control.Shift(dx,dy);
     }
//--- succeed
   return(true);
  }
//+------------------------------------------------------------------+
//| Set ID of control                                                |
//+------------------------------------------------------------------+
long CWndContainer::Id(const long id)
  {
//--- reserve ID for container
   long id_used=1;
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      id_used+=control.Id(id+id_used);
     }
   m_id=id;
//--- return number of used IDs
   return(id_used);
  }
//+------------------------------------------------------------------+
//| Enables event handling by the group of controls                  |
//+------------------------------------------------------------------+
bool CWndContainer::Enable(void)
  {
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Enable();
     }
//--- call of the method of the parent class
   return(CWnd::Enable());
  }
//+------------------------------------------------------------------+
//| Disables event handling by the group of controls                 |
//+------------------------------------------------------------------+
bool CWndContainer::Disable(void)
  {
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Disable();
     }
//--- call of the method of the parent class
   return(CWnd::Disable());
  }
//+------------------------------------------------------------------+
//| Makes the group of controls visible                              |
//+------------------------------------------------------------------+
bool CWndContainer::Show(void)
  {
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Show();
     }
//--- call of the method of the parent class
   return(CWnd::Show());
  }
//+------------------------------------------------------------------+
//| Makes the group of controls hidden                               |
//+------------------------------------------------------------------+
bool CWndContainer::Hide(void)
  {
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Hide();
     }
//--- call of the method of the parent class
   return(CWnd::Hide());
  }
//+------------------------------------------------------------------+
//| Handler of resizing                                              |
//+------------------------------------------------------------------+
bool CWndContainer::OnResize()
  {
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      if(!control.Align(Rect()))
         return(false);
     }
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of activating the group of controls                      |
//+------------------------------------------------------------------+
bool CWndContainer::OnActivate(void)
  {
   if(IS_ACTIVE)
      return(false);
   Activate();
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Activate();
     }
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Handler of deactivating the group of controls                    |
//+------------------------------------------------------------------+
bool CWndContainer::OnDeactivate(void)
  {
   if(!IS_ACTIVE)
      return(false);
   Deactivate();
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      control.Deactivate();
     }
//--- handled
   return(true);
  }
//+------------------------------------------------------------------+
//| Save                                                             |
//+------------------------------------------------------------------+
bool CWndContainer::Save(const int file_handle)
  {
   bool result=true;
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      result&=control.Save(file_handle);
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
//| Load                                                             |
//+------------------------------------------------------------------+
bool CWndContainer::Load(const int file_handle)
  {
   bool result=true;
//--- loop by elements of group
   int total=m_controls.Total();
   for(int i=0;i<total;i++)
     {
      CWnd *control=Control(i);
      //--- check of pointer
      if(control==NULL)
         continue;
      result&=control.Load(file_handle);
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
