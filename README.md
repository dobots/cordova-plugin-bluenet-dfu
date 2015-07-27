# Cordova plugin for DFU upload (Android ONLY)

The plugin uses the Nordic Android DFU Library available [here](https://github.com/NordicSemiconductor/Android-DFU-Library) and provides an uploadFirmware function for Cordova.

The following functions are available:

1. `uploadFirmware(updateCallback, errorCallback, parameter)`
both callback functions return json objects
```
return: {
	'status': String, status, such as connecting, disconnecting, progress, completed, etc.
	'progress': Integer, percentage of current upload prograss, min 1, max 100, (only if status == progress)
	'speed': Float, upload speed in Mb/s (only if status == progress)
	'avg_speed': Float, average upload speed in Mb/s (only if status == progress)
}
```
the parameter object has to be a json with the following fields:
```
parameter: {
	'address': String, bluetooth address of the device (required)
	'name': String, name of the device (required)
	'filePath': String, absolut path of the file which should be uploaded (required*)
	'fileUri': String, absolut path as an Uri, has to be encoded with encodeUri() (required*)
	'fileType': Integer, type of uploaded file, see Android DFU Library for details (optional, default TYPE_APPLICATION)
	'initFilePath': String, absolut path of the init file (optional*, default null which means no init file used)
	'initFileUri': String, absolut path of the init file as an Uri, has to be encoded with encodeUri() (optional*)
	'keepBond': Boolean, see Android DFU Library for details (optional, default false)
}
```
fields with a * means they are mutually exclusive, with the path having the priority. E.g. only 1 of the filePath or fileUri
has to be provided. If both are provided, the Path is used.
Same for the init file which is optional and doesn't have to be provided.

## Update Nordic Library
1. Download nordic library from https://github.com/NordicSemiconductor/Android-DFU-Library
2. Create an Eclipse Android library project as described on https://github.com/NordicSemiconductor/Android-DFU-Library/tree/release/documentation#eclipse (ignore step 6)
3. Copy the content of the Eclipse Android Library to the folder src/android/LibraryProject
4. copy file src/android/FakeR.java to src/android/LibraryProject/src/no/nordicsemi/android
5. Update src/android/LibraryProject/src/no/nordicsemi/android/dfu/DfuBaseService.java:
	1. add import no.nordicsemi.android.FakeR;
	2. create field
		private FakeR fakeR;
	3. assign field in onCreate:
		fakeR = new FakeR(this);
	4. replace all occurrences of getString(R.id.string.xxx) by
		getString(fakeR.getId("string", "xxx"))
	5. replace all occurrences of getString(R.string.xxx, yyy)
		String.format(getString(fakeR.getId("string", "xxx")), yyy)
	6. replace all occurrences of BuildConfig.DEBUG by
		(Boolean)fakeR.getBuildConfigValue("DEBUG")

## Copyrights

The copyrights (2014-2015) for the code belongs to the team of Distributed Organisms B.V. and are provided under an noncontagious open-source license:

* Authors: Dominik Egger
* Date: 27. Jul. 2015
* License: LGPL v3+, Apache, or MIT, your choice
* Almende B.V., http://www.almende.com and DoBots B.V., http://www.dobots.nl
* Rotterdam, The Netherlands
