# Keras Model Downloading

## Instructions

Installation of the required libraries is easiest with Anaconda

- `conda create -n tf_downloading python=3.7`
- `conda activate tf_downloading`
- `conda install tensorflow=1.14.0`
- `conda install keras`
- `conda install efficientnet`

There are two main files, `keras_download.py` and `fix_efficientnet.py`

### `keras_download.py`

This file simply loops through all models defined in `models.py`, downloads the weights, and saves the `.h5` file.

Run this script first: `(tf_downloading) wekaDeeplearning4j $ python keras_download.py`

### `fix_efficientnet.py`

The **EfficientNet** model as supplied by the [Qubvel Implementation](https://github.com/qubvel/efficientnet) has two
problems that stop it working in DL4J:

1. Custom `FixedDropout` layer type. This is fixed by replacing all `FixedDropout` layers with a simple `Dropout`
   layer (which is supported in DL4J). It's unlikely this will cause any noticable harm to the model.
2. Mismatching activation shapes (i.e., multiplying [112, 112, 32] with [1, 1, 32]). This isn't a problem in Keras as it
   implicitly broadcasts the non-matching dimensions (e.g., [1, 1, 32] is broadcasted to [112, 112, 32]), however, due
   to the way DL4J parses the `Multiply` layer, it requires the two sets of activations to be of equal shape. This is
   fixed by inserting a broadcasting `Lambda` layer to the Keras model.

### Fixes within DL4J

Deeplearning4j cannot parse `Lambda` layers by itself; it needs a matching Java implementation to be specified which it
can then use. To load the broadcasting `Lambda` layers inserted into the **EfficientNet** model in DL4J,
the `CustomBroadcast` layer needs to be used. This `CustomBroadcast` layer simply needs to be intialized with the
resulting `width` the activations should be broadcasted to.

Note that this is done by the `KerasModelConverter` automatically. Once a DL4J `.zip` file has been created, no Lambda
layers need to be specified.

### Other Notes

`KerasModelConverter` uses a feature which isn't currently supported in DL4J - `InputType.setDefaultCNN2DFormat()`. When
parsing the **EfficientNet** models, some layers are created with the 'default' channel order. Previously this has
always been channels first (`CNN2DFormat.NCHW`), however, the **EfficientNet** models require the default to be
opposite (`CNN2DFormat.NHWC`). This functionality isn't currently supported in DL4J (as of 1.0.0-beta7), so a custom
.jar must be created.

#### Installing Custom Jar

To fix this, a fork of Deeplearning4J has been created: [link](https://github.com/basedrhys/wekaDeeplearning4j)

You need to clone this repository, and build a new `deeplearning4j-nn` jar file.

Then, copy this file into the place of the currently downloaded `deeplearning4j-nn` jar file (
e.g. `deeplearning4j-nn-1.0.0-beta7.jar`)
and rename this new jar to replace the old one.

Your IDE should now load the newly created sources (from the custom jar), and KerasModelConverter should be able to run.

Once Deeplearning4j merges the [PR](https://github.com/eclipse/deeplearning4j/pull/8996)
and puts out a new release (after 1.0.0-beta7), this custom jar process won't be necessary;
the `setDefaultCNN2DFormat()`
method will already be on the `InputType` class.