package github.daneren2005.dsub.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import github.daneren2005.dsub.service.DownloadService;

public class A2dpIntentReceiver extends BroadcastReceiver {
	private static final String PLAYSTATUS_RESPONSE = "com.android.music.playstatusresponse";
	private String TAG = A2dpIntentReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "GOT INTENT " + intent);

		DownloadService downloadService = DownloadService.getInstance();

		if (downloadService != null){

			Intent avrcpIntent = new Intent(PLAYSTATUS_RESPONSE);

			avrcpIntent.putExtra("duration", (long) downloadService.getPlayerDuration());
			avrcpIntent.putExtra("position", (long) downloadService.getPlayerPosition());
			avrcpIntent.putExtra("ListSize", (long) downloadService.getSongs().size());

			switch (downloadService.getPlayerState()){
				case STARTED:
					avrcpIntent.putExtra("playing", true);
					break;
				case STOPPED:
					avrcpIntent.putExtra("playing", false);
					break;
				case PAUSED:
					avrcpIntent.putExtra("playing", false);
					break;
				case COMPLETED:
					avrcpIntent.putExtra("playing", false);
					break;
				default:
					return;
			}			

			context.sendBroadcast(avrcpIntent);
		}
	}
}