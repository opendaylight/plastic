import org.opendaylight.plastic.implementation.SimpleClassifier

class Tutorial9Classifier extends SimpleClassifier {

    static final Map fingerPrints = [
            'user-login-event': 'tutorial9a-in',
            'interface-removed-alarm': 'tutorial9b-in'
    ]

    String classify(Object parsedPayload) {
        String fingerprint = parsedPayload.keySet()[0]
        return fingerPrints[fingerprint]
      }
}

