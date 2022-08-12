//+------------------------------------------------------------------+
//|                                                      Defines.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Enumerations                                                     |
//+------------------------------------------------------------------+
//--- properties flags
enum ENUM_WND_PROP_FLAGS
  {
   WND_PROP_FLAG_CAN_DBL_CLICK  = 1,                              // can be double clicked by mouse
   WND_PROP_FLAG_CAN_DRAG       = 2,                              // can be dragged by mouse
   WND_PROP_FLAG_CLICKS_BY_PRESS= 4,                              // generates the "click" event series on pressing left mouse button
   WND_PROP_FLAG_CAN_LOCK       = 8,                              // control with fixed state (usually it is a button)
   WND_PROP_FLAG_READ_ONLY      =16                               // read only (usually it is a edit)
  };
//--- state flags
enum ENUM_WND_STATE_FLAGS
  {
   WND_STATE_FLAG_ENABLE        = 1,                              // "object is enabled" flag
   WND_STATE_FLAG_VISIBLE       = 2,                              // "object is visible" flag
   WND_STATE_FLAG_ACTIVE        = 4,                              // "object is active" flag
  };
//--- mouse flags
enum ENUM_MOUSE_FLAGS
  {
   MOUSE_INVALID_FLAGS          =-1,                              // no buttons state
   MOUSE_EMPTY                  = 0,                              // buttons are not pressed
   MOUSE_LEFT                   = 1,                              // left button pressed
   MOUSE_RIGHT                  = 2                               // right button pressed
  };
//--- alignment flags
enum ENUM_WND_ALIGN_FLAGS
  {
   WND_ALIGN_NONE               = 0,                               // no alignment
   WND_ALIGN_LEFT               = 1,                               // align by left border
   WND_ALIGN_TOP                = 2,                               // align by top border
   WND_ALIGN_RIGHT              = 4,                               // align by right border
   WND_ALIGN_BOTTOM             = 8,                               // align by bottom border
   WND_ALIGN_WIDTH              = WND_ALIGN_LEFT|WND_ALIGN_RIGHT,  // justify
   WND_ALIGN_HEIGHT             = WND_ALIGN_TOP|WND_ALIGN_BOTTOM,  // align by top and bottom border
   WND_ALIGN_CLIENT             = WND_ALIGN_WIDTH|WND_ALIGN_HEIGHT // align by all sides
  };
