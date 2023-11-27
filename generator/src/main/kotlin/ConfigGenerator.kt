import com.android.server.os.AppCompatProtos.*
import com.android.server.os.AppCompatProtos.CompatChange.*
import com.google.protobuf.ByteString
import com.google.protobuf.ProtocolMessageEnum
import java.io.File

val configs: AppCompatConfigs = sortConfigs(

)

fun mainGmsCerts() = certs(
    "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053", // "bd32"
    "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83", // "38d1"
    "1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00", // "58e1"
)

fun main() {
    val f = File("../app_compat_configs.pb")

    f.outputStream().use {
        configs.writeTo(it)
    }

    println("written configs to ${f.canonicalPath}")
}

fun sortConfigs(vararg list: AppCompatConfig) = AppCompatConfigs.newBuilder().run {
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
