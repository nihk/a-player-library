object Modules {
    object Player {
        private const val prefix = ":player"
        const val core = "$prefix-core"
        const val common = "$prefix-common"
        const val exoplayer = "$prefix-exoplayer"
        const val mediaplayer = "$prefix-mediaplayer"
        const val test = "$prefix-test"
        object Ui {
            private const val prefix = "${Player.prefix}-ui"
            const val default = "$prefix-default"
            const val sve = "$prefix-sve"
            const val core = "$prefix-core"
            const val common = "$prefix-common"
        }
    }
}