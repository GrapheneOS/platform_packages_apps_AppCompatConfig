import com.android.server.os.AppCompatProtos.*
import com.android.server.os.AppCompatProtos.CompatChange.*
import com.google.protobuf.ByteString
import com.google.protobuf.ProtocolMessageEnum
import java.io.File

fun getUnsortedConfigs(): List<AppCompatConfig> {
    val l = mutableListOf<AppCompatConfig>()

    l += app("com.google.android.GoogleCamera", certs(
        "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
        "1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00",
    )) {
        minVersion = 65820000
        changes(DISABLE_HARDENED_MALLOC, DISABLE_MEMORY_TAGGING)
    }

    val mainGmsCerts = certs(
        "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053", // "bd32"
        "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83", // "38d1"
        "1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00", // "58e1"
    )

    // Google Services Framework
    l += app("com.google.android.gsf", mainGmsCerts) {
        minVersion = 34
    }

    // GmsCore ("Play services")
    l += app("com.google.android.gms", mainGmsCerts) {
        minVersion = 23_40_00000
        changes(
            ALLOW_MEMORY_DYN_CODE_EXEC, // not clear why it's used
            ALLOW_STORAGE_DYN_CODE_EXEC, // for Dynamite modules
            SUPPRESS_NATIVE_DEBUGGING_NOTIFICATION, // doesn't break when ptrace access is blocked
        )
    }

    // Play Store
    l += app("com.android.vending", mainGmsCerts) {
        minVersion = 8_38_1_0000
        changes(
            ALLOW_STORAGE_DYN_CODE_EXEC, // for GmsCore Dynamite modules
        )
    }

    return l
}

fun main() {
    val configs: AppCompatConfigs = sortConfigs(getUnsortedConfigs())

    val f = File("../app_compat_configs.pb")

    f.outputStream().use {
        configs.writeTo(it)
    }

    println("written configs to ${f.canonicalPath}")
}

fun sortConfigs(list: List<AppCompatConfig>) = AppCompatConfigs.newBuilder().run {
    addAllConfigs(list.sortedBy { it.packageSpec.pkgName })
    build()
}

fun compatConfig(block: CompatConfig.Builder.() -> Unit): CompatConfig {
    return CompatConfig.newBuilder().run {
        block(this)
        build()
    }
}

typealias CertSha256 = ByteArray

@OptIn(ExperimentalStdlibApi::class)
fun certs(vararg list: String): List<CertSha256> {
    require(list.isNotEmpty())
    return list.map {
        val bytes = it.hexToByteArray()
        check(bytes.size == 32) { "invalid cert digest: $it" }
        bytes
    }
}

fun app(name: String, certDigests: List<CertSha256>, config: CompatConfig.Builder.() -> Unit): AppCompatConfig {
    return app(name, certDigests, listOf(config))
}

fun app(name: String, certDigests: List<CertSha256>, vararg configs: CompatConfig.Builder.() -> Unit): AppCompatConfig {
    return app(name, certDigests, configs.toList())
}

fun app(name: String, certDigests: List<CertSha256>, configs: List<CompatConfig.Builder.() -> Unit>): AppCompatConfig {
    require(certDigests.isNotEmpty())

    val pkgSpec = PackageSpec.newBuilder().run {
        pkgName = name
        addAllCertsSha256(certDigests.map { ByteString.copyFrom(it) })
        build()
    }

    return AppCompatConfig.newBuilder().run {
        packageSpec = pkgSpec
        addAllConfigs(configs.map { compatConfig(it) }.toList())
        build()
    }
}

fun CompatConfig.Builder.changes(vararg list: CompatChange) {
    compatChanges = compatChanges or enumBits(list)
}

fun <T : ProtocolMessageEnum> enumBits(bits: Array<T>): Long {
    var v = 0L
    bits.forEach {
        v = v or (1 shl it.number).toLong()
    }
    return v
}
