/**
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi.hotspot2;

import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSP;
import android.os.Parcelable;
import android.os.Parcel;

/**
 * Class representing Passpoint configuration.  This contains configurations specified in
 * PerProviderSubscription (PPS) Management Object (MO) tree.
 *
 * For more info, refer to Hotspot 2.0 PPS MO defined in section 9.1 of the Hotspot 2.0
 * Release 2 Technical Specification.
 *
 * Currently, only HomeSP and Credential subtrees are supported.
 *
 * @hide
 */
public final class PasspointConfiguration implements Parcelable {
    public HomeSP homeSp = null;
    public Credential credential = null;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(homeSp, flags);
        dest.writeParcelable(credential, flags);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        PasspointConfiguration that = (PasspointConfiguration) thatObject;
        return homeSp == null ? that.homeSp == null : homeSp.equals(that.homeSp) &&
                credential == null ? that.credential == null : credential.equals(that.credential);
    }

    public static final Creator<PasspointConfiguration> CREATOR =
        new Creator<PasspointConfiguration>() {
            @Override
            public PasspointConfiguration createFromParcel(Parcel in) {
                PasspointConfiguration config = new PasspointConfiguration();
                config.homeSp = in.readParcelable(null);
                config.credential = in.readParcelable(null);
                return config;
            }
            @Override
            public PasspointConfiguration[] newArray(int size) {
                return new PasspointConfiguration[size];
            }
        };
}
