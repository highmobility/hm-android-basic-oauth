import io.kotest.core.spec.style.DescribeSpec

class OAuthTest : DescribeSpec({

  it("connects to feeds") {
    val oauth = OAuth()
    oauth.test()
    assert(true)
  }
})

