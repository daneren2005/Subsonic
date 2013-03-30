package github.daneren2005.dsub.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.Util;

public class LibraryFunctionsFragment extends SubsonicTabFragment {
	protected void onShuffleRequested() {
		if(Util.isOffline(context)) {
			Intent intent = new Intent(context, DownloadActivity.class);
			intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
			Util.startActivityWithoutTransition(context, intent);
			return;
		}

		View dialogView = context.getLayoutInflater().inflate(R.layout.shuffle_dialog, null);
		final EditText startYearBox = (EditText)dialogView.findViewById(R.id.start_year);
		final EditText endYearBox = (EditText)dialogView.findViewById(R.id.end_year);
		final EditText genreBox = (EditText)dialogView.findViewById(R.id.genre);

		final SharedPreferences prefs = Util.getPreferences(context);
		final String oldStartYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, "");
		final String oldEndYear = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, "");
		final String oldGenre = prefs.getString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, "");

		startYearBox.setText(oldStartYear);
		endYearBox.setText(oldEndYear);
		genreBox.setText(oldGenre);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Shuffle By")
			.setView(dialogView)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					Intent intent = new Intent(context, DownloadActivity.class);
					intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
					String genre = genreBox.getText().toString();
					String startYear = startYearBox.getText().toString();
					String endYear = endYearBox.getText().toString();

					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_START_YEAR, startYear);
					editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_END_YEAR, endYear);
					editor.putString(Constants.PREFERENCES_KEY_SHUFFLE_GENRE, genre);
					editor.commit();

					Util.startActivityWithoutTransition(context, intent);
				}
			})
			.setNegativeButton("Cancel", null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
