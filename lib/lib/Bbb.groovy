package lib

import lib.Aaa
import lib.Ccc

class Bbb {
    Aaa aaa = new Aaa()
    Ccc ccc = new Ccc()

    void hello() {
        println("Hello from ${this.class.simpleName}")
    }
}
