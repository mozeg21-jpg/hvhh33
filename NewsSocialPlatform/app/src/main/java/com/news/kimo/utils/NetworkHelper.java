package com.news.kimo.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utility class for checking network availability and connection type.
 */
public class NetworkHelper {

    /**
     * Listener interface for network state changes (API 24+).
     */
    public interface OnNetworkStateChangedListener {
        void onAvailable();
        void onLost();
    }

    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private OnNetworkStateChangedListener listener;

    /**
     * Creates a new NetworkHelper instance.
     *
     * @param context Application or Activity context
     */
    public NetworkHelper(@NonNull Context context) {
        this.connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Check if the device has any active network connection.
     *
     * @return true if network is available, false otherwise
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null
                    && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    /**
     * Check if the device is connected to WiFi.
     *
     * @return true if connected to WiFi, false otherwise
     */
    public boolean isWifiConnected() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected();
        }
    }

    /**
     * Get the current connection type as a human-readable string.
     *
     * @return Connection type string: "WiFi", "Cellular", "Ethernet", "VPN", "Unknown", or "None"
     */
    @NonNull
    public String getConnectionType() {
        if (connectivityManager == null) {
            return "None";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return "None";
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return "None";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Cellular";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return "VPN";
            }
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                switch (networkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        return "WiFi";
                    case ConnectivityManager.TYPE_MOBILE:
                        return "Cellular";
                    case ConnectivityManager.TYPE_ETHERNET:
                        return "Ethernet";
                    case ConnectivityManager.TYPE_VPN:
                        return "VPN";
                    default:
                        return "Unknown";
                }
            }
        }

        return "None";
    }

    /**
     * Register a network callback to listen for connectivity changes.
     * Requires API 24 (Android Nougat) or above.
     *
     * @param listener The listener to receive network state changes
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void registerNetworkCallback(@NonNull OnNetworkStateChangedListener listener) {
        this.listener = listener;
        if (connectivityManager == null) {
            return;
        }

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (NetworkHelper.this.listener != null) {
                    NetworkHelper.this.listener.onAvailable();
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (NetworkHelper.this.listener != null) {
                    NetworkHelper.this.listener.onLost();
                }
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    /**
     * Unregister the previously registered network callback.
     * Should be called in onPause() or onDestroy() to avoid memory leaks.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
                // Callback was not registered or already unregistered
            }
            networkCallback = null;
            listener = null;
        }
    }
}
