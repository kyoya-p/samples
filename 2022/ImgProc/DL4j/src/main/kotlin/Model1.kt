import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.*
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions

val nnModel1 = NeuralNetConfiguration.Builder()
    .seed(7890)
    .l2(0.0005)
    .weightInit(WeightInit.XAVIER)
    .updater(Adam(1e-3))
    .list()
    .layer(ConvolutionLayer.Builder(5, 5).stride(1, 1).nOut(20).activation(Activation.IDENTITY).build())
    .layer(SubsamplingLayer.Builder(PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
    .layer(ConvolutionLayer.Builder(5, 5).stride(1, 1).nOut(50).activation(Activation.IDENTITY).build())
    .layer(SubsamplingLayer.Builder(PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
    .layer(DenseLayer.Builder().activation(Activation.RELU).nOut(500).build())
    .layer(OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nOut(10)
        .activation(Activation.SOFTMAX).build())
    .setInputType(InputType.convolutionalFlat(28, 28, 1))
    .build()
