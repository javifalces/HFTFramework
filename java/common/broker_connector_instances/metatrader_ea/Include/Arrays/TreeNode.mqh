//+------------------------------------------------------------------+
//|                                                     TreeNode.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include <Object.mqh>
//+------------------------------------------------------------------+
//| Class CTreeNode.                                                 |
//| Purpose: Base class of node of binary tree CTree.                |
//|          Derives from class CObject.                             |
//+------------------------------------------------------------------+
class CTreeNode : public CObject
  {
private:
   CTreeNode        *m_p_node;             // link to node up
   CTreeNode        *m_l_node;             // link to node left
   CTreeNode        *m_r_node;             // link to node right
   //--- variables
   int               m_balance;            // balance of node
   int               m_l_balance;          // balance of the left branch
   int               m_r_balance;          // balance of the right branch

public:
                     CTreeNode(void);
                    ~CTreeNode(void);
   //--- methods of access to protected data
   CTreeNode*        Parent(void)           const { return(m_p_node);    }
   void              Parent(CTreeNode *node)      { m_p_node=node;       }
   CTreeNode*        Left(void)             const { return(m_l_node);    }
   void              Left(CTreeNode *node)        { m_l_node=node;       }
   CTreeNode*        Right(void)            const { return(m_r_node);    }
   void              Right(CTreeNode *node)       { m_r_node=node;       }
   int               Balance(void)          const { return(m_balance);   }
   int               BalanceL(void)         const { return(m_l_balance); }
   int               BalanceR(void)         const { return(m_r_balance); }
   //--- method of identifying the object
   virtual int       Type(void) const { return(0x8888); }
   //--- methods for controlling
   int               RefreshBalance(void);
   CTreeNode        *GetNext(const CTreeNode *node);
   //--- methods for working with files
   bool              SaveNode(const int file_handle);
   bool              LoadNode(const int file_handle,CTreeNode *main);

protected:
   //--- method for creating an instance of class
   virtual CTreeNode *CreateSample(void) { return(NULL); }
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CTreeNode::CTreeNode(void) : m_p_node(NULL),
                             m_l_node(NULL),
                             m_r_node(NULL),
                             m_balance(0),
                             m_l_balance(0),
                             m_r_balance(0)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CTreeNode::~CTreeNode(void)
  {
//--- delete nodes of the next level
   if(m_l_node!=NULL)
      delete m_l_node;
   if(m_r_node!=NULL)
      delete m_r_node;
  }
//+------------------------------------------------------------------+
//| Calculating the balance of the node                              |
//+------------------------------------------------------------------+
int CTreeNode::RefreshBalance(void)
  {
//--- calculate the balance of the left branch
   if(m_l_node==NULL)
      m_l_balance=0;
   else
      m_l_balance=m_l_node.RefreshBalance();
//--- calculate the balance of the right branch
   if(m_r_node==NULL)
      m_r_balance=0;
   else
      m_r_balance=m_r_node.RefreshBalance();
//--- calculate the balance of the node
   if(m_r_balance>m_l_balance)
      m_balance=m_r_balance+1;
   else
      m_balance=m_l_balance+1;
//--- result
   return(m_balance);
  }
//+------------------------------------------------------------------+
//| Selecting next node                                              |
//+------------------------------------------------------------------+
CTreeNode *CTreeNode::GetNext(const CTreeNode *node)
  {
   if(Compare(node)>0)
      return(m_l_node);
//--- result
   return(m_r_node);
  }
//+------------------------------------------------------------------+
//| Writing node data to file                                        |
//+------------------------------------------------------------------+
bool CTreeNode::SaveNode(const int file_handle)
  {
   bool result=true;
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//--- write left node (if it is available)
   if(m_l_node!=NULL)
     {
      FileWriteInteger(file_handle,'L',SHORT_VALUE);
      result&=m_l_node.SaveNode(file_handle);
     }
   else
      FileWriteInteger(file_handle,'X',SHORT_VALUE);
//--- write data of current node
   result&=Save(file_handle);
//--- write right node (if it is available)
   if(m_r_node!=NULL)
     {
      FileWriteInteger(file_handle,'R',SHORT_VALUE);
      result&=m_r_node.SaveNode(file_handle);
     }
   else
      FileWriteInteger(file_handle,'X',SHORT_VALUE);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| Reading node data from file                                      |
//+------------------------------------------------------------------+
bool CTreeNode::LoadNode(const int file_handle,CTreeNode *main)
  {
   bool       result=true;
   short      s_val;
   CTreeNode *node;
//--- check
   if(file_handle==INVALID_HANDLE)
      return(false);
//--- read directions
   s_val=(short)FileReadInteger(file_handle,SHORT_VALUE);
   if(s_val=='L')
     {
      //--- read left node (if there is data)
      node=CreateSample();
      if(node==NULL)
         return(false);
      m_l_node=node;
      node.Parent(main);
      result&=node.LoadNode(file_handle,node);
     }
//--- read data of current node
   result&=Load(file_handle);
//--- read directions
   s_val=(short)FileReadInteger(file_handle,SHORT_VALUE);
   if(s_val=='R')
     {
      //--- read right node (if there is data)
      node=CreateSample();
      if(node==NULL)
         return(false);
      m_r_node=node;
      node.Parent(main);
      result&=node.LoadNode(file_handle,node);
     }
//--- result
   return(result);
  }
//+------------------------------------------------------------------+
