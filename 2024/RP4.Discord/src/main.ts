import express from 'express'
import { DiscordSDK } from '@discord/embedded-app-sdk';




const DISCORD_CLIENT_ID = process.env["DISCORD_CLIENT_ID"]!
// const DISCORD_CLIENT_SECERT = process.env["DISCORD_CLIENT_SECRET"]!

const discordSdk = new DiscordSDK(DISCORD_CLIENT_ID);

async function main() {
  try {
    console.log(`L1`);
    //    await discordSdk.ready();
    console.log(`L2`);
  } catch (e) {
    console.log(`Error:${e}`);
  } finally {
    console.log(`L3`);
  }

  const app = express();
  const port = 3000;
  app.use(express.static('dist'));
  app.listen(port, () => {
    console.log(`Start service on port ${port}`);
  });
}
main()