package nl.dobots.bluenetdfu;

import no.nordicsemi.android.dfu.DfuBaseService;
import android.app.Activity;

public class DfuService extends DfuBaseService {

	@Override
	protected Class<? extends Activity> getNotificationTarget() {
		return NotificationActivity.class;
	}

}
