package kg.dos2.taxi_client

class Trf {
    @kotlin.jvm.JvmField
    var id = 0
    @kotlin.jvm.JvmField
    var name = ""
    var tk_ord = 0
    @kotlin.jvm.JvmField
    var trf_km = 0
    @kotlin.jvm.JvmField
    var lnd = 0
    var tk_ord_fs = 0
    @kotlin.jvm.JvmField
    var wt = 0

    internal constructor() {}
    internal constructor(id: Int, name: String, tk_ord: Int, trf_km: Int, lnd: Int, tk_ord_fs: Int, wt: Int) {
        this.id = id
        this.name = name
        this.tk_ord = tk_ord
        this.trf_km = trf_km
        this.lnd = lnd
        this.tk_ord_fs = tk_ord_fs
        this.wt = wt
    }
}