package github.daneren2005.dsub.view.compat;

import android.support.v7.app.MediaRouteChooserDialogFragment;
import android.support.v7.app.MediaRouteControllerDialogFragment;
import android.support.v7.app.MediaRouteDialogFactory;

public class CustomMediaRouteDialogFactory extends MediaRouteDialogFactory {
	@Override
	public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
		return new CustomMediaRouteChooserDialogFragment();
	}

	@Override
	public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
		return new CustomMediaRouteControllerDialogFragment();
	}
}
