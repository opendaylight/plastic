import org.opendaylight.plastic.implementation.BasicMorpher

class LciOutputMorpher extends BasicMorpher {
    void tweakValues(Map inputs, Map outputs) {
        ignoreUnused("description")
    }
}