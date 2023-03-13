package me.fishhawk.lisu.source.ehentai

import io.kotest.core.spec.style.DescribeSpec
import source.Source
import source.ehentai.Api

class ApiTest : DescribeSpec({
    describe("Source test: ehentai api") {
        val api = Api(false, Source.cookiesStorage, Source.client)
        it("#popular") { api.popular() }
//        it("#latest") { api.latest(0) }
        it("#getGallery") { api.getGallery("2237436", "b6b5a0d937") }
    }
})
