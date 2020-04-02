package it.innove;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.facebook.react.bridge.*;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

public class LegacyScanManager extends ScanManager {

	private ArrayList<String> scanServiceUUIDs = new ArrayList<>();

	public LegacyScanManager(ReactApplicationContext reactContext, BleManager bleManager) {
		super(reactContext, bleManager);
	}

	@Override
	public void stopScan(Callback callback) {
		// update scanSessionId to prevent stopping next scan by running timeout thread
		scanSessionId.incrementAndGet();

		getBluetoothAdapter().stopLeScan(mLeScanCallback);
		callback.invoke();
	}

	@Override
	public void scan(ReadableArray serviceUUIDs, final int scanSeconds, ReadableMap options, Callback callback) {
		scanServiceUUIDs.clear();
		if (serviceUUIDs.size() > 0) {
			for (int i = 0; i < serviceUUIDs.size(); i++) {
				this.scanServiceUUIDs.add(serviceUUIDs.getString(i));
			}
		}
		getBluetoothAdapter().startLeScan(mLeScanCallback);

		if (scanSeconds > 0) {
			Thread thread = new Thread() {
				private int currentScanSession = scanSessionId.incrementAndGet();

				@Override
				public void run() {

					try {
						Thread.sleep(scanSeconds * 1000);
					} catch (InterruptedException ignored) {
					}

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							BluetoothAdapter btAdapter = getBluetoothAdapter();
							// check current scan session was not stopped
							if (scanSessionId.intValue() == currentScanSession) {
								if (btAdapter.getState() == BluetoothAdapter.STATE_ON) {
									btAdapter.stopLeScan(mLeScanCallback);
								}
								WritableMap map = Arguments.createMap();
								bleManager.sendEvent("BleManagerStopScan", map);
							}
						}
					});

				}

			};
			thread.start();
		}
		callback.invoke();
	}

	private boolean peripheralMatchesCriteria(Peripheral peripheral) {
		if (scanServiceUUIDs.size() > 0) {
			AdvertisingData data = new AdvertisingData(peripheral.advertisingDataBytes);
			for (String uuid: scanServiceUUIDs) {
				for (UUID advertisedUUID: data.getServiceUUIDs()) {
					if (advertisedUUID.toString().equalsIgnoreCase(uuid)) {
						return true;
					}
				}
			}

			return false;
		}

		return true;
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
							 final byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.i(bleManager.LOG_TAG, "DiscoverPeripheral: " + device.getAddress() + " - name: " + device.getName());

					Peripheral peripheral = bleManager.getPeripheral(device);
					if (peripheral == null) {
						peripheral = new Peripheral(device, rssi, scanRecord, bleManager.getReactContext());
					} else {
						peripheral.updateData(scanRecord);
						peripheral.updateRssi(rssi);
					}

					if (!peripheralMatchesCriteria(peripheral)) {
						return;
					}

					bleManager.savePeripheral(peripheral);

					WritableMap map = peripheral.asWritableMap();
					bleManager.sendEvent("BleManagerDiscoverPeripheral", map);
				}
			});
		}
	};
}
