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

	Copyright 2014 (C) Scott Jackson
*/

package github.daneren2005.dsub.domain;

import android.os.Parcel;
import android.os.Parcelable;

import org.fourthline.cling.model.meta.Device;

/**
 * Created by Scott on 11/1/2014.
 */
public class DLNADevice implements Parcelable {
	public Device renderer;
	public String id;
	public String name;
	public String description;
	public int volume;
	public int volumeMax;

	public static final Parcelable.Creator<DLNADevice> CREATOR = new Parcelable.Creator<DLNADevice>() {
		public DLNADevice createFromParcel(Parcel in) {
			return new DLNADevice(in);
		}

		public DLNADevice[] newArray(int size) {
			return new DLNADevice[size];
		}
	};

	private DLNADevice(Parcel in) {
		id = in.readString();
		name = in.readString();
		description = in.readString();
		volume = in.readInt();
		volumeMax = in.readInt();
	}

	public DLNADevice(Device renderer, String id, String name, String description, int volume, int volumeMax) {
		this.renderer = renderer;
		this.id = id;
		this.name = name;
		this.description = description;
		this.volume = volume;
		this.volumeMax = volumeMax;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(name);
		dest.writeString(description);
		dest.writeInt(volume);
		dest.writeInt(volumeMax);
	}
}