//+------------------------------------------------------------------+
//| Drawing styles and colors                                        |
//+------------------------------------------------------------------+
//--- common
#define CONTROLS_FONT_NAME                  "Trebuchet MS"
#define CONTROLS_FONT_SIZE                  (10)
//--- Text
#define CONTROLS_COLOR_TEXT                 C'0x3B,0x29,0x28'
#define CONTROLS_COLOR_TEXT_SEL             White
#define CONTROLS_COLOR_BG                   White
#define CONTROLS_COLOR_BG_SEL               C'0x33,0x99,0xFF'
//--- Button
#define CONTROLS_BUTTON_COLOR               C'0x3B,0x29,0x28'
#define CONTROLS_BUTTON_COLOR_BG            C'0xDD,0xE2,0xEB'
#define CONTROLS_BUTTON_COLOR_BORDER        C'0xB2,0xC3,0xCF'
//--- Label
#define CONTROLS_LABEL_COLOR                C'0x3B,0x29,0x28'
//--- Edit
#define CONTROLS_EDIT_COLOR                 C'0x3B,0x29,0x28'
#define CONTROLS_EDIT_COLOR_BG              White
#define CONTROLS_EDIT_COLOR_BORDER          C'0xB2,0xC3,0xCF'
//--- Scrolls
#define CONTROLS_SCROLL_COLOR_BG            C'0xEC,0xEC,0xEC'
#define CONTROLS_SCROLL_COLOR_BORDER        C'0xD3,0xD3,0xD3'
//--- Client
#define CONTROLS_CLIENT_COLOR_BG            C'0xDE,0xDE,0xDE'
#define CONTROLS_CLIENT_COLOR_BORDER        C'0x2C,0x2C,0x2C'
//--- ListView
#define CONTROLS_LISTITEM_COLOR_TEXT        C'0x3B,0x29,0x28'
#define CONTROLS_LISTITEM_COLOR_TEXT_SEL    White
#define CONTROLS_LISTITEM_COLOR_BG          White
#define CONTROLS_LISTITEM_COLOR_BG_SEL      C'0x33,0x99,0xFF'
#define CONTROLS_LIST_COLOR_BG              White
#define CONTROLS_LIST_COLOR_BORDER          C'0xB2,0xC3,0xCF'
//--- CheckGroup
#define CONTROLS_CHECKGROUP_COLOR_BG        C'0xF7,0xF7,0xF7'
#define CONTROLS_CHECKGROUP_COLOR_BORDER    C'0xB2,0xC3,0xCF'
//--- RadioGroup
#define CONTROLS_RADIOGROUP_COLOR_BG        C'0xF7,0xF7,0xF7'
#define CONTROLS_RADIOGROUP_COLOR_BORDER    C'0xB2,0xC3,0xCF'
//--- Dialog
#define CONTROLS_DIALOG_COLOR_BORDER_LIGHT  White
#define CONTROLS_DIALOG_COLOR_BORDER_DARK   C'0xB6,0xB6,0xB6'
#define CONTROLS_DIALOG_COLOR_BG            C'0xF0,0xF0,0xF0'
#define CONTROLS_DIALOG_COLOR_CAPTION_TEXT  C'0x28,0x29,0x3B'
#define CONTROLS_DIALOG_COLOR_CLIENT_BG     C'0xF7,0xF7,0xF7'
#define CONTROLS_DIALOG_COLOR_CLIENT_BORDER C'0xC8,0xC8,0xC8'
//+------------------------------------------------------------------+
//| Constants for the controls                                       |
//+------------------------------------------------------------------+
//--- common
#define CONTROLS_INVALID_ID                 (-1)     // invalid ID
#define CONTROLS_INVALID_INDEX              (-1)     // invalid index of array
#define CONTROLS_SELF_MESSAGE               (-1)     // message to oneself
#define CONTROLS_MAXIMUM_ID                 (10000)  // maximum number of IDs in application
#define CONTROLS_BORDER_WIDTH               (1)      // border width
#define CONTROLS_SUBWINDOW_GAP              (3)      // gap between sub-windows along the Y axis
#define CONTROLS_DRAG_SPACING               (50)     // sensitivity threshold for dragging
#define CONTROLS_DBL_CLICK_TIME             (100)    // double click interval
//--- BmpButton
#define CONTROLS_BUTTON_SIZE                (16)     // default size of button (16 x 16)
//--- Scrolls
#define CONTROLS_SCROLL_SIZE                (18)     // default lateral size of scrollbar
#define CONTROLS_SCROLL_THUMB_SIZE          (22)     // default length of scroll box
//--- RadioButton
#define CONTROLS_RADIO_BUTTON_X_OFF         (3)      // X offset of radio button (for RadioButton)
#define CONTROLS_RADIO_BUTTON_Y_OFF         (3)      // Y offset of radio button (for RadioButton)
#define CONTROLS_RADIO_LABEL_X_OFF          (20)     // X offset of label (for RadioButton)
#define CONTROLS_RADIO_LABEL_Y_OFF          (0)      // Y offset of label (for RadioButton)
//--- CheckBox
#define CONTROLS_CHECK_BUTTON_X_OFF         (3)      // X offset of check button (for CheckBox)
#define CONTROLS_CHECK_BUTTON_Y_OFF         (3)      // Y offset of check button (for CheckBox)
#define CONTROLS_CHECK_LABEL_X_OFF          (20)     // X offset of label (for CheckBox)
#define CONTROLS_CHECK_LABEL_Y_OFF          (0)      // Y offset of label (for CheckBox)
//--- Spin
#define CONTROLS_SPIN_BUTTON_X_OFF          (2)      // X offset of button from right (for SpinEdit)
#define CONTROLS_SPIN_MIN_HEIGHT            (18)     // minimal height (for SpinEdit)
#define CONTROLS_SPIN_BUTTON_SIZE           (8)      // default size of button (16 x 8) (for SpinEdit)
//--- Combo
#define CONTROLS_COMBO_BUTTON_X_OFF         (2)      // X offset of button from right (for ComboBox)
#define CONTROLS_COMBO_MIN_HEIGHT           (18)     // minimal height (for ComboBox)
#define CONTROLS_COMBO_ITEM_HEIGHT          (18)     // height of combo box item (for ComboBox)
#define CONTROLS_COMBO_ITEMS_VIEW           (8)      // number of items in combo box (for ComboBox)
//--- ListView
#define CONTROLS_LIST_ITEM_HEIGHT           (18)     // height of list item (for ListView)
//--- Dialog
#define CONTROLS_DIALOG_CAPTION_HEIGHT      (22)     // height of dialog header
#define CONTROLS_DIALOG_BUTTON_OFF          (3)      // offset of dialog buttons
#define CONTROLS_DIALOG_CLIENT_OFF          (2)      // offset of dialog client area
#define CONTROLS_DIALOG_MINIMIZE_LEFT       (10)     // left coordinate of dialog in minimized state
#define CONTROLS_DIALOG_MINIMIZE_TOP        (10)     // top coordinate of dialog in minimized state
#define CONTROLS_DIALOG_MINIMIZE_WIDTH      (100)    // width of dialog in minimized state
#define CONTROLS_DIALOG_MINIMIZE_HEIGHT     (4*CONTROLS_BORDER_WIDTH+CONTROLS_DIALOG_CAPTION_HEIGHT) // height of dialog in minimized state
//+------------------------------------------------------------------+
//| Macro                                                            |
//+------------------------------------------------------------------+
//--- check properties
#define IS_CAN_DBL_CLICK     ((m_prop_flags&WND_PROP_FLAG_CAN_DBL_CLICK)!=0)
#define IS_CAN_DRAG          ((m_prop_flags&WND_PROP_FLAG_CAN_DRAG)!=0)
#define IS_CLICKS_BY_PRESS   ((m_prop_flags&WND_PROP_FLAG_CLICKS_BY_PRESS)!=0)
#define IS_CAN_LOCK          ((m_prop_flags&WND_PROP_FLAG_CAN_LOCK)!=0)
#define IS_READ_ONLY         ((m_prop_flags&WND_PROP_FLAG_READ_ONLY)!=0)
//--- check state
#define IS_ENABLED           ((m_state_flags&WND_STATE_FLAG_ENABLE)!=0)
#define IS_VISIBLE           ((m_state_flags&WND_STATE_FLAG_VISIBLE)!=0)
#define IS_ACTIVE            ((m_state_flags&WND_STATE_FLAG_ACTIVE)!=0)
//+------------------------------------------------------------------+
//| Macro of event handling map                                      |
//+------------------------------------------------------------------+
#define INTERNAL_EVENT                           (-1)
//--- beginning of map
#define EVENT_MAP_BEGIN(class_name)              bool class_name::OnEvent(const int id,const long& lparam,const double& dparam,const string& sparam) {
//--- end of map
#define EVENT_MAP_END(parent_class_name)         return(parent_class_name::OnEvent(id,lparam,dparam,sparam)); }
//--- event handling by numeric ID
#define ON_EVENT(event,control,handler)          if(id==(event+CHARTEVENT_CUSTOM) && lparam==control.Id()) { handler(); return(true); }
//--- event handling by numeric ID by pointer of control
#define ON_EVENT_PTR(event,control,handler)      if(control!=NULL && id==(event+CHARTEVENT_CUSTOM) && lparam==control.Id()) { handler(); return(true); }
//--- event handling without ID analysis
#define ON_NO_ID_EVENT(event,handler)            if(id==(event+CHARTEVENT_CUSTOM)) { return(handler()); }
//--- event handling by row ID
#define ON_NAMED_EVENT(event,control,handler)    if(id==(event+CHARTEVENT_CUSTOM) && sparam==control.Name()) { handler(); return(true); }
//--- handling of indexed event
#define ON_INDEXED_EVENT(event,controls,handler) { int total=ArraySize(controls); for(int i=0;i<total;i++) if(id==(event+CHARTEVENT_CUSTOM) && lparam==controls[i].Id()) return(handler(i)); }
//--- handling of external event
#define ON_EXTERNAL_EVENT(event,handler)         if(id==(event+CHARTEVENT_CUSTOM)) { handler(lparam,dparam,sparam); return(true); }
//+------------------------------------------------------------------+
//| Events                                                           |
//+------------------------------------------------------------------+
#define ON_CLICK                (0)   // clicking on control event
#define ON_DBL_CLICK            (1)   // double clicking on control event
#define ON_SHOW                 (2)   // showing control event
#define ON_HIDE                 (3)   // hiding control event
#define ON_CHANGE               (4)   // changing control event
#define ON_START_EDIT           (5)   // start of editing event
#define ON_END_EDIT             (6)   // end of editing event
#define ON_SCROLL_INC           (7)   // increment of scrollbar event
#define ON_SCROLL_DEC           (8)   // decrement of scrollbar event
#define ON_MOUSE_FOCUS_SET      (9)   // the "mouse cursor entered the control" event
#define ON_MOUSE_FOCUS_KILL     (10)  // the "mouse cursor exited the control" event
#define ON_DRAG_START           (11)  // the "control dragging start" event
#define ON_DRAG_PROCESS         (12)  // the "control is being dragged" event
#define ON_DRAG_END             (13)  // the "control dragging end" event
#define ON_BRING_TO_TOP         (14)  // the "mouse events priority increase" event
#define ON_APP_CLOSE            (100) // "closing the application" event
//+------------------------------------------------------------------+
