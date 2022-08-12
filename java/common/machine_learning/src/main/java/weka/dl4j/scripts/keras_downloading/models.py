import efficientnet.keras as efn
import keras

# Commented out models cannot currently be loaded into DL4J 
# Look at the model zoo table in DEVELOPMENT.md for more information

MODELS = [
    (keras.applications.xception.Xception, 'Xception'),
    (keras.applications.vgg16.VGG16, 'VGG16'),
    (keras.applications.vgg19.VGG19, 'VGG19'),
    (keras.applications.resnet.ResNet50, 'ResNet50'),
    (keras.applications.resnet.ResNet101, 'ResNet101'),
    (keras.applications.resnet.ResNet152, 'ResNet152'),
    (keras.applications.resnet_v2.ResNet50V2, 'ResNet50V2'),
    (keras.applications.resnet_v2.ResNet101V2, 'ResNet101V2'),
    (keras.applications.resnet_v2.ResNet152V2, 'ResNet152V2'),
    (keras.applications.inception_v3.InceptionV3, 'InceptionV3'),
    # (keras.applications.inception_resnet_v2.InceptionResNetV2, 'InceptionResNetV2'),
    # (keras.applications.mobilenet.MobileNet, 'MobileNet'),
    # (keras.applications.mobilenet_v2.MobileNetV2, 'MobileNetV2'),
    (keras.applications.densenet.DenseNet121, 'DenseNet121'),
    (keras.applications.densenet.DenseNet169, 'DenseNet169'),
    (keras.applications.densenet.DenseNet201, 'DenseNet201'),
    (keras.applications.nasnet.NASNetLarge, 'NASNetLarge'),
    (keras.applications.nasnet.NASNetMobile, 'NASNetMobile'),
    (efn.EfficientNetB0, 'EfficientNetB0'),
    (efn.EfficientNetB1, 'EfficientNetB1'),
    (efn.EfficientNetB2, 'EfficientNetB2'),
    (efn.EfficientNetB3, 'EfficientNetB3'),
    (efn.EfficientNetB4, 'EfficientNetB4'),
    (efn.EfficientNetB5, 'EfficientNetB5'),
    (efn.EfficientNetB6, 'EfficientNetB6'),
    (efn.EfficientNetB7, 'EfficientNetB7'),
]

ONLY_EFFICIENTNET = [
    (efn.EfficientNetB0, 'EfficientNetB0'),
    (efn.EfficientNetB1, 'EfficientNetB1'),
    (efn.EfficientNetB2, 'EfficientNetB2'),
    (efn.EfficientNetB3, 'EfficientNetB3'),
    (efn.EfficientNetB4, 'EfficientNetB4'),
    (efn.EfficientNetB5, 'EfficientNetB5'),
    (efn.EfficientNetB6, 'EfficientNetB6'),
    (efn.EfficientNetB7, 'EfficientNetB7'),
]
