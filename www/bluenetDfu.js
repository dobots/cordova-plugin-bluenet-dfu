var bluenetDfuName = "BluenetDfuPlugin";
var bluenetDfu = {
	uploadFirmware : function(successCallback, errorCallback, params) {
		cordova.exec(successCallback, errorCallback, bluenetDfuName, "uploadFirmware", [params]);
	}
}
module.exports = bluenetDfu;
