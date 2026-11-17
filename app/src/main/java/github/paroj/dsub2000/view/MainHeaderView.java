package github.paroj.dsub2000.view;

import android.content.Context;
import android.view.LayoutInflater;

import github.paroj.dsub2000.R;

/**
 * The controls above the lists on the overview page. MainAdapter binds them directly.
 */
public class MainHeaderView extends UpdateView<String> {
	public MainHeaderView(Context context) {
		super(context, false);
		LayoutInflater.from(context).inflate(R.layout.main_header, this, true);
	}

	@Override
	protected void setObjectImpl(String header) {}
}
