import { Straightforward, middleware } from "straightforward"

async function proxy() {
    const sf = new Straightforward()
    await sf.listen(9191)
    console.log(`Proxy listening on http://localhost:9191`)
    sf.onRequest.use(async ({ req, res }, next) => {
        console.log(`http request: ${req.url}`)
        return next()
    })
    sf.onConnect.use(async ({ req }, next) => {
        console.log(`connect request: ${req.url}`)
        return next()
    })

    // sf.onRequest.use(middleware.auth({ user: "bob", pass: "alice" }))
    // sf.onConnect.use(middleware.auth({ user: "bob", pass: "alice" }))
    sf.onRequest.use(middleware.echo)
}

proxy()

