import java.io.StringReader

val yamlsrc = """
userList:
- id: 1
  name: user1
- id: 2
  name: user2
- id: 3
  name: user3
"""

val jsonsrc = """
"userList":[
  {"id":1,"name":"user1"}
  ,{"id":2,"name":"user2"}
  ,{"id":3,"name":null}
]    
"""

// データモデル定義 TODO:最上位Objectが配列のときはどうするのか
class UserList { //引数無しのconstructorが必要
    class User(var id: Int = 0, var name: String = "") //引数無しのconstructorが必要。遅延初期化のためvalは不可

    lateinit var userList: List<User> //遅延初期化のためlateinitが必要
}

fun main() {
    val yaml = Yaml_Snakeyaml()
    val obj = yaml.loadAs(jsonsrc, UserList::class.java) //UC
    // walkObject(obj)
    //println(obj.userList[2].id)

    yaml.parse(StringReader(jsonsrc)).forEach {
        println(it)
    }
}

fun walkObject(obj: Any?, ind: String = "") {
    if (obj == null) {
        println("${ind}className=Null")
    } else {
        val className = obj.javaClass.canonicalName
        println("${ind}className=$className")
        if (obj is Map<*, *>) {
            obj.forEach { k, v ->
                println("${ind} Key=$k: ")
                walkObject(v, ind + "  ")
            }
        } else if (obj is List<*>) {
            obj.forEachIndexed { i, e ->
                println("${ind} Index=$i: ")
                walkObject(e, ind + "  ")
            }
        } else {
            //TODO: UserのメンバはFieldではなくgetterで取得(UserListとの違いは?)
            obj.javaClass.fields.forEach {
                println("${ind} Field=${it.name}")
                walkObject(it.get(obj), ind + "  ")
            }
/*            obj.javaClass.methods.filter { it.name.startsWith("get") }.forEach {
                println("${ind} Field=${it.name}")
                val subObj = it.invoke(obj)
                walkObject(subObj, ind + "  ")
            }*/
        }
    }
}


interface IYaml {
    fun load(src: String): Object
    fun stringify(data: Object): String
    fun <T> loadAs(src: String, type: Class<T>): T
}
