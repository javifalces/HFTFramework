//+------------------------------------------------------------------+
//|                                               TestInterfaces.mqh |
//|            Copyright 2003-2012 Sergey Bochkanov (ALGLIB project) |
//|                   Copyright 2012-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//| Implementation of ALGLIB library in MetaQuotes Language 5        |
//|                                                                  |
//| The features of the library include:                             |
//| - Linear algebra (direct algorithms, EVD, SVD)                   |
//| - Solving systems of linear and non-linear equations             |
//| - Interpolation                                                  |
//| - Optimization                                                   |
//| - FFT (Fast Fourier Transform)                                   |
//| - Numerical integration                                          |
//| - Linear and nonlinear least-squares fitting                     |
//| - Ordinary differential equations                                |
//| - Computation of special functions                               |
//| - Descriptive statistics and hypothesis testing                  |
//| - Data analysis - classification, regression                     |
//| - Implementing linear algebra algorithms, interpolation, etc.    |
//|   in high-precision arithmetic (using MPFR)                      |
//|                                                                  |
//| This file is free software; you can redistribute it and/or       |
//| modify it under the terms of the GNU General Public License as   |
//| published by the Free Software Foundation (www.fsf.org); either  |
//| version 2 of the License, or (at your option) any later version. |
//|                                                                  |
//| This program is distributed in the hope that it will be useful,  |
//| but WITHOUT ANY WARRANTY; without even the implied warranty of   |
//| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the     |
//| GNU General Public License for more details.                     |
//+------------------------------------------------------------------+
#include <Math\Alglib\alglib.mqh>

