package github.daneren2005.dsub.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Scott on 11/1/2014.
 */
public class DLNADevice implements Parcelable {
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

	public DLNADevice(String id, String name, String description, int volume, int volumeMax) {
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
