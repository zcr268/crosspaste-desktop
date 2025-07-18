package com.crosspaste.utils

import com.crosspaste.db.sync.HostInfo

expect fun getNetUtils(): NetUtils

interface NetUtils {

    fun getHostInfoList(hostInfoFilter: HostInfoFilter): List<HostInfo>

    fun hostPreFixMatch(
        host1: String,
        host2: String,
        prefixLength: Short,
    ): Boolean

    fun getPreferredLocalIPAddress(): String?

    fun clearProviderCache()
}

interface HostInfoFilter {

    fun filter(hostInfo: HostInfo): Boolean
}

object NoFilter : HostInfoFilter {

    override fun filter(hostInfo: HostInfo): Boolean = true

    override fun equals(other: Any?): Boolean = other == NoFilter

    override fun hashCode(): Int = 0
}

class HostInfoFilterImpl(
    val hostAddress: String,
    val networkPrefixLength: Short,
) : HostInfoFilter {

    private val netUtils = getNetUtils()

    override fun filter(hostInfo: HostInfo): Boolean =
        networkPrefixLength == hostInfo.networkPrefixLength &&
            netUtils.hostPreFixMatch(hostAddress, hostInfo.hostAddress, networkPrefixLength)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostInfoFilterImpl) return false

        if (hostAddress != other.hostAddress) return false
        if (networkPrefixLength != other.networkPrefixLength) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hostAddress.hashCode()
        result = 31 * result + networkPrefixLength
        return result
    }
}
