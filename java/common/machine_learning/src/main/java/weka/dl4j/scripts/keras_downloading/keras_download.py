from os import path, makedirs
from models import MODELS
from multiprocessing.pool import ThreadPool
from utils import *


def download_and_save_model(model_def):
    model_fn = model_def[0]
    model_name = model_def[1]

    # Download the model
    keras_model = model_fn()
    
    save_model(keras_model, model_name)

    return model_name

# Download
results = ThreadPool(2).imap_unordered(download_and_save_model, MODELS)

for name in results:
    print("\n", name, "saved\n")
