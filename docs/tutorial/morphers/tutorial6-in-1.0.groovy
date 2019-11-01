
import org.opendaylight.plastic.implementation.BasicMorpher

class Tutorial6Morpher extends BasicMorpher
{
    static final Set ALLOWED_STATUS = [ 'UP', 'DOWN' ] as Set

    void tweakInputs(Map ins, Object payload) {
        if (!ALLOWED_STATUS.contains(ins['status'])) {
            abort("Found an unknown status: {}", ins['status']);
        }
    }
}
