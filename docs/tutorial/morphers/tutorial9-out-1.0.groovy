import org.opendaylight.plastic.implementation.BasicMorpher


class Tutorial9Morpher extends BasicMorpher
{
    void tweakValues(Map inputs, Object payload) {
        inputs['property0'] = payload.keySet()[0]
    }
}