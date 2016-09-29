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

package android.net.wifi.hotspot2.pps;

import android.net.wifi.ParcelUtil;
import android.os.Parcelable;
import android.os.Parcel;
import android.text.TextUtils;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Class representing Credential subtree in the PerProviderSubscription (PPS)
 * Management Object (MO) tree.
 * For more info, refer to Hotspot 2.0 PPS MO defined in section 9.1 of the Hotspot 2.0
 * Release 2 Technical Specification.
 *
 * In addition to the fields in the Credential subtree, it will also maintain necessary
 * information for the private key and certificates associated with this credential.
 *
 * @hide
 */
public final class Credential implements Parcelable {
    /**
     * Supported EAP (Extensible Authentication Protocol) methods.
     * Refer to http://www.iana.org/assignments/eap-numbers/eap-numbers.xml#eap-numbers-4
     * for complete list of method definitions.
     */
    public static final int EAP_NONE = -1;
    public static final int EAP_TLS = 13;
    public static final int EAP_SIM = 18;
    public static final int EAP_TTLS = 21;
    public static final int EAP_AKA = 23;
    public static final int EAP_AKAPrim = 50;

    /**
     * Value indicating unset timestamp.
     */
    public static final long UNSET_TIMESTAMP = -1L;

    /**
     * Value indicating an unset vendor ID for EAP (Extensible Authentication Protocol).
     */
    public static final int EAP_VENDOR_ID_UNSET = -1;

    /**
     * Value indicating an unset vendor type for EAP (Extensible Authentication Protocol).
     */
    public static final int EAP_VENDOR_TYPE_UNSET = -1;

    /**
     * The time this credential is created. It is in the format of number
     * of milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    public long creationTimeInMs = UNSET_TIMESTAMP;

    /**
     * The time this credential will expired. It is in the format of number
     * of milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    public long expirationTimeInMs = UNSET_TIMESTAMP;

    /**
     * The realm associated with this credential.  It will be used to determine
     * if this credential can be used to authenticate with a given hotspot by
     * comparing the realm specified in that hotspot's ANQP element.
     */
    public String realm = null;

    /**
     * When set to true, the device should check AAA (Authentication, Authorization,
     * and Accounting) server's certificate during EAP (Extensible Authentication
     * Protocol) authentication.
     */
    public boolean checkAAAServerCertStatus = false;

    /**
     * Username-password based credential.
     * Contains the fields under PerProviderSubscription/Credential/UsernamePassword subtree.
     */
    public static final class UserCredential implements Parcelable {
        /**
         * Username of the credential. Maximum length is 63 octets.
         */
        public String username = null;

        /**
         * Base64-encoded password. Maximum length is 255 octets.
         */
        public String password = null;

        /**
         * Flag indicating if the password is machine managed.
         */
        public boolean machineManaged = false;

        /**
         * The name of the application should be used to generate the password.
         */
        public String softTokenApp = null;

        /**
         * Flag indicating if this credential is usable on other mobile devices as well.
         */
        public boolean ableToShare = false;

        /**
         * EAP (Extensible Authentication Protocol) method type.
         * Refer to http://www.iana.org/assignments/eap-numbers/eap-numbers.xml#eap-numbers-4
         * for valid values.
         */
        public int eapType = EAP_NONE;

        /**
         * Vendor-ID for an expanded EAP (Extensible Authentication Protocol) method.
         */
        public int eapVendorId = EAP_VENDOR_ID_UNSET;

        /**
         * Vendor-Type for an expanded EAP (Extensible Authentication Protocol) method.
         */
        public int eapVendorType = EAP_VENDOR_TYPE_UNSET;

        /**
         * EAP (Extensible Authentication Protocol) type for an inner EAP method.
         */
        public int innerEapType = EAP_NONE;

        /**
         * Vendor-ID for an inner expanded EAP (Extensible Authentication Protocol) method.
         */
        public int innerEapVendorId = EAP_VENDOR_ID_UNSET;

        /**
         * Vendor-Type for an inner expanded EAP (Extensible Authentication Protocol) method.
         */
        public int innerEapVendorType = EAP_VENDOR_TYPE_UNSET;

