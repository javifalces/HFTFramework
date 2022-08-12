//+------------------------------------------------------------------+
//|                                                      DXUtils.mqh |
//|                        Copyright 2019, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2019, MetaQuotes Software Corp."
#property link      "https://www.mql5.com"
//---
#include "DXMath.mqh"
#include "DXData.mqh"
//--- data types in Wavefront OBJ file format
#define OBJ_DATA_UNKNOWN  0
#define OBJ_DATA_V        1
#define OBJ_DATA_VT       2
#define OBJ_DATA_VN       3
#define OBJ_DATA_F        4
//+------------------------------------------------------------------+
//| OBJFaceType                                                      |
//+------------------------------------------------------------------+
struct OBJFaceType
  {
   int               total;
   int               v[4];
   int               t[4];
   int               n[4];
  };
//+------------------------------------------------------------------+
//| Transforms right to left hand system, or backward                |
//| TVertex must have                                                |
//| DXVector4 normal and DXVector2 tcoord members                    |
//+------------------------------------------------------------------+
template <typename TVertex>
void DXInverseWinding(TVertex &vertices[],uint &indices[])
  {
//--- proccess vertices
   uint count=ArraySize(vertices);
   for(uint i=0; i<count; i++)
     {
      //--- invert normals
      vertices[i].normal.x=-vertices[i].normal.x;
      vertices[i].normal.y=-vertices[i].normal.y;
      vertices[i].normal.z=-vertices[i].normal.z;
      //--- flip texture coordinates
      vertices[i].tcoord.x=1.0f-vertices[i].tcoord.x;
     }
//--- change indices order
   count=ArraySize(indices);
   for(uint i=2; i<count; i+=3)
     {
      uint tmp    =indices[i];
      indices[i]  =indices[i-2];
      indices[i-2]=tmp;
     }
  }