//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Func                            |
//+------------------------------------------------------------------+
class CNDimensional_Func1 : public CNDimensional_Func
  {
public:
                     CNDimensional_Func1(void);
                    ~CNDimensional_Func1(void);

   virtual void      Func(double &x[],double &func,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Func1::CNDimensional_Func1(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Func1::~CNDimensional_Func1(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x0,x1)=100*(x0+3)^4 + (x1-3)^4        |
//+------------------------------------------------------------------+
void CNDimensional_Func1::Func(double &x[],double &func,CObject &obj)
  {
   func=100*MathPow(x[0]+3,4)+MathPow(x[1]-3,4);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Func                            |
//+------------------------------------------------------------------+
class CNDimensional_Func2 : public CNDimensional_Func
  {
public:
   //--- constructor, destructor
                     CNDimensional_Func2(void);
                    ~CNDimensional_Func2(void);
   //--- method
   virtual void      Func(double &x[],double &func,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Func2::CNDimensional_Func2(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Func2::~CNDimensional_Func2(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x0,x1)=(x0^2+1)^2 + (x1-1)^2          |
//+------------------------------------------------------------------+
void CNDimensional_Func2::Func(double &x[],double &func,CObject &obj)
  {
   func=MathPow(x[0]*x[0]+1,2)+MathPow(x[1]-1,2);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Func                            |
//+------------------------------------------------------------------+
class CNDimensional_Bad_Func : public CNDimensional_Func
  {
public:
   //--- constructor, destructor
                     CNDimensional_Bad_Func(void);
                    ~CNDimensional_Bad_Func(void);
   //--- method
   virtual void      Func(double &x[],double &func,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Bad_Func::CNDimensional_Bad_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Bad_Func::~CNDimensional_Bad_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates 'bad' function, i.e. function with      | 
//| incorrectly calculated derivatives                               |
//+------------------------------------------------------------------+
void CNDimensional_Bad_Func::Func(double &x[],double &func,CObject &obj)
  {
   func=100*MathPow(x[0]+3,4)+MathPow(x[1]-3,4);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Grad                            |
//+------------------------------------------------------------------+
class CNDimensional_Grad1 : public CNDimensional_Grad
  {
public:
   //--- constructor, destructor
                     CNDimensional_Grad1(void);
                    ~CNDimensional_Grad1(void);
   //--- method
   virtual void      Grad(double &x[],double &func,double &grad[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Grad1::CNDimensional_Grad1(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Grad1::~CNDimensional_Grad1(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x0,x1)=100*(x0+3)^4 + (x1-3)^4 and its| 
//| derivatives df/d0 and df/dx1                                     |
//+------------------------------------------------------------------+
void CNDimensional_Grad1::Grad(double &x[],double &func,
                               double &grad[],CObject &obj)
  {
   func=100*MathPow(x[0]+3,4)+MathPow(x[1]-3,4);
   grad[0]=400*MathPow(x[0]+3,3);
   grad[1]=4*MathPow(x[1]-3,3);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Grad                            |
//+------------------------------------------------------------------+
class CNDimensional_Grad2 : public CNDimensional_Grad
  {
public:
   //--- constructor, destructor
                     CNDimensional_Grad2(void);
                    ~CNDimensional_Grad2(void);
   //--- method
   virtual void      Grad(double &x[],double &func,double &grad[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Grad2::CNDimensional_Grad2(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Grad2::~CNDimensional_Grad2(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x0,x1)=(x0^2+1)^2 + (x1-1)^2 and its  |
//| derivatives df/d0 and df/dx1                                     |
//+------------------------------------------------------------------+
void CNDimensional_Grad2::Grad(double &x[],double &func,
                               double &grad[],CObject &obj)
  {
   func=MathPow(x[0]*x[0]+1,2)+MathPow(x[1]-1,2);
   grad[0]=4*(x[0]*x[0]+1)*x[0];
   grad[1]=2*(x[1]-1);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Grad                            |
//+------------------------------------------------------------------+
class CNDimensional_Bad_Grad : public CNDimensional_Grad
  {
public:
   //--- constructor, destructor
                     CNDimensional_Bad_Grad(void);
                    ~CNDimensional_Bad_Grad(void);
   //--- method
   virtual void      Grad(double &x[],double &func,double &grad[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Bad_Grad::CNDimensional_Bad_Grad(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Bad_Grad::~CNDimensional_Bad_Grad(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates 'bad' function, i.e. function with      |
//| incorrectly calculated derivatives                               |
//+------------------------------------------------------------------+
void CNDimensional_Bad_Grad::Grad(double &x[],double &func,double &grad[],CObject &obj)
  {
   func=100*MathPow(x[0]+3,4)+MathPow(x[1]-3,4);
   grad[0]=40*MathPow(x[0]+3,3);
   grad[1]=40*MathPow(x[1]-3,3);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Grad                            |
//+------------------------------------------------------------------+
class CNDimensional_S1_Grad : public CNDimensional_Grad
  {
public:
                     CNDimensional_S1_Grad(void);
                    ~CNDimensional_S1_Grad(void);

   virtual void      Grad(double &x[],double &func,double &grad[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_S1_Grad::CNDimensional_S1_Grad(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_S1_Grad::~CNDimensional_S1_Grad(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x)=(1+x)^(-0.2)+(1-x)^(-0.3)+1000*x   |
//| and its gradient. function is trimmed when we calculate it near  |
//| the singular points or outside of the [-1,+1]. Note that we do   |
//| NOT calculate gradient in this case.                             |
//+------------------------------------------------------------------+
void CNDimensional_S1_Grad::Grad(double &x[],double &func,double &grad[],CObject &obj)
  {
   if((x[0]<=-0.999999999999) || (x[0]>=+0.999999999999))
     {
      func=1.0E+300;
      return;
     }
   func=MathPow(1+x[0],-0.2)+MathPow(1-x[0],-0.3)+1000*x[0];
   grad[0]=-0.2*MathPow(1+x[0],-1.2)+0.3*MathPow(1-x[0],-1.3)+1000;
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Hess                            |
//+------------------------------------------------------------------+
class CNDimensional_Hess1 : public CNDimensional_Hess
  {
public:
                     CNDimensional_Hess1(void);
                    ~CNDimensional_Hess1(void);

   virtual void      Hess(double &x[],double &func,double &grad[],CMatrixDouble &hess,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Hess1::CNDimensional_Hess1(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Hess1::~CNDimensional_Hess1(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x0,x1)=100*(x0+3)^4 + (x1-3)^4        |
//| its derivatives df/d0 and df/dx1                                 |
//| and its Hessian.                                                 |
//+------------------------------------------------------------------+
void CNDimensional_Hess1::Hess(double &x[],double &func,double &grad[],
                               CMatrixDouble &hess,CObject &obj)
  {
   func=100*MathPow(x[0]+3,4)+MathPow(x[1]-3,4);
   grad[0]=400*MathPow(x[0]+3,3);
   grad[1]=4*MathPow(x[1]-3,3);
   hess[0].Set(0,1200*MathPow(x[0]+3,2));
   hess[0].Set(1,0);
   hess[1].Set(0,0);
   hess[1].Set(1,12*MathPow(x[1]-3,2));
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Hess                            |
//+------------------------------------------------------------------+
class CNDimensional_Hess2 : public CNDimensional_Hess
  {
public:
   //--- constructor, destructor
                     CNDimensional_Hess2(void);
                    ~CNDimensional_Hess2(void);
   //--- method
   virtual void      Hess(double &x[],double &func,double &grad[],CMatrixDouble &hess,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Hess2::CNDimensional_Hess2(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Hess2::~CNDimensional_Hess2(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x0,x1)=(x0^2+1)^2 + (x1-1)^2          |
//| its gradient and Hessian                                         |
//+------------------------------------------------------------------+
void CNDimensional_Hess2::Hess(double &x[],double &func,double &grad[],
                               CMatrixDouble &hess,CObject &obj)
  {
   func=MathPow(x[0]*x[0]+1,2)+MathPow(x[1]-1,2);
   grad[0]=4*(x[0]*x[0]+1)*x[0];
   grad[1]=2*(x[1]-1);
   hess[0].Set(0,12*x[0]*x[0]+4);
   hess[0].Set(1,0);
   hess[1].Set(0,0);
   hess[1].Set(1,2);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Hess                            |
//+------------------------------------------------------------------+
class CNDimensional_Bad_Hess : public CNDimensional_Hess
  {
public:
   //--- constructor, destructor
                     CNDimensional_Bad_Hess(void);
                    ~CNDimensional_Bad_Hess(void);
   //--- method
   virtual void      Hess(double &x[],double &func,double &grad[],CMatrixDouble &hess,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Bad_Hess::CNDimensional_Bad_Hess(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Bad_Hess::~CNDimensional_Bad_Hess(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates 'bad' function, i.e. function with      |
//| incorrectly calculated derivatives                               |
//+------------------------------------------------------------------+
void CNDimensional_Bad_Hess::Hess(double &x[],double &func,double &grad[],
                                  CMatrixDouble &hess,CObject &obj)
  {
   func=100*MathPow(x[0]+3,4)+MathPow(x[1]-3,4);
   grad[0]=40*MathPow(x[0]+3,3);
   grad[1]=40*MathPow(x[1]-3,3);
   hess[0].Set(0,120*MathPow(x[0]+3,2));
   hess[0].Set(1,1);
   hess[1].Set(0,1);
   hess[1].Set(1,120*MathPow(x[1]-3,2));
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_FVec                            |
//+------------------------------------------------------------------+
class CNDimensional_FVec1 : public CNDimensional_FVec
  {
public:
   //--- constructor, destructor
                     CNDimensional_FVec1(void);
                    ~CNDimensional_FVec1(void);
   //--- method
   virtual void      FVec(double &x[],double &fi[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_FVec1::CNDimensional_FVec1(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_FVec1::~CNDimensional_FVec1(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates                                         |
//| f0(x0,x1)=100*(x0+3)^4,                                          |
//| f1(x0,x1)=(x1-3)^4                                               |
//+------------------------------------------------------------------+
void CNDimensional_FVec1::FVec(double &x[],double &fi[],CObject &obj)
  {
   fi[0]=10*MathPow(x[0]+3,2);
   fi[1]=MathPow(x[1]-3,2);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_FVec                            |
//+------------------------------------------------------------------+
class CNDimensional_FVec2 : public CNDimensional_FVec
  {
public:
   //--- constructor, destructor
                     CNDimensional_FVec2(void);
                    ~CNDimensional_FVec2(void);
   //--- method
   virtual void      FVec(double &x[],double &fi[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_FVec2::CNDimensional_FVec2(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_FVec2::~CNDimensional_FVec2(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates                                         |
//| f0(x0,x1)=100*(x0+3)^4,                                          |
//| f1(x0,x1)=(x1-3)^4                                               |
//+------------------------------------------------------------------+
void CNDimensional_FVec2::FVec(double &x[],double &fi[],CObject &obj)
  {
   fi[0]=x[0]*x[0]+1;
   fi[1]=x[1]-1;
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_FVec                            |
//+------------------------------------------------------------------+
class CNDimensional_Bad_FVec : public CNDimensional_FVec
  {
public:
   //--- constructor, destructor
                     CNDimensional_Bad_FVec(void);
                    ~CNDimensional_Bad_FVec(void);
   //--- method
   virtual void      FVec(double &x[],double &fi[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Bad_FVec::CNDimensional_Bad_FVec(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Bad_FVec::~CNDimensional_Bad_FVec(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates 'bad' function, i.e. function with      |
//| incorrectly calculated derivatives                               |
//+------------------------------------------------------------------+
void CNDimensional_Bad_FVec::FVec(double &x[],double &fi[],CObject &obj)
  {
   fi[0]=10*MathPow(x[0]+3,2);
   fi[1]=MathPow(x[1]-3,2);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Jac                             |
//+------------------------------------------------------------------+
class CNDimensional_Jac1 : public CNDimensional_Jac
  {
public:
   //--- constructor, destructor
                     CNDimensional_Jac1(void);
                    ~CNDimensional_Jac1(void);
   //--- method
   virtual void      Jac(double &x[],double &fi[],CMatrixDouble &jac,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Jac1::CNDimensional_Jac1(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Jac1::~CNDimensional_Jac1(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates                                         |
//| f0(x0,x1)=100*(x0+3)^4,                                          |
//| f1(x0,x1)=(x1-3)^4                                               |
//| and Jacobian matrix J=[dfi/dxj]                                  |
//+------------------------------------------------------------------+
void CNDimensional_Jac1::Jac(double &x[],double &fi[],CMatrixDouble &jac,
                             CObject &obj)
  {
   fi[0]=10*MathPow(x[0]+3,2);
   fi[1]=MathPow(x[1]-3,2);
   jac[0].Set(0,20*(x[0]+3));
   jac[0].Set(1,0);
   jac[1].Set(0,0);
   jac[1].Set(1,2*(x[1]-3));
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Jac                             |
//+------------------------------------------------------------------+
class CNDimensional_Jac2 : public CNDimensional_Jac
  {
public:
   //--- constructor, destructor
                     CNDimensional_Jac2(void);
                    ~CNDimensional_Jac2(void);
   //--- method
   virtual void      Jac(double &x[],double &fi[],CMatrixDouble &jac,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Jac2::CNDimensional_Jac2(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Jac2::~CNDimensional_Jac2(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates                                         |
//| f0(x0,x1)=x0^2+1                                                 |
//| f1(x0,x1)=x1-1                                                   |
//| and Jacobian matrix J=[dfi/dxj]                                  |
//+------------------------------------------------------------------+
void CNDimensional_Jac2::Jac(double &x[],double &fi[],CMatrixDouble &jac,
                             CObject &obj)
  {
   fi[0]=x[0]*x[0]+1;
   fi[1]=x[1]-1;
   jac[0].Set(0,2*x[0]);
   jac[0].Set(1,0);
   jac[1].Set(0,0);
   jac[1].Set(1,1);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_Jac                             |
//+------------------------------------------------------------------+
class CNDimensional_Bad_Jac : public CNDimensional_Jac
  {
public:
   //--- constructor, destructor
                     CNDimensional_Bad_Jac(void);
                    ~CNDimensional_Bad_Jac(void);
   //--- method
   virtual void      Jac(double &x[],double &fi[],CMatrixDouble &jac,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Bad_Jac::CNDimensional_Bad_Jac(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Bad_Jac::~CNDimensional_Bad_Jac(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates 'bad' function,                         |
//| i.e. function with incorrectly calculated derivatives            |
//+------------------------------------------------------------------+
void CNDimensional_Bad_Jac::Jac(double &x[],double &fi[],CMatrixDouble &jac,
                                CObject &obj)
  {
   fi[0]=10*MathPow(x[0]+3,2);
   fi[1]=MathPow(x[1]-3,2);
   jac[0].Set(0,20*(x[0]+3));
   jac[0].Set(1,0);
   jac[1].Set(0,1);
   jac[1].Set(1,20*(x[1]-3));
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_PFunc                           |
//+------------------------------------------------------------------+
class CNDimensional_CX_1_Func : public CNDimensional_PFunc
  {
public:
   //--- constructor, destructor
                     CNDimensional_CX_1_Func(void);
                    ~CNDimensional_CX_1_Func(void);
   //--- method
   virtual void      PFunc(double &c[],double &x[],double &func,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_CX_1_Func::CNDimensional_CX_1_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_CX_1_Func::~CNDimensional_CX_1_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(c,x)=exp(-c0*sqr(x0)) where x is a    |
//| position on X-axis and c is adjustable parameter                 |
//+------------------------------------------------------------------+
void CNDimensional_CX_1_Func::PFunc(double &c[],double &x[],double &func,
                                    CObject &obj)
  {
   func=MathExp(-c[0]*x[0]*x[0]);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_PFunc                           |
//+------------------------------------------------------------------+
class CNDimensional_Debt_Func : public CNDimensional_PFunc
  {
public:
   //--- constructor, destructor
                     CNDimensional_Debt_Func(void);
                    ~CNDimensional_Debt_Func(void);
   //--- method
   virtual void      PFunc(double &c[],double &x[],double &func,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_Debt_Func::CNDimensional_Debt_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_Debt_Func::~CNDimensional_Debt_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates                                         |
//| f(c,x)=c[0]*(1+c[1]*(pow(x[0]-1999,c[2])-1))                     |
//+------------------------------------------------------------------+
void CNDimensional_Debt_Func::PFunc(double &c[],double &x[],double &func,
                                    CObject &obj)
  {
   func=c[0]*(1+c[1]*(MathPow(x[0]-1999,c[2])-1));
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_PGrad                           |
//+------------------------------------------------------------------+
class CNDimensional_CX_1_Grad : public CNDimensional_PGrad
  {
public:
   //--- constructor, destructor
                     CNDimensional_CX_1_Grad(void);
                    ~CNDimensional_CX_1_Grad(void);
   //--- method
   virtual void      PGrad(double &c[],double &x[],double &func,double &grad[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_CX_1_Grad::CNDimensional_CX_1_Grad(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_CX_1_Grad::~CNDimensional_CX_1_Grad(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(c,x)=exp(-c0*sqr(x0)) and gradient    |
//| G={df/dc[i]} where x is a position on X-axis and c is adjustable | 
//| parameter.                                                       |
//| IMPORTANT: gradient is calculated with respect to C, not to X    |
//+------------------------------------------------------------------+
void CNDimensional_CX_1_Grad::PGrad(double &c[],double &x[],double &func,
                                    double &grad[],CObject &obj)
  {
   func=MathExp(-c[0]*MathPow(x[0],2));
   grad[0]=-MathPow(x[0],2)*func;
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_PHess                           |
//+------------------------------------------------------------------+
class CNDimensional_CX_1_Hess : public CNDimensional_PHess
  {
public:
   //--- constructor, destructor
                     CNDimensional_CX_1_Hess(void);
                    ~CNDimensional_CX_1_Hess(void);
   //--- method
   virtual void      PHess(double &c[],double &x[],double &func,double &grad[],CMatrixDouble &hess,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_CX_1_Hess::CNDimensional_CX_1_Hess(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_CX_1_Hess::~CNDimensional_CX_1_Hess(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(c,x)=exp(-c0*sqr(x0)), gradient       |
//| G={df/dc[i]} and Hessian H={d2f/(dc[i]*dc[j])} where x is a      |
//| position on X-axis and c is adjustable parameter.                |
//| IMPORTANT: gradient/Hessian are calculated with respect to C,    |
//| not to X                                                         |
//+------------------------------------------------------------------+
void CNDimensional_CX_1_Hess::PHess(double &c[],double &x[],double &func,
                                    double &grad[],CMatrixDouble &hess,
                                    CObject &obj)
  {
   func=MathExp(-c[0]*MathPow(x[0],2));
   grad[0]=-MathPow(x[0],2)*func;
   hess[0].Set(0,MathPow(x[0],4)*func);
  }
//+------------------------------------------------------------------+
//| Derived class from CNDimensional_ODE_RP                          |
//+------------------------------------------------------------------+
class CNDimensional_ODE_Function_1_Dif : public CNDimensional_ODE_RP
  {
public:
   //--- constructor, destructor
                     CNDimensional_ODE_Function_1_Dif(void);
                    ~CNDimensional_ODE_Function_1_Dif(void);
   //--- method
   virtual void      ODE_RP(double &y[],double x,double &dy[],CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CNDimensional_ODE_Function_1_Dif::CNDimensional_ODE_Function_1_Dif(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CNDimensional_ODE_Function_1_Dif::~CNDimensional_ODE_Function_1_Dif(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(y[],x)=-y[0]                          |
//+------------------------------------------------------------------+
void CNDimensional_ODE_Function_1_Dif::ODE_RP(double &y[],double x,double &dy[],CObject &obj)
  {
   dy[0]=-y[0];
  }
//+------------------------------------------------------------------+
//| Derived class from CIntegrator1_Func                             |
//+------------------------------------------------------------------+
class CInt_Function_1_Func : public CIntegrator1_Func
  {
public:
   //--- constructor, destructor
                     CInt_Function_1_Func(void);
                    ~CInt_Function_1_Func(void);
   //--- method
   virtual void      Int_Func(double x,double xminusa,double bminusx,double &y,CObject &obj);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
CInt_Function_1_Func::CInt_Function_1_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CInt_Function_1_Func::~CInt_Function_1_Func(void)
  {

  }
//+------------------------------------------------------------------+
//| This callback calculates f(x)=exp(x)                             |
//+------------------------------------------------------------------+
void CInt_Function_1_Func::Int_Func(double x,double xminusa,double bminusx,
                                    double &y,CObject &obj)
  {
   y=MathExp(x);
  }
//+------------------------------------------------------------------+
//| A comparison of the two numbers                                  |
//+------------------------------------------------------------------+
bool Doc_Test_Int(int val,int test_val)
  {
//--- return result
   return(val==test_val);
  }
//+------------------------------------------------------------------+
//| A comparison of two numbers with an accuracy                     |
//+------------------------------------------------------------------+
bool Doc_Test_Real(double val,double test_val,double _threshold)
  {
//--- calculation
   double s=_threshold>=0 ? 1.0 : MathAbs(test_val);
   double threshold=MathAbs(_threshold);
//--- return result
   return(MathAbs(val-test_val)/s<=threshold);
  }
//+------------------------------------------------------------------+
//| A comparison of two numbers with an accuracy                     |
//+------------------------------------------------------------------+
bool Doc_Test_Complex(complex &val,complex &test_val,double _threshold)
  {
//--- calculation
   double s=_threshold>=0 ? 1.0 : CMath::AbsComplex(test_val);
   double threshold=MathAbs(_threshold);
//--- return result
   return(CMath::AbsComplex(val-test_val)/s<=threshold);
  }
//+------------------------------------------------------------------+
//| A comparison of two vectors                                      |
//+------------------------------------------------------------------+
bool Doc_Test_Int_Vector(int &val[],int &test_val[])
  {
//--- create a variable
   int i;
//--- check
   if(CAp::Len(val)!=CAp::Len(test_val))
      return(false);
//--- comparison
   for(i=0;i<CAp::Len(val);i++)
      if(val[i]!=test_val[i])
         return(false);
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| Comparison of the two matrices                                   |
//+------------------------------------------------------------------+
bool Doc_Test_Int_Matrix(CMatrixInt &val,CMatrixInt &test_val)
  {
//--- create variables
   int i,j;
//--- check
   if(CAp::Rows(val)!=CAp::Rows(test_val))
      return(false);
//--- check
   if(CAp::Cols(val)!=CAp::Cols(test_val))
      return(false);
//--- comparison
   for(i=0;i<CAp::Rows(val);i++)
      for(j=0;j<CAp::Cols(val);j++)
         if(val[i][j]!=test_val[i][j])
            return(false);
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| A comparison of two vectors with an accuracy                     |
//+------------------------------------------------------------------+
bool Doc_Test_Real_Vector(double &val[],double &test_val[],double _threshold)
  {
//--- create a variable
   int i;
//--- check
   if(CAp::Len(val)!=CAp::Len(test_val))
      return(false);
//--- comparison
   for(i=0;i<CAp::Len(val);i++)
     {
      double s=_threshold>=0 ? 1.0 : MathAbs(test_val[i]);
      double threshold=MathAbs(_threshold);
      //--- check
      if(MathAbs(val[i]-test_val[i])/s>threshold)
         return(false);
     }
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| A comparison of two vectors with an accuracy                     |
//+------------------------------------------------------------------+
bool Doc_Test_Real_Matrix(CMatrixDouble &val,CMatrixDouble &test_val,double _threshold)
  {
//--- create variables
   int i,j;
//--- check
   if(CAp::Rows(val)!=CAp::Rows(test_val))
      return(false);
//--- check
   if(CAp::Cols(val)!=CAp::Cols(test_val))
      return(false);
//--- comparison
   for(i=0;i<CAp::Rows(val);i++)
      for(j=0;j<CAp::Cols(val);j++)
        {
         double s=_threshold>=0 ? 1.0 : MathAbs(test_val[i][j]);
         double threshold=MathAbs(_threshold);
         //--- check
         if(MathAbs(val[i][j]-test_val[i][j])/s>threshold)
            return(false);
        }
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| A comparison of two vectors with an accuracy                     |
//+------------------------------------------------------------------+
bool Doc_Test_Complex_Vector(complex &val[],complex &test_val[],double _threshold)
  {
//--- create a variable
   int i;
//--- check
   if(CAp::Len(val)!=CAp::Len(test_val))
      return(false);
//--- comparison
   for(i=0;i<CAp::Len(val);i++)
     {
      double s=_threshold>=0 ? 1.0 : CMath::AbsComplex(test_val[i]);
      double threshold=MathAbs(_threshold);
      //--- check
      if(CMath::AbsComplex(val[i]-test_val[i])/s>threshold)
         return(false);
     }
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| A comparison of two matrices with an accuracy                    |
//+------------------------------------------------------------------+
bool Doc_Test_Complex_Matrix(CMatrixComplex &val,CMatrixComplex &test_val,double _threshold)
  {
//--- create variables
   int i,j;
//--- check
   if(CAp::Rows(val)!=CAp::Rows(test_val))
      return(false);
//--- check
   if(CAp::Cols(val)!=CAp::Cols(test_val))
      return(false);
//--- comparison
   for(i=0;i<CAp::Rows(val);i++)
      for(j=0;j<CAp::Cols(val);j++)
        {
         double s=_threshold>=0 ? 1.0 : CMath::AbsComplex(test_val[i][j]);
         double threshold=MathAbs(_threshold);
         //--- check
         if(CMath::AbsComplex(val[i][j]-test_val[i][j])/s>threshold)
            return(false);
        }
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| Add to the vector random element                                 |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Adding_Element(int &x[])
  {
//--- size calculation
   int n=ArraySize(x);
//--- increasing the length of the vector
   ArrayResize(x,n+1);
//--- set value
   x[n]=MathRand();
  }
//+------------------------------------------------------------------+
//| Add to the vector random element                                 |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Adding_Element(double &x[])
  {
//--- size calculation
   int n=ArraySize(x);
//--- increasing the length of the vector
   ArrayResize(x,n+1);
//--- set value
   x[n]=CMath::RandomReal();
  }
//+------------------------------------------------------------------+
//| Add to the vector random element                                 |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Adding_Element(complex &x[])
  {
//--- size calculation
   int n=ArraySize(x);
//--- increasing the length of the vector
   ArrayResize(x,n+1);
//--- set value
   x[n].re=CMath::RandomReal();
   x[n].im=CMath::RandomReal();
  }
//+------------------------------------------------------------------+
//| Removing the number of vector                                    |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Deleting_Element(int &x[])
  {
//--- size calculation
   int n=ArraySize(x);
//--- reduction length of the vector
   ArrayResize(x,n-1);
  }
//+------------------------------------------------------------------+
//| Removing the number of vector                                    |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Deleting_Element(double &x[])
  {
//--- size calculation
   int n=ArraySize(x);
//--- reduction length of the vector
   ArrayResize(x,n-1);
  }
//+------------------------------------------------------------------+
//| Removing the number of vector                                    |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Deleting_Element(complex &x[])
  {
//--- size calculation
   int n=ArraySize(x);
//--- reduction length of the vector
   ArrayResize(x,n-1);
  }
//+------------------------------------------------------------------+
//| Add a row in the matrix                                          |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Adding_Row(CMatrixInt &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- increase the dimension
   x.Resize(n+1,m);
//--- set values
   for(int i=0;i<m;i++)
      x[n].Set(i,MathRand());
  }
//+------------------------------------------------------------------+
//| Add a row in the matrix                                          |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Adding_Row(CMatrixDouble &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- increase the dimension
   x.Resize(n+1,m);
//--- set values
   for(int i=0;i<m;i++)
      x[n].Set(i,CMath::RandomReal());
  }
//+------------------------------------------------------------------+
//| Add a row in the matrix                                          |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Adding_Row(CMatrixComplex &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- increase the dimension
   x.Resize(n+1,m);
//--- set values
   for(int i=0;i<m;i++)
     {
      x[n].SetRe(i,CMath::RandomReal());
      x[n].SetIm(i,CMath::RandomReal());
     }
  }
//+------------------------------------------------------------------+
//| Deleting a row from the matrix                                   |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Deleting_Row(CMatrixInt &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- reduction of dimension
   x.Resize(n-1,m);
  }
//+------------------------------------------------------------------+
//| Deleting a row from the matrix                                   |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Deleting_Row(CMatrixDouble &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- reduction of dimension
   x.Resize(n-1,m);
  }
//+------------------------------------------------------------------+
//| Deleting a row from the matrix                                   |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Deleting_Row(CMatrixComplex &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- reduction of dimension
   x.Resize(n-1,m);
  }
//+------------------------------------------------------------------+
//| Add a col in the matrix                                          |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Adding_Col(CMatrixInt &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- increase the dimension
   x.Resize(n,m+1);
//--- set values
   for(int i=0;i<m;i++)
      x[i].Set(m,MathRand());
  }
//+------------------------------------------------------------------+
//| Add a col in the matrix                                          |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Adding_Col(CMatrixDouble &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- increase the dimension
   x.Resize(n,m+1);
//--- set values
   for(int i=0;i<m;i++)
      x[i].Set(m,CMath::RandomReal());
  }
//+------------------------------------------------------------------+
//| Add a col in the matrix                                          |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Adding_Col(CMatrixComplex &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- increase the dimension
   x.Resize(n,m+1);
//--- set values
   for(int i=0;i<m;i++)
     {
      x[i].SetRe(m,CMath::RandomReal());
      x[i].SetIm(m,CMath::RandomReal());
     }
  }
//+------------------------------------------------------------------+
//| Deleting a col from the matrix                                   |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Deleting_Col(CMatrixInt &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- reduction of dimension
   x.Resize(n,m-1);
  }
//+------------------------------------------------------------------+
//| Deleting a col from the matrix                                   |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Deleting_Col(CMatrixDouble &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- reduction of dimension
   x.Resize(n,m-1);
  }
//+------------------------------------------------------------------+
//| Deleting a col from the matrix                                   |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Deleting_Col(CMatrixComplex &x)
  {
//--- cols and rows calculation
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- reduction of dimension
   x.Resize(n,m-1);
  }
//+------------------------------------------------------------------+
//| Set number to a random position                                  |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Value(int &x[],int &val)
  {
//--- size calculation
   int n=ArraySize(x);
//--- ????????? ????? ? ????????? ?????? ???????
   if(n!=0)
      x[CMath::RandomInteger(n)]=val;
  }
//+------------------------------------------------------------------+
//| Set number to a random position                                  |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Value(double &x[],double val)
  {
//--- size calculation
   int n=ArraySize(x);
//--- set value
   if(n!=0)
      x[CMath::RandomInteger(n)]=val;
  }
//+------------------------------------------------------------------+
//| Set number to a random position                                  |
//+------------------------------------------------------------------+
void Spoil_Vector_By_Value(complex &x[],complex &val)
  {
//--- size calculation
   int n=ArraySize(x);
//--- set value
   if(n!=0)
      x[CMath::RandomInteger(n)]=val;
  }
//+------------------------------------------------------------------+
//| Set number to a random position                                  |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Value(CMatrixInt &x,int val)
  {
//--- get cols and rows
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- set value
   if(n!=0 && m!=0)
      x[CMath::RandomInteger(n)].Set(CMath::RandomInteger(m),val);
  }
//+------------------------------------------------------------------+
//| Set number to a random position                                  |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Value(CMatrixDouble &x,double val)
  {
//--- get cols and rows
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- set value
   if(n!=0 && m!=0)
      x[CMath::RandomInteger(n)].Set(CMath::RandomInteger(m),val);
  }
//+------------------------------------------------------------------+
//| Set number to a random position                                  |
//+------------------------------------------------------------------+
void Spoil_Matrix_By_Value(CMatrixComplex &x,complex &val)
  {
//--- get cols and rows
   int n=CAp::Rows(x);
   int m=CAp::Cols(x);
//--- set value
   if(n!=0 && m!=0)
      x[CMath::RandomInteger(n)].Set(CMath::RandomInteger(m),val);
  }
//+------------------------------------------------------------------+
//| Function test and exception handling                             |
//+------------------------------------------------------------------+
bool Func_spoil_scenario(int _spoil_scenario,bool &_TestResult)
  {
   if(CAp::exception_happened==true)
     {
      //--- check
      if(_spoil_scenario==-1)
         _TestResult=false;
      //--- reset exception
      CAp::exception_happened=false;
      //--- return result
      return(false);
     }
//--- return result
   return(true);
  }
//+------------------------------------------------------------------+
//| Nearest neighbor search, KNN queries                             |
//+------------------------------------------------------------------+
void TEST_NNeighbor_D_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   int           nx;
   int           ny;
   int           normtype;
   CKDTreeShell  kdt;
   double        x[];
   CMatrixDouble r;
   int           k;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- allocation
      a.Resize(4,2);
      //--- initialization
      a[0].Set(0,0);
      a[0].Set(1,0);
      a[1].Set(0,0);
      a[1].Set(1,1);
      a[2].Set(0,1);
      a[2].Set(1,0);
      a[3].Set(0,1);
      a[3].Set(1,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- change values
      nx=2;
      ny=0;
      normtype=2;
      //--- function call
      CAlglib::KDTreeBuild(a,nx,ny,normtype,kdt);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x,2);
      //--- initialization
      x[0]=-1;
      x[1]=0;
      //--- function call
      k=CAlglib::KDTreeQueryKNN(kdt,x,1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(k,1);
      //--- function call
      CAlglib::KDTreeQueryResultsX(kdt,r);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(1,2);
      //--- initialization
      tempmatrix[0].Set(0,0);
      tempmatrix[0].Set(1,0);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(r,tempmatrix,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","nneighbor_d_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Subsequent queries; buffered functions must use previously       |
//| allocated storage (if large enough), so buffer may contain some  |
//| info from previous call                                          |
//+------------------------------------------------------------------+
void TEST_NNeighbor_T_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   int           nx;
   int           ny;
   int           normtype;
   CKDTreeShell  kdt;
   double        x[];
   CMatrixDouble rx;
   int           k;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- allocation
      a.Resize(4,2);
      //--- initialization
      a[0].Set(0,0);
      a[0].Set(1,0);
      a[1].Set(0,0);
      a[1].Set(1,1);
      a[2].Set(0,1);
      a[2].Set(1,0);
      a[3].Set(0,1);
      a[3].Set(1,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- change values
      nx=2;
      ny=0;
      normtype=2;
      //--- allocation
      rx.Resize(0,0);
      //--- function call
      CAlglib::KDTreeBuild(a,nx,ny,normtype,kdt);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x,2);
      //--- initialization
      x[0]=2;
      x[1]=0;
      //--- function call
      k=CAlglib::KDTreeQueryKNN(kdt,x,2,true);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(k,2);
      //--- function call
      CAlglib::KDTreeQueryResultsX(kdt,rx);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,1);
      tempmatrix[0].Set(1,0);
      tempmatrix[1].Set(0,1);
      tempmatrix[1].Set(1,1);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(rx,tempmatrix,0.05);
      //--- allocation
      ArrayResize(x,2);
      //--- initialization
      x[0]=-2;
      x[1]=0;
      //--- function call
      k=CAlglib::KDTreeQueryKNN(kdt,x,1,true);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(k,1);
      //--- function call
      CAlglib::KDTreeQueryResultsX(kdt,rx);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,0);
      tempmatrix[0].Set(1,0);
      tempmatrix[1].Set(0,1);
      tempmatrix[1].Set(1,1);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(rx,tempmatrix,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","nneighbor_t_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Serialization of KD-trees                                        |
//+------------------------------------------------------------------+
void TEST_NNeighbor_D_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   int           nx;
   int           ny;
   int           normtype;
   CKDTreeShell  kdt0;
   CKDTreeShell  kdt1;
   string        s;
   double        x[];
   CMatrixDouble r0;
   CMatrixDouble r1;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- allocation
      a.Resize(4,2);
      //--- initialization
      a[0].Set(0,0);
      a[0].Set(1,0);
      a[1].Set(0,0);
      a[1].Set(1,1);
      a[2].Set(0,1);
      a[2].Set(1,0);
      a[3].Set(0,1);
      a[3].Set(1,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- change values
      nx=2;
      ny=0;
      normtype=2;
      //--- allocation
      r0.Resize(0,0);
      r1.Resize(0,0);
      //--- Build tree and serialize it
      CAlglib::KDTreeBuild(a,nx,ny,normtype,kdt0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::KDTreeSerialize(kdt0,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::KDTreeUnserialize(s,kdt1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- Compare results from KNN queries
      ArrayResize(x,2);
      //--- initialization
      x[0]=-1;
      x[1]=0;
      //--- function call
      CAlglib::KDTreeQueryKNN(kdt0,x,1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::KDTreeQueryResultsX(kdt0,r0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::KDTreeQueryKNN(kdt1,x,1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::KDTreeQueryResultsX(kdt1,r1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(1,2);
      //--- initialization
      tempmatrix[0].Set(0,0);
      tempmatrix[0].Set(1,0);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(r0,tempmatrix,0.05);
      //--- allocation
      tempmatrix.Resize(1,2);
      //--- initialization
      tempmatrix[0].Set(0,0);
      tempmatrix[0].Set(1,0);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(r1,tempmatrix,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","nneighbor_d_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Basic functionality (moments,adev,median,percentile)             |
//+------------------------------------------------------------------+
void TEST_BaseStat_D_Base(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double mean;
   double variance;
   double skewness;
   double kurtosis;
   double adev;
   double p;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x[i]=i*i;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- Here we demonstrate calculation of sample moments
      //--- (mean,variance,skewness,kurtosis)
      CAlglib::SampleMoments(x,mean,variance,skewness,kurtosis);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(mean,28.5,0.01);
      _TestResult=_TestResult && Doc_Test_Real(variance,801.1667,0.01);
      _TestResult=_TestResult && Doc_Test_Real(skewness,0.5751,0.01);
      _TestResult=_TestResult && Doc_Test_Real(kurtosis,-1.2666,0.01);
      //--- Average deviation
      CAlglib::SampleAdev(x,adev);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(adev,23.2,0.01);
      //--- Median and percentile
      CAlglib::SampleMedian(x,v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,20.5,0.01);
      p=0.5;
      //--- check
      if(_spoil_scenario==3)
         p=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         p=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         p=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::SamplePercentile(x,p,v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,20.5,0.01);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","basestat_d_base");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Correlation (covariance) between two random variables            |
//+------------------------------------------------------------------+
void TEST_BaseStat_D_C2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<10;_spoil_scenario++)
     {
      //--- We have two samples - x and y,and want to measure dependency between them
      ArrayResize(x,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x[i]=i*i;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y[i]=i;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- Three dependency measures are calculated:
      //--- * covariation
      //--- * Pearson correlation
      //--- * Spearman rank correlation
      v=CAlglib::Cov2(x,y);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,82.5,0.001);
      //--- function call
      v=CAlglib::PearsonCorr2(x,y);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,0.9627,0.001);
      //--- function call
      v=CAlglib::SpearmanCorr2(x,y);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,1.000,0.001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","basestat_d_c2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Correlation (covariance) between components of random vector     |
//+------------------------------------------------------------------+
void TEST_BaseStat_D_CM(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble x;
   CMatrixDouble c;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- X is a sample matrix:
      //--- * I-th row corresponds to I-th observation
      //--- * J-th column corresponds to J-th variable
      x.Resize(5,3);
      //--- initialization
      x[0].Set(0,1);
      x[0].Set(1,0);
      x[0].Set(2,1);
      x[1].Set(0,1);
      x[1].Set(1,1);
      x[1].Set(2,0);
      x[2].Set(0,-1);
      x[2].Set(1,1);
      x[2].Set(2,0);
      x[3].Set(0,-2);
      x[3].Set(1,-1);
      x[3].Set(2,1);
      x[4].Set(0,-1);
      x[4].Set(1,0);
      x[4].Set(2,9);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- Three dependency measures are calculated:
      //--- * covariation
      //--- * Pearson correlation
      //--- * Spearman rank correlation
      //--- Result is stored into C,with C[i,j] equal to correlation
      //--- (covariance) between I-th and J-th variables of X.
      CAlglib::CovM(x,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(3,3);
      //--- initialization
      tempmatrix[0].Set(0,1.8);
      tempmatrix[0].Set(1,0.6);
      tempmatrix[0].Set(2,-1.4);
      tempmatrix[1].Set(0,0.6);
      tempmatrix[1].Set(1,0.7);
      tempmatrix[1].Set(2,-0.8);
      tempmatrix[2].Set(0,-1.4);
      tempmatrix[2].Set(1,-0.8);
      tempmatrix[2].Set(2,14.7);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(c,tempmatrix,0.01);
      //--- function call
      CAlglib::PearsonCorrM(x,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(3,3);
      //--- initialization
      tempmatrix[0].Set(0,1);
      tempmatrix[0].Set(1,0.535);
      tempmatrix[0].Set(2,-0.272);
      tempmatrix[1].Set(0,0.535);
      tempmatrix[1].Set(1,1);
      tempmatrix[1].Set(2,-0.249);
      tempmatrix[2].Set(0,-0.272);
      tempmatrix[2].Set(1,-0.249);
      tempmatrix[2].Set(2,1);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(c,tempmatrix,0.01);
      //--- function call
      CAlglib::SpearmanCorrM(x,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(3,3);
      //--- initialization
      tempmatrix[0].Set(0,1);
      tempmatrix[0].Set(1,0.556);
      tempmatrix[0].Set(2,-0.306);
      tempmatrix[1].Set(0,0.556);
      tempmatrix[1].Set(1,1);
      tempmatrix[1].Set(2,-0.75);
      tempmatrix[2].Set(0,-0.306);
      tempmatrix[2].Set(1,-0.75);
      tempmatrix[2].Set(2,1);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(c,tempmatrix,0.01);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","basestat_d_cm");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Correlation (covariance) between two random vectors              |
//+------------------------------------------------------------------+
void TEST_BaseStat_D_CM2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble x;
   CMatrixDouble y;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- X and Y are sample matrices:
      //--- * I-th row corresponds to I-th observation
      //--- * J-th column corresponds to J-th variable
      x.Resize(5,3);
      //--- initialization
      x[0].Set(0,1);
      x[0].Set(1,0);
      x[0].Set(2,1);
      x[1].Set(0,1);
      x[1].Set(1,1);
      x[1].Set(2,0);
      x[2].Set(0,-1);
      x[2].Set(1,1);
      x[2].Set(2,0);
      x[3].Set(0,-2);
      x[3].Set(1,-1);
      x[3].Set(2,1);
      x[4].Set(0,-1);
      x[4].Set(1,0);
      x[4].Set(2,9);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- allocation
      y.Resize(5,2);
      //--- initialization
      y[0].Set(0,2);
      y[0].Set(1,3);
      y[1].Set(0,2);
      y[1].Set(1,1);
      y[2].Set(0,-1);
      y[2].Set(1,6);
      y[3].Set(0,-9);
      y[3].Set(1,9);
      y[4].Set(0,7);
      y[4].Set(1,1);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Value(y,CInfOrNaN::NegativeInfinity());
      CMatrixDouble c;
      //--- Three dependency measures are calculated:
      //--- * covariation
      //--- * Pearson correlation
      //--- * Spearman rank correlation
      //--- Result is stored into C,with C[i,j] equal to correlation
      //--- (covariance) between I-th variable of X and J-th variable of Y.
      CAlglib::CovM2(x,y,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(3,2);
      //--- initialization
      tempmatrix[0].Set(0,4.1);
      tempmatrix[0].Set(1,-3.25);
      tempmatrix[1].Set(0,2.45);
      tempmatrix[1].Set(1,-1.5);
      tempmatrix[2].Set(0,13.45);
      tempmatrix[2].Set(1,-5.75);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(c,tempmatrix,0.01);
      //--- function call
      CAlglib::PearsonCorrM2(x,y,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(3,2);
      //--- initialization
      tempmatrix[0].Set(0,0.519);
      tempmatrix[0].Set(1,-0.699);
      tempmatrix[1].Set(0,0.497);
      tempmatrix[1].Set(1,-0.518);
      tempmatrix[2].Set(0,0.596);
      tempmatrix[2].Set(1,-0.433);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(c,tempmatrix,0.01);
      //--- function call
      CAlglib::SpearmanCorrM2(x,y,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(3,2);
      //--- initialization
      tempmatrix[0].Set(0,0.541);
      tempmatrix[0].Set(1,-0.649);
      tempmatrix[1].Set(0,0.216);
      tempmatrix[1].Set(1,-0.433);
      tempmatrix[2].Set(0,0.433);
      tempmatrix[2].Set(1,-0.135);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(c,tempmatrix,0.01);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","basestat_d_cm2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Tests ability to detect errors in inputs                         |
//+------------------------------------------------------------------+
void TEST_BaseStat_T_Base(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double mean;
   double variance;
   double skewness;
   double kurtosis;
   double adev;
   double p;
   double v;
   double x1[];
   double x2[];
   double x3[];
   double x4[];
   double x5[];
   double x6[];
   double x7[];
   double x8[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<34;_spoil_scenario++)
     {
      //--- first,we test short form of functions
      ArrayResize(x1,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x1[i]=i*i;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x1,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x1,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x1,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::SampleMoments(x1,mean,variance,skewness,kurtosis);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x2,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x2[i]=i*i;
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Value(x2,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(x2,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(x2,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::SampleAdev(x2,adev);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x3,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x3[i]=i*i;
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(x3,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(x3,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Value(x3,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::SampleMedian(x3,v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x4,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x4[i]=i*i;
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(x4,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(x4,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(x4,CInfOrNaN::NegativeInfinity());
      //--- change value
      p=0.5;
      //--- check
      if(_spoil_scenario==12)
         p=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         p=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         p=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::SamplePercentile(x4,p,v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- and then we test full form
      ArrayResize(x5,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x5[i]=i*i;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(x5,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(x5,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(x5,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==18)
         Spoil_Vector_By_Deleting_Element(x5);
      //--- function call
      CAlglib::SampleMoments(x5,10,mean,variance,skewness,kurtosis);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x6,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x6[i]=i*i;
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Value(x6,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Value(x6,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==21)
         Spoil_Vector_By_Value(x6,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Deleting_Element(x6);
      //--- function call
      CAlglib::SampleAdev(x6,10,adev);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x7,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x7[i]=i*i;
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Value(x7,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==24)
         Spoil_Vector_By_Value(x7,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==25)
         Spoil_Vector_By_Value(x7,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==26)
         Spoil_Vector_By_Deleting_Element(x7);
      //--- function call
      CAlglib::SampleMedian(x7,10,v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x8,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x8[i]=i*i;
      //--- check
      if(_spoil_scenario==27)
         Spoil_Vector_By_Value(x8,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==28)
         Spoil_Vector_By_Value(x8,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==29)
         Spoil_Vector_By_Value(x8,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==30)
         Spoil_Vector_By_Deleting_Element(x8);
      //--- change value
      p=0.5;
      //--- check
      if(_spoil_scenario==31)
         p=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==32)
         p=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==33)
         p=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::SamplePercentile(x8,10,p,v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","basestat_t_base");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Tests ability to detect errors in inputs                         |
//+------------------------------------------------------------------+
void TEST_BaseStat_T_CovCorr(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double v;
   CMatrixDouble c;
   double        x1[];
   double        x2[];
   double        x3[];
   double        y1[];
   double        y2[];
   double        y3[];
   double        x1a[];
   double        x2a[];
   double        x3a[];
   double        y1a[];
   double        y2a[];
   double        y3a[];
   CMatrixDouble x4;
   CMatrixDouble x5;
   CMatrixDouble x6;
   CMatrixDouble x7;
   CMatrixDouble x8;
   CMatrixDouble x9;
   CMatrixDouble x10;
   CMatrixDouble x11;
   CMatrixDouble x12;
   CMatrixDouble x13;
   CMatrixDouble x14;
   CMatrixDouble x15;
   CMatrixDouble y10;
   CMatrixDouble y11;
   CMatrixDouble y12;
   CMatrixDouble y13;
   CMatrixDouble y14;
   CMatrixDouble y15;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<126;_spoil_scenario++)
     {
      //--- 2-sample short-form cov/corr are tested
      ArrayResize(x1,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x1[i]=i*i;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x1,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x1,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x1,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x1);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x1);
      //--- allocation
      ArrayResize(y1,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y1[i]=i;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y1,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y1,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y1,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y1);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y1);
      //--- function call
      v=CAlglib::Cov2(x1,y1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x2,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x2[i]=i*i;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(x2,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(x2,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(x2,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Adding_Element(x2);
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Deleting_Element(x2);
      //--- allocation
      ArrayResize(y2,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y2[i]=i;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(y2,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(y2,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(y2,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==18)
         Spoil_Vector_By_Adding_Element(y2);
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Deleting_Element(y2);
      //--- function call
      v=CAlglib::PearsonCorr2(x2,y2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x3,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x3[i]=i*i;
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Value(x3,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==21)
         Spoil_Vector_By_Value(x3,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Value(x3,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Adding_Element(x3);
      //--- check
      if(_spoil_scenario==24)
         Spoil_Vector_By_Deleting_Element(x3);
      //--- allocation
      ArrayResize(y3,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y3[i]=i;
      //--- check
      if(_spoil_scenario==25)
         Spoil_Vector_By_Value(y3,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==26)
         Spoil_Vector_By_Value(y3,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==27)
         Spoil_Vector_By_Value(y3,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==28)
         Spoil_Vector_By_Adding_Element(y3);
      //--- check
      if(_spoil_scenario==29)
         Spoil_Vector_By_Deleting_Element(y3);
      //--- function call
      v=CAlglib::SpearmanCorr2(x3,y3);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- 2-sample full-form cov/corr are tested
      ArrayResize(x1a,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x1a[i]=i*i;
      //--- check
      if(_spoil_scenario==30)
         Spoil_Vector_By_Value(x1a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==31)
         Spoil_Vector_By_Value(x1a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==32)
         Spoil_Vector_By_Value(x1a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==33)
         Spoil_Vector_By_Deleting_Element(x1a);
      //--- allocation
      ArrayResize(y1a,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y1a[i]=i;
      //--- check
      if(_spoil_scenario==34)
         Spoil_Vector_By_Value(y1a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==35)
         Spoil_Vector_By_Value(y1a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==36)
         Spoil_Vector_By_Value(y1a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==37)
         Spoil_Vector_By_Deleting_Element(y1a);
      //--- function call
      v=CAlglib::Cov2(x1a,y1a,10);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x2a,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x2a[i]=i*i;
      //--- check
      if(_spoil_scenario==38)
         Spoil_Vector_By_Value(x2a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==39)
         Spoil_Vector_By_Value(x2a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==40)
         Spoil_Vector_By_Value(x2a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==41)
         Spoil_Vector_By_Deleting_Element(x2a);
      //--- allocation
      ArrayResize(y2a,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y2a[i]=i;
      //--- check
      if(_spoil_scenario==42)
         Spoil_Vector_By_Value(y2a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==43)
         Spoil_Vector_By_Value(y2a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==44)
         Spoil_Vector_By_Value(y2a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==45)
         Spoil_Vector_By_Deleting_Element(y2a);
      //--- function call
      v=CAlglib::PearsonCorr2(x2a,y2a,10);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(x3a,10);
      //--- initialization
      for(int i=0;i<10;i++)
         x3a[i]=i*i;
      //--- check
      if(_spoil_scenario==46)
         Spoil_Vector_By_Value(x3a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==47)
         Spoil_Vector_By_Value(x3a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==48)
         Spoil_Vector_By_Value(x3a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==49)
         Spoil_Vector_By_Deleting_Element(x3a);
      //--- allocation
      ArrayResize(y3a,10);
      //--- initialization
      for(int i=0;i<10;i++)
         y3a[i]=i;
      //--- check
      if(_spoil_scenario==50)
         Spoil_Vector_By_Value(y3a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==51)
         Spoil_Vector_By_Value(y3a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==52)
         Spoil_Vector_By_Value(y3a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==53)
         Spoil_Vector_By_Deleting_Element(y3a);
      //--- function call
      v=CAlglib::SpearmanCorr2(x3a,y3a,10);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- vector short-form cov/corr are tested.
      x4.Resize(5,3);
      //--- initialization
      x4[0].Set(0,1);
      x4[0].Set(1,0);
      x4[0].Set(2,1);
      x4[1].Set(0,1);
      x4[1].Set(1,1);
      x4[1].Set(2,0);
      x4[2].Set(0,-1);
      x4[2].Set(1,1);
      x4[2].Set(2,0);
      x4[3].Set(0,-2);
      x4[3].Set(1,-1);
      x4[3].Set(2,1);
      x4[4].Set(0,-1);
      x4[4].Set(1,0);
      x4[4].Set(2,9);
      //--- check
      if(_spoil_scenario==54)
         Spoil_Matrix_By_Value(x4,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==55)
         Spoil_Matrix_By_Value(x4,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==56)
         Spoil_Matrix_By_Value(x4,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::CovM(x4,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x5.Resize(5,3);
      //--- initialization
      x5[0].Set(0,1);
      x5[0].Set(1,0);
      x5[0].Set(2,1);
      x5[1].Set(0,1);
      x5[1].Set(1,1);
      x5[1].Set(2,0);
      x5[2].Set(0,-1);
      x5[2].Set(1,1);
      x5[2].Set(2,0);
      x5[3].Set(0,-2);
      x5[3].Set(1,-1);
      x5[3].Set(2,1);
      x5[4].Set(0,-1);
      x5[4].Set(1,0);
      x5[4].Set(2,9);
      //--- check
      if(_spoil_scenario==57)
         Spoil_Matrix_By_Value(x5,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==58)
         Spoil_Matrix_By_Value(x5,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==59)
         Spoil_Matrix_By_Value(x5,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::PearsonCorrM(x5,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x6.Resize(5,3);
      //--- initialization
      x6[0].Set(0,1);
      x6[0].Set(1,0);
      x6[0].Set(2,1);
      x6[1].Set(0,1);
      x6[1].Set(1,1);
      x6[1].Set(2,0);
      x6[2].Set(0,-1);
      x6[2].Set(1,1);
      x6[2].Set(2,0);
      x6[3].Set(0,-2);
      x6[3].Set(1,-1);
      x6[3].Set(2,1);
      x6[4].Set(0,-1);
      x6[4].Set(1,0);
      x6[4].Set(2,9);
      //--- check
      if(_spoil_scenario==60)
         Spoil_Matrix_By_Value(x6,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==61)
         Spoil_Matrix_By_Value(x6,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==62)
         Spoil_Matrix_By_Value(x6,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::SpearmanCorrM(x6,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- vector full-form cov/corr are tested.
      x7.Resize(5,3);
      //--- initialization
      x7[0].Set(0,1);
      x7[0].Set(1,0);
      x7[0].Set(2,1);
      x7[1].Set(0,1);
      x7[1].Set(1,1);
      x7[1].Set(2,0);
      x7[2].Set(0,-1);
      x7[2].Set(1,1);
      x7[2].Set(2,0);
      x7[3].Set(0,-2);
      x7[3].Set(1,-1);
      x7[3].Set(2,1);
      x7[4].Set(0,-1);
      x7[4].Set(1,0);
      x7[4].Set(2,9);
      //--- check
      if(_spoil_scenario==63)
         Spoil_Matrix_By_Value(x7,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==64)
         Spoil_Matrix_By_Value(x7,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==65)
         Spoil_Matrix_By_Value(x7,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==66)
         Spoil_Matrix_By_Deleting_Row(x7);
      //--- check
      if(_spoil_scenario==67)
         Spoil_Matrix_By_Deleting_Col(x7);
      //--- function call
      CAlglib::CovM(x7,5,3,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x8.Resize(5,3);
      //--- initialization
      x8[0].Set(0,1);
      x8[0].Set(1,0);
      x8[0].Set(2,1);
      x8[1].Set(0,1);
      x8[1].Set(1,1);
      x8[1].Set(2,0);
      x8[2].Set(0,-1);
      x8[2].Set(1,1);
      x8[2].Set(2,0);
      x8[3].Set(0,-2);
      x8[3].Set(1,-1);
      x8[3].Set(2,1);
      x8[4].Set(0,-1);
      x8[4].Set(1,0);
      x8[4].Set(2,9);
      //--- check
      if(_spoil_scenario==68)
         Spoil_Matrix_By_Value(x8,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==69)
         Spoil_Matrix_By_Value(x8,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==70)
         Spoil_Matrix_By_Value(x8,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==71)
         Spoil_Matrix_By_Deleting_Row(x8);
      //--- check
      if(_spoil_scenario==72)
         Spoil_Matrix_By_Deleting_Col(x8);
      //--- function call
      CAlglib::PearsonCorrM(x8,5,3,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x9.Resize(5,3);
      //--- initialization
      x9[0].Set(0,1);
      x9[0].Set(1,0);
      x9[0].Set(2,1);
      x9[1].Set(0,1);
      x9[1].Set(1,1);
      x9[1].Set(2,0);
      x9[2].Set(0,-1);
      x9[2].Set(1,1);
      x9[2].Set(2,0);
      x9[3].Set(0,-2);
      x9[3].Set(1,-1);
      x9[3].Set(2,1);
      x9[4].Set(0,-1);
      x9[4].Set(1,0);
      x9[4].Set(2,9);
      //--- check
      if(_spoil_scenario==73)
         Spoil_Matrix_By_Value(x9,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==74)
         Spoil_Matrix_By_Value(x9,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==75)
         Spoil_Matrix_By_Value(x9,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==76)
         Spoil_Matrix_By_Deleting_Row(x9);
      //--- check
      if(_spoil_scenario==77)
         Spoil_Matrix_By_Deleting_Col(x9);
      //--- function call
      CAlglib::SpearmanCorrM(x9,5,3,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- cross-vector short-form cov/corr are tested.
      x10.Resize(5,3);
      //--- initialization
      x10[0].Set(0,1);
      x10[0].Set(1,0);
      x10[0].Set(2,1);
      x10[1].Set(0,1);
      x10[1].Set(1,1);
      x10[1].Set(2,0);
      x10[2].Set(0,-1);
      x10[2].Set(1,1);
      x10[2].Set(2,0);
      x10[3].Set(0,-2);
      x10[3].Set(1,-1);
      x10[3].Set(2,1);
      x10[4].Set(0,-1);
      x10[4].Set(1,0);
      x10[4].Set(2,9);
      //--- check
      if(_spoil_scenario==78)
         Spoil_Matrix_By_Value(x10,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==79)
         Spoil_Matrix_By_Value(x10,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==80)
         Spoil_Matrix_By_Value(x10,CInfOrNaN::NegativeInfinity());
      //--- allocation
      y10.Resize(5,2);
      //--- initialization
      y10[0].Set(0,2);
      y10[0].Set(1,3);
      y10[1].Set(0,2);
      y10[1].Set(1,1);
      y10[2].Set(0,-1);
      y10[2].Set(1,6);
      y10[3].Set(0,-9);
      y10[3].Set(1,9);
      y10[4].Set(0,7);
      y10[4].Set(1,1);
      //--- check
      if(_spoil_scenario==81)
         Spoil_Matrix_By_Value(y10,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==82)
         Spoil_Matrix_By_Value(y10,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==83)
         Spoil_Matrix_By_Value(y10,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::CovM2(x10,y10,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x11.Resize(5,3);
      //--- initialization
      x11[0].Set(0,1);
      x11[0].Set(1,0);
      x11[0].Set(2,1);
      x11[1].Set(0,1);
      x11[1].Set(1,1);
      x11[1].Set(2,0);
      x11[2].Set(0,-1);
      x11[2].Set(1,1);
      x11[2].Set(2,0);
      x11[3].Set(0,-2);
      x11[3].Set(1,-1);
      x11[3].Set(2,1);
      x11[4].Set(0,-1);
      x11[4].Set(1,0);
      x11[4].Set(2,9);
      //--- check
      if(_spoil_scenario==84)
         Spoil_Matrix_By_Value(x11,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==85)
         Spoil_Matrix_By_Value(x11,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==86)
         Spoil_Matrix_By_Value(x11,CInfOrNaN::NegativeInfinity());
      //--- allocation
      y11.Resize(5,2);
      //--- initialization
      y11[0].Set(0,2);
      y11[0].Set(1,3);
      y11[1].Set(0,2);
      y11[1].Set(1,1);
      y11[2].Set(0,-1);
      y11[2].Set(1,6);
      y11[3].Set(0,-9);
      y11[3].Set(1,9);
      y11[4].Set(0,7);
      y11[4].Set(1,1);
      //--- check
      if(_spoil_scenario==87)
         Spoil_Matrix_By_Value(y11,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==88)
         Spoil_Matrix_By_Value(y11,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==89)
         Spoil_Matrix_By_Value(y11,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::PearsonCorrM2(x11,y11,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x12.Resize(5,3);
      //--- initialization
      x12[0].Set(0,1);
      x12[0].Set(1,0);
      x12[0].Set(2,1);
      x12[1].Set(0,1);
      x12[1].Set(1,1);
      x12[1].Set(2,0);
      x12[2].Set(0,-1);
      x12[2].Set(1,1);
      x12[2].Set(2,0);
      x12[3].Set(0,-2);
      x12[3].Set(1,-1);
      x12[3].Set(2,1);
      x12[4].Set(0,-1);
      x12[4].Set(1,0);
      x12[4].Set(2,9);
      //--- check
      if(_spoil_scenario==90)
         Spoil_Matrix_By_Value(x12,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==91)
         Spoil_Matrix_By_Value(x12,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==92)
         Spoil_Matrix_By_Value(x12,CInfOrNaN::NegativeInfinity());
      //--- allocation
      y12.Resize(5,2);
      //--- initialization
      y12[0].Set(0,2);
      y12[0].Set(1,3);
      y12[1].Set(0,2);
      y12[1].Set(1,1);
      y12[2].Set(0,-1);
      y12[2].Set(1,6);
      y12[3].Set(0,-9);
      y12[3].Set(1,9);
      y12[4].Set(0,7);
      y12[4].Set(1,1);
      //--- check
      if(_spoil_scenario==93)
         Spoil_Matrix_By_Value(y12,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==94)
         Spoil_Matrix_By_Value(y12,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==95)
         Spoil_Matrix_By_Value(y12,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::SpearmanCorrM2(x12,y12,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- cross-vector full-form cov/corr are tested.
      x13.Resize(5,3);
      //--- initialization
      x13[0].Set(0,1);
      x13[0].Set(1,0);
      x13[0].Set(2,1);
      x13[1].Set(0,1);
      x13[1].Set(1,1);
      x13[1].Set(2,0);
      x13[2].Set(0,-1);
      x13[2].Set(1,1);
      x13[2].Set(2,0);
      x13[3].Set(0,-2);
      x13[3].Set(1,-1);
      x13[3].Set(2,1);
      x13[4].Set(0,-1);
      x13[4].Set(1,0);
      x13[4].Set(2,9);
      //--- check
      if(_spoil_scenario==96)
         Spoil_Matrix_By_Value(x13,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==97)
         Spoil_Matrix_By_Value(x13,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==98)
         Spoil_Matrix_By_Value(x13,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==99)
         Spoil_Matrix_By_Deleting_Row(x13);
      //--- check
      if(_spoil_scenario==100)
         Spoil_Matrix_By_Deleting_Col(x13);
      //--- allocation
      y13.Resize(5,2);
      //--- initialization
      y13[0].Set(0,2);
      y13[0].Set(1,3);
      y13[1].Set(0,2);
      y13[1].Set(1,1);
      y13[2].Set(0,-1);
      y13[2].Set(1,6);
      y13[3].Set(0,-9);
      y13[3].Set(1,9);
      y13[4].Set(0,7);
      y13[4].Set(1,1);
      //--- check
      if(_spoil_scenario==101)
         Spoil_Matrix_By_Value(y13,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==102)
         Spoil_Matrix_By_Value(y13,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==103)
         Spoil_Matrix_By_Value(y13,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==104)
         Spoil_Matrix_By_Deleting_Row(y13);
      //--- check
      if(_spoil_scenario==105)
         Spoil_Matrix_By_Deleting_Col(y13);
      //--- function call
      CAlglib::CovM2(x13,y13,5,3,2,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x14.Resize(5,3);
      //--- initialization
      x14[0].Set(0,1);
      x14[0].Set(1,0);
      x14[0].Set(2,1);
      x14[1].Set(0,1);
      x14[1].Set(1,1);
      x14[1].Set(2,0);
      x14[2].Set(0,-1);
      x14[2].Set(1,1);
      x14[2].Set(2,0);
      x14[3].Set(0,-2);
      x14[3].Set(1,-1);
      x14[3].Set(2,1);
      x14[4].Set(0,-1);
      x14[4].Set(1,0);
      x14[4].Set(2,9);
      //--- check
      if(_spoil_scenario==106)
         Spoil_Matrix_By_Value(x14,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==107)
         Spoil_Matrix_By_Value(x14,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==108)
         Spoil_Matrix_By_Value(x14,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==109)
         Spoil_Matrix_By_Deleting_Row(x14);
      //--- check
      if(_spoil_scenario==110)
         Spoil_Matrix_By_Deleting_Col(x14);
      //--- allocation
      y14.Resize(5,2);
      //--- initialization
      y14[0].Set(0,2);
      y14[0].Set(1,3);
      y14[1].Set(0,2);
      y14[1].Set(1,1);
      y14[2].Set(0,-1);
      y14[2].Set(1,6);
      y14[3].Set(0,-9);
      y14[3].Set(1,9);
      y14[4].Set(0,7);
      y14[4].Set(1,1);
      //--- check
      if(_spoil_scenario==111)
         Spoil_Matrix_By_Value(y14,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==112)
         Spoil_Matrix_By_Value(y14,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==113)
         Spoil_Matrix_By_Value(y14,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==114)
         Spoil_Matrix_By_Deleting_Row(y14);
      //--- check
      if(_spoil_scenario==115)
         Spoil_Matrix_By_Deleting_Col(y14);
      //--- function call
      CAlglib::PearsonCorrM2(x14,y14,5,3,2,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      x15.Resize(5,3);
      //--- initialization
      x15[0].Set(0,1);
      x15[0].Set(1,0);
      x15[0].Set(2,1);
      x15[1].Set(0,1);
      x15[1].Set(1,1);
      x15[1].Set(2,0);
      x15[2].Set(0,-1);
      x15[2].Set(1,1);
      x15[2].Set(2,0);
      x15[3].Set(0,-2);
      x15[3].Set(1,-1);
      x15[3].Set(2,1);
      x15[4].Set(0,-1);
      x15[4].Set(1,0);
      x15[4].Set(2,9);
      //--- check
      if(_spoil_scenario==116)
         Spoil_Matrix_By_Value(x15,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==117)
         Spoil_Matrix_By_Value(x15,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==118)
         Spoil_Matrix_By_Value(x15,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==119)
         Spoil_Matrix_By_Deleting_Row(x15);
      //--- check
      if(_spoil_scenario==120)
         Spoil_Matrix_By_Deleting_Col(x15);
      //--- allocation
      y15.Resize(5,2);
      //--- initialization
      y15[0].Set(0,2);
      y15[0].Set(1,3);
      y15[1].Set(0,2);
      y15[1].Set(1,1);
      y15[2].Set(0,-1);
      y15[2].Set(1,6);
      y15[3].Set(0,-9);
      y15[3].Set(1,9);
      y15[4].Set(0,7);
      y15[4].Set(1,1);
      //--- check
      if(_spoil_scenario==121)
         Spoil_Matrix_By_Value(y15,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==122)
         Spoil_Matrix_By_Value(y15,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==123)
         Spoil_Matrix_By_Value(y15,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==124)
         Spoil_Matrix_By_Deleting_Row(y15);
      //--- check
      if(_spoil_scenario==125)
         Spoil_Matrix_By_Deleting_Col(y15);
      //--- function call
      CAlglib::SpearmanCorrM2(x15,y15,5,3,2,c);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","basestat_t_covcorr");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Real matrix inverse                                              |
//+------------------------------------------------------------------+
void TEST_MatInv_D_R1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- allocation
      a.Resize(2,2);
      //--- initialization
      a[0].Set(0,1);
      a[0].Set(1,-1);
      a[1].Set(0,1);
      a[1].Set(1,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(a);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(a);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(a);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(a);
      //--- create variables
      int info;
      CMatInvReportShell rep;
      //--- function call
      CAlglib::RMatrixInverse(a,info,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,0.5);
      tempmatrix[0].Set(1,0.5);
      tempmatrix[1].Set(0,-0.5);
      tempmatrix[1].Set(1,0.5);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Real_Matrix(a,tempmatrix,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(rep.GetR1(),0.5,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(rep.GetRInf(),0.5,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_d_r1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Complex matrix inverse                                           |
//+------------------------------------------------------------------+
void TEST_MatInv_D_C1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixComplex a;
   CMatrixComplex tempmatrix;
   complex        tempcomplex1;
   complex        tempcomplex2;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- initialization
      tempcomplex1.re=0;
      tempcomplex1.im=1;
      tempcomplex2.re=0;
      tempcomplex2.im=-0.5;
      //--- allocation
      a.Resize(2,2);
      //--- initialization
      a[0].Set(0,tempcomplex1);
      a[0].Set(1,-1);
      a[1].Set(0,tempcomplex1);
      a[1].Set(1,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(a);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(a);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(a);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(a);
      //--- create variables
      int info;
      CMatInvReportShell rep;
      //--- function call
      CAlglib::CMatrixInverse(a,info,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,tempcomplex2);
      tempmatrix[0].Set(1,tempcomplex2);
      tempmatrix[1].Set(0,-0.5);
      tempmatrix[1].Set(1,0.5);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Complex_Matrix(a,tempmatrix,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(rep.GetR1(),0.5,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(rep.GetRInf(),0.5,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_d_c1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| SPD matrix inverse                                               |
//+------------------------------------------------------------------+
void TEST_MatInv_D_SPD1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   CMatrixDouble tempmatrix;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- allocation
      a.Resize(2,2);
      //--- initialization
      a[0].Set(0,2);
      a[0].Set(1,1);
      a[1].Set(0,1);
      a[1].Set(1,2);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(a);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(a);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(a);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(a);
      //--- create variables
      int info;
      CMatInvReportShell rep;
      //--- function call
      CAlglib::SPDMatrixInverse(a,info,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,0.666666);
      tempmatrix[0].Set(1,-0.333333);
      tempmatrix[1].Set(0,-0.333333);
      tempmatrix[1].Set(1,0.666666);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Real_Matrix(a,tempmatrix,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_d_spd1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| HPD matrix inverse                                               |
//+------------------------------------------------------------------+
void TEST_MatInv_D_HPD1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixComplex a;
   CMatrixComplex tempmatrix;
   complex cnan;
   complex cpositiveinfinity;
   complex cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- allocation
      a.Resize(2,2);
      //--- initialization
      a[0].Set(0,2);
      a[0].Set(1,1);
      a[1].Set(0,1);
      a[1].Set(1,2);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(a);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(a);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(a);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(a);
      //--- create variables
      int info;
      CMatInvReportShell rep;
      //--- function call
      CAlglib::HPDMatrixInverse(a,info,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,0.666666);
      tempmatrix[0].Set(1,-0.333333);
      tempmatrix[1].Set(0,-0.333333);
      tempmatrix[1].Set(1,0.666666);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Complex_Matrix(a,tempmatrix,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_d_hpd1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Real matrix inverse: singular matrix                             |
//+------------------------------------------------------------------+
void TEST_MatInv_T_R1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
//--- allocation
   a.Resize(2,2);
//--- initialization
   a[0].Set(0,1);
   a[0].Set(1,-1);
   a[1].Set(0,-2);
   a[1].Set(1,2);
//--- create variables
   int info;
   CMatInvReportShell rep;
//--- function call
   CAlglib::RMatrixInverse(a,info,rep);
//--- handling exceptions
   Func_spoil_scenario(_spoil_scenario,_TestResult);
//--- check result
   _TestResult=_TestResult && Doc_Test_Int(info,-3);
   _TestResult=_TestResult && Doc_Test_Real(rep.GetR1(),0.0,0.00005);
   _TestResult=_TestResult && Doc_Test_Real(rep.GetRInf(),0.0,0.00005);
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_t_r1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Complex matrix inverse: singular matrix                          |
//+------------------------------------------------------------------+
void TEST_MatInv_T_C1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   CMatrixComplex a;
   complex tempcomplex1;
   complex tempcomplex2;
   _TestResult=true;
//--- create variables
   tempcomplex1.re=0;
   tempcomplex1.im=1;
   tempcomplex2.re=0;
   tempcomplex2.im=-1;
//--- allocation
   a.Resize(2,2);
//--- initialization
   a[0].Set(0,tempcomplex1);
   a[0].Set(1,tempcomplex2);
   a[1].Set(0,-2);
   a[1].Set(1,2);
//--- create variables
   int info;
   CMatInvReportShell rep;
//--- function call
   CAlglib::CMatrixInverse(a,info,rep);
//--- handling exceptions
   Func_spoil_scenario(_spoil_scenario,_TestResult);
//--- check result
   _TestResult=_TestResult && Doc_Test_Int(info,-3);
   _TestResult=_TestResult && Doc_Test_Real(rep.GetR1(),0.0,0.00005);
   _TestResult=_TestResult && Doc_Test_Real(rep.GetRInf(),0.0,0.00005);
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_t_c1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Attempt to use SPD function on nonsymmetrix matrix               |
//+------------------------------------------------------------------+
void TEST_MatInv_E_SPD1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
//--- allocation
   a.Resize(2,2);
//--- initialization
   a[0].Set(0,1);
   a[0].Set(1,0);
   a[1].Set(0,1);
   a[1].Set(1,1);
//--- create variables
   int info;
   CMatInvReportShell rep;
//--- function call
   CAlglib::SPDMatrixInverse(a,info,rep);
//--- handling exceptions
   if(Func_spoil_scenario(_spoil_scenario,_TestResult))
      _TestResult=false;
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_e_spd1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Attempt to use SPD function on nonsymmetrix matrix               |
//+------------------------------------------------------------------+
void TEST_MatInv_E_HPD1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixComplex a;
//--- allocation
   a.Resize(2,2);
//--- initialization
   a[0].Set(0,1);
   a[0].Set(1,0);
   a[1].Set(0,1);
   a[1].Set(1,1);
//--- create variables
   int info;
   CMatInvReportShell rep;
//--- function call
   CAlglib::HPDMatrixInverse(a,info,rep);
//--- handling exceptions
   if(Func_spoil_scenario(_spoil_scenario,_TestResult))
      _TestResult=false;
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matinv_e_hpd1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by CG                                     |
//+------------------------------------------------------------------+
void TEST_MinCG_D_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- with nonlinear conjugate gradient method.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- create a variable
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      int maxits=0;
      //--- create variables
      CMinCGStateShell state;
      CMinCGReportShell rep;
      //--- function call
      CAlglib::MinCGCreate(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","mincg_d_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization with additional settings and restarts     |
//+------------------------------------------------------------------+
void TEST_MinCG_D_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<18;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- with nonlinear conjugate gradient method.
      //--- Several advanced techniques are demonstrated:
      //--- * upper limit on step size
      //--- * restart from new point
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      double stpmax=0.1;
      //--- check
      if(_spoil_scenario==12)
         stpmax=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         stpmax=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         stpmax=CInfOrNaN::NegativeInfinity();
      int maxits=0;
      //--- create variables
      CMinCGStateShell state;
      CMinCGReportShell rep;
      //--- first run
      CAlglib::MinCGCreate(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGSetStpMax(state,stpmax);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      //--- second run - algorithm is restarted with mincgrestartfrom()
      ArrayResize(x,2);
      //--- initialization
      x[0]=10;
      x[1]=10;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::MinCGRestartFrom(state,x);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","mincg_d_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by CG with numerical differentiation      |
//+------------------------------------------------------------------+
void TEST_MinCG_NumDiff(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Func1 ffunc;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<15;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- using numerical differentiation to calculate gradient.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      double diffstep=1.0e-6;
      //--- check
      if(_spoil_scenario==12)
         diffstep=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         diffstep=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         diffstep=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinCGStateShell state;
      CMinCGReportShell rep;
      //--- function call
      CAlglib::MinCGCreateF(x,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGOptimize(state,ffunc,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","mincg_numdiff");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by CG, function with singularities        |
//+------------------------------------------------------------------+
void TEST_MinCG_FTRIM(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double                x[];
   double                temparray[];
   CObject               obj;
   CNDimensional_S1_Grad fs1grad;
   CNDimensional_Rep     frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x)=(1+x)^(-0.2) + (1-x)^(-0.3) + 1000*x.
      //--- This function has singularities at the boundary of the [-1,+1],but technique called
      //--- "function trimming" allows us to solve this optimization problem.
      //--- See http://www.CAlglib::net/optimization/tipsandtricks.php#ftrimming for more information
      //--- on this subject.
      ArrayResize(x,1);
      //--- initialization
      x[0]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=1.0e-6;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinCGStateShell state;
      CMinCGReportShell rep;
      //--- function call
      CAlglib::MinCGCreate(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGOptimize(state,fs1grad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinCGResults(state,x,rep);
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=-0.99917305;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.000005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","mincg_ftrim");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization with bound constraints                    |
//+------------------------------------------------------------------+
void TEST_MinBLEIC_D_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   double              bndl[];
   double              bndu[];
   CObject             obj;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<22;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- subject to bound constraints -1<=x<=+1,-1<=y<=+1,using BLEIC optimizer.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(bndl,2);
      //--- initialization
      bndl[0]=-1;
      bndl[1]=-1;
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Value(bndl,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(bndl);
      //--- allocation
      ArrayResize(bndu,2);
      //--- initialization
      bndu[0]=1;
      bndu[1]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(bndu,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Deleting_Element(bndu);
      CMinBLEICStateShell state;
      CMinBLEICReportShell rep;
      //--- These variables define stopping conditions for the underlying CG algorithm.
      //--- They should be stringent enough in order to guarantee overall stability
      //--- of the outer iterations.
      //--- We use very simple condition - |g|<=epsg
      double epsg=0.000001;
      //--- check
      if(_spoil_scenario==7)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==8)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==10)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==11)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==12)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==13)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- These variables define stopping conditions for the outer iterations:
      //--- * epso controls convergence of outer iterations;algorithm will stop
      //---   when difference between solutions of subsequent unconstrained problems
      //---   will be less than 0.0001
      //--- * epsi controls amount of infeasibility allowed in the final solution
      double epso=0.00001;
      //--- check
      if(_spoil_scenario==16)
         epso=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         epso=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         epso=CInfOrNaN::NegativeInfinity();
      double epsi=0.00001;
      //--- check
      if(_spoil_scenario==19)
         epsi=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==20)
         epsi=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==21)
         epsi=CInfOrNaN::NegativeInfinity();
      //--- Now we are ready to actually optimize something:
      //--- * first we create optimizer
      //--- * we add boundary constraints
      //--- * we tune stopping conditions
      //--- * and,finally,optimize and obtain results...
      CAlglib::MinBLEICCreate(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetBC(state,bndl,bndu);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetInnerCond(state,epsg,epsf,epsx);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetOuterCond(state,epso,epsi);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- ...and evaluate these results
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-1;
      temparray[1]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minbleic_d_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization with linear inequality constraints        |
//+------------------------------------------------------------------+
void TEST_MinBLEIC_D_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   int                 ct[];
   CMatrixDouble       c;
   CObject             obj;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<24;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- subject to inequality constraints:
      //--- * x>=2 (posed as general linear constraint),
      //--- * x+y>=6
      //--- using BLEIC optimizer.
      ArrayResize(x,2);
      //--- initialization
      x[0]=5;
      x[1]=5;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- allocation
      c.Resize(2,3);
      //--- initialization
      c[0].Set(0,1);
      c[0].Set(1,0);
      c[0].Set(2,2);
      c[1].Set(0,1);
      c[1].Set(1,1);
      c[1].Set(2,6);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Value(c,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Value(c,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Value(c,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Row(c);
      //--- check
      if(_spoil_scenario==7)
         Spoil_Matrix_By_Deleting_Col(c);
      //--- allocation
      ArrayResize(ct,2);
      //--- initialization
      ct[0]=1;
      ct[1]=1;
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Deleting_Element(ct);
      //--- create variables
      CMinBLEICStateShell state;
      CMinBLEICReportShell rep;
      //--- These variables define stopping conditions for the underlying CG algorithm.
      //--- They should be stringent enough in order to guarantee overall stability
      //--- of the outer iterations.
      //--- We use very simple condition - |g|<=epsg
      double epsg=0.000001;
      //--- check
      if(_spoil_scenario==9)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==12)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==15)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==16)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==17)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- These variables define stopping conditions for the outer iterations:
      //--- * epso controls convergence of outer iterations;algorithm will stop
      //---   when difference between solutions of subsequent unconstrained problems
      //---   will be less than 0.0001
      //--- * epsi controls amount of infeasibility allowed in the final solution
      double epso=0.00001;
      //--- check
      if(_spoil_scenario==18)
         epso=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==19)
         epso=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==20)
         epso=CInfOrNaN::NegativeInfinity();
      double epsi=0.00001;
      //--- check
      if(_spoil_scenario==21)
         epsi=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==22)
         epsi=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==23)
         epsi=CInfOrNaN::NegativeInfinity();
      //--- Now we are ready to actually optimize something:
      //--- * first we create optimizer
      //--- * we add linear constraints
      //--- * we tune stopping conditions
      //--- * and,finally,optimize and obtain results...
      CAlglib::MinBLEICCreate(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetLC(state,c,ct);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetInnerCond(state,epsg,epsf,epsx);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetOuterCond(state,epso,epsi);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- ...and evaluate these results
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=2;
      temparray[1]=4;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minbleic_d_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization with bound constraints and numerical      |
//| differentiation                                                  |
//+------------------------------------------------------------------+
void TEST_MinBLEIC_NumDiff(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   double              bndl[];
   double              bndu[];
   CObject             obj;
   CNDimensional_Func1 ffunc;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<25;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- subject to bound constraints -1<=x<=+1,-1<=y<=+1,using BLEIC optimizer.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(bndl,2);
      //--- initialization
      bndl[0]=-1;
      bndl[1]=-1;
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Value(bndl,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(bndl);
      //--- allocation
      ArrayResize(bndu,2);
      //--- initialization
      bndu[0]=1;
      bndu[1]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(bndu,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Deleting_Element(bndu);
      CMinBLEICStateShell state;
      CMinBLEICReportShell rep;
      //--- These variables define stopping conditions for the underlying CG algorithm.
      //--- They should be stringent enough in order to guarantee overall stability
      //--- of the outer iterations.
      //--- We use very simple condition - |g|<=epsg
      double epsg=0.000001;
      //--- check
      if(_spoil_scenario==7)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==8)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==10)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==11)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==12)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==13)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- These variables define stopping conditions for the outer iterations:
      //--- * epso controls convergence of outer iterations;algorithm will stop
      //---   when difference between solutions of subsequent unconstrained problems
      //---   will be less than 0.0001
      //--- * epsi controls amount of infeasibility allowed in the final solution
      double epso=0.00001;
      //--- check
      if(_spoil_scenario==16)
         epso=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         epso=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         epso=CInfOrNaN::NegativeInfinity();
      double epsi=0.00001;
      //--- check
      if(_spoil_scenario==19)
         epsi=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==20)
         epsi=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==21)
         epsi=CInfOrNaN::NegativeInfinity();
      //--- This variable contains differentiation step
      double diffstep=1.0e-6;
      //--- check
      if(_spoil_scenario==22)
         diffstep=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==23)
         diffstep=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==24)
         diffstep=CInfOrNaN::NegativeInfinity();
      //--- Now we are ready to actually optimize something:
      //--- * first we create optimizer
      //--- * we add boundary constraints
      //--- * we tune stopping conditions
      //--- * and,finally,optimize and obtain results...
      CAlglib::MinBLEICCreateF(x,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetBC(state,bndl,bndu);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetInnerCond(state,epsg,epsf,epsx);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetOuterCond(state,epso,epsi);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICOptimize(state,ffunc,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- ...and evaluate these results
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-1;
      temparray[1]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minbleic_numdiff");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by BLEIC, function with singularities     |
//+------------------------------------------------------------------+
void TEST_MinBLEIC_FTRIM(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double                x[];
   double                temparray[];
   CObject               obj;
   CNDimensional_S1_Grad fs1grad;
   CNDimensional_Rep     frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<18;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x)=(1+x)^(-0.2) + (1-x)^(-0.3) + 1000*x.
      //--- This function is undefined outside of (-1,+1) and has singularities at x=-1 and x=+1.
      //--- Special technique called "function trimming" allows us to solve this optimization problem 
      //--- - withusing boundary constraints!
      //--- See http://www.CAlglib::net/optimization/tipsandtricks.php#ftrimming for more information
      //--- on this subject.
      ArrayResize(x,1);
      //--- initialization
      x[0]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=1.0e-6;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      double epso=1.0e-6;
      //--- check
      if(_spoil_scenario==12)
         epso=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         epso=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         epso=CInfOrNaN::NegativeInfinity();
      double epsi=1.0e-6;
      //--- check
      if(_spoil_scenario==15)
         epsi=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==16)
         epsi=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==17)
         epsi=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CMinBLEICStateShell state;
      CMinBLEICReportShell rep;
      //--- function call
      CAlglib::MinBLEICCreate(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetInnerCond(state,epsg,epsf,epsx);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICSetOuterCond(state,epso,epsi);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICOptimize(state,fs1grad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinBLEICResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=-0.99917305;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.000005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minbleic_ftrim");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Simple unconstrained MCPD model (no entry/exit states)           |
//+------------------------------------------------------------------+
void TEST_MCPD_Simple1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble track0;
   CMatrixDouble track1;
   CMatrixDouble tempmatrix;
   CMatrixDouble p;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- The very simple MCPD example
      //--- We have a loan portfolio. Our loans can be in one of two states:
      //--- * normal loans ("good" ones)
      //--- * past due loans ("bad" ones)
      //--- We assume that:
      //--- * loans can transition from any state to any other state. In 
      //---   particular,past due loan can become "good" one at any moment 
      //---   with same (fixed) probability. Not realistic,but it is toy example :)
      //--- * portfolio size does not change over time
      //
      //--- Thus,we have following model
      //---     state_new=P*state_old
      //--- where
      //---         ( p00  p01 )
      //---     P = (          )
      //---         ( p10  p11 )
      //--- We want to model transitions between these two states using MCPD
      //--- approach (Markov Chains for Proportional/Population Data),i.e.
      //--- to restore hidden transition matrix P using actual portfolio data.
      //--- We have:
      //--- * poportional data,i.e. proportion of loans in the normal and past 
      //---   due states (not portfolio size measured in some currency,although 
      //---   it is possible to work with population data too)
      //--- * two tracks,i.e. two sequences which describe portfolio
      //---   evolution from two different starting states: [1,0] (all loans 
      //---   are "good") and [0.8,0.2] (only 80% of portfolio is in the "good"
      //---   state)
      CMCPDStateShell s;
      CMCPDReportShell rep;
      //--- allocation
      track0.Resize(5,2);
      //--- initialization
      track0[0].Set(0,1);
      track0[0].Set(1,0);
      track0[1].Set(0,0.95);
      track0[1].Set(1,0.05);
      track0[2].Set(0,0.9275);
      track0[2].Set(1,0.0725);
      track0[3].Set(0,0.91738);
      track0[3].Set(1,0.08263);
      track0[4].Set(0,0.91282);
      track0[4].Set(1,0.08718);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(track0,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(track0,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(track0,CInfOrNaN::NegativeInfinity());
      //--- allocation
      track1.Resize(4,2);
      //--- initialization
      track1[0].Set(0,0.8);
      track1[0].Set(1,0.2);
      track1[1].Set(0,0.86);
      track1[1].Set(1,0.14);
      track1[2].Set(0,0.887);
      track1[2].Set(1,0.113);
      track1[3].Set(0,0.89915);
      track1[3].Set(1,0.10085);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Value(track1,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Value(track1,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Value(track1,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::MCPDCreate(2,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddTrack(s,track0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddTrack(s,track1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDSolve(s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDResults(s,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- Hidden matrix P is equal to
      //---         ( 0.95  0.50 )
      //---         (            )
      //---         ( 0.05  0.50 )
      //--- which means that "good" loans can become "bad" with 5% probability,
      //--- while "bad" loans will return to good state with 50% probability.
      tempmatrix.Resize(2,2);
      //--- initialization
      tempmatrix[0].Set(0,0.95);
      tempmatrix[0].Set(1,0.5);
      tempmatrix[1].Set(0,0.05);
      tempmatrix[1].Set(1,0.5);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(p,tempmatrix,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","mcpd_simple1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Simple MCPD model (no entry/exit states) with equality           |
//| constraints                                                      |
//+------------------------------------------------------------------+
void TEST_MCPD_Simple2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble track0;
   CMatrixDouble track1;
   CMatrixDouble tempmatrix;
   CMatrixDouble p;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- Simple MCPD example
      //--- We have a loan portfolio. Our loans can be in one of three states:
      //--- * normal loans
      //--- * past due loans
      //--- * charged off loans
      //--- We assume that:
      //--- * normal loan can stay normal or become past due (but not charged off)
      //--- * past due loan can stay past due,become normal or charged off
      //--- * charged off loan will stay charged off for the rest of eternity
      //--- * portfolio size does not change over time
      //--- Not realistic,but it is toy example :)
      //--- Thus,we have following model
      //---     state_new=P*state_old
      //--- where
      //---         ( p00  p01    )
      //---     P = ( p10  p11    )
      //---         (      p21  1 )
      //--- i.e. four elements of P are known a priori.
      //--- Although it is possible (given enough data) to In order to enforce 
      //--- this property we set equality constraints on these elements.
      //--- We want to model transitions between these two states using MCPD
      //--- approach (Markov Chains for Proportional/Population Data),i.e.
      //--- to restore hidden transition matrix P using actual portfolio data.
      //--- We have:
      //--- * poportional data,i.e. proportion of loans in the current and past 
      //---   due states (not portfolio size measured in some currency,although 
      //---   it is possible to work with population data too)
      //--- * two tracks,i.e. two sequences which describe portfolio
      //---   evolution from two different starting states: [1,0,0] (all loans 
      //---   are "good") and [0.8,0.2,0.0] (only 80% of portfolio is in the "good"
      //---   state)
      CMCPDStateShell s;
      CMCPDReportShell rep;
      //--- allocation
      track0.Resize(5,3);
      //--- initialization
      track0[0].Set(0,1);
      track0[0].Set(1,0);
      track0[0].Set(2,0);
      track0[1].Set(0,0.95);
      track0[1].Set(1,0.05);
      track0[1].Set(2,0);
      track0[2].Set(0,0.9275);
      track0[2].Set(1,0.06);
      track0[2].Set(2,0.0125);
      track0[3].Set(0,0.911125);
      track0[3].Set(1,0.061375);
      track0[3].Set(2,0.0275);
      track0[4].Set(0,0.896256);
      track0[4].Set(1,0.0609);
      track0[4].Set(2,0.042844);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(track0,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(track0,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(track0,CInfOrNaN::NegativeInfinity());
      //--- allocation
      track1.Resize(5,3);
      //--- initialization
      track1[0].Set(0,0.8);
      track1[0].Set(1,0.2);
      track1[0].Set(2,0);
      track1[1].Set(0,0.86);
      track1[1].Set(1,0.09);
      track1[1].Set(2,0.05);
      track1[2].Set(0,0.862);
      track1[2].Set(1,0.0655);
      track1[2].Set(2,0.0725);
      track1[3].Set(0,0.85165);
      track1[3].Set(1,0.059475);
      track1[3].Set(2,0.088875);
      track1[4].Set(0,0.838805);
      track1[4].Set(1,0.057451);
      track1[4].Set(2,0.103744);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Value(track1,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Value(track1,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Value(track1,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::MCPDCreate(3,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddTrack(s,track0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddTrack(s,track1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddEC(s,0,2,0.0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddEC(s,1,2,0.0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddEC(s,2,2,1.0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDAddEC(s,2,0,0.0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDSolve(s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MCPDResults(s,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- Hidden matrix P is equal to
      //---         ( 0.95 0.50      )
      //---         ( 0.05 0.25      )
      //---         (      0.25 1.00 ) 
      //--- which means that "good" loans can become past due with 5% probability,
      //--- while past due loans will become charged off with 25% probability or
      //--- return back to normal state with 50% probability.
      tempmatrix.Resize(3,3);
      //--- initialization
      tempmatrix[0].Set(0,0.95);
      tempmatrix[0].Set(1,0.5);
      tempmatrix[0].Set(2,0);
      tempmatrix[1].Set(0,0.05);
      tempmatrix[1].Set(1,0.25);
      tempmatrix[1].Set(2,0);
      tempmatrix[2].Set(0,0);
      tempmatrix[2].Set(1,0.25);
      tempmatrix[2].Set(2,1);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Matrix(p,tempmatrix,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","mcpd_simple2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by L-BFGS                                 |
//+------------------------------------------------------------------+
void TEST_MinLBFGS_D_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- using LBFGS method.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLBFGSStateShell state;
      CMinLBFGSReportShell rep;
      //--- function call
      CAlglib::MinLBFGSCreate(1,x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlbfgs_d_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization with additional settings and restarts     |
//+------------------------------------------------------------------+
void TEST_MinLBFGS_D_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<18;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- using LBFGS method.
      //--- Several advanced techniques are demonstrated:
      //--- * upper limit on step size
      //--- * restart from new point
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      double stpmax=0.1;
      //--- check
      if(_spoil_scenario==12)
         stpmax=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         stpmax=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         stpmax=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLBFGSStateShell state;
      CMinLBFGSReportShell rep;
      //--- first run
      CAlglib::MinLBFGSCreate(1,x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSSetStpMax(state,stpmax);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      //--- second run - algorithm is restarted
      ArrayResize(x,2);
      //--- initialization
      x[0]=10;
      x[1]=10;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::MinLBFGSRestartFrom(state,x);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSOptimize(state,fgrad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlbfgs_d_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by L-BFGS with numerical differentiation  |
//+------------------------------------------------------------------+
void TEST_MinLBFGS_NumDiff(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Func1 ffunc;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<15;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x,y)=100*(x+3)^4+(y-3)^4
      //--- using numerical differentiation to calculate gradient.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      double diffstep=1.0e-6;
      //--- check
      if(_spoil_scenario==12)
         diffstep=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==13)
         diffstep=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==14)
         diffstep=CInfOrNaN::NegativeInfinity();
      int maxits=0;
      CMinLBFGSStateShell state;
      CMinLBFGSReportShell rep;
      //--- function call
      CAlglib::MinLBFGSCreateF(1,x,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSOptimize(state,ffunc,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlbfgs_numdiff");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear optimization by LBFGS, function with singularities     |
//+------------------------------------------------------------------+
void TEST_MinLBFGS_FTRIM(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double                x[];
   double                temparray[];
   CObject               obj;
   CNDimensional_S1_Grad fs1grad;
   CNDimensional_Rep     frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of f(x)=(1+x)^(-0.2) + (1-x)^(-0.3) + 1000*x.
      //--- This function has singularities at the boundary of the [-1,+1],but technique called
      //--- "function trimming" allows us to solve this optimization problem.
      //--- See http://www.CAlglib::net/optimization/tipsandtricks.php#ftrimming for more information
      //--- on this subject.
      ArrayResize(x,1);
      //--- initialization
      x[0]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=1.0e-6;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLBFGSStateShell state;
      CMinLBFGSReportShell rep;
      //--- function call
      CAlglib::MinLBFGSCreate(1,x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSOptimize(state,fs1grad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLBFGSResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=-0.99917305;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.000005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlbfgs_ftrim");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Solving y'=-y with ODE solver                                    |
//+------------------------------------------------------------------+
void TEST_ODESolver_D1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double                           y[];
   double                           x[];
   int                              m;
   double                           xtbl[];
   double                           temparray[];
   CMatrixDouble                    ytbl;
   CMatrixDouble                    tempmatrix;
   CObject                          obj;
   CNDimensional_ODE_Function_1_Dif fode;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<13;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,1);
      //--- initialization
      y[0]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(x,4);
      //--- initialization
      x[0]=0;
      x[1]=1;
      x[2]=2;
      x[3]=3;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double eps=0.00001;
      //--- check
      if(_spoil_scenario==7)
         eps=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==8)
         eps=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         eps=CInfOrNaN::NegativeInfinity();
      double h=0;
      //--- check
      if(_spoil_scenario==10)
         h=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==11)
         h=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==12)
         h=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CODESolverStateShell s;
      CODESolverReportShell rep;
      //--- function call
      CAlglib::ODESolverRKCK(y,x,eps,h,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::ODESolverSolve(s,fode,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::ODESolverResults(s,m,xtbl,ytbl,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=0;
      temparray[1]=1;
      temparray[2]=2;
      temparray[3]=3;
      //--- allocation
      tempmatrix.Resize(4,1);
      //--- initialization
      tempmatrix[0].Set(0,1);
      tempmatrix[1].Set(0,0.367);
      tempmatrix[2].Set(0,0.135);
      tempmatrix[3].Set(0,0.05);
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(m,4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(xtbl,temparray,0.005);
      _TestResult=_TestResult && Doc_Test_Real_Matrix(ytbl,tempmatrix,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","odesolver_d1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Complex FFT: simple example                                      |
//+------------------------------------------------------------------+
void TEST_FFT_Complex_D1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex z[];
   complex temparray[];
   complex tempcomplex1;
   complex tempcomplex2;
   complex cnan;
   complex cpositiveinfinity;
   complex cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- first we demonstrate forward FFT:
      //--- [1i,1i,1i,1i] is converted to [4i,0,0,0]
      tempcomplex1.re=0;
      tempcomplex1.im=1;
      tempcomplex2.re=0;
      tempcomplex2.im=4;
      //--- allocation
      ArrayResize(z,4);
      //--- initialization
      z[0]=tempcomplex1;
      z[1]=tempcomplex1;
      z[2]=tempcomplex1;
      z[3]=tempcomplex1;
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(z,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(z,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(z,cnegativeinfinity);
      //--- function call
      CAlglib::FFTC1D(z);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=tempcomplex2;
      temparray[1]=0;
      temparray[2]=0;
      temparray[3]=0;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(z,temparray,0.0001);
      //--- now we convert [4i,0,0,0] back to [1i,1i,1i,1i]
      //--- with backward FFT
      CAlglib::FFTC1DInv(z);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=tempcomplex1;
      temparray[1]=tempcomplex1;
      temparray[2]=tempcomplex1;
      temparray[3]=tempcomplex1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(z,temparray,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","fft_complex_d1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Complex FFT: advanced example                                    |
//+------------------------------------------------------------------+
void TEST_FFT_Complex_D2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex z[];
   complex temparray[];
   complex tempcomplex1;
   complex tempcomplex2;
   complex tempcomplex3;
   complex cnan;
   complex cpositiveinfinity;
   complex cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- first we demonstrate forward FFT:
      //--- [0,1,0,1i] is converted to [1+1i,-1-1i,-1-1i,1+1i]
      tempcomplex1.re=0;
      tempcomplex1.im=1;
      //--- allocation
      ArrayResize(z,4);
      //--- initialization
      z[0]=0;
      z[1]=1;
      z[2]=0;
      z[3]=tempcomplex1;
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(z,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(z,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(z,cnegativeinfinity);
      //--- function call
      CAlglib::FFTC1D(z);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex2.re=1;
      tempcomplex2.im=1;
      tempcomplex3.re=-1;
      tempcomplex3.im=-1;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=tempcomplex2;
      temparray[1]=tempcomplex3;
      temparray[2]=tempcomplex3;
      temparray[3]=tempcomplex2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(z,temparray,0.0001);
      //--- now we convert result back with backward FFT
      CAlglib::FFTC1DInv(z);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=0;
      temparray[1]=1;
      temparray[2]=0;
      temparray[3]=tempcomplex1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(z,temparray,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","fft_complex_d2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Real FFT: simple example                                         |
//+------------------------------------------------------------------+
void TEST_FFT_Real_D1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double  x[];
   complex tempcarray[];
   double  temparray[];
   complex f[];
   double  x2[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- first we demonstrate forward FFT:
      //--- [1,1,1,1] is converted to [4,0,0,0]
      ArrayResize(x,4);
      //--- initialization
      x[0]=1;
      x[1]=1;
      x[2]=1;
      x[3]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::FFTR1D(x,f);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(tempcarray,4);
      //--- initialization
      tempcarray[0]=4;
      tempcarray[1]=0;
      tempcarray[2]=0;
      tempcarray[3]=0;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(f,tempcarray,0.0001);
      //--- now we convert [4,0,0,0] back to [1,1,1,1]
      //--- with backward FFT
      CAlglib::FFTR1DInv(f,x2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=1;
      temparray[1]=1;
      temparray[2]=1;
      temparray[3]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x2,temparray,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","fft_real_d1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Real FFT: advanced example                                       |
//+------------------------------------------------------------------+
void TEST_FFT_Real_D2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double  x[];
   double  temparray[];
   complex tempcarray[];
   complex f[];
   double  x2[];
   complex tempcomplex1;
   complex tempcomplex2;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- first we demonstrate forward FFT:
      //--- [1,2,3,4] is converted to [10,-2+2i,-2,-2-2i]
      //--- note that output array is self-adjoint:
      //--- * f[0]=conj(f[0])
      //--- * f[1]=conj(f[3])
      //--- * f[2]=conj(f[2])
      ArrayResize(x,4);
      //--- initialization
      x[0]=1;
      x[1]=2;
      x[2]=3;
      x[3]=4;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::FFTR1D(x,f);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex1.re=-2;
      tempcomplex1.im=2;
      tempcomplex2.re=-2;
      tempcomplex2.im=-2;
      //--- allocation
      ArrayResize(tempcarray,4);
      //--- initialization
      tempcarray[0]=10;
      tempcarray[1]=tempcomplex1;
      tempcarray[2]=-2;
      tempcarray[3]=tempcomplex2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(f,tempcarray,0.0001);
      //--- now we convert [10,-2+2i,-2,-2-2i] back to [1,2,3,4]
      CAlglib::FFTR1DInv(f,x2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=1;
      temparray[1]=2;
      temparray[2]=3;
      temparray[3]=4;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x2,temparray,0.0001);
      //--- remember that F is self-adjoint? It means that we can pass just half
      //--- (slightly larger than half) of F to inverse real FFT and still get our result.
      //--- I.e. instead [10,-2+2i,-2,-2-2i] we pass just [10,-2+2i,-2] and everything works!
      //--- NOTE: in this case we should explicitly pass array length (which is 4) to ALGLIB;
      //--- if not,it will automatically use array length to determine FFT size and
      //--- will erroneously make half-length FFT.
      ArrayResize(f,3);
      //--- initialization
      f[0]=10;
      f[1]=tempcomplex1;
      f[2]=-2;
      //--- function call
      CAlglib::FFTR1DInv(f,4,x2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=1;
      temparray[1]=2;
      temparray[2]=3;
      temparray[3]=4;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x2,temparray,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","fft_real_d2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| error detection in backward FFT                                  |
//+------------------------------------------------------------------+
void TEST_FFT_Complex_E1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex tempcomplex1;
   complex tempcomplex2;
   complex z[];
   complex temparray[];
   complex cnan;
   complex cpositiveinfinity;
   complex cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<3;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(z,4);
      //--- initialization
      z[0]=0;
      z[1]=2;
      z[2]=0;
      z[3]=-2;
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(z,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(z,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(z,cnegativeinfinity);
      //--- function call
      CAlglib::FFTC1DInv(z);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex1.re=0;
      tempcomplex1.im=1;
      tempcomplex2.re=0;
      tempcomplex2.im=-1;
      //--- allocation
      ArrayResize(temparray,4);
      //--- initialization
      temparray[0]=0;
      temparray[1]=tempcomplex1;
      temparray[2]=0;
      temparray[3]=tempcomplex2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex_Vector(z,temparray,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","fft_complex_e1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Integrating f=exp(x) by adaptive integrator                      |
//+------------------------------------------------------------------+
void TEST_AutoGK_D1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CObject              obj;
   CInt_Function_1_Func fint;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- This example demonstrates integration of f=exp(x) on [0,1]:
      //--- * first,autogkstate is initialized
      //--- * then we call integration function
      //--- * and finally we obtain results with autogkresults() call
      double a=0;
      //--- check
      if(_spoil_scenario==0)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==1)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==2)
         a=CInfOrNaN::NegativeInfinity();
      double b=1;
      //--- check
      if(_spoil_scenario==3)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         b=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CAutoGKStateShell s;
      double v;
      CAutoGKReportShell rep;
      //--- function call
      CAlglib::AutoGKSmooth(a,b,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::AutoGKIntegrate(s,fint,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::AutoGKResults(s,v,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,1.7182,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","autogk_d1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Interpolation and differentiation using barycentric              |
//| representation                                                   |
//+------------------------------------------------------------------+
void TEST_PolInt_D_CalcDiff(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- Here we demonstrate polynomial interpolation and differentiation
      //--- of y=x^2-x sampled at [0,1,2]. Barycentric representation of polynomial is used.
      ArrayResize(x,3);
      //--- initialization
      x[0]=0;
      x[1]=1;
      x[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      double t=-1;
      //--- check
      if(_spoil_scenario==10)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      double v;
      double dv;
      double d2v;
      CBarycentricInterpolantShell p;
      //--- barycentric model is created
      CAlglib::PolynomialBuild(x,y,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- barycentric interpolation is demonstrated
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      //--- barycentric differentation is demonstrated
      CAlglib::BarycentricDiff1(p,t,v,dv);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(dv,-3.0,0.00005);
      //--- second derivatives with barycentric representation
      CAlglib::BarycentricDiff1(p,t,v,dv);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(dv,-3.0,0.00005);
      //--- function call
      CAlglib::BarycentricDiff2(p,t,v,dv,d2v);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(dv,-3.0,0.00005);
      _TestResult=_TestResult && Doc_Test_Real(d2v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_d_calcdiff");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Conversion between power basis and barycentric representation    |
//+------------------------------------------------------------------+
void TEST_PolInt_D_Conv(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double a[];
   double temparray[];
   double a2[];
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- Here we demonstrate conversion of y=x^2-x
      //--- between power basis and barycentric representation.
      ArrayResize(a,3);
      //--- initialization
      a[0]=0;
      a[1]=-1;
      a[2]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(a,CInfOrNaN::NegativeInfinity());
      double t=2;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      CBarycentricInterpolantShell p;
      //--- a=[0,-1,+1] is decomposition of y=x^2-x in the power basis:
      //---     y=0 - 1*x + 1*x^2
      //--- We convert it to the barycentric form.
      CAlglib::PolynomialPow2Bar(a,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- now we have barycentric interpolation;we can use it for interpolation
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.005);
      //--- we can also convert back from barycentric representation to power basis
      CAlglib::PolynomialBar2Pow(p,a2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,3);
      //--- initialization
      temparray[0]=0;
      temparray[1]=-1;
      temparray[2]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(a2,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_d_conv");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation on special grids (equidistant,          |
//| Chebyshev I/II)                                                  |
//+------------------------------------------------------------------+
void TEST_PolInt_D_Spec(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y_eqdist[];
   double y_cheb1[];
   double y_cheb2[];
   double a_eqdist[];
   double a_cheb1[];
   double a_cheb2[];
   double temparray[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<11;_spoil_scenario++)
     {
      //--- Temporaries:
      //--- * values of y=x^2-x sampled at three special grids:
      //---   * equdistant grid spanning [0,2],  x[i]=2*i/(N-1),i=0..N-1
      //---   * Chebyshev-I grid spanning [-1,+1],x[i]=1 + Cos(PI*(2*i+1)/(2*n)),i=0..N-1
      //---   * Chebyshev-II grid spanning [-1,+1],x[i]=1 + Cos(PI*i/(n-1)),i=0..N-1
      //--- * barycentric interpolants for these three grids
      //--- * vectors to store coefficients of quadratic representation
      ArrayResize(y_eqdist,3);
      //--- initialization
      y_eqdist[0]=0;
      y_eqdist[1]=0;
      y_eqdist[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y_eqdist,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y_eqdist,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y_eqdist,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(y_cheb1,3);
      //--- initialization
      y_cheb1[0]=-0.116025;
      y_cheb1[1]=0;
      y_cheb1[2]=1.616025;
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Value(y_cheb1,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y_cheb1,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y_cheb1,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(y_cheb2,3);
      //--- initialization
      y_cheb2[0]=0;
      y_cheb2[1]=0;
      y_cheb2[2]=2;
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y_cheb2,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y_cheb2,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Value(y_cheb2,CInfOrNaN::NegativeInfinity());
      //--- create variables
      CBarycentricInterpolantShell p_eqdist;
      CBarycentricInterpolantShell p_cheb1;
      CBarycentricInterpolantShell p_cheb2;
      //--- First,we demonstrate construction of barycentric interpolants on
      //--- special grids. We unpack power representation to ensure that
      //--- interpolant was built correctly.
      //--- In all three cases we should get same quadratic function.
      CAlglib::PolynomialBuildEqDist(0.0,2.0,y_eqdist,p_eqdist);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::PolynomialBar2Pow(p_eqdist,a_eqdist);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,3);
      //--- initialization
      temparray[0]=0;
      temparray[1]=-1;
      temparray[2]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(a_eqdist,temparray,0.00005);
      //--- function call
      CAlglib::PolynomialBuildCheb1(-1,+1,y_cheb1,p_cheb1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::PolynomialBar2Pow(p_cheb1,a_cheb1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,3);
      //--- initialization
      temparray[0]=0;
      temparray[1]=-1;
      temparray[2]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(a_cheb1,temparray,0.00005);
      //--- function call
      CAlglib::PolynomialBuildCheb2(-1,+1,y_cheb2,p_cheb2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::PolynomialBar2Pow(p_cheb2,a_cheb2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,3);
      //--- initialization
      temparray[0]=0;
      temparray[1]=-1;
      temparray[2]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(a_cheb2,temparray,0.00005);
      //--- Now we demonstrate polynomial interpolation withconstruction 
      //--- of the barycentricinterpolant structure.
      //--- We calculate interpolant value at x=-2.
      //--- In all three cases we should get same f=6
      double t=-2;
      //--- check
      if(_spoil_scenario==9)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==10)
         t=CInfOrNaN::NegativeInfinity();
      double v;
      //--- function call
      v=CAlglib::PolynomialCalcEqDist(0.0,2.0,y_eqdist,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      //--- function call
      v=CAlglib::PolynomialCalcCheb1(-1,+1,y_cheb1,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      //--- function call
      v=CAlglib::PolynomialCalcCheb2(-1,+1,y_cheb2,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_d_spec");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<10;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,3);
      //--- initialization
      x[0]=0;
      x[1]=1;
      x[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y);
      double t=-1;
      //--- check
      if(_spoil_scenario==8)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CBarycentricInterpolantShell p;
      double v;
      //--- function call
      CAlglib::PolynomialBuild(x,y,3,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      t=-1;
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         t=CInfOrNaN::NegativeInfinity();
      CBarycentricInterpolantShell p;
      //--- function call
      CAlglib::PolynomialBuildEqDist(0.0,2.0,y,3,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_3(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=-0.116025;
      y[1]=0;
      y[2]=1.616025;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      t=-1;
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         t=CInfOrNaN::NegativeInfinity();
      CBarycentricInterpolantShell p;
      //--- function call
      CAlglib::PolynomialBuildCheb1(-1.0,+1.0,y,3,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- create a variable
      double v;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_3");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_4(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double a;
   double b;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      t=-2;
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         t=CInfOrNaN::NegativeInfinity();
      a=-1;
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         a=CInfOrNaN::NegativeInfinity();
      b=1;
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         b=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      CBarycentricInterpolantShell p;
      //--- function call
      CAlglib::PolynomialBuildCheb2(a,b,y,3,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_4");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_5(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      t=-1;
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         t=CInfOrNaN::NegativeInfinity();
      //--- function call
      v=CAlglib::PolynomialCalcEqDist(0.0,2.0,y,3,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_5");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_6(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double a;
   double b;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=-0.116025;
      y[1]=0;
      y[2]=1.616025;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      t=-1;
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         t=CInfOrNaN::NegativeInfinity();
      a=-1;
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         a=CInfOrNaN::NegativeInfinity();
      b=1;
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         b=CInfOrNaN::NegativeInfinity();
      //--- function call
      v=CAlglib::PolynomialCalcCheb1(a,b,y,3,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_6");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation,full list of parameters.                |
//+------------------------------------------------------------------+
void TEST_PolInt_T_7(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double a;
   double b;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(y);
      t=-2;
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         t=CInfOrNaN::NegativeInfinity();
      a=-1;
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         a=CInfOrNaN::NegativeInfinity();
      b=1;
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         b=CInfOrNaN::NegativeInfinity();
      //--- function call
      v=CAlglib::PolynomialCalcCheb2(a,b,y,3,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_7");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation: y=x^2-x,equidistant grid, barycentric  |
//| form                                                             |
//+------------------------------------------------------------------+
void TEST_PolInt_T_8(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      t=-1;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      CBarycentricInterpolantShell p;
      //--- function call
      CAlglib::PolynomialBuildEqDist(0.0,2.0,y,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_8");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation: y=x^2-x, Chebyshev grid (first kind),  |
//| barycentric form                                                 |
//+------------------------------------------------------------------+
void TEST_PolInt_T_9(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double a;
   double b;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<11;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=-0.116025;
      y[1]=0;
      y[2]=1.616025;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      t=-1;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      a=-1;
      //--- check
      if(_spoil_scenario==5)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::NegativeInfinity();
      b=1;
      //--- check
      if(_spoil_scenario==8)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      CBarycentricInterpolantShell p;
      //--- function call
      CAlglib::PolynomialBuildCheb1(a,b,y,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_9");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation: y=x^2-x, Chebyshev grid (second kind), |
//| barycentric form                                                 |
//+------------------------------------------------------------------+
void TEST_PolInt_T_10(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double a;
   double b;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<11;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      t=-2;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      a=-1;
      //--- check
      if(_spoil_scenario==5)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::NegativeInfinity();
      b=1;
      //--- check
      if(_spoil_scenario==8)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      CBarycentricInterpolantShell p;
      //--- function call
      CAlglib::PolynomialBuildCheb2(a,b,y,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_10");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation: y=x^2-x,equidistant grid               |
//+------------------------------------------------------------------+
void TEST_PolInt_T_11(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      t=-1;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      //--- function call
      v=CAlglib::PolynomialCalcEqDist(0.0,2.0,y,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_11");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation: y=x^2-x,Chebyshev grid (first kind)    |
//+------------------------------------------------------------------+
void TEST_PolInt_T_12(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
   double t;
   double a;
   double b;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<11;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=-0.116025;
      y[1]=0;
      y[2]=1.616025;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      t=-1;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      a=-1;
      //--- check
      if(_spoil_scenario==5)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::NegativeInfinity();
      b=+1;
      //--- check
      if(_spoil_scenario==8)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::NegativeInfinity();
      //--- function call
      v=CAlglib::PolynomialCalcCheb1(a,b,y,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_12");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial interpolation: y=x^2-x,Chebyshev grid (second kind)   |
//+------------------------------------------------------------------+
void TEST_PolInt_T_13(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double y[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<11;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(y,3);
      //--- initialization
      y[0]=0;
      y[1]=0;
      y[2]=2;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      double t=-2;
      //--- check
      if(_spoil_scenario==3)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==4)
         t=CInfOrNaN::NegativeInfinity();
      double a=-1;
      //--- check
      if(_spoil_scenario==5)
         a=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==6)
         a=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==7)
         a=CInfOrNaN::NegativeInfinity();
      double b=+1;
      //--- check
      if(_spoil_scenario==8)
         b=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==9)
         b=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==10)
         b=CInfOrNaN::NegativeInfinity();
      double v;
      //--- function call
      v=CAlglib::PolynomialCalcCheb2(a,b,y,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,6.0,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","polint_t_13");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Piecewise linear spline interpolation                            |
//+------------------------------------------------------------------+
void TEST_Spline1D_D_Linear(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double t;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- We use piecewise linear spline to interpolate f(x)=x^2 sampled 
      //--- at 5 equidistant nodes on [-1,+1].
      ArrayResize(x,5);
      //--- initialization
      x[0]=-1;
      x[1]=-0.5;
      x[2]=0;
      x[3]=0.5;
      x[4]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,5);
      //--- initialization
      y[0]=1;
      y[1]=0.25;
      y[2]=0;
      y[3]=0.25;
      y[4]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      t=0.25;
      //--- check
      if(_spoil_scenario==10)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         t=CInfOrNaN::NegativeInfinity();
      //--- create a variable
      CSpline1DInterpolantShell s;
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- build spline
      CAlglib::Spline1DBuildLinear(x,y,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- calculate S(0.25) - it is quite different from 0.25^2=0.0625
      v=CAlglib::Spline1DCalc(s,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,0.125,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","spline1d_d_linear");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Cubic spline interpolation                                       |
//+------------------------------------------------------------------+
void TEST_Spline1D_D_Cubic(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<10;_spoil_scenario++)
     {
      //--- We use cubic spline to interpolate f(x)=x^2 sampled 
      //--- at 5 equidistant nodes on [-1,+1].
      //--- First,we use default boundary conditions ("parabolically terminated
      //--- spline") because cubic spline built with such boundary conditions 
      //--- will exactly reproduce any quadratic f(x).
      //--- Then we try to use natural boundary conditions
      //---     d2S(-1)/dx^2=0.0
      //---     d2S(+1)/dx^2=0.0
      //--- and see that such spline interpolated f(x) with small error.
      ArrayResize(x,5);
      //--- initialization
      x[0]=-1;
      x[1]=-0.5;
      x[2]=0;
      x[3]=0.5;
      x[4]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,5);
      //--- initialization
      y[0]=1;
      y[1]=0.25;
      y[2]=0;
      y[3]=0.25;
      y[4]=1;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y);
      double t=0.25;
      //--- check
      if(_spoil_scenario==8)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      double v;
      CSpline1DInterpolantShell s;
      int natural_bound_type=2;
      //--- Test exact boundary conditions: build S(x),calculare S(0.25)
      //--- (almost same as original function)
      CAlglib::Spline1DBuildCubic(x,y,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::Spline1DCalc(s,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,0.0625,0.00001);
      //--- Test natural boundary conditions: build S(x),calculare S(0.25)
      //--- (small interpolation error)
      CAlglib::Spline1DBuildCubic(x,y,5,natural_bound_type,0.0,natural_bound_type,0.0,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::Spline1DCalc(s,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,0.0580,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","spline1d_d_cubic");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Differentiation on the grid using cubic splines                  |
//+------------------------------------------------------------------+
void TEST_Spline1D_D_GridDiff(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double d1[];
   double d2[];
   double temparray1[];
   double temparray2[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<10;_spoil_scenario++)
     {
      //--- We use cubic spline to do grid differentiation,i.e. having
      //--- values of f(x)=x^2 sampled at 5 equidistant nodes on [-1,+1]
      //--- we calculate derivatives of cubic spline at nodes WITHOUT
      //--- CONSTRUCTION OF SPLINE OBJECT.
      //--- There are efficient functions spline1dgriddiffcubic() and
      //--- spline1dgriddiff2cubic() for such calculations.
      //--- We use default boundary conditions ("parabolically terminated
      //--- spline") because cubic spline built with such boundary conditions 
      //--- will exactly reproduce any quadratic f(x).
      //--- Actually,we could use natural conditions,but we feel that 
      //--- spline which exactly reproduces f() will show us more 
      //--- understandable results.
      ArrayResize(x,5);
      //--- initialization
      x[0]=-1;
      x[1]=-0.5;
      x[2]=0;
      x[3]=0.5;
      x[4]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,5);
      //--- initialization
      y[0]=1;
      y[1]=0.25;
      y[2]=0;
      y[3]=0.25;
      y[4]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- We calculate first derivatives: they must be equal to 2*x
      CAlglib::Spline1DGridDiffCubic(x,y,d1);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray1,5);
      //--- initialization
      temparray1[0]=-2;
      temparray1[1]=-1;
      temparray1[2]=0;
      temparray1[3]=1;
      temparray1[4]=2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(d1,temparray1,0.0001);
      //--- Now test griddiff2,which returns first AND second derivatives.
      //--- First derivative is 2*x,second is equal to 2.0
      CAlglib::Spline1DGridDiff2Cubic(x,y,d1,d2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray2,5);
      //--- initialization
      temparray2[0]=2;
      temparray2[1]=2;
      temparray2[2]=2;
      temparray2[3]=2;
      temparray2[4]=2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(d1,temparray1,0.0001);
      _TestResult=_TestResult && Doc_Test_Real_Vector(d2,temparray2,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","spline1d_d_griddiff");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Resampling using cubic splines                                   |
//+------------------------------------------------------------------+
void TEST_Spline1D_D_ConvDiff(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x_old[];
   double y_old[];
   double x_new[];
   double y_new[];
   double d1_new[];
   double d2_new[];
   double temparray1[];
   double temparray2[];
   double temparray3[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<11;_spoil_scenario++)
     {
      //--- We use cubic spline to do resampling,i.e. having
      //--- values of f(x)=x^2 sampled at 5 equidistant nodes on [-1,+1]
      //--- we calculate values/derivatives of cubic spline on 
      //--- another grid (equidistant with 9 nodes on [-1,+1])
      //--- WITHOUT CONSTRUCTION OF SPLINE OBJECT.
      //--- There are efficient functions spline1dconvcubic(),
      //--- spline1dconvdiffcubic() and spline1dconvdiff2cubic() 
      //--- for such calculations.
      //--- We use default boundary conditions ("parabolically terminated
      //--- spline") because cubic spline built with such boundary conditions 
      //--- will exactly reproduce any quadratic f(x).
      //--- Actually,we could use natural conditions,but we feel that 
      //--- spline which exactly reproduces f() will show us more 
      //--- understandable results.
      ArrayResize(x_old,5);
      //--- initialization
      x_old[0]=-1;
      x_old[1]=-0.5;
      x_old[2]=0;
      x_old[3]=0.5;
      x_old[4]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x_old,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x_old,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x_old,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(x_old);
      //--- allocation
      ArrayResize(y_old,5);
      //--- initialization
      y_old[0]=1;
      y_old[1]=0.25;
      y_old[2]=0;
      y_old[3]=0.25;
      y_old[4]=1;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y_old,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y_old,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y_old,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y_old);
      //--- allocation
      ArrayResize(x_new,9);
      //--- initialization
      x_new[0]=-1;
      x_new[1]=-0.75;
      x_new[2]=-0.5;
      x_new[3]=-0.25;
      x_new[4]=0;
      x_new[5]=0.25;
      x_new[6]=0.5;
      x_new[7]=0.75;
      x_new[8]=1;
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Value(x_new,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(x_new,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(x_new,CInfOrNaN::NegativeInfinity());
      //--- First,conversion withdifferentiation.
      CAlglib::Spline1DConvCubic(x_old,y_old,x_new,y_new);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray1,9);
      //--- initialization
      temparray1[0]=1;
      temparray1[1]=0.5625;
      temparray1[2]=0.25;
      temparray1[3]=0.0625;
      temparray1[4]=0;
      temparray1[5]=0.0625;
      temparray1[6]=0.25;
      temparray1[7]=0.5625;
      temparray1[8]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(y_new,temparray1,0.0001);
      //--- Then,conversion with differentiation (first derivatives only)
      CAlglib::Spline1DConvDiffCubic(x_old,y_old,x_new,y_new,d1_new);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray2,9);
      //--- initialization
      temparray2[0]=-2;
      temparray2[1]=-1.5;
      temparray2[2]=-1;
      temparray2[3]=-0.5;
      temparray2[4]=0;
      temparray2[5]=0.5;
      temparray2[6]=1.0;
      temparray2[7]=1.5;
      temparray2[8]=2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(y_new,temparray1,0.0001);
      _TestResult=_TestResult && Doc_Test_Real_Vector(d1_new,temparray2,0.0001);
      //--- Finally,conversion with first and second derivatives
      CAlglib::Spline1DConvDiff2Cubic(x_old,y_old,x_new,y_new,d1_new,d2_new);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray3,9);
      //--- initialization
      temparray3[0]=2;
      temparray3[1]=2;
      temparray3[2]=2;
      temparray3[3]=2;
      temparray3[4]=2;
      temparray3[5]=2;
      temparray3[6]=2;
      temparray3[7]=2;
      temparray3[8]=2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(y_new,temparray1,0.0001);
      _TestResult=_TestResult && Doc_Test_Real_Vector(d1_new,temparray2,0.0001);
      _TestResult=_TestResult && Doc_Test_Real_Vector(d2_new,temparray3,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","spline1d_d_convdiff");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Unconstrained dense quadratic programming                        |
//+------------------------------------------------------------------+
void TEST_MinQP_D_U1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   double b[];
   double x0[];
   double x[];
   double temparray[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<13;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=x0^2 + x1^2 -6*x0 - 4*x1
      //--- Exact solution is [x0,x1]=[3,2]
      //--- We provide algorithm with starting point,although in this case
      //--- (dense matrix,no constraints) it can work withsuch information.
      //--- IMPORTANT: this solver minimizes  following  function:
      //---     f(x)=0.5*x'*A*x + b'*x.
      //--- Note that quadratic term has 0.5 before it. So if you want to minimize
      //--- quadratic function,you should rewrite it in such way that quadratic term
      //--- is multiplied by 0.5 too.
      //--- For example,our function is f(x)=x0^2+x1^2+...,but we rewrite it as 
      //---     f(x)=0.5*(2*x0^2+2*x1^2) + ....
      //--- and pass diag(2,2) as quadratic term - NOT diag(1,1)!
      a.Resize(2,2);
      //--- initialization
      a[0].Set(0,2);
      a[0].Set(1,0);
      a[1].Set(0,0);
      a[1].Set(1,2);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(a);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(a);
      //--- allocation
      ArrayResize(b,2);
      //--- initialization
      b[0]=-6;
      b[1]=-4;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Deleting_Element(b);
      //--- allocation
      ArrayResize(x0,2);
      //--- initialization
      x0[0]=0;
      x0[1]=1;
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(x0,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(x0,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(x0,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Deleting_Element(x0);
      //--- create variables
      CMinQPStateShell state;
      CMinQPReportShell rep;
      //--- function call
      CAlglib::MinQPCreate(2,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetQuadraticTerm(state,a);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetLinearTerm(state,b);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetStartingPoint(state,x0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPOptimize(state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=3;
      temparray[1]=2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minqp_d_u1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Constrained dense quadratic programming                          |
//+------------------------------------------------------------------+
void TEST_MinQP_D_BC1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble a;
   double b[];
   double x0[];
   double bndl[];
   double bndu[];
   double temparray[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<17;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=x0^2 + x1^2 -6*x0 - 4*x1
      //--- subject to bound constraints 0<=x0<=2.5,0<=x1<=2.5
      //--- Exact solution is [x0,x1]=[2.5,2]
      //--- We provide algorithm with starting point. With such small problem good starting
      //--- point is not really necessary,but with high-dimensional problem it can save us
      //--- a lot of time.
      //--- IMPORTANT: this solver minimizes  following  function:
      //---     f(x)=0.5*x'*A*x + b'*x.
      //--- Note that quadratic term has 0.5 before it. So if you want to minimize
      //--- quadratic function,you should rewrite it in such way that quadratic term
      //--- is multiplied by 0.5 too.
      //--- For example,our function is f(x)=x0^2+x1^2+...,but we rewrite it as 
      //---     f(x)=0.5*(2*x0^2+2*x1^2) + ....
      //--- and pass diag(2,2) as quadratic term - NOT diag(1,1)!
      a.Resize(2,2);
      //--- initialization
      a[0].Set(0,2);
      a[0].Set(1,0);
      a[1].Set(0,0);
      a[1].Set(1,2);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(a,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(a,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(a);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(a);
      //--- allocation
      ArrayResize(b,2);
      //--- initialization
      b[0]=-6;
      b[1]=-4;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Deleting_Element(b);
      //--- allocation
      ArrayResize(x0,2);
      //--- initialization
      x0[0]=0;
      x0[1]=1;
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(x0,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(x0,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(x0,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Deleting_Element(x0);
      //--- allocation
      ArrayResize(bndl,2);
      //--- initialization
      bndl[0]=0;
      bndl[1]=0;
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Value(bndl,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Deleting_Element(bndl);
      //--- allocation
      ArrayResize(bndu,2);
      //--- initialization
      bndu[0]=2.5;
      bndu[1]=2.5;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(bndu,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Deleting_Element(bndu);
      double x[];
      CMinQPStateShell state;
      CMinQPReportShell rep;
      //--- function call
      CAlglib::MinQPCreate(2,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetQuadraticTerm(state,a);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetLinearTerm(state,b);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetStartingPoint(state,x0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPSetBC(state,bndl,bndu);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPOptimize(state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinQPResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=2.5;
      temparray[1]=2;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minqp_d_bc1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear least squares optimization using function vector only  |
//+------------------------------------------------------------------+
void TEST_MinLM_D_V(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_FVec1 ffvec;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=f0^2+f1^2,where 
      //---     f0(x0,x1)=10*(x0+3)^2
      //---     f1(x0,x1)=(x1-3)^2
      //--- using "V" mode of the Levenberg-Marquardt optimizer.
      //--- Optimization algorithm uses:
      //--- * function vector f[]={f1,f2}
      //--- No other information (Jacobian,gradient,etc.) is needed.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      int maxits=0;
      //--- objects of classes
      CMinLMStateShell  state;
      CMinLMReportShell rep;
      //--- function call
      CAlglib::MinLMCreateV(2,x,0.0001,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffvec,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_d_v");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear least squares optimization using function vector and   |
//| Jacobian                                                         |
//+------------------------------------------------------------------+
void TEST_MinLM_D_VJ(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
//--- TEST minlm_d_vj
//---      Nonlinear least squares optimization using function vector and Jacobian
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_FVec1 ffvec;
   CNDimensional_Jac1  fjac;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=f0^2+f1^2,where 
      //---     f0(x0,x1)=10*(x0+3)^2
      //---     f1(x0,x1)=(x1-3)^2
      //--- using "VJ" mode of the Levenberg-Marquardt optimizer.
      //--- Optimization algorithm uses:
      //--- * function vector f[]={f1,f2}
      //--- * Jacobian matrix J={dfi/dxj}.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLMStateShell state;
      CMinLMReportShell rep;
      //--- function call
      CAlglib::MinLMCreateVJ(2,x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffvec,fjac,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_d_vj");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear Hessian-based optimization for general functions       |
//+------------------------------------------------------------------+
void TEST_MinLM_D_FGH(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Func1 ffunc;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Hess1 fhess;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=100*(x0+3)^4+(x1-3)^4
      //--- using "FGH" mode of the Levenberg-Marquardt optimizer.
      //--- F is treated like a monolitic function withinternal structure,
      //--- i.e. we do NOT represent it as a sum of squares.
      //--- Optimization algorithm uses:
      //--- * function value F(x0,x1)
      //--- * gradient G={dF/dxi}
      //--- * Hessian H={d2F/(dxi*dxj)}
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLMStateShell state;
      CMinLMReportShell rep;
      //--- function call
      CAlglib::MinLMCreateFGH(x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffunc,fgrad,fhess,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_d_fgh");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Bound constrained nonlinear least squares optimization           |
//+------------------------------------------------------------------+
void TEST_MinLM_D_VB(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              bndl[];
   double              bndu[];
   double              temparray[];
   CObject             obj;
   CNDimensional_FVec1 ffvec;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<16;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=f0^2+f1^2,where 
      //---     f0(x0,x1)=10*(x0+3)^2
      //---     f1(x0,x1)=(x1-3)^2
      //--- with boundary constraints
      //---     -1 <= x0 <= +1
      //---     -1 <= x1 <= +1
      //--- using "V" mode of the Levenberg-Marquardt optimizer.
      //--- Optimization algorithm uses:
      //--- * function vector f[]={f1,f2}
      //--- No other information (Jacobian,gradient,etc.) is needed.
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(bndl,2);
      //--- initialization
      bndl[0]=-1;
      bndl[1]=-1;
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Value(bndl,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(bndl);
      //--- allocation
      ArrayResize(bndu,2);
      //--- initialization
      bndu[0]=1;
      bndu[1]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(bndu,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Deleting_Element(bndu);
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==7)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==8)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==10)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==11)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==12)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==13)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLMStateShell state;
      CMinLMReportShell rep;
      //--- function call
      CAlglib::MinLMCreateV(2,x,0.0001,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetBC(state,bndl,bndu);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffvec,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-1;
      temparray[1]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_d_vb");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Efficient restarts of LM optimizer                               |
//+------------------------------------------------------------------+
void TEST_MinLM_D_Restarts(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_FVec1 ffvec1;
   CNDimensional_FVec2 ffvec2;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<15;_spoil_scenario++)
     {
      //--- This example demonstrates minimization of F(x0,x1)=f0^2+f1^2,where 
      //---     f0(x0,x1)=10*(x0+3)^2
      //---     f1(x0,x1)=(x1-3)^2
      //--- using several starting points and efficient restarts.
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==0)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==1)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==2)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==3)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==6)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLMStateShell state;
      CMinLMReportShell rep;
      //--- create optimizer using minlmcreatev()
      ArrayResize(x,2);
      //--- initialization
      x[0]=10;
      x[1]=10;
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::MinLMCreateV(2,x,0.0001,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffvec1,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      //--- restart optimizer using minlmrestartfrom()
      //--- we can use different starting point,different function,
      //--- different stopping conditions,but problem size
      //--- must remain unchanged.
      ArrayResize(x,2);
      //--- initialization
      x[0]=4;
      x[1]=4;
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- function call
      CAlglib::MinLMRestartFrom(state,x);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffvec2,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=0;
      temparray[1]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_d_restarts");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear least squares optimization, FJ scheme (obsolete, but   |
//| supported)                                                       |
//+------------------------------------------------------------------+
void TEST_MinLM_T_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double              x[];
   double              temparray[];
   CObject             obj;
   CNDimensional_Func1 ffunc;
   CNDimensional_Jac1  fjac;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      int maxits=0;
      CMinLMStateShell state;
      CMinLMReportShell rep;
      //--- function call
      CAlglib::MinLMCreateFJ(2,x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffunc,fjac,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_t_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear least squares optimization, FGJ scheme (obsolete, but  |
//| supported)                                                       |
//+------------------------------------------------------------------+
void TEST_MinLM_T_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double  x[];
   double  temparray[];
   CObject obj;
   CNDimensional_Func1 ffunc;
   CNDimensional_Grad1 fgrad;
   CNDimensional_Jac1  fjac;
   CNDimensional_Rep   frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<12;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,2);
      //--- initialization
      x[0]=0;
      x[1]=0;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      double epsg=0.0000000001;
      //--- check
      if(_spoil_scenario==3)
         epsg=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==4)
         epsg=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==5)
         epsg=CInfOrNaN::NegativeInfinity();
      double epsf=0;
      //--- check
      if(_spoil_scenario==6)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==7)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==8)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0;
      //--- check
      if(_spoil_scenario==9)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==10)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      CMinLMStateShell state;
      CMinLMReportShell rep;
      //--- function call
      CAlglib::MinLMCreateFGJ(2,x,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMSetCond(state,epsg,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMOptimize(state,ffunc,fgrad,fjac,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::MinLMResults(state,x,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=-3;
      temparray[1]=3;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(rep.GetTerminationType(),4);
      _TestResult=_TestResult && Doc_Test_Real_Vector(x,temparray,0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","minlm_t_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear fitting using function value only                      |
//+------------------------------------------------------------------+
void TEST_LSFit_D_NLF(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble           x;
   double                  y[];
   double                  c[];
   double                  temparray[];
   double                  w[];
   CObject                 obj;
   CNDimensional_CX_1_Func fcx1func;
   CNDimensional_Rep       frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<27;_spoil_scenario++)
     {
      //--- In this example we demonstrate exponential fitting
      //--- by f(x)=exp(-c*x^2)
      //--- using function value only.
      //--- Gradient is estimated using combination of numerical differences
      //--- and secant updates. diffstep variable stores differentiation step 
      //--- (we have to tell algorithm what step to use).
      x.Resize(11,1);
      //--- initialization
      x[0].Set(0,-1);
      x[1].Set(0,-0.8);
      x[2].Set(0,-0.6);
      x[3].Set(0,-0.4);
      x[4].Set(0,-0.2);
      x[5].Set(0,0);
      x[6].Set(0,0.2);
      x[7].Set(0,0.4);
      x[8].Set(0,0.6);
      x[9].Set(0,0.8);
      x[10].Set(0,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0.22313;
      y[1]=0.382893;
      y[2]=0.582748;
      y[3]=0.786628;
      y[4]=0.941765;
      y[5]=1;
      y[6]=0.941765;
      y[7]=0.786628;
      y[8]=0.582748;
      y[9]=0.382893;
      y[10]=0.22313;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(c,1);
      //--- initialization
      c[0]=0.3;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(c,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(c,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(c,CInfOrNaN::NegativeInfinity());
      double epsf=0;
      //--- check
      if(_spoil_scenario==13)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0.000001;
      //--- check
      if(_spoil_scenario==16)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      int info;
      CLSFitStateShell state;
      CLSFitReportShell rep;
      double diffstep=0.0001;
      //--- check
      if(_spoil_scenario==19)
         diffstep=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==20)
         diffstep=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==21)
         diffstep=CInfOrNaN::NegativeInfinity();
      //--- Fitting withweights
      CAlglib::LSFitCreateF(x,y,c,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      //--- Fitting with weights
      //--- (you can change weights and see how it changes result)
      ArrayResize(w,11);
      //--- initialization
      w[0]=1;
      w[1]=1;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      w[6]=1;
      w[7]=1;
      w[8]=1;
      w[9]=1;
      w[10]=1;
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==24)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==25)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==26)
         Spoil_Vector_By_Deleting_Element(w);
      //--- function call
      CAlglib::LSFitCreateWF(x,y,w,c,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_nlf");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear fitting using gradient                                 |
//+------------------------------------------------------------------+
void TEST_LSFit_D_NLFG(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble           x;
   double                  y[];
   double                  c[];
   double                  temparray[];
   double                  w[];
   CObject                 obj;
   CNDimensional_CX_1_Func fcx1func;
   CNDimensional_CX_1_Grad fcx1grad;
   CNDimensional_Rep       frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<24;_spoil_scenario++)
     {
      //--- In this example we demonstrate exponential fitting
      //--- by f(x)=exp(-c*x^2)
      //--- using function value and gradient (with respect to c).
      x.Resize(11,1);
      //--- initialization
      x[0].Set(0,-1);
      x[1].Set(0,-0.8);
      x[2].Set(0,-0.6);
      x[3].Set(0,-0.4);
      x[4].Set(0,-0.2);
      x[5].Set(0,0);
      x[6].Set(0,0.2);
      x[7].Set(0,0.4);
      x[8].Set(0,0.6);
      x[9].Set(0,0.8);
      x[10].Set(0,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0.22313;
      y[1]=0.382893;
      y[2]=0.582748;
      y[3]=0.786628;
      y[4]=0.941765;
      y[5]=1;
      y[6]=0.941765;
      y[7]=0.786628;
      y[8]=0.582748;
      y[9]=0.382893;
      y[10]=0.22313;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(c,1);
      //--- initialization
      c[0]=0.3;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(c,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(c,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(c,CInfOrNaN::NegativeInfinity());
      double epsf=0;
      //--- check
      if(_spoil_scenario==13)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0.000001;
      //--- check
      if(_spoil_scenario==16)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      int info;
      CLSFitStateShell state;
      CLSFitReportShell rep;
      //--- Fitting withweights
      CAlglib::LSFitCreateFG(x,y,c,true,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,fcx1grad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      //--- Fitting with weights
      //--- (you can change weights and see how it changes result)
      ArrayResize(w,11);
      //--- initialization
      w[0]=1;
      w[1]=1;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      w[6]=1;
      w[7]=1;
      w[8]=1;
      w[9]=1;
      w[10]=1;
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==21)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Deleting_Element(w);
      //--- function call
      CAlglib::LSFitCreateWFG(x,y,w,c,true,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,fcx1grad,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_nlfg");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear fitting using gradient and Hessian                     |
//+------------------------------------------------------------------+
void TEST_LSFit_D_NLFGH(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble           x;
   double                  y[];
   double                  c[];
   double                  temparray[];
   double                  w[];
   CObject                 obj;
   CNDimensional_CX_1_Func fcx1func;
   CNDimensional_CX_1_Grad fcx1grad;
   CNDimensional_CX_1_Hess fcx1hess;
   CNDimensional_Rep       frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<24;_spoil_scenario++)
     {
      //--- In this example we demonstrate exponential fitting
      //--- by f(x)=exp(-c*x^2)
      //--- using function value,gradient and Hessian (with respect to c)
      x.Resize(11,1);
      //--- initialization
      x[0].Set(0,-1);
      x[1].Set(0,-0.8);
      x[2].Set(0,-0.6);
      x[3].Set(0,-0.4);
      x[4].Set(0,-0.2);
      x[5].Set(0,0);
      x[6].Set(0,0.2);
      x[7].Set(0,0.4);
      x[8].Set(0,0.6);
      x[9].Set(0,0.8);
      x[10].Set(0,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0.22313;
      y[1]=0.382893;
      y[2]=0.582748;
      y[3]=0.786628;
      y[4]=0.941765;
      y[5]=1;
      y[6]=0.941765;
      y[7]=0.786628;
      y[8]=0.582748;
      y[9]=0.382893;
      y[10]=0.22313;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(c,1);
      //--- initialization
      c[0]=0.3;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(c,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(c,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(c,CInfOrNaN::NegativeInfinity());
      double epsf=0;
      //--- check
      if(_spoil_scenario==13)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0.000001;
      //--- check
      if(_spoil_scenario==16)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      int info;
      CLSFitStateShell state;
      CLSFitReportShell rep;
      //--- Fitting withweights
      CAlglib::LSFitCreateFGH(x,y,c,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,fcx1grad,fcx1hess,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      //--- Fitting with weights
      //--- (you can change weights and see how it changes result)
      ArrayResize(w,11);
      //--- initialization
      w[0]=1;
      w[1]=1;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      w[6]=1;
      w[7]=1;
      w[8]=1;
      w[9]=1;
      w[10]=1;
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==21)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Deleting_Element(w);
      //--- function call
      CAlglib::LSFitCreateWFGH(x,y,w,c,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,fcx1grad,fcx1hess,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_nlfgh");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Bound contstrained nonlinear fitting using function value only   |
//+------------------------------------------------------------------+
void TEST_LSFit_D_NLFB(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble           x;
   double                  y[];
   double                  c[];
   double                  temparray[];
   double                  w[];
   double                  bndl[];
   double                  bndu[];
   CObject                 obj;
   CNDimensional_CX_1_Func fcx1func;
   CNDimensional_Rep       frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<26;_spoil_scenario++)
     {
      //--- In this example we demonstrate exponential fitting by
      //---     f(x)=exp(-c*x^2)
      //--- subject to bound constraints
      //---     0.0 <= c <= 1.0
      //--- using function value only.
      //--- Gradient is estimated using combination of numerical differences
      //--- and secant updates. diffstep variable stores differentiation step 
      //--- (we have to tell algorithm what step to use).
      //--- Unconstrained solution is c=1.5,but because of constraints we should
      //--- get c=1.0 (at the boundary).
      x.Resize(11,1);
      //--- initialization
      x[0].Set(0,-1);
      x[1].Set(0,-0.8);
      x[2].Set(0,-0.6);
      x[3].Set(0,-0.4);
      x[4].Set(0,-0.2);
      x[5].Set(0,0);
      x[6].Set(0,0.2);
      x[7].Set(0,0.4);
      x[8].Set(0,0.6);
      x[9].Set(0,0.8);
      x[10].Set(0,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0.22313;
      y[1]=0.382893;
      y[2]=0.582748;
      y[3]=0.786628;
      y[4]=0.941765;
      y[5]=1;
      y[6]=0.941765;
      y[7]=0.786628;
      y[8]=0.582748;
      y[9]=0.382893;
      y[10]=0.22313;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(c,1);
      //--- initialization
      c[0]=0.3;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(c,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(c,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(c,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(bndl,1);
      //--- initialization
      bndl[0]=0;
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Value(bndl,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Deleting_Element(bndl);
      //--- allocation
      ArrayResize(bndu,1);
      //--- initialization
      bndu[0]=1;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(bndu,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Deleting_Element(bndu);
      double epsf=0;
      //--- check
      if(_spoil_scenario==17)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==18)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==19)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=0.000001;
      //--- check
      if(_spoil_scenario==20)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==21)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==22)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int maxits=0;
      int info;
      CLSFitStateShell state;
      CLSFitReportShell rep;
      double diffstep=0.0001;
      //--- check
      if(_spoil_scenario==23)
         diffstep=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==24)
         diffstep=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==25)
         diffstep=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::LSFitCreateF(x,y,c,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetBC(state,bndl,bndu);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fcx1func,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.05);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_nlfb");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Nonlinear fitting with custom scaling and bound constraints      |
//+------------------------------------------------------------------+
void TEST_LSFit_D_NLScale(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble           x;
   double                  y[];
   double                  c[];
   double                  bndl[];
   double                  bndu[];
   double                  s[];
   double                  temparray[];
   CObject                 obj;
   CNDimensional_Debt_Func fdebtfunc;
   CNDimensional_Rep       frep;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<30;_spoil_scenario++)
     {
      //--- In this example we demonstrate fitting by
      //---     f(x)=c[0]*(1+c[1]*((x-1999)^c[2]-1))
      //--- subject to bound constraints
      //---     -INF  < c[0] < +INF
      //---      -10 <= c[1] <= +10
      //---      0.1 <= c[2] <= 2.0
      //--- Data we want to fit are time series of Japan national debt
      //--- collected from 2000 to 2008 measured in USD (dollars,not
      //--- millions of dollars).
      //--- Our variables are:
      //---     c[0] - debt value at initial moment (2000),
      //---     c[1] - direction coefficient (growth or decrease),
      //---     c[2] - curvature coefficient.
      //--- You may see that our variables are badly scaled - first one 
      //--- is order of 10^12,and next two are somewhere ab1 in 
      //--- magnitude. Such problem is difficult to solve withsome
      //--- kind of scaling.
      //--- That is exactly where lsfitsetscale() function can be used.
      //--- We set scale of our variables to [1.0E12,1,1],which allows
      //--- us to easily solve this problem.
      //--- You can try commenting lsfitsetscale() call - and you will 
      //--- see that algorithm will fail to converge.
      x.Resize(9,1);
      //--- initialization
      x[0].Set(0,2000);
      x[1].Set(0,2001);
      x[2].Set(0,2002);
      x[3].Set(0,2003);
      x[4].Set(0,2004);
      x[5].Set(0,2005);
      x[6].Set(0,2006);
      x[7].Set(0,2007);
      x[8].Set(0,2008);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(x);
      //--- allocation
      ArrayResize(y,9);
      //--- initialization
      y[0]=4323239600000.0;
      y[1]=4560913100000.0;
      y[2]=5564091500000.0;
      y[3]=6743189300000.0;
      y[4]=7284064600000.0;
      y[5]=7050129600000.0;
      y[6]=7092221500000.0;
      y[7]=8483907600000.0;
      y[8]=8625804400000.0;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(c,3);
      //--- initialization
      c[0]=1.0e+13;
      c[1]=1;
      c[2]=1;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(c,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(c,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(c,CInfOrNaN::NegativeInfinity());
      double epsf=0;
      //--- check
      if(_spoil_scenario==13)
         epsf=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         epsf=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         epsf=CInfOrNaN::NegativeInfinity();
      double epsx=1.0e-5;
      //--- check
      if(_spoil_scenario==16)
         epsx=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         epsx=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         epsx=CInfOrNaN::NegativeInfinity();
      //--- allocation
      ArrayResize(bndl,3);
      //--- initialization
      bndl[0]=-CInfOrNaN::PositiveInfinity();
      bndl[1]=-10;
      bndl[2]=0.1;
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Value(bndl,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Deleting_Element(bndl);
      //--- allocation
      ArrayResize(bndu,3);
      //--- initialization
      bndu[0]=CInfOrNaN::PositiveInfinity();
      bndu[1]=10;
      bndu[2]=2;
      //--- check
      if(_spoil_scenario==21)
         Spoil_Vector_By_Value(bndu,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Deleting_Element(bndu);
      //--- allocation
      ArrayResize(s,3);
      //--- initialization
      s[0]=1.0e+12;
      s[1]=1;
      s[2]=1;
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Value(s,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==24)
         Spoil_Vector_By_Value(s,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==25)
         Spoil_Vector_By_Value(s,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==26)
         Spoil_Vector_By_Deleting_Element(s);
      //--- create variables
      int maxits=0;
      int info;
      CLSFitStateShell state;
      CLSFitReportShell rep;
      double diffstep=1.0e-5;
      //--- check
      if(_spoil_scenario==27)
         diffstep=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==28)
         diffstep=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==29)
         diffstep=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::LSFitCreateF(x,y,c,diffstep,state);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetCond(state,epsf,epsx,maxits);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetBC(state,bndl,bndu);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitSetScale(state,s);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitFit(state,fdebtfunc,frep,0,obj);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      CAlglib::LSFitResults(state,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,3);
      //--- initialization
      temparray[0]=4.142560e+12;
      temparray[1]=0.43424;
      temparray[2]=0.565376;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,2);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,-0.005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_nlscale");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Unconstrained (general) linear least squares fitting with and    |
//| withweights                                                      |
//+------------------------------------------------------------------+
void TEST_LSFit_D_Lin(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble fmatrix;
   int           info;
   double        c[];
   double        y[];
   double        temparray[];
   double        w[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<13;_spoil_scenario++)
     {
      //--- In this example we demonstrate linear fitting by f(x|a)=a*exp(0.5*x).
      //--- We have:
      //--- * y - vector of experimental data
      //--- * fmatrix -  matrix of basis functions calculated at sample points
      //---              Actually,we have only one basis function F0=exp(0.5*x).
      fmatrix.Resize(11,1);
      //--- initialization
      fmatrix[0].Set(0,0.606531);
      fmatrix[1].Set(0,0.67032);
      fmatrix[2].Set(0,0.740818);
      fmatrix[3].Set(0,0.818731);
      fmatrix[4].Set(0,0.904837);
      fmatrix[5].Set(0,1);
      fmatrix[6].Set(0,1.105171);
      fmatrix[7].Set(0,1.221403);
      fmatrix[8].Set(0,1.349859);
      fmatrix[9].Set(0,1.491825);
      fmatrix[10].Set(0,1.648721);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(fmatrix,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(fmatrix,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(fmatrix,CInfOrNaN::NegativeInfinity());
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=1.133719;
      y[1]=1.306522;
      y[2]=1.504604;
      y[3]=1.554663;
      y[4]=1.884638;
      y[5]=2.072436;
      y[6]=2.257285;
      y[7]=2.534068;
      y[8]=2.622017;
      y[9]=2.897713;
      y[10]=3.219371;
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y);
      //--- create a variable
      CLSFitReportShell rep;
      //--- Linear fitting withweights
      CAlglib::LSFitLinear(y,fmatrix,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.9865;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.00005);
      //--- Linear fitting with individual weights.
      //--- Slightly different result is returned.
      ArrayResize(w,11);
      //--- initialization
      w[0]=1.414213;
      w[1]=1;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      w[6]=1;
      w[7]=1;
      w[8]=1;
      w[9]=1;
      w[10]=1;
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Deleting_Element(w);
      //--- function call
      CAlglib::LSFitLinearW(y,w,fmatrix,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,1);
      //--- initialization
      temparray[0]=1.983354;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.00005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_lin");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Constrained (general) linear least squares fitting with and      |
//| withweights                                                      |
//+------------------------------------------------------------------+
void TEST_LSFit_D_Linc(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   CMatrixDouble fmatrix;
   CMatrixDouble cmatrix;
   double        y[];
   double        c[];
   double        temparray[];
   double        w[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<20;_spoil_scenario++)
     {
      //--- In this example we demonstrate linear fitting by f(x|a,b)=a*x+b
      //--- with simple constraint f(0)=0.
      //--- We have:
      //--- * y - vector of experimental data
      //--- * fmatrix -  matrix of basis functions sampled at [0,1] with step 0.2:
      //---                  [ 1.0   0.0 ]
      //---                  [ 1.0   0.2 ]
      //---                  [ 1.0   0.4 ]
      //---                  [ 1.0   0.6 ]
      //---                  [ 1.0   0.8 ]
      //---                  [ 1.0   1.0 ]
      //---              first column contains value of first basis function (constant term)
      //---              second column contains second basis function (linear term)
      //--- * cmatrix -  matrix of linear constraints:
      //---                  [ 1.0  0.0  0.0 ]
      //---              first two columns contain coefficients before basis functions,
      //---              last column contains desired value of their sum.
      //---              So [1,0,0] means "1*constant_term + 0*linear_term=0" 
      ArrayResize(y,6);
      //--- initialization
      y[0]=0.072436;
      y[1]=0.246944;
      y[2]=0.491263;
      y[3]=0.5223;
      y[4]=0.714064;
      y[5]=0.921929;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      fmatrix.Resize(6,2);
      //--- initialization
      fmatrix[0].Set(0,1);
      fmatrix[0].Set(1,0);
      fmatrix[1].Set(0,1);
      fmatrix[1].Set(1,0.2);
      fmatrix[2].Set(0,1);
      fmatrix[2].Set(1,0.4);
      fmatrix[3].Set(0,1);
      fmatrix[3].Set(1,0.6);
      fmatrix[4].Set(0,1);
      fmatrix[4].Set(1,0.8);
      fmatrix[5].Set(0,1);
      fmatrix[5].Set(1,1);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Value(fmatrix,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Value(fmatrix,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Matrix_By_Value(fmatrix,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Matrix_By_Adding_Row(fmatrix);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Matrix_By_Adding_Col(fmatrix);
      //--- check
      if(_spoil_scenario==10)
         Spoil_Matrix_By_Deleting_Row(fmatrix);
      //--- check
      if(_spoil_scenario==11)
         Spoil_Matrix_By_Deleting_Col(fmatrix);
      //--- allocation
      cmatrix.Resize(1,3);
      //--- initialization
      cmatrix[0].Set(0,1);
      cmatrix[0].Set(1,0);
      cmatrix[0].Set(2,0);
      //--- check
      if(_spoil_scenario==12)
         Spoil_Matrix_By_Value(cmatrix,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==13)
         Spoil_Matrix_By_Value(cmatrix,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==14)
         Spoil_Matrix_By_Value(cmatrix,CInfOrNaN::NegativeInfinity());
      //--- create variables
      int info;
      CLSFitReportShell rep;
      //--- Constrained fitting withweights
      CAlglib::LSFitLinearC(y,fmatrix,cmatrix,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=0;
      temparray[1]=0.932933;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.0005);
      //--- Constrained fitting with individual weights
      ArrayResize(w,6);
      //--- initialization
      w[0]=1;
      w[1]=1.414213;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==18)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Deleting_Element(w);
      //--- function call
      CAlglib::LSFitLinearWC(y,w,fmatrix,cmatrix,info,c,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- allocation
      ArrayResize(temparray,2);
      //--- initialization
      temparray[0]=0;
      temparray[1]=0.938322;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && Doc_Test_Real_Vector(c,temparray,0.0005);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_linc");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Unconstrained polynomial fitting                                 |
//+------------------------------------------------------------------+
void TEST_LSFit_D_Pol(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double w[];
   double xc[];
   double yc[];
   int    dc[];
   int    m;
   double t;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<20;_spoil_scenario++)
     {
      //--- This example demonstrates polynomial fitting.
      //--- Fitting is done by two (M=2) functions from polynomial basis:
      //---     f0=1
      //---     f1=x
      //--- Basically,it just a linear fit;more complex polynomials may be used
      //--- (e.g. parabolas with M=3,cubic with M=4),but even such simple fit allows
      //--- us to demonstrate polynomialfit() function in action.
      //--- We have:
      //--- * x      set of abscissas
      //--- * y      experimental data
      //--- Additionally we demonstrate weighted fitting,where second point has
      //--- more weight than other ones.
      ArrayResize(x,11);
      //--- initialization
      x[0]=0;
      x[1]=0.1;
      x[2]=0.2;
      x[3]=0.3;
      x[4]=0.4;
      x[5]=0.5;
      x[6]=0.6;
      x[7]=0.7;
      x[8]=0.8;
      x[9]=0.9;
      x[10]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0;
      y[1]=0.05;
      y[2]=0.26;
      y[3]=0.32;
      y[4]=0.33;
      y[5]=0.43;
      y[6]=0.6;
      y[7]=0.6;
      y[8]=0.77;
      y[9]=0.98;
      y[10]=1.02;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      m=2;
      t=2;
      //--- check
      if(_spoil_scenario==10)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==11)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      int info;
      CBarycentricInterpolantShell p;
      CPolynomialFitReportShell rep;
      //--- Fitting withindividual weights
      //--- NOTE: result is returned as barycentricinterpolant structure.
      //---       if you want to get representation in the power basis,
      //---       you can use barycentricbar2pow() function to convert
      //---       from barycentric to power representation (see docs for 
      //---       POLINT subpackage for more info).
      CAlglib::PolynomialFit(x,y,m,info,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.011,0.002);
      //--- Fitting with individual weights
      //--- NOTE: slightly different result is returned
      ArrayResize(w,11);
      //--- initialization
      w[0]=1;
      w[1]=1.414213562;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      w[6]=1;
      w[7]=1;
      w[8]=1;
      w[9]=1;
      w[10]=1;
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Deleting_Element(w);
      //--- allocation
      ArrayResize(xc,0);
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Adding_Element(xc);
      //--- allocation
      ArrayResize(yc,0);
      //--- check
      if(_spoil_scenario==18)
         Spoil_Vector_By_Adding_Element(yc);
      //--- allocation
      ArrayResize(dc,0);
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Adding_Element(dc);
      //--- function call
      CAlglib::PolynomialFitWC(x,y,w,xc,yc,dc,m,info,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.023,0.002);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_pol");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Constrained polynomial fitting                                   |
//+------------------------------------------------------------------+
void TEST_LSFit_D_Polc(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double w[];
   double xc[];
   double yc[];
   int    dc[];
   int    m;
   int    info;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<29;_spoil_scenario++)
     {
      //--- This example demonstrates polynomial fitting.
      //--- Fitting is done by two (M=2) functions from polynomial basis:
      //---     f0=1
      //---     f1=x
      //--- with simple constraint on function value
      //---     f(0)=0
      //--- Basically,it just a linear fit;more complex polynomials may be used
      //--- (e.g. parabolas with M=3,cubic with M=4),but even such simple fit allows
      //--- us to demonstrate polynomialfit() function in action.
      //--- We have:
      //--- * x      set of abscissas
      //--- * y      experimental data
      //--- * xc     points where constraints are placed
      //--- * yc     constraints on derivatives
      //--- * dc     derivative indices
      //---          (0 means function itself,1 means first derivative)
      ArrayResize(x,2);
      //--- initialization
      x[0]=1;
      x[1]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,2);
      //--- initialization
      y[0]=0.9;
      y[1]=1.1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(w,2);
      //--- initialization
      w[0]=1;
      w[1]=1;
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Adding_Element(w);
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Deleting_Element(w);
      //--- allocation
      ArrayResize(xc,1);
      //--- initialization
      xc[0]=0;
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Value(xc,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(xc,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(xc,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==18)
         Spoil_Vector_By_Adding_Element(xc);
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Deleting_Element(xc);
      //--- allocation
      ArrayResize(yc,1);
      //--- initialization
      yc[0]=0;
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Value(yc,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==21)
         Spoil_Vector_By_Value(yc,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==22)
         Spoil_Vector_By_Value(yc,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==23)
         Spoil_Vector_By_Adding_Element(yc);
      //--- check
      if(_spoil_scenario==24)
         Spoil_Vector_By_Deleting_Element(yc);
      //--- allocation
      ArrayResize(dc,1);
      //--- initialization
      dc[0]=0;
      //--- check
      if(_spoil_scenario==25)
         Spoil_Vector_By_Adding_Element(dc);
      //--- check
      if(_spoil_scenario==26)
         Spoil_Vector_By_Deleting_Element(dc);
      double t=2;
      //--- check
      if(_spoil_scenario==27)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==28)
         t=CInfOrNaN::NegativeInfinity();
      m=2;
      //--- create variables
      CBarycentricInterpolantShell p;
      CPolynomialFitReportShell rep;
      //--- function call
      CAlglib::PolynomialFitWC(x,y,w,xc,yc,dc,m,info,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.000,0.001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_polc");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Unconstrained fitting by penalized regression spline             |
//+------------------------------------------------------------------+
void TEST_LSFit_D_Spline(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   int    info;
   double v;
   double rho;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<19;_spoil_scenario++)
     {
      //--- In this example we demonstrate penalized spline fitting of noisy data
      //--- We have:
      //--- * x - abscissas
      //--- * y - vector of experimental data,straight line with small noise
      ArrayResize(x,10);
      //--- initialization
      x[0]=0;
      x[1]=0.1;
      x[2]=0.2;
      x[3]=0.3;
      x[4]=0.4;
      x[5]=0.5;
      x[6]=0.6;
      x[7]=0.7;
      x[8]=0.8;
      x[9]=0.9;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Adding_Element(x);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,10);
      //--- initialization
      y[0]=0.1;
      y[1]=0;
      y[2]=0.3;
      y[3]=0.4;
      y[4]=0.3;
      y[5]=0.4;
      y[6]=0.62;
      y[7]=0.68;
      y[8]=0.75;
      y[9]=0.95;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Adding_Element(y);
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Deleting_Element(y);
      //--- create variables
      CSpline1DInterpolantShell s;
      CSpline1DFitReportShell rep;
      //--- Fit with VERY small amount of smoothing (rho=-5.0)
      //--- and large number of basis functions (M=50).
      //--- With such small regularization penalized spline almost fully reproduces function values
      rho=-5.0;
      //--- check
      if(_spoil_scenario==10)
         rho=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==11)
         rho=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==12)
         rho=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::Spline1DFitPenalized(x,y,50,rho,info,s,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      //--- function call
      v=CAlglib::Spline1DCalc(s,0.0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,0.10,0.01);
      //--- Fit with VERY large amount of smoothing (rho=10.0)
      //--- and large number of basis functions (M=50).
      //--- With such regularization our spline should become close to the straight line fit.
      //--- We will compare its value in x=1.0 with results obtained from such fit.
      rho=+10.0;
      //--- check
      if(_spoil_scenario==13)
         rho=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==14)
         rho=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==15)
         rho=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::Spline1DFitPenalized(x,y,50,rho,info,s,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      //--- function call
      v=CAlglib::Spline1DCalc(s,1.0);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,0.969,0.001);
      //--- In real life applications you may need some moderate degree of fitting,
      //--- so we try to fit once more with rho=3.0.
      rho=+3.0;
      //--- check
      if(_spoil_scenario==16)
         rho=CInfOrNaN::NaN();
      //--- check
      if(_spoil_scenario==17)
         rho=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==18)
         rho=CInfOrNaN::NegativeInfinity();
      //--- function call
      CAlglib::Spline1DFitPenalized(x,y,50,rho,info,s,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Int(info,1);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_d_spline");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial fitting, full list of parameters.                     |
//+------------------------------------------------------------------+
void TEST_LSFit_T_PolFit_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   int    info;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<10;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,11);
      //--- initialization
      x[0]=0;
      x[1]=0.1;
      x[2]=0.2;
      x[3]=0.3;
      x[4]=0.4;
      x[5]=0.5;
      x[6]=0.6;
      x[7]=0.7;
      x[8]=0.8;
      x[9]=0.9;
      x[10]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0;
      y[1]=0.05;
      y[2]=0.26;
      y[3]=0.32;
      y[4]=0.33;
      y[5]=0.43;
      y[6]=0.6;
      y[7]=0.6;
      y[8]=0.77;
      y[9]=0.98;
      y[10]=1.02;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y);
      int m=2;
      double t=2;
      //--- check
      if(_spoil_scenario==8)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==9)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CBarycentricInterpolantShell p;
      CPolynomialFitReportShell rep;
      //--- function call
      CAlglib::PolynomialFit(x,y,11,m,info,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.011,0.002);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_t_polfit_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial fitting, full list of parameters.                     |
//+------------------------------------------------------------------+
void TEST_LSFit_T_PolFit_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double w[];
   double xc[];
   double yc[];
   int    dc[];
   int    m;
   double t;
   int    info;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<14;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,11);
      //--- initialization
      x[0]=0;
      x[1]=0.1;
      x[2]=0.2;
      x[3]=0.3;
      x[4]=0.4;
      x[5]=0.5;
      x[6]=0.6;
      x[7]=0.7;
      x[8]=0.8;
      x[9]=0.9;
      x[10]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,11);
      //--- initialization
      y[0]=0;
      y[1]=0.05;
      y[2]=0.26;
      y[3]=0.32;
      y[4]=0.33;
      y[5]=0.43;
      y[6]=0.6;
      y[7]=0.6;
      y[8]=0.77;
      y[9]=0.98;
      y[10]=1.02;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(w,11);
      //--- initialization
      w[0]=1;
      w[1]=1.414213562;
      w[2]=1;
      w[3]=1;
      w[4]=1;
      w[5]=1;
      w[6]=1;
      w[7]=1;
      w[8]=1;
      w[9]=1;
      w[10]=1;
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Deleting_Element(w);
      //--- allocation
      ArrayResize(xc,0);
      ArrayResize(yc,0);
      ArrayResize(dc,0);
      //--- initialization
      m=2;
      t=2;
      //--- check
      if(_spoil_scenario==12)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==13)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CBarycentricInterpolantShell p;
      CPolynomialFitReportShell rep;
      //--- function call
      CAlglib::PolynomialFitWC(x,y,w,11,xc,yc,dc,0,m,info,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.023,0.002);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_t_polfit_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Polynomial fitting,full list of parameters.                      |
//+------------------------------------------------------------------+
void TEST_LSFit_T_PolFit_3(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double x[];
   double y[];
   double w[];
   double xc[];
   double yc[];
   int    dc[];
   int    m;
   double t;
   int    info;
   double v;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<23;_spoil_scenario++)
     {
      //--- allocation
      ArrayResize(x,2);
      //--- initialization
      x[0]=1;
      x[1]=1;
      //--- check
      if(_spoil_scenario==0)
         Spoil_Vector_By_Value(x,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Vector_By_Value(x,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Vector_By_Value(x,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Vector_By_Deleting_Element(x);
      //--- allocation
      ArrayResize(y,2);
      //--- initialization
      y[0]=0.9;
      y[1]=1.1;
      //--- check
      if(_spoil_scenario==4)
         Spoil_Vector_By_Value(y,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Value(y,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==6)
         Spoil_Vector_By_Value(y,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Deleting_Element(y);
      //--- allocation
      ArrayResize(w,2);
      //--- initialization
      w[0]=1;
      w[1]=1;
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Value(w,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==9)
         Spoil_Vector_By_Value(w,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==10)
         Spoil_Vector_By_Value(w,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==11)
         Spoil_Vector_By_Deleting_Element(w);
      //--- allocation
      ArrayResize(xc,1);
      //--- initialization
      xc[0]=0;
      //--- check
      if(_spoil_scenario==12)
         Spoil_Vector_By_Value(xc,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==13)
         Spoil_Vector_By_Value(xc,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==14)
         Spoil_Vector_By_Value(xc,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==15)
         Spoil_Vector_By_Deleting_Element(xc);
      //--- allocation
      ArrayResize(yc,1);
      //--- initialization
      yc[0]=0;
      //--- check
      if(_spoil_scenario==16)
         Spoil_Vector_By_Value(yc,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==17)
         Spoil_Vector_By_Value(yc,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==18)
         Spoil_Vector_By_Value(yc,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==19)
         Spoil_Vector_By_Deleting_Element(yc);
      //--- allocation
      ArrayResize(dc,1);
      //--- initialization
      dc[0]=0;
      //--- check
      if(_spoil_scenario==20)
         Spoil_Vector_By_Deleting_Element(dc);
      m=2;
      t=2;
      //--- check
      if(_spoil_scenario==21)
         t=CInfOrNaN::PositiveInfinity();
      //--- check
      if(_spoil_scenario==22)
         t=CInfOrNaN::NegativeInfinity();
      //--- create variables
      CBarycentricInterpolantShell p;
      CPolynomialFitReportShell rep;
      //--- function call
      CAlglib::PolynomialFitWC(x,y,w,2,xc,yc,dc,1,m,info,p,rep);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- function call
      v=CAlglib::BarycentricCalc(p,t);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(v,2.000,0.001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","lsfit_t_polfit_3");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, real matrix, short form                 |
//+------------------------------------------------------------------+
void TEST_MatDet_D_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double        a;
   CMatrixDouble b;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,1);
      b[0].Set(1,2);
      b[1].Set(0,2);
      b[1].Set(1,1);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(b);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::RMatrixDet(b);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(a,-3,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_d_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, real matrix, full form                  |
//+------------------------------------------------------------------+
void TEST_MatDet_D_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double        a;
   CMatrixDouble b;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,5);
      b[0].Set(1,4);
      b[1].Set(0,4);
      b[1].Set(1,5);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::RMatrixDet(b,2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(a,9,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_d_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, complex matrix, short form              |
//+------------------------------------------------------------------+
void TEST_MatDet_D_3(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex        a;
   complex        tempcomplex1;
   complex        tempcomplex2;
   complex        tempcomplex3;
   CMatrixComplex b;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- initialization
      tempcomplex1.re=1;
      tempcomplex1.im=1;
      tempcomplex2.re=1;
      tempcomplex2.im=-1;
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,tempcomplex1);
      b[0].Set(1,2);
      b[1].Set(0,2);
      b[1].Set(1,tempcomplex2);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(b);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::CMatrixDet(b);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex3.re=-2;
      tempcomplex3.im=0;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex(a,tempcomplex3,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_d_3");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, complex matrix, full form               |
//+------------------------------------------------------------------+
void TEST_MatDet_D_4(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex        a;
   complex        tempcomplex1;
   complex        tempcomplex2;
   complex        tempcomplex3;
   CMatrixComplex b;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- initialization
      tempcomplex1.re=0;
      tempcomplex1.im=5;
      tempcomplex2.re=0;
      tempcomplex2.im=4;
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,tempcomplex1);
      b[0].Set(1,4);
      b[1].Set(0,tempcomplex2);
      b[1].Set(1,5);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::CMatrixDet(b,2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex3.re=0;
      tempcomplex3.im=9;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex(a,tempcomplex3,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_d_4");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, complex matrix with zero imaginary part,|
//| short form                                                       |
//+------------------------------------------------------------------+
void TEST_MatDet_D_5(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex        a;
   complex        tempcomplex;
   CMatrixComplex b;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<7;_spoil_scenario++)
     {
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,9);
      b[0].Set(1,1);
      b[1].Set(0,2);
      b[1].Set(1,1);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(b);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::CMatrixDet(b);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex.re=7;
      tempcomplex.im=0;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex(a,tempcomplex,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_d_5");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, real matrix, full form                  |
//+------------------------------------------------------------------+
void TEST_MatDet_T_0(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double        a;
   CMatrixDouble b;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,3);
      b[0].Set(1,4);
      b[1].Set(0,-4);
      b[1].Set(1,3);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::RMatrixDet(b,2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(a,25,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_t_0");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, real matrix, LU, short form             |
//+------------------------------------------------------------------+
void TEST_MatDet_T_1(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double        a;
   CMatrixDouble b;
   int           p[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<9;_spoil_scenario++)
     {
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,1);
      b[0].Set(1,2);
      b[1].Set(0,2);
      b[1].Set(1,5);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(b);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- allocation
      ArrayResize(p,2);
      //--- initialization
      p[0]=1;
      p[1]=1;
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Adding_Element(p);
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Deleting_Element(p);
      //--- function call
      a=CAlglib::RMatrixLUDet(b,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(a,-5,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_t_1");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, real matrix, LU, full form              |
//+------------------------------------------------------------------+
void TEST_MatDet_T_2(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   double        a;
   int           p[];
   CMatrixDouble b;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,5);
      b[0].Set(1,4);
      b[1].Set(0,4);
      b[1].Set(1,5);
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NaN());
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,CInfOrNaN::PositiveInfinity());
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,CInfOrNaN::NegativeInfinity());
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- allocation
      ArrayResize(p,2);
      //--- initialization
      p[0]=0;
      p[1]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Deleting_Element(p);
      //--- function call
      a=CAlglib::RMatrixLUDet(b,p,2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Real(a,25,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_t_2");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, complex matrix, full form               |
//+------------------------------------------------------------------+
void TEST_MatDet_T_3(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex        a;
   CMatrixComplex b;
   complex        tempcomplex1;
   complex        tempcomplex2;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<5;_spoil_scenario++)
     {
      //--- initialization
      tempcomplex1.re=0;
      tempcomplex1.im=5;
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,tempcomplex1);
      b[0].Set(1,4);
      b[1].Set(0,-4);
      b[1].Set(1,tempcomplex1);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- function call
      a=CAlglib::CMatrixDet(b,2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex2.re=-9;
      tempcomplex2.im=0;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex(a,tempcomplex2,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_t_3");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, complex matrix, LU, short form          |
//+------------------------------------------------------------------+
void TEST_MatDet_T_4(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex        a;
   int            p[];
   complex        tempcomplex1;
   complex        tempcomplex2;
   CMatrixComplex b;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<9;_spoil_scenario++)
     {
      //--- initialization
      tempcomplex1.re=0;
      tempcomplex1.im=5;
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,1);
      b[0].Set(1,2);
      b[1].Set(0,2);
      b[1].Set(1,tempcomplex1);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Adding_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Adding_Col(b);
      //--- check
      if(_spoil_scenario==5)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==6)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- allocation
      ArrayResize(p,2);
      //--- initialization
      p[0]=1;
      p[1]=1;
      //--- check
      if(_spoil_scenario==7)
         Spoil_Vector_By_Adding_Element(p);
      //--- check
      if(_spoil_scenario==8)
         Spoil_Vector_By_Deleting_Element(p);
      //--- function call
      a=CAlglib::CMatrixLUDet(b,p);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex2.re=0;
      tempcomplex2.im=-5;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex(a,tempcomplex2,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_t_4");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
//| Determinant calculation, complex matrix, LU, full form           |
//+------------------------------------------------------------------+
void TEST_MatDet_T_5(int &_spoil_scenario,bool &_TestResult,bool &_TotalResult)
  {
   _TestResult=true;
//--- create variables
   complex        a;
   complex        tempcomplex1;
   complex        tempcomplex2;
   CMatrixComplex b;
   complex        cnan;
   complex        cpositiveinfinity;
   complex        cnegativeinfinity;
   int            p[];
//--- testing
   for(_spoil_scenario=-1;_spoil_scenario<6;_spoil_scenario++)
     {
      //--- initialization
      tempcomplex1.re=0;
      tempcomplex1.im=4;
      //--- allocation
      b.Resize(2,2);
      //--- initialization
      b[0].Set(0,5);
      b[0].Set(1,tempcomplex1);
      b[1].Set(0,4);
      b[1].Set(1,5);
      //--- initialization
      cnan.re=CInfOrNaN::NaN();
      cnan.im=CInfOrNaN::NaN();
      cpositiveinfinity.re=CInfOrNaN::PositiveInfinity();
      cpositiveinfinity.im=CInfOrNaN::PositiveInfinity();
      cnegativeinfinity.re=CInfOrNaN::NegativeInfinity();
      cnegativeinfinity.im=CInfOrNaN::NegativeInfinity();
      //--- check
      if(_spoil_scenario==0)
         Spoil_Matrix_By_Value(b,cnan);
      //--- check
      if(_spoil_scenario==1)
         Spoil_Matrix_By_Value(b,cpositiveinfinity);
      //--- check
      if(_spoil_scenario==2)
         Spoil_Matrix_By_Value(b,cnegativeinfinity);
      //--- check
      if(_spoil_scenario==3)
         Spoil_Matrix_By_Deleting_Row(b);
      //--- check
      if(_spoil_scenario==4)
         Spoil_Matrix_By_Deleting_Col(b);
      //--- allocation
      ArrayResize(p,2);
      //--- initialization
      p[0]=0;
      p[1]=1;
      //--- check
      if(_spoil_scenario==5)
         Spoil_Vector_By_Deleting_Element(p);
      //--- function call
      a=CAlglib::CMatrixLUDet(b,p,2);
      //--- handling exceptions
      if(!Func_spoil_scenario(_spoil_scenario,_TestResult))
         continue;
      //--- initialization
      tempcomplex2.re=25;
      tempcomplex2.im=0;
      //--- check result
      _TestResult=_TestResult && Doc_Test_Complex(a,tempcomplex2,0.0001);
      _TestResult=_TestResult && (_spoil_scenario==-1);
     }
//--- check
   if(!_TestResult)
      Print("{0,-32} FAILED","matdet_t_5");
//--- change total result
   _TotalResult=_TotalResult && _TestResult;
  }
//+------------------------------------------------------------------+
