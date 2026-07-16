package cloud.apposs.robot.harness.plugin.browser;

import cloud.apposs.robot.harness.util.Strings;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class UrlSafetyChecker {
    private static final Set<String> BLOCKED_HOSTNAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "localhost",
            "ip6-localhost",
            "metadata.google.internal",
            "metadata.aws.internal",
            "instance-data",
            "169.254.169.254",     // AWS / Azure / GCP IMDS
            "100.100.100.200",     // Alibaba Cloud IMDS
            "192.0.0.192",         // Azure IMDS alternative
            "0.0.0.0",
            "::1"
    )));

    public static void check(String url) {
        if (Strings.isBlank(url)) {
            throw new SecurityException("URL is required");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Malformed URL: " + url);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new SecurityException("Only http:// and https:// URLs are allowed (got: " + scheme + ")");
        }
        String host = uri.getHost();
        if (Strings.isBlank(host)) {
            throw new SecurityException("URL must have a host");
        }
        String hostname = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        if (BLOCKED_HOSTNAMES.contains(hostname.toLowerCase())) {
            throw new SecurityException("SSRF blocked: " + hostname + " is a restricted hostname");
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(hostname)) {
                if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress() || isMetadataIp(addr)) {
                    throw new SecurityException("SSRF blocked: " + hostname
                            + " resolves to restricted address " + addr.getHostAddress());
                }
            }
        } catch (SecurityException cause) {
            throw cause;
        } catch (Exception cause) {
            // DNS resolution failure — let the caller deal with it (browser will show its own error).
        }
    }

    private static boolean isMetadataIp(InetAddress addr) {
        String ip = addr.getHostAddress();
        return "169.254.169.254".equals(ip)
                || "100.100.100.200".equals(ip)
                || "192.0.0.192".equals(ip)
                || "fd00:ec2::254".equalsIgnoreCase(ip);
    }
}
