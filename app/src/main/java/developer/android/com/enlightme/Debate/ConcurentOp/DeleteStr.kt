package developer.android.com.enlightme.Debate.ConcurentOp

import developer.android.com.enlightme.objects.DebateEntity
import kotlinx.serialization.Serializable

@Serializable
class DeleteStr (debateEntity: DebateEntity, start: Int, len: Int, target: String): Operation(debateEntity){
    // Delete a character or some character in the string
    var start: Int // starting position of the string to be deleted if start == -1, the operation is equivalent to the identity
    var len: Int   // length of the string to be deleted
    val target: String // title or description. This targets the string to be changed
    init{
        this.start = start
        this.len = len
        this.target = target
    }
    override fun perform(){
        when (target) {
            "title" -> {
                debateEntity.title = debateEntity.title.substring(0,start) +
                        debateEntity.title.substring(start+len)
            }
            "description" -> {
                debateEntity.description = debateEntity.description.substring(0,start) +
                        debateEntity.description.substring(start+len)
            }
            else -> {
                throw Exception("Target cannot be confirmed")
            }
        }
    }
    override fun forward(operation: Operation){
        if (operation is InsertStr){
            if (operation.start >= this.start){
                this.start = start + operation.strIns.length
            }
        }
        if (operation is DeleteStr){
            if (operation.start > this.start){
                this.start = start - operation.len
            }
            if(operation.start == this.start){
                this.start = 0
                this.len = 0
                // Equivalent to identity operation
            }
        }
    }
    override fun backward(operation: Operation): Operation{
        if(operation is InsertStr){
            if(operation.start < this.start){
                this.start = this.start - operation.strIns.length
                return operation
            }
            if(operation.start > this.start){
                operation.start = operation.start - this.len
                return operation
            }
            if(operation.start == this.start){
                this.start = 0
                this.len = 0
                operation.strIns = ""
                return operation
            }
        }
        if(operation is DeleteStr){
            if(operation.start <= this.start){
                this.start = this.start + operation.len
                return operation
            }
            if(operation.start > this.start){
                operation.start = operation.start - this.len
                return operation
            }
        }
        return operation
    }
    fun compare(op: DeleteStr): Boolean{
        return ((this.start == op.start) && (this.len == op.len))
    }
}