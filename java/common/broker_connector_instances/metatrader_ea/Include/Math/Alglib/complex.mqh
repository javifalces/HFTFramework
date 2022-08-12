//+------------------------------------------------------------------+
//|                                                      complex.mqh |
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
//+------------------------------------------------------------------+
//| Complex numbers                                                  |
//+------------------------------------------------------------------+
struct complex
  {
public:
   double            re; // real part
   double            im; // imaginary part

public:
                     complex(void);
                     complex(const double x);
                     complex(const double x,const double y);
                    ~complex(void);
   //--- operations
   void              Copy(const complex &rhs);
   bool              Eq(const complex &lhs,const complex &rhs);
   bool              NotEq(const complex &lhs,const complex &rhs);
   complex           Add(const complex &lhs,const complex &rhs);
   complex           Sub(const complex &lhs,const complex &rhs);
   complex           Mul(const complex &lhs,const complex &rhs);
   complex           Div(const complex &lhs,const complex &rhs);
   complex           Conjugate(void);
   //--- overloading
   void              operator=(const double rhs);
   void              operator=(const complex &rhs);
   void              operator+=(const complex &rhs);
   void              operator-=(const complex &rhs);
   bool              operator==(const complex &rhs);
   bool              operator==(const double rhs);
   bool              operator!=(const complex &rhs);
   bool              operator!=(const double rhs);
   complex           operator+(const complex &rhs);
   complex           operator+(const double rhs);
   complex           operator+(void);
   complex           operator-(const complex &rhs);
   complex           operator-(const double rhs);
   complex           operator-(void);
   complex           operator*(const complex &rhs);
   complex           operator*(const double rhs);
   complex           operator/(const complex &rhs);
   complex           operator/(const double rhs);
  };
//+------------------------------------------------------------------+
//| Constructor without parameters                                   |
//+------------------------------------------------------------------+
complex::complex(void): re(0),im(0)
  {

  }
//+------------------------------------------------------------------+
//| Constructor with one parameter                                   |
//+------------------------------------------------------------------+
complex::complex(const double x): re(x),im(0)
  {

  }
