import {  AzureFunction, Context,  Cookie,  HttpRequest,  HttpResponseSimple,} from "@azure/functions";

const httpTrigger: AzureFunction = async (  context: Context) => {
  await context.log(`**** Start function`);
  const req:HttpRequest=context.req
  await context.log(`header.cookie=${req.headers.cookie}`);
  const cookies = req.headers.cookie;
  const cookieMap = cookies.split(";").map((e) => e.trim()).reduce((map, cookie) => {
    const [key, value] = cookie.split("=");
    map[key] = value;
    return map;
  }, {});

  if (req.query.tgOrigin) {
    await context.log(`** Redirect to ${req.query.tgOrigin}`)
    const url = new URL(req.url)
    const res: HttpResponseSimple = {
      status: 302,
      headers: { Location: `${url.origin}${url.pathname}` },
      cookies: [{ name: "tgOrigin", value: `${req.query.tgOrigin}` }],
    }
    context.res = res
  } else if (cookieMap["tgOrigin"]) {
    await context.log(`** Reverse Proxy to ${cookieMap["tgOrigin"]}`);
    const tgPath = context.req.query.tgPath as string | undefined;
    context.res = <HttpResponseSimple>{
      status: 200,
      body: `Reverse Proxy to ${cookieMap["tgOrigin"]}`,
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
