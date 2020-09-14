package com.aaronjwood.portauthority.network;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class Wireless extends Network {

    public static class NoWifiManagerException extends Exception {
    }

    public static class NoWifiInterfaceException extends Exception {

    }

    /**
     * Constructor to set the activity for context
     *
     * @param context The activity to use for context
     */
    public Wireless(Context context) {
        super(context);
    }

    /**
     * Gets the MAC address of the wireless interface.
     *
     * @return MAC address
     */
    @Override
    public String getMacAddress() throws UnknownHostException, SocketException, NoWifiManagerException, NoWifiInterfaceException {
        String address = getWifiInfo().getMacAddress(); //Won't work on Android 6+ https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id
        if (!"02:00:00:00:00:00".equals(address)) {
            return address;
        }

        //This should get us the device's MAC address on Android 6+
        NetworkInterface iface = NetworkInterface.getByInetAddress(getPrivateLanAddress());
        if (iface == null) {
            throw new NoWifiInterfaceException();
        }

        byte[] mac = iface.getHardwareAddress();

        StringBuilder buf = new StringBuilder();
        for (byte aMac : mac) {
            buf.append(String.format("%02x:", aMac));
        }

        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }

        return buf.toString();
    }

    /**
     * Gets the device's wireless address
     *
     * @return Wireless address
     */
    private InetAddress getPrivateLanAddress() throws UnknownHostException, NoWifiManagerException {
        String ipAddress = getPrivateLanIp(String.class);
        return InetAddress.getByName(ipAddress);
    }

    /**
     * Gets the signal strength of the wireless network that the device is connected to
     *
     * @return Signal strength
     */
    public int getSignalStrength() throws NoWifiManagerException {
        return getWifiInfo().getRssi();
    }

    /**
     * Gets the BSSID of the wireless network that the device is connected to
     *
     * @return BSSID
     */
    public String getBSSID() throws NoWifiManagerException {
        return getWifiInfo().getBSSID();
    }

    /**
     * Gets the SSID of the wireless network that the device is connected to
     *
     * @return SSID
     */
    public String getSSID() throws NoWifiManagerException {
        String ssid = getWifiInfo().getSSID();
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    /**
     * Gets the device's internal LAN IP address associated with the WiFi network
     *
     * @param type
     * @param <T>
     * @return Local WiFi network LAN IP address
     */
    public <T> T getPrivateLanIp(Class<T> type) throws UnknownHostException, NoWifiManagerException {
        int ip = getWifiInfo().getIpAddress();

        //Endianness can be a potential issue on some hardware
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ip = Integer.reverseBytes(ip);
        }

        byte[] ipByteArray = BigInteger.valueOf(ip).toByteArray();


        if (type.isInstance("")) {
            return type.cast(InetAddress.getByAddress(ipByteArray).getHostAddress());
        } else {
            return type.cast(new BigInteger(InetAddress.getByAddress(ipByteArray).getAddress()).intValue());
        }
    }

    /**
     * Gets the device's internal LAN IP address associated with the cellular network
     *
     * @return Local cellular network LAN IP address
     */
    public static String getPrivateCellIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en != null && en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            return "Unknown";
        }

        return "Unknown";
    }

    /**
     * Gets the current link speed of the wireless network that the device is connected to
     *
     * @return Wireless link speed
     */
    public int getLinkSpeed() throws NoWifiManagerException {
        return getWifiInfo().getLinkSpeed();
    }

    /**
     * Determines if WiFi is enabled on the device or not
     *
     * @return True if enabled, false if disabled
     */
    public boolean isEnabled() throws NoWifiManagerException {
        return getWifiManager().isWifiEnabled();
    }

    /**
     * Gets the Android WiFi manager in the context of the current activity
     *
     * @return WifiManager
     */
    private WifiManager getWifiManager() throws NoWifiManagerException {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager == null) {
            throw new NoWifiManagerException();
        }

        return manager;
    }

    /**
     * Gets the Android WiFi information in the context of the current activity
     *
     * @return WiFi information
     */
    private WifiInfo getWifiInfo() throws NoWifiManagerException {
        return getWifiManager().getConnectionInfo();
    }

}
