import org.opendaylight.plastic.implementation.BasicMorpher

class StressInMorpher extends BasicMorpher {
    StressInMorpher() {
        ignoreUnused('*')
    }
    void tweakValues(Map ins, Map outs) {
        outs.each { k,v ->
            outs[k] = ++v
        }
    }
}