        /**
         * Non-EAP inner authentication method. Valid values are "PAP", "CHAP", "MS-CHAP",
         * and "MS-CHAP-V2".
         */
        public String nonEapInnerMethod = null;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(username);
            dest.writeString(password);
            dest.writeInt(machineManaged ? 1 : 0);
            dest.writeString(softTokenApp);
            dest.writeInt(ableToShare ? 1 : 0);
            dest.writeInt(eapType);
            dest.writeInt(eapVendorId);
            dest.writeInt(eapVendorType);
            dest.writeInt(innerEapType);
            dest.writeInt(innerEapVendorId);
            dest.writeInt(innerEapVendorType);
            dest.writeString(nonEapInnerMethod);
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            UserCredential that = (UserCredential) thatObject;
            return TextUtils.equals(username, that.username) &&
                    TextUtils.equals(password, that.password) &&
                    machineManaged == that.machineManaged &&
                    softTokenApp == that.softTokenApp &&
                    ableToShare == that.ableToShare &&
                    eapType == that.eapType &&
                    eapVendorId == that.eapVendorId &&
                    eapVendorType == that.eapVendorType &&
                    innerEapType == that.innerEapType &&
                    innerEapVendorId == that.innerEapVendorId &&
                    innerEapVendorType == that.innerEapVendorType &&
                    TextUtils.equals(nonEapInnerMethod, that.nonEapInnerMethod);
        }

