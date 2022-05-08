import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.factory.Nd4j

// 学習
fun main() {
    val model = MultiLayerNetwork(nnModel1)
    model.init()

//    val imageLoader = NativeImageLoader(28, 28)
//    val imageScaler = ImagePreProcessingScaler(0.0, 1.0)
//    val inPtn = Nd4j.create(60_000, 28 * 28)
//    val outPtn = Nd4j.create(60_000, 10)
//
//    data class TeachPtn(val file: File, val res: Int)
//
//    (0..9).flatMap { r -> File("build/dataset/mnist_png/training/$r/").listFiles().map { TeachPtn(it, r) } }
//        .forEachIndexed { i, e ->
//            println("$i: ${e.file}")
//            val img = imageLoader.asRowVector(e.file)
//            imageScaler.transform(img)
//            inPtn.putRow(i.toLong(), img)
//            outPtn.put(i, e.res, 1.0)
//        }
//    val teachPtnList = DataSet(inPtn, outPtn)

    val mnistTrain = MnistDataSetIterator(100, true, 3456)

    model.fit(mnistTrain)
    Nd4j.writeTxt(model.params(), "build/weight.json")
}