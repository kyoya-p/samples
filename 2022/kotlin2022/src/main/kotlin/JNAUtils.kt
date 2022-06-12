import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.PointerType

fun <T : PointerType> ptrTypeToPtr(objects: Array<T>): Pointer {
    val memory = Memory(Native.POINTER_SIZE * objects.size.toLong())
    var offset = 0L
    for (obj in objects) {
        memory.setPointer(Native.POINTER_SIZE * offset, obj.pointer)
        offset++
    }
    return memory
}
