/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicFragmentActivity;
import github.daneren2005.dsub.util.Util;

/**
 * @author Sindre Mehus
 */
public class ErrorDialog {

    public ErrorDialog(Context context, int messageId, boolean finishActivityOnCancel) {
        this(context, context.getResources().getString(messageId), finishActivityOnCancel);
    }

    public ErrorDialog(final Context context, String message, final boolean finishActivityOnClose) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(R.string.error_label);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (finishActivityOnClose) {
                     restart(context);
                }
            }
        });
        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (finishActivityOnClose) {
                    restart(context);
                }
            }
        });

		try {
        	builder.create().show();
		} catch(Exception e) {
			// Don't care, just means no activity to attach to
		}
    }
    
	private void restart(Context context) {
		Intent intent = new Intent(context, SubsonicFragmentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Util.startActivityWithoutTransition(context, intent);
	}
}