        public static final Creator<UserCredential> CREATOR =
            new Creator<UserCredential>() {
                @Override
                public UserCredential createFromParcel(Parcel in) {
                    UserCredential userCredential = new UserCredential();
                    userCredential.username = in.readString();
                    userCredential.password = in.readString();
                    userCredential.machineManaged = in.readInt() != 0;
                    userCredential.softTokenApp = in.readString();
                    userCredential.ableToShare = in.readInt() != 0;
                    userCredential.eapType = in.readInt();
                    userCredential.eapVendorId = in.readInt();
                    userCredential.eapVendorType = in.readInt();
                    userCredential.innerEapType = in.readInt();
                    userCredential.innerEapVendorId = in.readInt();
                    userCredential.innerEapVendorType = in.readInt();
                    userCredential.nonEapInnerMethod = in.readString();
                    return userCredential;
                }

                @Override
                public UserCredential[] newArray(int size) {
                    return new UserCredential[size];
                }
            };
    }
    public UserCredential userCredential = null;

    /**
     * Certificate based credential.
     * Contains fields under PerProviderSubscription/Credential/DigitalCertificate subtree.
     */
    public static final class CertificateCredential implements Parcelable {
        /**
         * Certificate type. Valid values are "802.1ar" and "x509v3".
         */
        public String certType = null;

        /**
         * The SHA-256 fingerprint of the certificate.
         */
        public byte[] certSha256FingerPrint = null;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(certType);
            dest.writeByteArray(certSha256FingerPrint);
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            CertificateCredential that = (CertificateCredential) thatObject;
            return TextUtils.equals(certType, that.certType) &&
                    Arrays.equals(certSha256FingerPrint, that.certSha256FingerPrint);
        }

        public static final Creator<CertificateCredential> CREATOR =
            new Creator<CertificateCredential>() {
                @Override
                public CertificateCredential createFromParcel(Parcel in) {
                    CertificateCredential certCredential = new CertificateCredential();
                    certCredential.certType = in.readString();
                    certCredential.certSha256FingerPrint = in.createByteArray();
                    return certCredential;
                }

                @Override
                public CertificateCredential[] newArray(int size) {
                    return new CertificateCredential[size];
                }
            };
    }
    public CertificateCredential certCredential = null;

    /**
     * SIM (Subscriber Identify Module) based credential.
     * Contains fields under PerProviderSubscription/Credential/SIM subtree.
     */
    public static final class SimCredential implements Parcelable {
        /**
         * International Mobile device Subscriber Identity.
         */
        public String imsi = null;

        /**
         * EAP (Extensible Authentication Protocol) method type for using SIM credential.
         */
        public int eapType = EAP_NONE;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            SimCredential that = (SimCredential) thatObject;
            return TextUtils.equals(imsi, that.imsi) &&
                    eapType == that.eapType;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(imsi);
            dest.writeInt(eapType);
        }

        public static final Creator<SimCredential> CREATOR =
            new Creator<SimCredential>() {
                @Override
                public SimCredential createFromParcel(Parcel in) {
                    SimCredential simCredential = new SimCredential();
                    simCredential.imsi = in.readString();
                    simCredential.eapType = in.readInt();
                    return simCredential;
                }

                @Override
                public SimCredential[] newArray(int size) {
                    return new SimCredential[size];
                }
            };
    }
    public SimCredential simCredential = null;

    /**
     * CA (Certificate Authority) X509 certificate.
     */
    public X509Certificate caCertificate = null;

    /**
     * Client side X509 certificate chain.
     */
    public X509Certificate[] clientCertificateChain = null;

    /**
     * Client side private key.
     */
    public PrivateKey clientPrivateKey = null;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(creationTimeInMs);
        dest.writeLong(expirationTimeInMs);
        dest.writeString(realm);
        dest.writeInt(checkAAAServerCertStatus ? 1 : 0);
        dest.writeParcelable(userCredential, flags);
        dest.writeParcelable(certCredential, flags);
        dest.writeParcelable(simCredential, flags);
        ParcelUtil.writeCertificate(dest, caCertificate);
        ParcelUtil.writeCertificates(dest, clientCertificateChain);
        ParcelUtil.writePrivateKey(dest, clientPrivateKey);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        Credential that = (Credential) thatObject;
        return creationTimeInMs == that.creationTimeInMs &&
                expirationTimeInMs == that.expirationTimeInMs &&
                TextUtils.equals(realm, that.realm) &&
                checkAAAServerCertStatus == that.checkAAAServerCertStatus &&
                userCredential == null ? that.userCredential == null :
                    userCredential.equals(that.userCredential) &&
                certCredential == null ? that.certCredential == null :
                    certCredential.equals(that.certCredential) &&
                simCredential == null ? that.simCredential == null :
                    simCredential.equals(that.simCredential) &&
                isX509CertificateEquals(caCertificate, that.caCertificate) &&
                isX509CertificatesEquals(clientCertificateChain, that.clientCertificateChain) &&
                isPrivateKeyEquals(clientPrivateKey, that.clientPrivateKey);
    }

    public static final Creator<Credential> CREATOR =
        new Creator<Credential>() {
            @Override
            public Credential createFromParcel(Parcel in) {
                Credential credential = new Credential();
                credential.creationTimeInMs = in.readLong();
                credential.expirationTimeInMs = in.readLong();
                credential.realm = in.readString();
                credential.checkAAAServerCertStatus = in.readInt() != 0;
                credential.userCredential = in.readParcelable(null);
                credential.certCredential = in.readParcelable(null);
                credential.simCredential = in.readParcelable(null);
                credential.caCertificate = ParcelUtil.readCertificate(in);
                credential.clientCertificateChain = ParcelUtil.readCertificates(in);
                credential.clientPrivateKey = ParcelUtil.readPrivateKey(in);
                return credential;
            }

            @Override
            public Credential[] newArray(int size) {
                return new Credential[size];
            }
        };

    private static boolean isPrivateKeyEquals(PrivateKey key1, PrivateKey key2) {
        if (key1 == null && key2 == null) {
            return true;
        }

        /* Return false if only one of them is null */
        if (key1 == null || key2 == null) {
            return false;
        }

        return TextUtils.equals(key1.getAlgorithm(), key2.getAlgorithm()) &&
                Arrays.equals(key1.getEncoded(), key2.getEncoded());
    }

    private static boolean isX509CertificateEquals(X509Certificate cert1, X509Certificate cert2) {
        if (cert1 == null && cert2 == null) {
            return true;
        }

        /* Return false if only one of them is null */
        if (cert1 == null || cert2 == null) {
            return false;
        }

        boolean result = false;
        try {
            result = Arrays.equals(cert1.getEncoded(), cert2.getEncoded());
        } catch (CertificateEncodingException e) {
            /* empty, return false. */
        }
        return result;
    }

    private static boolean isX509CertificatesEquals(X509Certificate[] certs1,
                                                    X509Certificate[] certs2) {
        if (certs1 == null && certs2 == null) {
            return true;
        }

        /* Return false if only one of them is null */
        if (certs1 == null || certs2 == null) {
            return false;
        }

        if (certs1.length != certs2.length) {
            return false;
        }

        for (int i = 0; i < certs1.length; i++) {
            if (!isX509CertificateEquals(certs1[i], certs2[i])) {
                return false;
            }
        }

        return true;
    }
}
