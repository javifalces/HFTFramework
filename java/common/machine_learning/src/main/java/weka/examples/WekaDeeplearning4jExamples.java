package weka.examples;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.Dl4jMlpClassifier;
import weka.core.Instances;
import weka.core.converters.ImageDirectoryLoader;
import weka.core.progress.ProgressManager;
import weka.dl4j.interpretability.AbstractCNNSaliencyMapWrapper;
import weka.dl4j.interpretability.WekaScoreCAM;
import weka.dl4j.iterators.instance.ImageInstanceIterator;
import weka.dl4j.inference.*;
import weka.dl4j.zoo.*;
import weka.dl4j.zoo.keras.EfficientNet;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Dl4jMlpFilter;

import java.io.File;
import java.io.FileReader;
import java.util.Random;

public class WekaDeeplearning4jExamples {

	public static void main(String[] args) throws Exception {
		inference();
	}

	public static void textProgressBarTest() throws InterruptedException {
		ProgressManager progressManager = new ProgressManager("Test indeterminate message");
		progressManager.start();

		Thread.sleep(100000);

		progressManager.finish();
	}

	public static void commandLineProgressTest() throws Exception {
		String folderPath = "datasets/nominal/mnist-minimal";
		Instances inst = new Instances(new FileReader("datasets/nominal/mnist.meta.tiny.arff"));
		inst.setClassIndex(1);

		Dl4jMlpFilter filter = new Dl4jMlpFilter();

		ImageInstanceIterator iterator = new ImageInstanceIterator();
		iterator.setTrainBatchSize(4);
		iterator.setImagesLocation(new File(folderPath));

		KerasEfficientNet kerasEfficientNet = new KerasEfficientNet();
		kerasEfficientNet.setVariation(EfficientNet.VARIATION.EFFICIENTNET_B1);
		filter.setZooModelType(kerasEfficientNet);

		filter.setUseDefaultFeatureLayer(false);
		filter.setTransformationLayerNames(new String[] { "top_dropout", "block7b_se_expand" });

		filter.setInstanceIterator(iterator);
		filter.setInputFormat(inst);

		Instances filteredInstances = Filter.useFilter(inst, filter);
		System.out.println(filteredInstances);
	}

	private static void filter() throws Exception {
		String folderPath = "src/test/resources/nominal/plant-seedlings-small";
		ImageDirectoryLoader loader = new ImageDirectoryLoader();
		loader.setInputDirectory(new File(folderPath));
		Instances inst = loader.getDataSet();
		inst.setClassIndex(1);

		Dl4jMlpFilter filter = new Dl4jMlpFilter();

		ImageInstanceIterator iterator = new ImageInstanceIterator();
		iterator.setImagesLocation(new File(folderPath));

		KerasEfficientNet kerasEfficientNet = new KerasEfficientNet();
		kerasEfficientNet.setVariation(EfficientNet.VARIATION.EFFICIENTNET_B1);
		filter.setZooModelType(kerasEfficientNet);

		filter.setUseDefaultFeatureLayer(false);
		filter.setTransformationLayerNames(new String[] { "top_dropout", "block7b_se_expand" });

		filter.setInstanceIterator(iterator);
		filter.setInputFormat(inst);

		Instances filteredInstances = Filter.useFilter(inst, filter);
		System.out.println(filteredInstances);
	}

	private static void train() throws Exception {
		String folderPath = "src/test/resources/nominal/plant-seedlings-small";
		ImageDirectoryLoader loader = new ImageDirectoryLoader();
		loader.setInputDirectory(new File(folderPath));
		Instances inst = loader.getDataSet();
		inst.setClassIndex(1);

		Dl4jMlpClassifier classifier = new Dl4jMlpClassifier();
		classifier.setNumEpochs(0);
		classifier.setLoadLayerSpecification(true);

		KerasDenseNet kerasEfficientNet = new KerasDenseNet();
		//        kerasEfficientNet.setVariation(ResNet.VARIATION.RESNET50V2);
		//        kerasEfficientNet.setVariation(EfficientNet.VARIATION.EFFICIENTNET_B1);
		classifier.setZooModel(kerasEfficientNet);

		ImageInstanceIterator iterator = new ImageInstanceIterator();
		iterator.setImagesLocation(new File(folderPath));

		classifier.setInstanceIterator(iterator);

		// Stratify and split the data
		Random rand = new Random(0);
		inst.randomize(rand);
		inst.stratify(5);
		Instances train = inst.trainCV(5, 0);
		Instances test = inst.testCV(5, 0);

		// Build the classifier on the training data
		classifier.buildClassifier(train);

		// Evaluate the model on test data
		Evaluation eval = new Evaluation(test);
		eval.evaluateModel(classifier, test);

		// Output some summary statistics
		System.out.println(eval.toSummaryString());
		System.out.println(eval.toMatrixString());
	}

	public static void inference() throws Exception {
		Dl4jCNNExplorer explorer = new Dl4jCNNExplorer();

		AbstractZooModel zooModel = new Dl4jResNet50();
		//        zooModel.setVariation(ResNet.VARIATION.RESNET101V2);
		explorer.setZooModelType(zooModel);

		//        CustomModelSetup modelSetup = new CustomModelSetup();
		//        modelSetup.setSerializedModelFile(new File("src/test/resources/models/custom_1x28x28_mnist_30e.model"));
		//        modelSetup.setInputChannels(1);
		//        modelSetup.setInputWidth(28);
		//        modelSetup.setInputHeight(28);
		//        explorer.setCustomModelSetup(modelSetup);
		//        explorer.setUseCustomModel(true);

		//        ModelOutputDecoder decoder = new ModelOutputDecoder();
		//        decoder.setBuiltInClassMap(ModelOutputDecoder.ClassmapType.CUSTOM);
		//        decoder.setClassMapFile(new File("datasets/nominal/mnist.meta.minimal.arff"));
		//        explorer.setModelOutputDecoder(decoder);

		// Dog ID = 222
		// Cat ID = 281
		AbstractCNNSaliencyMapWrapper wrapper = new WekaScoreCAM();
		wrapper.setBatchSize(8);
		explorer.setSaliencyMapWrapper(wrapper);
		explorer.setGenerateSaliencyMap(true);

		// Predict for dog
		explorer.init();
		//        explorer.makePrediction(new File("C:\\Users\\comptonr\\Desktop\\catAndDog.jpg"));
		explorer.processImage(new File("src/test/resources/images/dog.jpg"));
		System.out.println(explorer.getCurrentPredictions().toSummaryString());

		wrapper.setTargetClassIDs("-1");
		wrapper.setOutputFile(new File("output.png"));
		explorer.setSaliencyMapWrapper(wrapper);
		explorer.generateAndSaveOutputMap();
	}
}
