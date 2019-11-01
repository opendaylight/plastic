import org.opendaylight.plastic.implementation.BasicMorpher


class Tutorial8Morpher extends BasicMorpher
{
    Tutorial8Morpher() {
        optionalInputs("MTU")
        ignoreUnusedOutputs("MTU")
    }

    void tweakParsed(Object input, Object output) {
        if (!isBound('MTU')) {
            output.interface[0].remove('mtu')
        }
        if (isEmpty(input.'ip-addresses-v4')) {
            output.interface[0].remove('vlan-interface-std:vlan')
        }
    }
}