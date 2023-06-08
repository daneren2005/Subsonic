package github.daneren2005.dsub.view.compat;

import androidx.mediarouter.app.MediaRouteChooserDialogFragment;
import androidx.mediarouter.app.MediaRouteControllerDialogFragment;
import androidx.mediarouter.app.MediaRouteDialogFactory;

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