//+------------------------------------------------------------------+
//| Load static 3D models from Maya 2011                             |
//| https://en.wikibooks.org/wiki/DirectX/10.0/Direct3D/Loading_Maya |
//+------------------------------------------------------------------+
bool DXLoadObjData(const string filename,DXVector4 &v_positions[],DXVector2 &v_tcoords[],DXVector4 &v_normals[],OBJFaceType &faces[],bool show_debug=false)
  {
   int total_positions=0;
   int total_tcoords=0;
   int total_normals=0;
   int total_faces=0;
//---
   ResetLastError();
   int file_handle=FileOpen(filename,FILE_READ|FILE_TXT|FILE_ANSI);
   if(file_handle==INVALID_HANDLE)
     {
      printf("FileOpen %s failed, error code=%d",filename,GetLastError());
      return(false);
     }
   int str_length=0;
   string str="";
   int total_lines=0;
   string str_parsed[];
   string str_parsed_faces[];
//---
   while(!FileIsEnding(file_handle))
     {
      str=FileReadString(file_handle,str_length);
      total_lines++;
      //--- replace two spaces to one
      StringReplace(str,"\t"," ");
      while(StringFind(str,"  ",0)!=-1)
         StringReplace(str,"  "," ");
      //--- split string to tokens
      int total_items=StringSplit(str,' ',str_parsed);
      if(total_items<1)
         continue;
      //--- when examining the file you can ignore every line unless it starts with a "V", "VT", "VN", or "F".
      //--- the extra information in the file will not be needed for converting .obj to our file format.
      StringToUpper(str_parsed[0]);
      //--- parse data type
      int data_type=OBJ_DATA_UNKNOWN;
      if(str_parsed[0]=="V")
         data_type=OBJ_DATA_V;
      else
         if(str_parsed[0]=="VT")
            data_type=OBJ_DATA_VT;
         else
            if(str_parsed[0]=="VN")
               data_type=OBJ_DATA_VN;
            else
               if(str_parsed[0]=="F")
                  data_type=OBJ_DATA_F;
      //--- proceed data type
      switch(data_type)
        {
         //--- 1. "V" lines are for the vertices, each is listed in X, Y, Z float format.
         case OBJ_DATA_V:
           {
            total_positions++;
            if(total_items<4)
              {
               PrintFormat("obj data error at line=%d: %s",total_lines,str);
               FileClose(file_handle);
               return(false);
              }
            ArrayResize(v_positions,total_positions,total_positions+1000);
            int idx=total_positions-1;
            v_positions[idx].x=(float)StringToDouble(str_parsed[1]);
            v_positions[idx].y=(float)StringToDouble(str_parsed[2]);
            v_positions[idx].z=(float)StringToDouble(str_parsed[3]);
            v_positions[idx].w=1.0f;
            //--- debug message
            if(show_debug)
               printf("posiiton %d:  %f   %f   %f",idx,v_positions[idx].x,v_positions[idx].y,v_positions[idx].z);
            //---
            break;
           }
         //--- 2. "VT" lines are for the texture coordinates, they are listed in TU, TV float format.
         case OBJ_DATA_VT:
           {
            total_tcoords++;
            if(total_items<3)
              {
               PrintFormat("obj data error at line=%d: %s",total_lines,str);
               FileClose(file_handle);
               return(false);
              }
            ArrayResize(v_tcoords,total_tcoords,total_tcoords+1000);
            int idx=total_tcoords-1;
            v_tcoords[idx].x=(float)StringToDouble(str_parsed[1]);
            v_tcoords[idx].y=(float)StringToDouble(str_parsed[2]);
            //--- debug message
            if(show_debug)
               printf("tcoord %d:  %f   %f",idx,v_tcoords[idx].x,v_tcoords[idx].y);
            //---
            break;
           }
         //--- 3. "VN" lines are for the normal vectors, most of them are duplicated again
         //--- since it records them for every vertex in every triangle in the model, they are listed in NX, NY, NZ float format.
         case OBJ_DATA_VN:
           {
            total_normals++;
            if(total_items<4)
              {
               PrintFormat("obj data error at line=%d: %s",total_lines,str);
               FileClose(file_handle);
               return(false);
              }
            //---
            ArrayResize(v_normals,total_normals,total_normals+1000);
            int idx=total_normals-1;
            v_normals[idx].x=(float)StringToDouble(str_parsed[1]);
            v_normals[idx].y=(float)StringToDouble(str_parsed[2]);
            v_normals[idx].z=(float)StringToDouble(str_parsed[3]);
            v_normals[idx].w=0.0f;
            //--- debug message
            if(show_debug)
               printf("normal %d:  %f   %f   %f",idx,v_normals[idx].x,v_normals[idx].y,v_normals[idx].z);
            //---
            break;
           }
         //--- 4. "F" lines are for each face in the model.
         //--- the values listed are indexes into the vertices, texture coordinates, and normal vectors. The format of each face is:
         //--- f Vertex1/Texture1/Normal1 Vertex2/Texture2/Normal2 Vertex3/Texture3/Normal3
         //--- so a line that says "f 3/13/5 4/14/6 5/15/7" then translates to "Vertex3/Texture13/Normal5 Vertex4/Texture14/Normal6 Vertex5/Texture15/Normal7".
         case OBJ_DATA_F:
           {
            total_faces++;
            //--- check size
            if(total_items<4)
              {
               PrintFormat("obj data error at line=%d: %s",total_lines,str);
               FileClose(file_handle);
               return(false);
              }
            //---
            ArrayResize(faces,total_faces,total_faces+1000);
            int idx=total_faces-1;
            //--- clear all face indices
            for(uint i=0; i<4; i++)
              {
               faces[idx].v[0]=0;
               faces[idx].t[0]=0;
               faces[idx].n[0]=0;
              }
            //--- read indices
            faces[idx].total=MathMin(total_items-1,4);
            for(int i=0; i<faces[idx].total; i++)
              {
               int elements=StringSplit(str_parsed[i+1],'/',str_parsed_faces);
               if(elements>0)
                  faces[idx].v[i]=(int)StringToInteger(str_parsed_faces[0]);
               if(elements>1)
                  faces[idx].t[i]=(int)StringToInteger(str_parsed_faces[1]);
               if(elements>2)
                  faces[idx].n[i]=(int)StringToInteger(str_parsed_faces[2]);
              }
            //--- debug message
            if(show_debug)
               printf("%face d: %d vertex(%d,%d,%d,%d)  texture(%d,%d,%d,%d)  normal(%d,%d,%d,%d)",idx+1,
                      faces[idx].v[0],faces[idx].v[1],faces[idx].v[2],faces[idx].v[3],
                      faces[idx].t[0],faces[idx].t[1],faces[idx].t[2],faces[idx].t[3],
                      faces[idx].n[0],faces[idx].n[1],faces[idx].n[2],faces[idx].n[3]);
            //---
            break;
           }

         default:
           {
            break;
           }
        }
     }
//--- close the file
   FileClose(file_handle);
//---
   if(show_debug)
     {
      printf("File %s loaded successfully. Total lines: %d",filename,total_lines);
      printf("total v_positions=%d",total_positions);
      printf("total v_normals=%d",total_normals);
      printf("total v_tcoords=%d",total_tcoords);
      printf("total faces=%d",total_faces);
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Load static 3D models from Maya 2011                             |
//| https://en.wikibooks.org/wiki/DirectX/10.0/Direct3D/Loading_Maya |
//| TVertex must have                                                |
//| DXVector4 position, normal and DXVector2 tcoord members          |
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXLoadObjModel(const string filename,TVertex &vertices[],uint &indices[],float scale=1.0f)
  {
//--- intermediate data arrays
   DXVector4 v_positions[];
   DXVector2 v_tcoords[];
   DXVector4 v_normals[];
   OBJFaceType faces[];
//--- load data
   if(!DXLoadObjData(filename,v_positions,v_tcoords,v_normals,faces,false))
     {
      printf("Error loading model data from %s",filename);
      return(false);
     }
//---
   int total_v_positions=ArraySize(v_positions);
   int total_v_tcoords  =ArraySize(v_tcoords);
   int total_v_normals  =ArraySize(v_normals);
   int total_faces      =ArraySize(faces);
//--- check faces
   if(total_faces==0)
     {
      printf("No model data.");
      return(false);
     }
//--- check consistency of the indices
   int vertices_count=0;
   int indices_count =0;
   bool split_vertices=false;
   for(int i=0; i<total_faces; i++)
     {
      //--- each face have to be 3 or 4 sided
      if(faces[i].total<3 || faces[i].total>4)
        {
         printf("Error in %d face, face vertices count is %d",i,faces[i].total);
         return(false);
        }
      //---
      for(int j=0; j<faces[i].total; j++)
        {
         if(faces[i].v[j]<=0 || faces[i].v[j]>total_v_positions)
           {
            printf("Error in %d face, %d vertext index is %d. Total posiitons=%d",i,j,faces[i].v[j],total_v_positions);
            return(false);
           }
         if(total_v_tcoords>0)
           {
            if(faces[i].t[j]<=0 || faces[i].t[j]>total_v_tcoords)
              {
               printf("Error in %d face, %d tcoord index is %d. Total tcoords=%d",i,j,faces[i].v[j],total_v_positions);
               return(false);
              }
            if(faces[i].t[j]!=faces[i].v[j])
               split_vertices=true;
           }
         if(total_v_normals>0)
           {
            if(faces[i].n[j]<=0 || faces[i].n[j]>total_v_normals)
              {
               printf("Error in %d face, %d normal index is %d. Total normals=%d",i,j,faces[i].v[j],total_v_positions);
               return(false);
              }
            if(faces[i].n[j]!=faces[i].v[j])
               split_vertices=true;
           }
        }
      //--- calc counts
      vertices_count+=faces[i].total;
      if(faces[i].total<4)
         indices_count+=3;
      else
         indices_count+=6;
     }
   printf("Data consistency checked.");
//--- prepare arrays
   if(!split_vertices)
      vertices_count=total_v_positions;
   ArrayResize(vertices,vertices_count);
   ArrayResize(indices, indices_count);
   int v_idx=0,i_idx=0;
   for(int i=0; i<total_faces; i++)
     {
      for(int j=0; j<faces[i].total; j++)
        {
         if(!split_vertices)
            v_idx=faces[i].v[j]-1;
         //--- posiitons
         vertices[v_idx].position.x=v_positions[faces[i].v[j]-1].x*scale;
         vertices[v_idx].position.y=v_positions[faces[i].v[j]-1].y*scale;
         vertices[v_idx].position.z=v_positions[faces[i].v[j]-1].z*scale;
         vertices[v_idx].position.w=1.0f;
         //--- normal
         if(faces[i].n[j])
            DXVec4Normalize(vertices[v_idx].normal,v_normals[faces[i].n[j]-1]);
         //--- tcoord
         if(faces[i].t[j])
            vertices[v_idx].tcoord=v_tcoords[faces[i].t[j]-1];
         //--- indices
         indices[i_idx++]=v_idx;
         //--- increment indx
         if(split_vertices)
            v_idx++;
        }
      //--- the end of second triangle in 4-sided face
      if(faces[i].total==4)
        {
         indices[i_idx]=indices[i_idx-4];
         i_idx++;
         indices[i_idx]=indices[i_idx-3];
         i_idx++;
        }
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Box                                                              |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeBox(const DXVector3 &from,const DXVector3 &to,TVertex &vertices[],uint &indices[])
  {
//--- prepare arrays
   const int faces=6;
   if(ArrayResize(vertices,4*faces)!=4*faces)
      return(false);
//--- set indices
   uint ind[]= {0,1,2, 2,3,0, 4,5,6, 6,7,4, 8,9,10, 10,11,8, 12,13,14, 14,15,12, 16,17,18, 18,19,16, 20,21,22, 22,23,20};
   ArrayResize(indices,0);
   if(ArrayCopy(indices,ind)!=ArraySize(ind))
      return(false);
//--- prepare boundaries
   float left=from.x,right=to.x,bottom=from.y,top=to.y,near=from.z,far=to.z;
   if(from.x>to.x)
     {
      right=from.x;
      left= to.x;
     }
   if(from.y>to.y)
     {
      top=   from.y;
      bottom=to.y;
     }
   if(from.z>to.z)
     {
      far=from.z;
      near=to.z;
     }
//--- left face
   vertices[0].position=DXVector4(left,top,   far, 1.0);
   vertices[1].position=DXVector4(left,top,   near,1.0);
   vertices[2].position=DXVector4(left,bottom,near,1.0);
   vertices[3].position=DXVector4(left,bottom,far, 1.0);
   for(int i=0; i<4; i++)
      vertices[i].normal=DXVector4(-1.0,0.0,0.0,0.0);
//--- right face
   vertices[4].position=DXVector4(right,top,   near,1.0);
   vertices[5].position=DXVector4(right,top,   far, 1.0);
   vertices[6].position=DXVector4(right,bottom,far, 1.0);
   vertices[7].position=DXVector4(right,bottom,near,1.0);
   for(int i=4; i<8; i++)
      vertices[i].normal=DXVector4(1.0,0.0,0.0,0.0);
//--- front face
   vertices[8].position =DXVector4(left, top,   near,1.0);
   vertices[9].position =DXVector4(right,top,   near,1.0);
   vertices[10].position=DXVector4(right,bottom,near,1.0);
   vertices[11].position=DXVector4(left, bottom,near,1.0);
   for(int i=8; i<12; i++)
      vertices[i].normal=DXVector4(0.0,0.0,-1.0,0.0);
//--- back face
   vertices[12].position=DXVector4(right,top,   far,1.0);
   vertices[13].position=DXVector4(left, top,   far,1.0);
   vertices[14].position=DXVector4(left, bottom,far,1.0);
   vertices[15].position=DXVector4(right,bottom,far,1.0);
   for(int i=12; i<16; i++)
      vertices[i].normal=DXVector4(0.0,0.0,1.0,0.0);
//--- top face
   vertices[16].position=DXVector4(left, top,far, 1.0);
   vertices[17].position=DXVector4(right,top,far, 1.0);
   vertices[18].position=DXVector4(right,top,near,1.0);
   vertices[19].position=DXVector4(left, top,near,1.0);
   for(int i=16; i<20; i++)
      vertices[i].normal=DXVector4(0.0,1.0,0.0,0.0);
//--- bottom face
   vertices[20].position=DXVector4(left, bottom,near,1.0);
   vertices[21].position=DXVector4(right,bottom,near,1.0);
   vertices[22].position=DXVector4(right,bottom,far, 1.0);
   vertices[23].position=DXVector4(left, bottom,far, 1.0);
   for(int i=20; i<24; i++)
      vertices[i].normal=DXVector4(0.0,-1.0,0.0,0.0);
//--- texture coordinates
   for(int i=0; i<faces; i++)
     {
      vertices[i*4+0].tcoord=DXVector2(0.0f,0.0f);
      vertices[i*4+1].tcoord=DXVector2(1.0f,0.0f);
      vertices[i*4+2].tcoord=DXVector2(1.0f,1.0f);
      vertices[i*4+3].tcoord=DXVector2(0.0f,1.0f);
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Sphere                                                           |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeSphere(float radius,uint tessellation,TVertex &vertices[],uint &indices[])
  {
   if(tessellation<3)
      tessellation=3;
   uint segments_y =tessellation;
   uint segments_xz=tessellation*2;
//--- prepare arrays
   uint count=(segments_y+1)*(segments_xz+1);
   if(ArrayResize(vertices,count)!=count)
      return(false);
   count=6*segments_y*(segments_xz);
   if(ArrayResize(indices,count)!=count)
      return(false);
//--- create rings of vertices at progressively higher latitudes.
   for(uint i=0,idx=0; i<=segments_y; i++)
     {
      DXVector2 tcoord=DXVector2(0.0f,1.0f-(float)i/segments_y);
      float latitude=(i*DX_PI/segments_y)-DX_PI_DIV2;
      float dy =(float)sin(latitude);
      float dxz=(float)cos(latitude);
      //--- create a single ring of vertices at this latitude.
      for(uint j=0; j<=segments_xz; j++,idx++)
        {
         float longitude=(j%segments_xz)*DX_PI_MUL2/segments_xz;
         //--- normal
         DXVector3 normal=DXVector3((float)sin(longitude)*dxz,dy,(float)cos(longitude)*dxz);
         vertices[idx].normal  =DXVector4(normal.x,normal.y,normal.z,0.0);
         //--- position
         DXVec3Scale(normal,normal,radius);
         vertices[idx].position=DXVector4(normal.x,normal.y,normal.z,1.0);
         //--- texture coords
         tcoord.x = float(j)/segments_xz;
         vertices[idx].tcoord  =tcoord;
        }
     }
//--- fill the index buffer with triangles joining each pair of latitude rings.
   uint stride=segments_xz+1;
   uint idx=0;
   for(uint i=0; i<segments_y; i++)
     {
      for(uint j=0; j<segments_xz; j++)
        {
         uint next_i=i+1;
         uint next_j=(j+1)%stride;

         indices[idx++]=next_i*stride + next_j;
         indices[idx++]=next_i*stride + j;
         indices[idx++]=     i*stride + j;

         indices[idx++]=     i*stride + j;
         indices[idx++]=     i*stride + next_j;
         indices[idx++]=next_i*stride + next_j;
        }
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Torus                                                            |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeTorus(float outer_radius,float inner_radius,uint tessellation,TVertex &vertices[],uint &indices[])
  {
   if(tessellation<3)
      tessellation=3;
//--- prepare arrays
   uint count=(tessellation+1)*(tessellation+1);
   if(ArrayResize(vertices,count)!=count)
      return(false);
   count=6*tessellation*tessellation;
   if(ArrayResize(indices,count)!=count)
      return(false);
//---
   uint v=0,idx=0;
   uint stride=tessellation+1;
//--- first we loop around the main ring of the torus.
   for(uint i=0; i<=tessellation; i++)
     {
      DXVector2 tcoord=DXVector2(float(i)/tessellation,0.0f);
      //--- create a transform matrix that will align geometry to slice perpendicularly though the current ring position.
      DXMatrix rotation,transform;
      DXMatrixRotationY(rotation,-1.0f*(i%tessellation)*DX_PI_MUL2/tessellation-DX_PI_DIV2);
      DXMatrixTranslation(transform,outer_radius,0.0f,0.0f);
      DXMatrixMultiply(transform,transform,rotation);
      //--- now we loop along the other axis, around the side of the tube.
      for(uint j=0; j<=tessellation; j++)
        {
         //--- calc normal and position
         float angle=(j%tessellation)*DX_PI_MUL2/tessellation+DX_PI;
         vertices[v].normal=DXVector4((float)cos(angle),(float)sin(angle),0.0f,0.0f);
         vertices[v].position=DXVector4(vertices[v].normal.x*inner_radius,vertices[v].normal.y*inner_radius,0.0f,1.0f);
         DXVec4Transform(vertices[v].normal,  vertices[v].normal,  transform);
         DXVec4Transform(vertices[v].position,vertices[v].position,transform);
         //--- calc texture coord
         tcoord.y=1-float(j)/tessellation;
         vertices[v].tcoord=tcoord;
         v++;
         //--- create indices for two triangles.
         if(i<tessellation && j<tessellation)
           {
            uint next_i=i+1;
            uint next_j=j+1;

            indices[idx++]=next_i*stride + next_j;
            indices[idx++]=next_i*stride + j;
            indices[idx++]=     i*stride + j;

            indices[idx++]=     i*stride + j;
            indices[idx++]=     i*stride + next_j;
            indices[idx++]=next_i*stride + next_j;
           }
        }
     }
//---
   return(true);
  }
//+------------------------------------------------------------------+
//| Cylinder                                                         |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeCylinder(float radius,float height,uint tessellation,TVertex &vertices[],uint &indices[])
  {
   return(DXComputeTruncatedCone(radius,radius,height,tessellation,vertices,indices));
  }
//+------------------------------------------------------------------+
//| Truncated Cone                                                   |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeTruncatedCone(float radius_top,float radius_bottom,float height,uint tessellation,TVertex &vertices[],uint &indices[])
  {
   if(tessellation<3)
      tessellation=3;
//--- prepare arrays
   uint count=2*(tessellation+1)+2*tessellation;
   if(ArrayResize(vertices,count)!=count)
      return(false);
   count=6*tessellation+6*(tessellation-1);
   if(ArrayResize(indices,count)!=count)
      return(false);
//--- prepare normal
   DXVector2 normal=DXVector2(height,radius_bottom-radius_top);
   DXVec2Normalize(normal,normal);
   float dy=height/2.0f;
   uint v=0,idx=0;
   uint stride=2;
//--- create top and bottom rings of vertices
   for(uint i=0; i<=tessellation; i++)
     {
      float u=1.0f-(float)i/tessellation;
      float angle=(i*DX_PI_MUL2/tessellation);
      float dx=(float)sin(angle);
      float dz=(float)cos(angle);
      //---
      vertices[v].normal  =DXVector4(dx*normal.x,normal.y,dz*normal.x,0.0f);
      vertices[v].position=DXVector4(dx*radius_bottom,-dy,dz*radius_bottom,1.0f);
      vertices[v].tcoord  =DXVector2(u,1.0f);
      v++;
      vertices[v].normal  =vertices[v-1].normal;
      vertices[v].position=DXVector4(dx*radius_top,dy,dz*radius_top,1.0f);
      vertices[v].tcoord  =DXVector2(u,0.0f);
      v++;
      //--- creater side surface
      if(i<tessellation)
        {
         uint next_i=i+1;

         indices[idx++]=next_i*stride + 1;
         indices[idx++]=     i*stride + 0;
         indices[idx++]=next_i*stride + 0;


         indices[idx++]=     i*stride + 0;
         indices[idx++]=next_i*stride + 1;
         indices[idx++]=     i*stride + 1;
        }
     }
//--- first cap vertex
   uint cap_first=v;
//--- create caps
   for(uint i=0; i<tessellation; i++)
     {
      float angle=(i*DX_PI_MUL2/tessellation);
      float dx=(float)sin(angle);
      float dz=(float)cos(angle);

      vertices[v].normal  =DXVector4(0.0f,-1.0f,0.0f,0.0f);
      vertices[v].position=DXVector4(dx*radius_bottom,-dy,dz*radius_bottom,1.0f);
      vertices[v].tcoord  =DXVector2(0.5f+0.5f*dx,0.5f+0.5f*dz);
      v++;
      vertices[v].normal  =DXVector4(0.0f,1.0f,0.0f,0.0f);
      vertices[v].position=DXVector4(dx*radius_top,dy,dz*radius_top,1.0f);
      vertices[v].tcoord  =DXVector2(0.5f+0.5f*dx,0.5f-0.5f*dz);
      v++;
      //--- creater caps surface
      if(i>0 && i<tessellation-1)
        {
         uint next_i=i+1;

         indices[idx++]=cap_first;
         indices[idx++]=cap_first+next_i*stride;
         indices[idx++]=cap_first+     i*stride;

         indices[idx++]=cap_first+1;
         indices[idx++]=cap_first+     i*stride+1;
         indices[idx++]=cap_first+next_i*stride+1;
        }
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| Cone                                                             |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeCone(float radius,float height,uint tessellation,TVertex &vertices[],uint &indices[])
  {
   if(tessellation<3)
      tessellation=3;
//--- prepare arrays
   uint count=2*(tessellation+1)+tessellation;
   if(ArrayResize(vertices,count)!=count)
      return(false);
   count=3*tessellation+3*(tessellation-1);
   if(ArrayResize(indices,count)!=count)
      return(false);
//--- prepare normal
   DXVector2 normal=DXVector2(height,radius);
   DXVec2Normalize(normal,normal);
   float dy=height/2.0f;
   uint v=0,idx=0;
   uint stride=2;
//--- create top and bottom rings of vertices
   for(uint i=0; i<=tessellation; i++)
     {
      float u=1.0f-(float)i/tessellation;
      float angle=(i*DX_PI_MUL2/tessellation);
      float dx=(float)sin(angle);
      float dz=(float)cos(angle);
      //---
      vertices[v].normal  =DXVector4(dx*normal.x,normal.y,dz*normal.x,0.0f);
      vertices[v].position=DXVector4(dx*radius,-dy,dz*radius,1.0f);
      vertices[v].tcoord  =DXVector2(u,1.0f);
      v++;
      vertices[v].normal  =vertices[v-1].normal;
      vertices[v].position=DXVector4(0.0f,dy,0.0f,1.0f);
      vertices[v].tcoord  =DXVector2(u,0.0f);
      v++;
      //--- creater side surface
      if(i<tessellation)
        {
         uint next_i=i+1;

         indices[idx++]=next_i*stride + 1;
         indices[idx++]=     i*stride + 0;
         indices[idx++]=next_i*stride + 0;
        }
     }
//--- first cap vertex
   uint cap_first=v;
//--- create caps
   for(uint i=0; i<tessellation; i++)
     {
      float angle=(i*DX_PI_MUL2/tessellation);
      float dx=(float)sin(angle);
      float dz=(float)cos(angle);

      vertices[v].normal  =DXVector4(0.0f,-1.0f,0.0f,0.0f);
      vertices[v].position=DXVector4(dx*radius,-dy,dz*radius,1.0f);
      vertices[v].tcoord  =DXVector2(0.5f+0.5f*dx,0.5f+0.5f*dz);
      v++;
      //--- creater caps surface
      if(i>0 && i<tessellation-1)
        {
         indices[idx++]=cap_first;
         indices[idx++]=cap_first+i+1;
         indices[idx++]=cap_first+i;
        }
     }
   return(true);
  }
//+------------------------------------------------------------------+
//| Surface                                                          |
//| TVertex must have                                                |
//| DXVector4 position, DXVector4 normal and DXVector2 tcoord members|
//+------------------------------------------------------------------+
template <typename TVertex>
bool DXComputeSurface(double &data[],uint data_width,uint data_height,double data_range,
                      const DXVector3 &from,const DXVector3 &to,DXVector2 &texture_size,
                      bool two_sided,bool use_normals,
                      TVertex &vertices[],uint &indices[])
  {
//---
   if(data_width<2 || data_height<2)
      return(false);
//--- prepare arrays for vertices and triangles
   uint count=data_width*data_height*(two_sided?2:1);
   if(!ArrayResize(vertices,count))
      return(false);
   count=6*(data_width-1)*(data_height-1)*(two_sided?2:1);
   if(!ArrayResize(indices,count))
      return(false);
//--- find min and max value
   float min_value=+FLT_MAX;
   float max_value=-FLT_MAX;
   for(uint j=0; j<data_height; j++)
      for(uint i=0; i<data_width; i++)
        {
         float value=(float)data[j*data_width+i];
         if(min_value>value)
            min_value=value;
         if(max_value<value)
            max_value=value;
        }
//--- check and fix data range
   float avg_value=0.5f*(max_value+min_value);
   float range=(float)data_range;
   if(range<=0.0f)
      range=max_value-min_value;
   if(range<FLT_EPSILON)
      range=FLT_EPSILON;
//---
   min_value=avg_value-0.5f*range;
   max_value=avg_value+0.5f*range;
//---
   DXVector3 range_3d;
   DXVec3Subtract(range_3d,to,from);
   float step_x=range_3d.x/(data_width-1);
   float step_y=range_3d.y/range;
   float step_z=range_3d.z/(data_height-1);
//--- calculate vertices positions and colors
   for(uint j=0; j<data_height; j++)
     {
      for(uint i=0; i<data_width; i++)
        {
         uint idx=j*data_width+i;
         //--- calc value
         float value=(float)data[idx]-min_value;
         //--- calc position
         vertices[idx].position.x = from.x+i    *step_x;
         vertices[idx].position.y = from.y+value*step_y;
         vertices[idx].position.z = from.z+j    *step_z;
         vertices[idx].position.w = 1.0f;
         //--- set texture coordinates
         vertices[idx].tcoord=DXVector2(i*step_x/texture_size.x,j*step_z/texture_size.y);
        }
     }
//--- calculate normals
   if(use_normals)
     {
      for(uint j=0; j<data_height; j++)
        {
         for(uint i=0; i<data_width; i++)
           {
            DXVector4 v1,v2,normal;
            //--- v1
            if(i<=0)
               DXVec4Subtract(v1,vertices[j*data_width+i].position,vertices[j*data_width+i+1].position);
            else
               if(i>=data_width-1)
                  DXVec4Subtract(v1,vertices[j*data_width+i-1].position,vertices[j*data_width+i].position);
               else
                  DXVec4Subtract(v1,vertices[j*data_width+i-1].position,vertices[j*data_width+i+1].position);
            //--- v2
            if(j<=0)
               DXVec4Subtract(v2,vertices[j*data_width+i].position,vertices[(j+1)*data_width+i].position);
            else
               if(j>=data_height-1)
                  DXVec4Subtract(v2,vertices[(j-1)*data_width+i].position,vertices[j*data_width+i].position);
               else
                  DXVec4Subtract(v2,vertices[(j-1)*data_width+i].position,vertices[(j+1)*data_width+i].position);
            //--- normal
            DXVec4Cross(normal,v2,v1,DXVector4(0.0f,0.0f,0.0f,1.0f));
            float inv_len=(float)(1.0/sqrt(normal.x*normal.x+normal.y*normal.y+normal.z*normal.z));
            vertices[j*data_width+i].normal.x=normal.x*inv_len;
            vertices[j*data_width+i].normal.y=normal.y*inv_len;
            vertices[j*data_width+i].normal.z=normal.z*inv_len;
            vertices[j*data_width+i].normal.w=0.0f;
           }
        }
     }
   else
     {
      DXVector4 n=DXVector4(0.0f,0.0f,0.0f,0.0f);
      for(int i=0; i<ArraySize(vertices); i++)
         vertices[i].normal=n;
     }
//--- calculate triangles for every rectangle
   int n=0;
   for(uint i=0; i<data_width-1; i++)
     {
      for(uint j=0; j<data_height-1; j++)
        {
         //--- left triangle
         indices[n++]=j*data_width+(i+1);
         indices[n++]=j*data_width+i;
         indices[n++]=(j+1)*data_width+i;
         //--- right triangle
         indices[n++]=(j+1)*data_width+(i+1);
         indices[n++]=j*data_width+(i+1);
         indices[n++]=(j+1)*data_width+(i);
        }
     }
//--- generate back side
   if(two_sided)
     {
      uint offset=data_height*data_width;
      for(uint j=0; j<data_height; j++)
        {
         for(uint i=0; i<data_width; i++)
           {
            uint idx=offset+j*data_width+i;
            //--- copy vertices in backward direction
            vertices[idx]=vertices[j*data_width+data_width-i-1];
            //--- inverse normals
            DXVec4Scale(vertices[idx].normal,vertices[idx].normal,-1.0f);
           }
        }
      //--- calculate triangles for opposit side
      for(uint i=0; i<data_width-1; i++)
        {
         for(uint j=0; j<data_height-1; j++)
           {
            //--- left triangle
            indices[n++]=offset+j*data_width+(i+1);
            indices[n++]=offset+j*data_width+i;
            indices[n++]=offset+(j+1)*data_width+i;
            //--- right triangle
            indices[n++]=offset+(j+1)*data_width+(i+1);
            indices[n++]=offset+j*data_width+(i+1);
            indices[n++]=offset+(j+1)*data_width+(i);
           }
        }
     }
//---
   return(true);
  }
//+---------------------------------------------------------------------+
//| Computes Matlab jet color scheme colors on [0;1] range              |
//| dark blue > blue > light blue > green > yellow > red > dark red     |
//+---------------------------------------------------------------------+
void DXComputeColorJet(const float value,DXColor &cout)
  {
   float v=value*1.1f-0.05f;
   cout.r = fmin(fmax(v<0.75f ? 4*v-1.5f : 4.5f-4*v,0.0f),1.0f);
   cout.g = fmin(fmax(v<0.5f  ? 4*v-0.5f : 3.5f-4*v,0.0f),1.0f);
   cout.b = fmin(fmax(v<0.25f ? 4*v+0.5f : 2.5f-4*v,0.0f),1.0f);
   cout.a = 1.0;
  }
//+---------------------------------------------------------------------+
//| Computes hot to cold color scheme colors on [0;1] range             |
//| blue > light blue > green > yellow > red                            |
//+---------------------------------------------------------------------+
void DXComputeColorColdToHot(const float value,DXColor &cout)
  {
   float v=2*value-1.0f;
   cout.r = fmin(fmax(2*v,0.0f),1.0f);
   cout.g = fmin(fmax(2.0f-2*fabs(v),0.0f),1.0f);
   cout.b = fmin(fmax(-2*v,0.0f),1.0f);
   cout.a = 1.0;
  }
//+---------------------------------------------------------------------+
//| Computes red to green color scheme colors on [0;1] range            |
//| red > yellow > dark green                                           |
//+---------------------------------------------------------------------+
void DXComputeColorRedToGreen(const float value,DXColor &cout)
  {
   if(value<=0.5)
     {
      cout.r=1.0f;
      cout.g=DXScalarLerp(0.01f,0.95f,fmin(fmax(2*value,0.0f),1.0f));
     }
   else
     {
      cout.r=DXScalarLerp(0.1f,1.0f, fmin(fmax(2.0f-2*value,0.0f),1.0f));
      cout.g=DXScalarLerp(0.6f,0.95f,fmin(fmax(2.0f-2*value,0.0f),1.0f));
     }
   cout.b=0.0f;
   cout.a=1.0f;
  }
//+------------------------------------------------------------------+
