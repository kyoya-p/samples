import { AzureFunction, Context, HttpRequest } from "@azure/functions"
import express from "express"


const app = express();

app.get("/", async (req: HttpRequest, context: Context) => {
  // クエリパラメータを取得する
  const name = req.query.name || "";

  // 応答を返す
  return context.res.json({
    name,
  });
});

export default app;



const httpTriggerX: AzureFunction = async function (context: Context, req: HttpRequest): Promise<void> {
    context.log('HTTP trigger function processed a request.');
    const name = (req.query.name || (req.body && req.body.name));
    const responseMessage = name
        ? "Hello, " + name + ". This HTTP triggered function executed successfully."
        : "This HTTP triggered function executed successfully. Pass a name in the query string or in the request body for a personalized response.";

    context.res = {
        // status: 200, /* Defaults to 200 */
        body: responseMessage
    };

};

// export default httpTrigger;