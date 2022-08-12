struct VSInput
  {
   float4 position : POSITION;
  };
  
struct PSInput
  {
   float4 position : SV_POSITION;
  };

PSInput VSMain(VSInput input)
  {
   PSInput output;
   
   output.position=(input.position);
   
   return(output);
  }