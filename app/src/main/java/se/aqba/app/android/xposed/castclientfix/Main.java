package se.aqba.app.android.xposed.castclientfix;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.res.TypedArray;
import android.media.MediaDrm;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

public class Main implements IXposedHookLoadPackage {
    private static final String TAG = "XposedCastClientFix";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(!loadPackageParam.packageName.equals("com.google.android.gms"))
            return;

        findAndHookMethod("javax.net.ssl.SSLContext", loadPackageParam.classLoader, "init", KeyManager[].class, TrustManager[].class, SecureRandom.class, mSSLInitOverride);
    }

    XC_MethodHook mSSLInitOverride = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            TrustManager[] trustManagers = (TrustManager[])param.args[1];
            if(trustManagers != null) {
                boolean isCast = false;
                for(TrustManager trustManager: trustManagers) {
                    if(trustManager.getClass().getName().startsWith("com.google.android.gms.cast")) {
                        isCast = true;
                        break;
                    }
                }

                if(isCast) {
                    Log.d(TAG, "Overriding cast Trust Manager");

                    CastTrustManager trustManager = new CastTrustManager();
                    TrustManager[] castTrustManagers = new TrustManager[1];
                    castTrustManagers[0] = trustManager;

                    param.args[1] = castTrustManagers;
                }
            }
        }
    };

    private class CastTrustManager implements X509TrustManager {
        public X509Certificate[] trusted = new X509Certificate[0];

        @Override
        public void checkClientTrusted(X509Certificate[] cert, String s) {
            Log.d(TAG, "checkClientTrusted");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            Log.d(TAG, "checkServerTrusted");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trusted;
        }
    };
}
