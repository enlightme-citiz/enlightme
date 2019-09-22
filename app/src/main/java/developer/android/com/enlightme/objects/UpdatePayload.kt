package developer.android.com.enlightme.objects

import kotlinx.serialization.Serializable

@Serializable
class UpdatePayload(hist_elt: HistElt, state_vector: MutableMap<String, Int>, path_to_root: MutableList<MutableList<Int>>){
    var hist_elt: HistElt
    var state_vector: MutableMap<String, Int>
    var path_to_root: MutableList<MutableList<Int>>
    init{
        this.hist_elt = hist_elt
        this.state_vector = state_vector
        this.path_to_root = path_to_root
    }
}
// Object that is send to the network and used to carry update informations (operation and state vector)