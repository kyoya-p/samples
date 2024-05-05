import { DiscordSDK } from '@discord/embedded-app-sdk';
const DISCORD_CLIENT_ID = process.env["DISCORD_CLIENT_ID"]!
const DISCORD_CLIENT_SECERT = process.env["DISCORD_CLIENT_SECRET"]!
const discordSdk = new DiscordSDK(DISCORD_CLIENT_ID);

async function setup() {
  console.log(`L1`);

  // Wait for READY payload from the discord client
  await discordSdk.ready();

  console.log(`L2`);

  // Pop open the OAuth permission modal and request for access to scopes listed in scope array below
  const { code } = await discordSdk.commands.authorize({
    client_id: DISCORD_CLIENT_ID,
    response_type: 'code',
    state: '',
    prompt: 'none',
    scope: ['identify'],
  });

  console.log(`L3`);
  // Retrieve an access_token from your application's server
  const response = await fetch('/api/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      code,
    }),
  });

  console.log(`L4`);

  const { access_token } = await response.json();

  // Authenticate with Discord client (using the access_token)
  const auth = await discordSdk.commands.authenticate({
    access_token,
  });
  console.log(`[${auth}]`);
}

setup();