//+------------------------------------------------------------------+
//| Constructor with two parameters                                  |
//+------------------------------------------------------------------+
complex::complex(const double x,const double y): re(x),im(y)
  {

  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
complex::~complex(void)
  {

  }
//+------------------------------------------------------------------+
//| Copy complex                                                     |
//+------------------------------------------------------------------+
void complex::Copy(const complex &rhs)
  {
   re=rhs.re;
   im=rhs.im;
  }
//+------------------------------------------------------------------+
//| Comparison (==)                                                  |
//+------------------------------------------------------------------+
bool complex::Eq(const complex &lhs,const complex &rhs)
  {
//--- comparison
   if(lhs.re==rhs.re && lhs.im==rhs.im)
      return(true);
//--- numbers are not equal
   return(false);
  }
//+------------------------------------------------------------------+
//| Comparison (!=)                                                  |
//+------------------------------------------------------------------+
bool complex::NotEq(const complex &lhs,const complex &rhs)
  {
//--- comparison
   if(lhs.re!=rhs.re || lhs.im!=rhs.im)
      return(true);
//--- numbers are equal
   return(false);
  }
//+------------------------------------------------------------------+
//| Sum                                                              |
//+------------------------------------------------------------------+
complex complex::Add(const complex &lhs,const complex &rhs)
  {
   complex res;
//--- sum
   res.re=lhs.re+rhs.re;
   res.im=lhs.im+rhs.im;
//--- return result
   return(res);
  }
//+------------------------------------------------------------------+
//| Subtraction                                                      |
//+------------------------------------------------------------------+
complex complex::Sub(const complex &lhs,const complex &rhs)
  {
   complex res;
//--- subtraction
   res.re=lhs.re-rhs.re;
   res.im=lhs.im-rhs.im;
//--- return result
   return(res);
  }
//+------------------------------------------------------------------+
//| Multiplication                                                   |
//+------------------------------------------------------------------+
complex complex::Mul(const complex &lhs,const complex &rhs)
  {
   complex res;
//--- multiplication
   res.re=lhs.re*rhs.re-lhs.im*rhs.im;
   res.im=lhs.re*rhs.im+lhs.im*rhs.re;
//--- return result
   return(res);
  }
//+------------------------------------------------------------------+
//| Division                                                          |
//+------------------------------------------------------------------+
complex complex::Div(const complex &lhs,const complex &rhs)
  {
//--- empty complex value
   complex res(EMPTY_VALUE,EMPTY_VALUE);
//--- check
   if(rhs.re==0 && rhs.im==0)
     {
      Print(__FUNCTION__+": number is zero");
      return(res);
     }
//--- create variables
   double e;
   double f;
//--- division
   if(MathAbs(rhs.im)<MathAbs(rhs.re))
     {
      e=rhs.im/rhs.re;
      f=rhs.re+rhs.im*e;
      res.re=(lhs.re+lhs.im*e)/f;
      res.im=(lhs.im-lhs.re*e)/f;
      //--- return result
      return(res);
     }
   e=rhs.re/rhs.im;
   f=rhs.im+rhs.re*e;
   res.re=(lhs.im+lhs.re*e)/f;
   res.im=(-lhs.re+lhs.im*e)/f;
//--- return result
   return(res);
  }
//+------------------------------------------------------------------+
//| Conjugate                                                        |
//+------------------------------------------------------------------+
complex complex::Conjugate(void)
  {
//--- conjugate
   complex res(re,-im);
//--- return result
   return res;
  }
//+------------------------------------------------------------------+
//| Overloading (=)                                                  |
//+------------------------------------------------------------------+
void complex::operator=(const double rhs)
  {
   re=rhs;
   im=0;
  }
//+------------------------------------------------------------------+
//| Overloading (=)                                                  |
//+------------------------------------------------------------------+
void complex::operator=(const complex &rhs)
  {
   this.Copy(rhs);
  }
//+------------------------------------------------------------------+
//| Overloading (+=)                                                 |
//+------------------------------------------------------------------+
void complex::operator+=(const complex &rhs)
  {
   re+=rhs.re;
   im+=rhs.im;
  }
//+------------------------------------------------------------------+
//| Overloading (-=)                                                 |
//+------------------------------------------------------------------+
void complex::operator-=(const complex &rhs)
  {
   re+=rhs.re;
   im+=rhs.im;
  }
//+------------------------------------------------------------------+
//| Overloading (==)                                                 |
//+------------------------------------------------------------------+
bool complex::operator==(const complex &rhs)
  {
   return(Eq(this,rhs));
  }
//+------------------------------------------------------------------+
//| Overloading (==)                                                 |
//+------------------------------------------------------------------+
bool complex::operator==(const double rhs)
  {
   complex r(rhs,0);
//--- return result
   return(Eq(this,r));
  }
//+------------------------------------------------------------------+
//| Overloading (!=)                                                 |
//+------------------------------------------------------------------+
bool complex::operator!=(const complex &rhs)
  {
   return(NotEq(this,rhs));
  }
//+------------------------------------------------------------------+
//| Overloading (!=)                                                 |
//+------------------------------------------------------------------+
bool complex::operator!=(const double rhs)
  {
   complex r(rhs,0);
//--- return result
   return(NotEq(this,r));
  }
//+------------------------------------------------------------------+
//| Overloading of binary (+)                                        |
//+------------------------------------------------------------------+
complex complex::operator+(const complex &rhs)
  {
   return(Add(this,rhs));
  }
//+------------------------------------------------------------------+
//| Overloading of binary (+)                                        |
//+------------------------------------------------------------------+
complex complex::operator+(const double rhs)
  {
   complex r(rhs,0);
//--- return result
   return(Add(this,r));
  }
//+------------------------------------------------------------------+
//| Overloading of unary (+)                                         |
//+------------------------------------------------------------------+
complex complex::operator+(void)
  {
//--- return result
   return(this);
  }
//+------------------------------------------------------------------+
//| Overloading of binary (-)                                        |
//+------------------------------------------------------------------+
complex complex::operator-(const complex &rhs)
  {
   return(Sub(this,rhs));
  }
//+------------------------------------------------------------------+
//| Overloading of binary (-)                                        |
//+------------------------------------------------------------------+
complex complex::operator-(const double rhs)
  {
   complex r(rhs,0);
//--- return result
   return(Sub(this,r));
  }
//+------------------------------------------------------------------+
//| Overloading of unary (-)                                         |
//+------------------------------------------------------------------+
complex complex::operator-(void)
  {
   complex c(-this.re,-this.im);
//--- return result
   return(c);
  }
//+------------------------------------------------------------------+
//| Overloading (*)                                                  |
//+------------------------------------------------------------------+
complex complex::operator*(const complex &rhs)
  {
   return(Mul(this,rhs));
  }
//+------------------------------------------------------------------+
//| Overloading (*)                                                  |
//+------------------------------------------------------------------+
complex complex::operator*(const double rhs)
  {
   complex r(rhs,0);
//--- return result
   return(Mul(this,r));
  }
//+------------------------------------------------------------------+
//| Overloading (/)                                                  |
//+------------------------------------------------------------------+
complex complex::operator/(const complex &rhs)
  {
   return(Div(this,rhs));
  }
//+------------------------------------------------------------------+
//| Overloading (/)                                                  |
//+------------------------------------------------------------------+
complex complex::operator/(const double rhs)
  {
   complex r(rhs,0);
//--- return result
   return(Div(this,r));
  }
//+------------------------------------------------------------------+
