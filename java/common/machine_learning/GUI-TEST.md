# GUI Test

WekaDeeplearning4j comes with an automated JUnit test suite, however, some bugs may only occur in the GUI version of
WEKA; because of this, it's important to test a range of cases in the GUI manually before confirming the package is
ready for release.

These scenarios can be tested either:

- Manually, as per the instructions, or;
- Automatically, by running the parenthesised `.sh` script from `weka-run-test-scripts/`. This folder also
  contains `run_all_tests.sh`, which will run all of these tests
  (you should still check the output to ensure parameters are applied correctly). Check out
  the [README](weka-run-test-scripts/README.md) for more info.

## Scenarios

### `Dl4jMlpClassifier`

The following are run with randomly generated data (using the default `Generate` window)

- [ ] (`Dl4jMlpClassifier_random_default.sh`) Run with all default parameters

The following are run with the `mnist-minimal` dataset loaded, with the `ImageInstanceIterator` using the following
settings:

- `directory of images` pointing to the `mnist-minimal/` image folder
- `desired width = 224`
- `desired height = 224`
- `desired number of channels = 3`

You may like to set `number of epochs` to something smaller and set `Test options` to
`Percentage split` to run these quicker.

- [ ] (`Dl4jMlpClassifier_mnist_default.sh`) Run model with all default parameters
- [ ] (`Dl4jMlpClassifier_mnist_extraLayer.sh`) Run model with an added `DenseLayer` with `nOut = 32`
- [ ] (`Dl4jMlpClassifier_mnist_AlexNet.sh`) Run model with `Dl4jAlexNet` as the zoo model
- [ ] (`Dl4jMlpClassifier_mnist_ResNet50.sh`) Run model with `Dl4jResNet50` as the zoo model
- [ ] (`Dl4jMlpClassifier_mnist_EffNetB2.sh`) Run model with `KerasEfficientNet` as the zoo model,
  with `EFFICIENTNET_B2` as the variation
    - Ensure that the model actually uses the variation, you should see a log message something like
      `...Using cached model at /home/rhys/.deeplearning4j/models/keras_efficientnet/KerasEfficientNetB2.zip...`

### `Dl4jMlpFilter`

The following are run with the `mnist-minimal` dataset loaded. You'll need to click `Undo` after each test to revert the
instances.

- [ ] (`Dl4jMlpFilter_mnist_default.sh`) Run with default filter settings (uses `Dl4jResNet50` as the model)
- [ ] (`Dl4jMlpFilter_mnist_extraLayer.sh`) Run with `Dl4jResNet50` as zoo model, set `Use default feature layer` to
  false, and add `res4a_branch2b` to the `feature extraction layers`
    - Ensure that the logging output contains something
      like `...Getting features from layers: [res4a_branch2b, flatten_1]`
      and that there are attributes from both feature layers (i.e., named `res4a_branch2b` and `flatten_1`)
- [ ] (`Dl4jMlpFilter_mnist_DenseNet.sh`) Run with `KerasDenseNet` as the zoo model
  and `Use default feature extraction layer` set to `True`
- [ ] (`Dl4jMlpFilter_mnist_ResNet101v2.sh`) Run with `KerasResNet` as the zoo model, variation set to `RESNET101V2`,
  and `Use default feature extraction layer` set to `True`
    - Ensure that `RESNET101V2` is actually used as the variation. You should see a logging message something like
      `...Using cached model at /home/rhys/.deeplearning4j/models/keras_resnet/KerasResNet101V2.zip`
- [ ] (`Dl4jMlpFilter_mnist_EffNetB1ExtraLayer.sh`) Run with `KerasEfficientNet` as the zoo model, variation set
  to `EFFICIENTNET_B1`,
  `Use default feature layer` to `False`, and add `block4c_expand_conv` to the `feature extraction layers`.
    - Again, ensure the variation is properly set, and the resulting attributes contain features from both layers.

The following are run using the `mnist_784` convolutional
dataset (`src/test/resources/nominal/mnist_784_train_minimal.arff`)

- [ ] (`Dl4jMlpFilter_mnist_LeNet.sh`) Run with `Dl4jLeNet` as the zoo model, `Use default feature layer` to `True`, and
  a default `ConvolutionInstanceIterator` as the `instance iterator`