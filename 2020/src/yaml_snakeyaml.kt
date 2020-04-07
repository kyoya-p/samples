import org.yaml.snakeyaml.Yaml
import java.io.Reader


class Yaml_Snakeyaml : IYaml {
    val yaml = Yaml()
    override fun load(src: String): Object = yaml.load(yamlsrc)
    override fun <T> loadAs(src: String, type: Class<T>): T = yaml.loadAs(yamlsrc, type)
    override fun stringify(data: Object): String = yaml.dump(data)

    fun parse(src: Reader) = yaml.parse(src)
}
