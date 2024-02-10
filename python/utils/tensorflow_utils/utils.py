# from tensorflow.contrib import keras
from tensorflow import keras


def save_model(file: str, model: keras.models.Model):
    model.save(file)


def load_model(file: str) -> keras.models.Model:
    import os

    if not os.path.exists(file):
        raise FileNotFoundError(rf"{file} not found to load model!")
    return keras.models.load_model(file)
