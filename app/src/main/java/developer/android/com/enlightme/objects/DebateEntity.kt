package developer.android.com.enlightme.objects

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import developer.android.com.enlightme.Debate.ConcurentOp.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.util.*

@Serializable
class DebateEntity {
    var path_to_root = mutableListOf<MutableList<Int>>() // gives the path from the root to
    // reach this argument
    var title = ""
    var description = ""
    var side_1 = "Side 1"
    var side_2 = "Side 2"
    var side_1_entity = mutableListOf<DebateEntity>()
    var side_2_entity  = mutableListOf<DebateEntity>()
    // History of changes brought to the debate by each attendee. This history is shared between attendees to deal with
    // concurent editing.
    var hist_debate = HistDebate()
    var state_vector = mutableMapOf<String, Int>() //Vector that list the number of changes that
    // each attendee performed. See doc:
    // State Vector: Let N be the number of sites in the collaborative system. These sites are numbered from 1 to N.
    // On every site S, and for every object O, a vector Vs,o composed of N elements is defined where: Vs,o[j] =
    // number of operations generated by site j, received by the site S and executed by S on its copy of the object O.

    var updateWaitingList = listOf<UpdatePayload>()

    //Modification and update procedure are coded here. Two things should be considered:
    //1. History management
    //2. modifications brought to the debatEntity object
    // Each time the history management should be performed first and then changes should be applied
    // First case: An update arrive from outside
    // Check for concurency and change history if necessary to integrate the operation in the history
    // Apply the change
    // Second case: The user make a change and broadcast it.
    // Create an update element
    // Broadcaast it
    // Manage the update as if it where an update coming from someone else (case 1)
    // Function needed:
    //For History management
    // Separate history
    // Backward transpose
    // Integrate in the history
    //Network
    // Broadcast the update
    //Update management
    // Deal with a change from the user
    // Deal with a change coming from outside
    fun send_update(context: Context, listEndpointId:List<String>, histElt: HistElt){
        //Serializer registering of abstact class.
        // See https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md for
        // more informations
        val messageModule = SerializersModule {
            polymorphic(Operation::class) {
                InsertArg::class with InsertArg.serializer()
                DeleteArg::class with DeleteArg.serializer()
                InsertStr::class with InsertStr.serializer()
                DeleteStr::class with DeleteStr.serializer()
            }
        }
        val json = Json(context = messageModule)
        val update_payload = UpdatePayload(histElt, path_to_root)
        val pld = Payload.fromBytes(json.stringify(UpdatePayload.serializer(),
            update_payload).toByteArray(Charsets.UTF_8))
        Nearby.getConnectionsClient(context).sendPayload(listEndpointId, pld)
    }
    // Backward transpose
    fun backwardTranspose(j: Int){
        hist_debate.histEltList[j-1].operation = hist_debate.histEltList[j].operation.backward(hist_debate.histEltList[j-1].operation)
        //if (hist_debate.histEltList[j-1].id_author == hist_debate.histEltList[j].id_author){
        //    val idA = hist_debate.histEltList[j-1].id_author
        //    hist_debate.histEltList[j-1].state_vector[idA] = hist_debate.histEltList[j-1].state_vector[idA] ?: 0 + 1
        //    hist_debate.histEltList[j].state_vector[idA] = hist_debate.histEltList[j].state_vector[idA] ?: 1 - 1
        //}
        val svj = hist_debate.histEltList[j].state_vector
        hist_debate.histEltList[j].state_vector = hist_debate.histEltList[j-1].state_vector
        hist_debate.histEltList[j-1].state_vector = svj

        val idaj = hist_debate.histEltList[j].id_author
        hist_debate.histEltList[j].id_author = hist_debate.histEltList[j-1].id_author
        hist_debate.histEltList[j-1].id_author = idaj

        val opTp = hist_debate.histEltList[j-1]
        hist_debate.histEltList[j-1] = hist_debate.histEltList[j]
        hist_debate.histEltList[j] = opTp
    }
    // Separate history
    fun separateHist(histEltOp: HistElt): Int{
        var n1 = 0
        //TODO check for loop boundaries
        for (i in hist_debate.histEltList.indices){
            val svH = hist_debate.histEltList[i].state_vector
            if (svH[histEltOp.id_author] ?: 0 < histEltOp.state_vector[histEltOp.id_author] ?: 0){ // case lhs yields ?: -1
                // when no operation from this user has been recorded yet. So  opH preceeed histEltOp. We ensure that
                // by assigning -1
                // case rhs yields ?: 1 when histEltOp.id_author sends its first modification. Thus no preceding
                // operation exist. Thus we assign 0
                // opH precedes histEltOp.
                if (i>n1){
                    for (j in i..n1+1){
                        backwardTranspose(j)
                    }
                }
                n1 += 1
            }
        }
        return n1
    }

    fun updateShouldWait(stateVectorOp: MutableMap<String, Int>): Boolean{
        for ((svOpK, svOp) in stateVectorOp){
            if ((state_vector[svOpK] ?: svOp-1) < svOp){
                return true
            }
        }
        return false
    }

    fun send_whole_debate(context: Context, endpointId: String){
        val json = Json(JsonConfiguration.Stable)
        var pld = Payload.fromBytes(json.stringify(serializer(),
            this).toByteArray(Charsets.UTF_8))
        Nearby.getConnectionsClient(context).sendPayload(endpointId, pld)
        pld = Payload.fromBytes(endpointId.toByteArray(Charsets.UTF_8))
        Nearby.getConnectionsClient(context).sendPayload(endpointId, pld)
    }
}