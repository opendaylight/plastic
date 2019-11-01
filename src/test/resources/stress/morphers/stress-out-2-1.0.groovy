import org.opendaylight.plastic.implementation.BasicMorpher

class StressOutMorpher extends BasicMorpher {
    void tweakValues(Map ins, Map outs) {
        outs.each { k,v ->
            outs[k] = v*10
        }
    }
}
