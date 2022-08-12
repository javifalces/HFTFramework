from keras.applications.resnet import ResNet50
from keras.models import load_model
from keras.models import Model
from keras.layers import Dropout, Lambda
import keras
import re
import efficientnet.keras as efn
from models import ONLY_EFFICIENTNET
from os import path, remove
from utils import save_model

def insert_layer_nonseq(model, layer_regex, insert_layer_factory,
                        insert_layer_name=None, position='after', set_name=True):
    
    # Auxiliary dictionary to describe the network graph
    network_dict = {'input_layers_of': {}, 'new_output_tensor_of': {}}

    # Set the input layers of each layer
    for layer in model.layers:
        for node in layer._outbound_nodes:
            layer_name = node.outbound_layer.name
            if layer_name not in network_dict['input_layers_of']:
                network_dict['input_layers_of'].update(
                        {layer_name: [layer.name]})
            else:
                network_dict['input_layers_of'][layer_name].append(layer.name)

    # Set the output tensor of the input layer
    network_dict['new_output_tensor_of'].update(
            {model.layers[0].name: model.input})
    
    broadcast_index = 0

    used_names = {}

    # Iterate over all layers after the input
    for i, layer in enumerate(model.layers):
        if i == 0:
            continue

        # Determine input tensors
        layer_input = [network_dict['new_output_tensor_of'][layer_aux] 
                for layer_aux in network_dict['input_layers_of'][layer.name]]
        if len(layer_input) == 1:
            layer_input = layer_input[0]

        # Insert layer if name matches the regular expression
        if re.match(layer_regex, layer.name):
            if position == 'replace':
                x = layer_input
            elif position == 'after':
                x = layer(layer_input)
            elif position == 'before':
                pass
            else:
                raise ValueError('position must be: before, after or replace')

            next_layer = model.layers[i + 1]        
            next_layer_shape = next_layer.get_output_shape_at(0)
            new_width = str(next_layer_shape[1])
            new_depth = str(next_layer_shape[3]) ## Assuming channels last

            new_name = "broadcast_w" + new_width + "_d" + new_depth

            if new_name in used_names:
                val = used_names[new_name] + 1
                used_names[new_name] = val
            else:
                used_names[new_name] = 1
                val = 1
            
            new_layer = insert_layer_factory(new_name + "_" + str(val), 
                                (int(new_width), int(new_width), int(new_depth)))

            if set_name:
                new_layer.name = '{}_{}'.format(layer.name, new_layer.name)

            x = new_layer(x)
            # print('New layer: {} Old layer: {} Type: {}'.format(new_layer.name,
            #                                                 layer.name, position))
            if position == 'before':
                x = layer(x)
        else:
            x = layer(layer_input)

        # Set new output tensor (the original one, or the one of the inserted layer)
        network_dict['new_output_tensor_of'].update({layer.name: x})

    return Model(inputs=model.inputs, outputs=x)

from keras import backend

def dropout_layer_factory(*args):
    return Dropout(rate=0.2, name='dropout')

def broadcast_layer_factory(new_name, shape=None):
    if shape is not None:
        return Lambda(lambda x: x, name=new_name, output_shape=shape)
    else:
        return Lambda(lambda x: x, name=new_name)

def remove_fixed_dropout(model, re_pattern=r'.*block\w\w_drop.*'):
    new_model = insert_layer_nonseq(model, re_pattern, dropout_layer_factory, position='replace')
    new_model.save('temp.h5')
    return load_model('temp.h5')


def fix_broadcast(model, re_pattern=r'.*block\w\w_se_expand.*'):
    new_model = insert_layer_nonseq(model, re_pattern, broadcast_layer_factory, set_name=False)
    new_model.save('temp.h5')
    return load_model('temp.h5')

for model_def in ONLY_EFFICIENTNET:
    model_fn = model_def[0]
    model_name = model_def[1]

    # Download the model
    raw_model = model_fn()

    no_fixed_dropout = remove_fixed_dropout(raw_model)
    broadcasted = fix_broadcast(no_fixed_dropout)

    save_model(broadcasted, model_name + 'Fixed')

    print("Finshed fixing " + model_name)

if path.exists("temp.h5"):
    remove("temp.h5")

