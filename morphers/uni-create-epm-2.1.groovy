class UniCreateEpnm21Morpher {

    void tweakValues(Map inMap, Map outMap) {
        def remap = [
                'uni-service-type': ["CARRIER-ETHERNET", "carrier-ethernet-vpn"],
                'uni-admin-status': ["UP", "true"],
                'uni-ce-data-profile-mode': ["FULL-DUPLEX", "FULL DUPLEX"]
        ]

        remap.each { k, v -> if (inMap[k] == v[0]) outMap[k] = v[1] }
    }
}