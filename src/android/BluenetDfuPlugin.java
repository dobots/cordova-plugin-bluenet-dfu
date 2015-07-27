package nl.dobots.bluenetdfu;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BluenetDfuPlugin extends CordovaPlugin {

	private static final String TAG = BluenetDfuPlugin.class.getSimpleName();

	public static final String PROPERTY_ADDRESS = "address";
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_FILE_TYPE = "fileType";
	public static final String PROPERTY_FILE_PATH = "filePath";
	public static final String PROPERTY_FILE_URI = "fileUri";
	public static final String PROPERTY_INIT_FILE_PATH = "initFilePath";
	public static final String PROPERTY_INIT_FILE_URI = "initFileUri";
	public static final String PROPERTY_KEEP_BOND = "keepBond";

	// Action Name Strings
	private final String uploadFirmwareAction = "uploadFirmware";

	// Error Codes
	private final String errorUploadFirmware = "uploadFirmware";

	private final String KEY_STATUS = "status";
	private final String KEY_PROGRESS = "progress";
	private final String KEY_SPEED = "speed";
	private final String KEY_AVG_SPEED = "avg_speed";

	private CallbackContext _uploadFirmwareCallbackContext;
	private Context _context;

	// private boolean _uploadInProgress = false;

	private boolean _dfuRegistered = false;
	private BroadcastReceiver _dfuUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (_uploadFirmwareCallbackContext == null) return;

			String action = intent.getAction();

			if (action.equals(DfuService.BROADCAST_PROGRESS)) {
				// Log.i(TAG, ">>>>>>>>>>>>>> Progress: " + intent.getExtras().toString());

				int progress = intent.getIntExtra(DfuService.EXTRA_DATA, 0);

				switch(progress) {
				case DfuService.PROGRESS_COMPLETED: {
					updateStatus("completed", false);
					// _uploadInProgress = false;
					clearNotification();
					break;
				}
				case DfuService.PROGRESS_ABORTED: {
					updateStatus("aborted", true);
					// _uploadInProgress = false;
					clearNotification();
					break;
				}
				case DfuService.PROGRESS_CONNECTING: {
					updateStatus("connecting", false);
					break;
				}
				case DfuService.PROGRESS_DISCONNECTING: {
					updateStatus("disconnecting", false);
					break;
				}
				case DfuService.PROGRESS_ENABLING_DFU_MODE: {
					updateStatus("enabling dfu mode", false);
					break;
				}
				case DfuService.PROGRESS_STARTING: {
					updateStatus("starting", false);
					break;
				}
				case DfuService.PROGRESS_VALIDATING: {
					updateStatus("validating", false);
					break;
				}
				default:
					int currentPart = intent.getIntExtra(DfuService.EXTRA_PART_CURRENT, 1);
					int totalParts = intent.getIntExtra(DfuService.EXTRA_PARTS_TOTAL, 1);
					float speed = intent.getFloatExtra(DfuService.EXTRA_SPEED_B_PER_MS, 0.0F);
					float avg_speed = intent.getFloatExtra(DfuService.EXTRA_AVG_SPEED_B_PER_MS, 0.0F);

					if (totalParts == 1) {
						updateProgress(progress, speed, avg_speed);
					} else {
						updateProgress((int)(currentPart / totalParts * 100.0), speed, avg_speed);
					}
					return;
				}

			} else if (action.equals(DfuService.BROADCAST_ERROR)) {
				updateStatus("error: "  + intent.getExtras().toString(), true);
				// _uploadInProgress = false;
				clearNotification();
			}
		}
	};

	private void clearNotification() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// if this activity is still open and upload process was completed, cancel the notification
				final NotificationManager manager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
				manager.cancel(DfuService.NOTIFICATION_ID);
			}
		}, 200);
	}

	private void updateProgress(int progress, float speed, float avg_speed) {

		// Log.i(TAG, String.format("<<<<<<<<<<<<<<<<<<<< updateProgress: %d", progress));

		JSONObject returnObj = new JSONObject();
		addStatus(returnObj, "progress");
		try {
			returnObj.put(KEY_PROGRESS, progress);
			returnObj.put(KEY_SPEED, speed);
			returnObj.put(KEY_AVG_SPEED, avg_speed);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		PluginResult pluginResult;

		pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);

		pluginResult.setKeepCallback(true);
		_uploadFirmwareCallbackContext.sendPluginResult(pluginResult);
	}

	private void updateStatus(String status, boolean error) {

		// Log.i(TAG, "-------------------- updateStatus: " + status);

		JSONObject returnObj = new JSONObject();
		addStatus(returnObj, status);

		PluginResult pluginResult;

		if (error) {
			pluginResult = new PluginResult(PluginResult.Status.ERROR, returnObj);
		} else {
			pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
		}

		pluginResult.setKeepCallback(true);
		_uploadFirmwareCallbackContext.sendPluginResult(pluginResult);
	}

	private void addStatus(JSONObject json, String status) {
		try {
			json.put(KEY_STATUS, status);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}


	@Override
	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

		// Log.i(TAG, "incoming call: " + action);
		_context = this.cordova.getActivity().getApplicationContext();

		if (uploadFirmwareAction.equals(action)) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					uploadFirwmareAction(args, callbackContext);
				}
			});
			return true;
		}

		return false;

	}

	private void uploadFirwmareAction(JSONArray args, CallbackContext callbackContext) {

		if (!_dfuRegistered) {
			final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(_context);
			broadcastManager.registerReceiver(_dfuUpdateReceiver, makeDfuUpdateIntentFilter());

			_dfuRegistered = true;
		}

		JSONObject returnObj = new JSONObject();
		PluginResult pluginResult;

		// if (_uploadInProgress) {

		// 	try {
		// 		returnObj.put(KEY_STATUS, "already_uploading");

		// 		pluginResult = new PluginResult(PluginResult.Status.ERROR, returnObj);
		// 		pluginResult.setKeepCallback(true);
		// 		_uploadFirmwareCallbackContext.sendPluginResult(pluginResult);

		// 	} catch (JSONException e) {
		// 		// TODO Auto-generated catch block
		// 		e.printStackTrace();
		// 	}

		// 	return;
		// }

		// _uploadInProgress = true;
		_uploadFirmwareCallbackContext = callbackContext;

		JSONObject json;
		try {
			json = args.getJSONObject(0);

			Log.i(TAG, "json: " + json.toString());

			// extract data from json argument
			String address = json.getString(PROPERTY_ADDRESS);
			String name = json.getString(PROPERTY_NAME);

			// check if file type provided
			int fileType = DfuService.TYPE_APPLICATION;
			if (json.has(PROPERTY_FILE_TYPE)) {
				fileType = json.getInt(PROPERTY_FILE_TYPE);
			}

			// check if file path or Uri is provided
			boolean hasFilePath = true;
			String filePath;
			Uri fileStreamUri;
			if (json.has(PROPERTY_FILE_PATH)) {
				hasFilePath = true;
				filePath = json.getString(PROPERTY_FILE_PATH);
				fileStreamUri = null;
			} else if (json.has(PROPERTY_FILE_URI)) {
				hasFilePath = false;
				filePath = "";
				fileStreamUri = Uri.parse(json.getString(PROPERTY_FILE_URI));
			} else {
				// if none provided, exit with error
				returnObj.put(KEY_STATUS, "no file specified");
				pluginResult = new PluginResult(PluginResult.Status.ERROR, returnObj);
				pluginResult.setKeepCallback(true);
				_uploadFirmwareCallbackContext.sendPluginResult(pluginResult);
				return;
			}

			// check if init file (path or Uri) is provided
			boolean hasInitFile;
			boolean hasInitFilePath;
			String initFilePath = "";
			Uri initFileStreamUri = null;
			if (json.has(PROPERTY_INIT_FILE_PATH)) {
				hasInitFile = true;
				hasInitFilePath = true;
				initFilePath = json.getString(PROPERTY_INIT_FILE_PATH);
				initFileStreamUri = null;
			} else if (json.has(PROPERTY_INIT_FILE_URI)) {
				hasInitFile = true;
				hasInitFilePath = false;
				initFilePath = "";
				initFileStreamUri = Uri.parse(json.getString(PROPERTY_INIT_FILE_URI));
			} else {
				hasInitFile = false;
				hasInitFilePath = false;
			}

			boolean hasBond = false;
			boolean keepBond = false;
			if (hasBond = json.has(PROPERTY_KEEP_BOND)) {
				keepBond = json.getBoolean(PROPERTY_KEEP_BOND);
			}

			// create and add data to service intent
			final Intent service = new Intent(_context, DfuService.class);
			service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, address);
			service.putExtra(DfuService.EXTRA_DEVICE_NAME, name);
			service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE,
					fileType == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
			service.putExtra(DfuService.EXTRA_FILE_TYPE, fileType);
			if (hasFilePath) {
				service.putExtra(DfuService.EXTRA_FILE_PATH, filePath); // a path or URI must be provided.
			} else {
				service.putExtra(DfuService.EXTRA_FILE_URI, fileStreamUri);
			}
			// Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
			// In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
			if (hasInitFile) {
				if (hasInitFilePath) {
					service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, initFilePath);
				} else {
					service.putExtra(DfuService.EXTRA_INIT_FILE_URI, initFileStreamUri);
				}
			}
			if (hasBond) {
				service.putExtra(DfuService.EXTRA_KEEP_BOND, keepBond);
			}

			// start dfu service
			_context.startService(service);

		} catch (JSONException e) {
			e.printStackTrace();

			try {
				returnObj.put(KEY_STATUS, "failed");

				pluginResult = new PluginResult(PluginResult.Status.ERROR, returnObj);
				pluginResult.setKeepCallback(true);
				_uploadFirmwareCallbackContext.sendPluginResult(pluginResult);
			} catch (JSONException e1) {
			}
		}

	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);

//		Log.i(TAG, "------------- onResume");

		// We are using LocalBroadcastReceiver instead of a normal BroadcastReceiver for
		// optimization purposes
		if (!_dfuRegistered) {
			final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(_context);
			broadcastManager.registerReceiver(_dfuUpdateReceiver, makeDfuUpdateIntentFilter());

			_dfuRegistered = true;
		}
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);

//		Log.i(TAG, "------------- onPause");

		if (!_dfuRegistered) {
			final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(_context);
			broadcastManager.unregisterReceiver(_dfuUpdateReceiver);

			_dfuRegistered = false;
		}
	}

	private static IntentFilter makeDfuUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(DfuService.BROADCAST_PROGRESS);
		intentFilter.addAction(DfuService.BROADCAST_ERROR);
//		intentFilter.addAction(DfuService.BROADCAST_LOG);
		return intentFilter;
	}

}
