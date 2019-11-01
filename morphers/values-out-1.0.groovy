import org.opendaylight.plastic.implementation.BasicMorpher

class ValuesOutputMorpher extends BasicMorpher {
    void tweakValues(Map inputs, Map outputs) {
        ignoreUnusedOutputs("jkl")
    }
}