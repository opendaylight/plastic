class UniCreateEpnm21Morpher {

    void tweakValues(Map inMap, Map outMap) {
        def remap = [
                'nui-service-type': ["CARRIER-ETHERNET", "carrier-ethernet-vpn"],
                'nui-admin-status': ["UP", "true"],
                'nui-abc-data-profile-mode': ["FULL-DUPLEX", "FULL DUPLEX"]
        ]

        remap.each { k, v -> if (inMap[k] == v[0]) outMap[k] = v[1] }
    }
}