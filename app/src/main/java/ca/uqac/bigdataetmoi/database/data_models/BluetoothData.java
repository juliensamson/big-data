package ca.uqac.bigdataetmoi.database.data_models;

import java.util.Date;

/**
 * Created by Raph on 21/11/2017.
 */

@SuppressWarnings("HardCodedStringLiteral")
public class BluetoothData implements Data
{
    public static final String DATA_ID = "bluetoothdata";

    private String deviceName;
    private String address;
    private Date mDate;

    public BluetoothData(Date date, String deviceName, String address) {

        this.mDate = date;
        this.deviceName = deviceName;
        this.address = address;
    }

    @Override
    public String getDataID() { return DATA_ID; }

    public String getDeviceName() {
        return deviceName;
    }

    public String getAddress() {
        return address;
    }
}