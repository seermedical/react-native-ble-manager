package it.innove;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.LocalName;
import com.neovisionaries.bluetooth.ble.advertising.UUIDs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdvertisingData {
    private String name;
    private UUID[] serviceUUIDs;

    public AdvertisingData(byte[] advertisingData) {
        List<ADStructure> results = ADPayloadParser.getInstance().parse(advertisingData);
        for (ADStructure structure: results) {
            switch (structure.getType()) {
                case 7:
                    if (structure instanceof UUIDs) {
                        serviceUUIDs = ((UUIDs) structure).getUUIDs();
                    }
                    break;
                case 9:
                    if (structure instanceof LocalName) {
                        name = ((LocalName) structure).getLocalName();
                    }
                    break;
            }
        }

    }

    public UUID[] getServiceUUIDs() {
        if (serviceUUIDs == null) {
            serviceUUIDs = new UUID[0];
        }

        return serviceUUIDs;
    }

    public String getName() {
        if (name == null) {
            name = "";
        }

        return name;
    }
}
