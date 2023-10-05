import Koa from 'koa'
const app = new Koa()

// response
app.use(ctx => {
  ctx.body = 'Hello Koa'
})

app.listen(8080)

console.log("start ws server port 8080.")
