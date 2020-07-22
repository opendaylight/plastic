import lib.Ccc
import org.opendaylight.plastic.implementation.BasicMorpher

class AaaBbbCccOutputMorpher extends BasicMorpher {
    Ccc ccc = new Ccc()
    void tweakValues(Map inputs, Map outputs) {
        ccc.hello()
    }
}