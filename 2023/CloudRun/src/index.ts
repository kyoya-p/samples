import Koa from 'koa'

const app = new Koa()
app.use(ctx => {  ctx.body = 'Hello Koa'})
app.listen(8080)
console.log("start server. port:8080")
