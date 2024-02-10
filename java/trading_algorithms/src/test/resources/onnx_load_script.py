#%%
import os.path

import numpy as np
import pandas as pd
import sklearn
base_path=rf"C:\Users\javif\Coding\market_making_fw\java\output_sample\script_test"
input_path = rf"{base_path}\input_df.csv"
if os.path.isfile(input_path):
    input_df=pd.read_csv(input_path,index_col=0)
else:
    input_np = np.random.random([10,5])
    input_df=pd.DataFrame(input_np)
    input_df.to_csv(rf"{base_path}\input_df.csv")
#%%
from sklearn.preprocessing import StandardScaler
model = StandardScaler()
model.fit(input_df.values)
output_df=pd.DataFrame(model.transform(input_df.values))
output_df.to_csv(rf"{base_path}\output_df.csv")
#%%
from trading_algorithms.trend_predictor.trend_predictor_utils import *
filepath=rf"{base_path}\script_normalizer.onnx"
save_normalizer(model,input_len=input_df.shape[1],filepath=filepath)

#%% nn
target_path = rf"{base_path}\target_df.csv"
if os.path.isfile(input_path):
    target_df=pd.read_csv(target_path,index_col=0)
else:
    target_np = np.random.random([10,1])
    target_df=pd.DataFrame(target_np)
    target_df=target_df>0.5
    target_df.to_csv(rf"{base_path}\target_df.csv")
#%%
from utils.tensorflow_utils.parameter_tuning_utils import *
tf_model = create_feedforward_model(10,22,1,1,0.01,0.0)
tf_model.fit(output_df.values,target_df.values,epochs=100,batch_size=1)
filepath=rf"{base_path}\script_nn.onnx"

save_keras_onnx_model(tf_model,filepath,10,1,opset_version=12)
#%%
input_values = output_df.iloc[0].values

keras_path=os.path.dirname(filepath)+rf"\tf_model"
model_loaded=tf.keras.models.load_model(keras_path)

output_values=model_loaded.predict(output_df.values)
#%%

#result=load_onnx_model(filepath,input_feed={'dense_3_input': output_df.values})
#%%
expected_output = tf_model.predict(output_df.values)
expected_output_df=pd.DataFrame(expected_output)
filepath=rf"{base_path}\nn_output.csv"
expected_output_df.to_csv(filepath)
