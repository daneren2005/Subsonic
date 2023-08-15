package github.daneren2005.dsub.view.compat;

import android.content.Context;
import android.os.Bundle;
import androidx.mediarouter.app.MediaRouteChooserDialog;
import androidx.mediarouter.app.MediaRouteChooserDialogFragment;

import github.daneren2005.dsub.util.ThemeUtil;

public class CustomMediaRouteChooserDialogFragment extends MediaRouteChooserDialogFragment {
	@Override
	public MediaRouteChooserDialog onCreateChooserDialog(Context context, Bundle savedInstanceState) {
		return new MediaRouteChooserDialog(context, ThemeUtil.getThemeRes(context));
	}
}
