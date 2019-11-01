
import org.opendaylight.plastic.implementation.BasicMorpher

class Tutorial7Morpher extends BasicMorpher
{
    void tweakValues(Map ins, Map outs) {
        outs['node-urlencoded'] = urlEncode(ins['node'])
    }
}
