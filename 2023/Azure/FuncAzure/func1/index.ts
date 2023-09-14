import {  AzureFunction, Context,    HttpRequest,  HttpResponseSimple,} from "@azure/functions";

const httpTrigger: AzureFunction = async (  context: Context) => {
  const req:HttpRequest=context.req
  const cookies = req.headers.cookie;
  const cookieMap = cookies.split(";").map((e) => e.trim()).reduce((map, cookie) => {
    const [key, value] = cookie.split("=");
    map[key] = value
    return map
  }, {});

  if (req.query.tgOrigin) {
    const url = new URL(req.url)
    const location = `${url.origin}${url.pathname}`
    const tgOrigin = decodeURIComponent(req.query.tgOrigin)
    await context.log(`** Redirect to ${location}. cookie[tgOrigin]=${tgOrigin}`)
    const res: HttpResponseSimple = {
      status: 302,
      headers: { Location: location },
      cookies: [{ name: "tgOrigin", value: tgOrigin }],
    }
    context.res = res
  } else if (cookieMap["tgOrigin"]) {
    const tgOrigin = decodeURIComponent( (cookieMap["tgOrigin"]))
    await context.log(`** Reverse Proxy to ${tgOrigin}`);
    const tgPath = decodeURIComponent(context.req.query.tgPath as string | undefined)
    context.res = <HttpResponseSimple>{
      status: 200,
      body: `Reverse Proxy to ${tgOrigin}`,
    };
  } else {
    await context.log(`** Set the tgOrigin parameter to specify the target host.`);
    context.res = <HttpResponseSimple>{
      status: 200,
      body: `${req.url}?tgOrigin=<origin-url>`,
    };
  }
};
export default httpTrigger;
